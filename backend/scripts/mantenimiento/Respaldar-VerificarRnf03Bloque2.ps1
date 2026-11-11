[CmdletBinding()]
param(
    [Parameter()]
    [string]$MySqlBinDirectory =
        'C:\laragon\bin\mysql\mysql-8.4.3-winx64\bin',

    [Parameter()]
    [string]$MySqlHost = '127.0.0.1',

    [Parameter()]
    [ValidateRange(1, 65535)]
    [int]$MySqlPort = 3306,

    [Parameter()]
    [string]$MySqlUser = 'root',

    [Parameter()]
    [string]$LoginPath,

    [Parameter()]
    [string]$BackupDirectory = (
        Join-Path (
            [Environment]::GetFolderPath('MyDocuments')
        ) 'EVOHKA_PRIVADO\respaldos'
    ),

    [Parameter()]
    [string]$VerificationDatabase
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$SourceDatabase = 'gestion_arriendos'
$ExpectedTables = @(
    'contratos',
    'mensajes',
    'notificaciones',
    'pagos',
    'propiedades',
    'reportes',
    'solicitudes',
    'usuarios'
)
$Timestamp = Get-Date -Format 'yyyyMMdd_HHmmss'

if ([string]::IsNullOrWhiteSpace($VerificationDatabase)) {
    $VerificationDatabase =
        "gestion_arriendos_verificacion_rnf03_b2_$Timestamp"
}

if (
    $VerificationDatabase -notmatch
        '^gestion_arriendos_verificacion_rnf03_b2_[0-9]{8}_[0-9]{6}$'
) {
    throw 'La base de verificación no cumple el nombre protegido RNF03 B2.'
}

if ($VerificationDatabase -eq $SourceDatabase) {
    throw 'La base de verificación nunca puede ser gestion_arriendos.'
}

$MySqlExe = Join-Path $MySqlBinDirectory 'mysql.exe'
$MySqlDumpExe = Join-Path $MySqlBinDirectory 'mysqldump.exe'

foreach ($executable in @($MySqlExe, $MySqlDumpExe)) {
    if (-not (Test-Path -LiteralPath $executable -PathType Leaf)) {
        throw "No se encontró la herramienta requerida: $executable"
    }
}

function New-ProcessInfo {
    param(
        [Parameter(Mandatory)]
        [string]$FilePath,

        [Parameter(Mandatory)]
        [string[]]$Arguments
    )

    $info = [System.Diagnostics.ProcessStartInfo]::new()
    $info.FileName = $FilePath
    $info.UseShellExecute = $false
    $info.CreateNoWindow = $true
    $info.RedirectStandardOutput = $true
    $info.RedirectStandardError = $true
    foreach ($argument in $Arguments) {
        $null = $info.ArgumentList.Add($argument)
    }
    return $info
}

function Get-ConnectionArguments {
    $arguments = [System.Collections.Generic.List[string]]::new()
    if ([string]::IsNullOrWhiteSpace($LoginPath)) {
        $arguments.Add('--no-defaults')
    } else {
        if ($LoginPath -notmatch '^[A-Za-z0-9_.-]+$') {
            throw 'El nombre de login-path contiene caracteres no permitidos.'
        }
        $arguments.Add("--login-path=$LoginPath")
    }
    $arguments.Add('--protocol=tcp')
    $arguments.Add("--host=$MySqlHost")
    $arguments.Add("--port=$MySqlPort")
    $arguments.Add("--user=$MySqlUser")
    $arguments.Add('--default-character-set=utf8mb4')
    return ,$arguments
}

function Invoke-CapturedProcess {
    param(
        [Parameter(Mandatory)]
        [string]$FilePath,

        [Parameter(Mandatory)]
        [string[]]$Arguments,

        [Parameter(Mandatory)]
        [string]$Stage
    )

    $process = [System.Diagnostics.Process]::new()
    $process.StartInfo = New-ProcessInfo $FilePath $Arguments
    $null = $process.Start()
    $outputTask = $process.StandardOutput.ReadToEndAsync()
    $errorTask = $process.StandardError.ReadToEndAsync()
    $process.WaitForExit()
    $output = $outputTask.GetAwaiter().GetResult()
    $errorOutput = $errorTask.GetAwaiter().GetResult()

    if ($process.ExitCode -ne 0) {
        $safeError = $errorOutput.Trim()
        throw "$Stage falló con código $($process.ExitCode). $safeError"
    }

    return $output
}

function Invoke-MySqlQuery {
    param(
        [Parameter(Mandatory)]
        [string]$Sql,

        [Parameter()]
        [string]$Stage = 'Consulta MySQL'
    )

    $arguments = Get-ConnectionArguments
    $arguments.Add('--batch')
    $arguments.Add('--skip-column-names')
    $arguments.Add('--raw')
    $arguments.Add("--execute=$Sql")
    return (
        Invoke-CapturedProcess $MySqlExe $arguments.ToArray() $Stage
    ).Trim()
}

function Invoke-Dump {
    param(
        [Parameter(Mandatory)]
        [string]$OutputPath
    )

    $arguments = Get-ConnectionArguments
    foreach ($argument in @(
        '--single-transaction',
        '--quick',
        '--skip-lock-tables',
        '--routines',
        '--triggers',
        '--events',
        '--hex-blob',
        '--no-tablespaces',
        '--set-gtid-purged=OFF',
        $SourceDatabase
    )) {
        $arguments.Add($argument)
    }

    $process = [System.Diagnostics.Process]::new()
    $process.StartInfo = New-ProcessInfo $MySqlDumpExe $arguments.ToArray()
    $null = $process.Start()
    $errorTask = $process.StandardError.ReadToEndAsync()

    try {
        $file = [System.IO.File]::Create($OutputPath)
        try {
            $process.StandardOutput.BaseStream.CopyTo($file)
        } finally {
            $file.Dispose()
        }
        $process.WaitForExit()
        $errorOutput = $errorTask.GetAwaiter().GetResult()
        if ($process.ExitCode -ne 0) {
            throw "mysqldump falló con código $($process.ExitCode). $($errorOutput.Trim())"
        }
    } catch {
        if (Test-Path -LiteralPath $OutputPath -PathType Leaf) {
            Remove-Item -LiteralPath $OutputPath -Force
        }
        throw
    }
}

function Invoke-Restore {
    param(
        [Parameter(Mandatory)]
        [string]$InputPath
    )

    $arguments = Get-ConnectionArguments
    $arguments.Add($VerificationDatabase)

    $info = New-ProcessInfo $MySqlExe $arguments.ToArray()
    $info.RedirectStandardInput = $true
    $process = [System.Diagnostics.Process]::new()
    $process.StartInfo = $info
    $null = $process.Start()
    $outputTask = $process.StandardOutput.ReadToEndAsync()
    $errorTask = $process.StandardError.ReadToEndAsync()

    $input = [System.IO.File]::OpenRead($InputPath)
    try {
        $input.CopyTo($process.StandardInput.BaseStream)
    } finally {
        $input.Dispose()
        $process.StandardInput.Close()
    }

    $process.WaitForExit()
    $null = $outputTask.GetAwaiter().GetResult()
    $errorOutput = $errorTask.GetAwaiter().GetResult()
    if ($process.ExitCode -ne 0) {
        throw (
            'La restauración falló. La base independiente se conserva ' +
            "para inspección. Detalle: $($errorOutput.Trim())"
        )
    }
}

function Protect-PrivateDirectory {
    param(
        [Parameter(Mandatory)]
        [string]$Path
    )

    if (-not (Test-Path -LiteralPath $Path -PathType Container)) {
        $null = New-Item -ItemType Directory -Path $Path
    }

    $identity = [System.Security.Principal.WindowsIdentity]::GetCurrent()
    $acl = [System.Security.AccessControl.DirectorySecurity]::new()
    $acl.SetOwner($identity.User)
    $acl.SetAccessRuleProtection($true, $false)

    $inheritance =
        [System.Security.AccessControl.InheritanceFlags]'ContainerInherit, ObjectInherit'
    $propagation = [System.Security.AccessControl.PropagationFlags]::None
    $allow = [System.Security.AccessControl.AccessControlType]::Allow
    $fullControl = [System.Security.AccessControl.FileSystemRights]::FullControl
    $readExecute =
        [System.Security.AccessControl.FileSystemRights]'ReadAndExecute, Synchronize'

    $rules = @(
        [System.Security.AccessControl.FileSystemAccessRule]::new(
            $identity.User,
            $fullControl,
            $inheritance,
            $propagation,
            $allow
        ),
        [System.Security.AccessControl.FileSystemAccessRule]::new(
            [System.Security.Principal.SecurityIdentifier]::new('S-1-5-18'),
            $fullControl,
            $inheritance,
            $propagation,
            $allow
        ),
        [System.Security.AccessControl.FileSystemAccessRule]::new(
            [System.Security.Principal.SecurityIdentifier]::new('S-1-5-32-544'),
            $readExecute,
            $inheritance,
            $propagation,
            $allow
        )
    )
    foreach ($rule in $rules) {
        $null = $acl.AddAccessRule($rule)
    }
    Set-Acl -LiteralPath $Path -AclObject $acl
}

function Get-Lines {
    param([string]$Text)
    if ([string]::IsNullOrWhiteSpace($Text)) {
        return @()
    }
    return @($Text -split "`r?`n" | Where-Object { $_ -ne '' })
}

function Get-BcryptSummary {
    param([string]$Database)

    $pattern = '^[$]2[aby][$][0-9]{2}[$][./A-Za-z0-9]{53}$'
    $marker = '{EVOHKA_BCRYPT_V1}'
    $sql = @"
SELECT
  COUNT(*) AS usuarios_total,
  COALESCE(SUM(contrasena_algoritmo = 'bcrypt'), 0) AS bcrypt_total,
  COALESCE(SUM(contrasena_algoritmo = 'legacy_plain'), 0) AS legacy_total,
  COALESCE(SUM(
    contrasena_algoritmo = 'bcrypt'
    AND REGEXP_LIKE(
      CONVERT(contrasena USING utf8mb4) COLLATE utf8mb4_bin,
      _utf8mb4'$pattern' COLLATE utf8mb4_bin,
      'c'
    )
  ), 0) AS bcrypt_validas,
  COALESCE(SUM(
    contrasena_algoritmo = 'bcrypt'
    AND NOT REGEXP_LIKE(
      CONVERT(contrasena USING utf8mb4) COLLATE utf8mb4_bin,
      _utf8mb4'$pattern' COLLATE utf8mb4_bin,
      'c'
    )
  ), 0) AS bcrypt_invalidas,
  COALESCE(SUM(
    contrasena_algoritmo IS NULL
    OR contrasena_algoritmo NOT IN ('bcrypt', 'legacy_plain')
  ), 0) AS algoritmos_desconocidos,
  COALESCE(SUM(
    LEFT(contrasena, CHAR_LENGTH('$marker')) = '$marker'
  ), 0) AS credenciales_con_marcador
FROM ``$Database``.``usuarios``;
"@
    $values = (Invoke-MySqlQuery $sql "Auditoría bcrypt de $Database") -split "`t"
    if ($values.Count -ne 7) {
        throw "La auditoría bcrypt de $Database devolvió un formato inesperado."
    }
    return [PSCustomObject]@{
        Usuarios = [int]$values[0]
        BcryptTotal = [int]$values[1]
        LegacyTotal = [int]$values[2]
        BcryptValidas = [int]$values[3]
        BcryptInvalidas = [int]$values[4]
        AlgoritmosDesconocidos = [int]$values[5]
        CredencialesConMarcador = [int]$values[6]
    }
}

function Assert-BcryptSummary {
    param(
        [Parameter(Mandatory)]
        [pscustomobject]$Summary,

        [Parameter(Mandatory)]
        [string]$Database
    )

    if (
        $Summary.Usuarios -ne 3 -or
        $Summary.BcryptTotal -ne 3 -or
        $Summary.LegacyTotal -ne 0 -or
        $Summary.BcryptValidas -ne 3 -or
        $Summary.BcryptInvalidas -ne 0 -or
        $Summary.AlgoritmosDesconocidos -ne 0 -or
        $Summary.CredencialesConMarcador -ne 0
    ) {
        throw "La auditoría agregada bcrypt no es válida en $Database."
    }
}

function Get-SchemaSignature {
    param([string]$Database)

    $sql = @"
SELECT CONCAT_WS('|', table_name, column_name, ordinal_position, column_type,
  is_nullable, COALESCE(column_default, '<NULL>'), extra,
  COALESCE(collation_name, '<NULL>'), column_key)
FROM information_schema.columns
WHERE table_schema = '$Database'
ORDER BY table_name, ordinal_position;
"@
    return Get-Lines (Invoke-MySqlQuery $sql "Estructura de $Database")
}

function Get-IndexSignature {
    param([string]$Database)

    $sql = @"
SELECT CONCAT_WS('|', table_name, index_name, seq_in_index,
  COALESCE(column_name, '<NULL>'), non_unique, index_type)
FROM information_schema.statistics
WHERE table_schema = '$Database'
ORDER BY table_name, index_name, seq_in_index;
"@
    return Get-Lines (Invoke-MySqlQuery $sql "Índices de $Database")
}

function Get-ForeignKeySignature {
    param([string]$Database)

    $sql = @"
SELECT CONCAT_WS('|', kcu.table_name, kcu.constraint_name, kcu.column_name,
  kcu.referenced_table_name, kcu.referenced_column_name,
  rc.update_rule, rc.delete_rule)
FROM information_schema.key_column_usage AS kcu
INNER JOIN information_schema.referential_constraints AS rc
  ON rc.constraint_schema = kcu.constraint_schema
 AND rc.constraint_name = kcu.constraint_name
 AND rc.table_name = kcu.table_name
WHERE kcu.table_schema = '$Database'
  AND kcu.referenced_table_name IS NOT NULL
ORDER BY kcu.table_name, kcu.constraint_name, kcu.ordinal_position;
"@
    return Get-Lines (Invoke-MySqlQuery $sql "Claves foráneas de $Database")
}

function Get-ExactRowCounts {
    param(
        [string]$Database,
        [string[]]$Tables
    )

    $queries = foreach ($table in $Tables) {
        "SELECT '$table', COUNT(*) FROM ``$Database``.``$table``"
    }
    $sql = ($queries -join ' UNION ALL ') + ';'
    $result = @{}
    foreach ($line in Get-Lines (Invoke-MySqlQuery $sql "Conteos de $Database")) {
        $parts = $line -split "`t"
        $result[$parts[0]] = [long]$parts[1]
    }
    return $result
}

function Assert-EqualSequence {
    param(
        [string[]]$Expected,
        [string[]]$Actual,
        [string]$Description
    )

    if (($Expected -join "`n") -cne ($Actual -join "`n")) {
        throw "La restauración no conserva $Description."
    }
}

$sourceExists = [int](Invoke-MySqlQuery (
    "SELECT COUNT(*) FROM information_schema.schemata " +
    "WHERE schema_name = '$SourceDatabase';"
) 'Comprobación de base origen')
if ($sourceExists -ne 1) {
    throw 'No existe la base origen gestion_arriendos.'
}

$destinationExists = [int](Invoke-MySqlQuery (
    "SELECT COUNT(*) FROM information_schema.schemata " +
    "WHERE schema_name = '$VerificationDatabase';"
) 'Comprobación de base destino')
if ($destinationExists -ne 0) {
    throw (
        "La base protegida $VerificationDatabase ya existe. " +
        'El procedimiento abortó sin borrarla ni sobrescribirla.'
    )
}

$sourceTables = Get-Lines (Invoke-MySqlQuery @"
SELECT table_name
FROM information_schema.tables
WHERE table_schema = '$SourceDatabase'
  AND table_type = 'BASE TABLE'
ORDER BY table_name;
"@ 'Inventario de tablas origen')
Assert-EqualSequence $ExpectedTables $sourceTables 'las ocho tablas oficiales'

$preMigrationState = (Invoke-MySqlQuery @"
SELECT
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = '$SourceDatabase'
     AND table_name = 'usuarios'
     AND column_name = 'sesion_version'),
  (SELECT COUNT(*) FROM information_schema.tables
   WHERE table_schema = '$SourceDatabase'
     AND table_name = 'recuperaciones_contrasena');
"@ 'Comprobación de estado previo a migración 003') -split "`t"
if (
    $preMigrationState.Count -ne 2 -or
    [int]$preMigrationState[0] -ne 0 -or
    [int]$preMigrationState[1] -ne 0
) {
    throw (
        'La base origen no está en el estado POST-BCRYPT/PRE-003 esperado. ' +
        'No se creó ni restauró ningún respaldo.'
    )
}

$sourceBcrypt = Get-BcryptSummary $SourceDatabase
Assert-BcryptSummary $sourceBcrypt $SourceDatabase
$sourceSchema = Get-SchemaSignature $SourceDatabase
$sourceIndexes = Get-IndexSignature $SourceDatabase
$sourceForeignKeys = Get-ForeignKeySignature $SourceDatabase
$sourceRows = Get-ExactRowCounts $SourceDatabase $ExpectedTables

Protect-PrivateDirectory $BackupDirectory
$BackupDirectory = (Resolve-Path -LiteralPath $BackupDirectory).Path
$backupPath = Join-Path $BackupDirectory "EVOHKA_POST_BCRYPT_$Timestamp.sql"
$partialPath = "$backupPath.partial"
$reportPath = Join-Path $BackupDirectory (
    "EVOHKA_POST_BCRYPT_${Timestamp}_verificacion.txt"
)

foreach ($path in @($backupPath, $partialPath, $reportPath)) {
    if (Test-Path -LiteralPath $path) {
        throw "El archivo privado ya existe y no será sobrescrito: $path"
    }
}

Invoke-Dump $partialPath

$unsafeDumpDirective = Select-String -LiteralPath $partialPath -Pattern (
    '^[\s]*(CREATE[\s]+DATABASE|USE[\s]+)'
)
if ($unsafeDumpDirective) {
    Remove-Item -LiteralPath $partialPath -Force
    throw 'El dump contiene una directiva de base insegura y no será restaurado.'
}

Move-Item -LiteralPath $partialPath -Destination $backupPath

Invoke-MySqlQuery (
    "CREATE DATABASE ``$VerificationDatabase`` " +
    'CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;'
) 'Creación de base independiente' | Out-Null

Invoke-Restore $backupPath

$destinationTables = Get-Lines (Invoke-MySqlQuery @"
SELECT table_name
FROM information_schema.tables
WHERE table_schema = '$VerificationDatabase'
  AND table_type = 'BASE TABLE'
ORDER BY table_name;
"@ 'Inventario de tablas restauradas')
Assert-EqualSequence $ExpectedTables $destinationTables 'las tablas restauradas'

$destinationBcrypt = Get-BcryptSummary $VerificationDatabase
Assert-BcryptSummary $destinationBcrypt $VerificationDatabase
$destinationSchema = Get-SchemaSignature $VerificationDatabase
$destinationIndexes = Get-IndexSignature $VerificationDatabase
$destinationForeignKeys = Get-ForeignKeySignature $VerificationDatabase
$destinationRows = Get-ExactRowCounts $VerificationDatabase $ExpectedTables

Assert-EqualSequence $sourceSchema $destinationSchema 'las columnas y tipos'
Assert-EqualSequence $sourceIndexes $destinationIndexes 'los índices'
Assert-EqualSequence $sourceForeignKeys $destinationForeignKeys 'las claves foráneas'

foreach ($table in $ExpectedTables) {
    if ($sourceRows[$table] -ne $destinationRows[$table]) {
        throw "El conteo restaurado no coincide para la tabla $table."
    }
}

$report = @(
    'EVOHKA RNF03 - verificación POST-BCRYPT / PRE-003',
    "Fecha: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss K')",
    "Base origen: $SourceDatabase",
    "Base restaurada: $VerificationDatabase",
    "Respaldo: $backupPath",
    "Tablas: $($destinationTables.Count)",
    "Usuarios: $($destinationBcrypt.Usuarios)",
    "Bcrypt válidas: $($destinationBcrypt.BcryptValidas)",
    "Legacy: $($destinationBcrypt.LegacyTotal)",
    "Bcrypt inválidas: $($destinationBcrypt.BcryptInvalidas)",
    "Marcadores pendientes: $($destinationBcrypt.CredencialesConMarcador)",
    'Columnas y tipos coincidentes: sí',
    'Índices coincidentes: sí',
    'Claves foráneas coincidentes: sí',
    'Conteos exactos por tabla coincidentes: sí',
    'Estado: RESPALDO_RNF03_BLOQUE2_VERIFICADO'
)
[System.IO.File]::WriteAllLines(
    $reportPath,
    $report,
    [System.Text.UTF8Encoding]::new($false)
)

[PSCustomObject]@{
    Estado = 'RESPALDO_RNF03_BLOQUE2_VERIFICADO'
    Respaldo = $backupPath
    Reporte = $reportPath
    BaseVerificacion = $VerificationDatabase
    Tablas = $destinationTables.Count
    Usuarios = $destinationBcrypt.Usuarios
    BcryptValidas = $destinationBcrypt.BcryptValidas
    Legacy = $destinationBcrypt.LegacyTotal
    BcryptInvalidas = $destinationBcrypt.BcryptInvalidas
    ConteosPorTablaCoinciden = $true
    EstructuraCoincide = $true
}
