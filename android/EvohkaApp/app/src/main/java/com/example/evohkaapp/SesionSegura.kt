package com.example.evohkaapp

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.util.concurrent.atomic.AtomicBoolean
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal data class CambiosAlmacenSesion(
    val textos: Map<String, String> = emptyMap(),
    val enteros: Map<String, Int> = emptyMap(),
    val booleanos: Map<String, Boolean> = emptyMap(),
    val eliminar: Set<String> = emptySet(),
    val limpiar: Boolean = false
)

internal interface AlmacenSesion {
    fun texto(clave: String): String?
    fun entero(clave: String, valorPredeterminado: Int): Int
    fun booleano(clave: String, valorPredeterminado: Boolean): Boolean
    fun aplicar(cambios: CambiosAlmacenSesion)
}

internal class AlmacenPreferenciasSesion(
    private val preferencias: SharedPreferences
) : AlmacenSesion {
    override fun texto(clave: String): String? =
        preferencias.getString(clave, null)

    override fun entero(clave: String, valorPredeterminado: Int): Int =
        preferencias.getInt(clave, valorPredeterminado)

    override fun booleano(clave: String, valorPredeterminado: Boolean): Boolean =
        preferencias.getBoolean(clave, valorPredeterminado)

    override fun aplicar(cambios: CambiosAlmacenSesion) {
        val editor = preferencias.edit()
        if (cambios.limpiar) {
            editor.clear()
        }
        cambios.textos.forEach(editor::putString)
        cambios.enteros.forEach(editor::putInt)
        cambios.booleanos.forEach(editor::putBoolean)
        cambios.eliminar.forEach(editor::remove)
        check(editor.commit()) {
            "No fue posible guardar la sesión local"
        }
    }
}

internal data class ValorCifrado(
    val ivBase64: String,
    val contenidoBase64: String
)

internal interface ProtectorToken {
    fun cifrar(valor: String): ValorCifrado
    fun descifrar(valor: ValorCifrado): String
    fun eliminarClave()
}

internal class ProtectorTokenAndroidKeystore : ProtectorToken {
    private val almacenClaves: KeyStore
        get() = KeyStore.getInstance(PROVEEDOR_KEYSTORE).apply {
            load(null)
        }

    override fun cifrar(valor: String): ValorCifrado {
        val cifrador = Cipher.getInstance(TRANSFORMACION)
        cifrador.init(Cipher.ENCRYPT_MODE, obtenerOCrearClave())
        cifrador.updateAAD(DATOS_ASOCIADOS)

        return ValorCifrado(
            ivBase64 = Base64.encodeToString(cifrador.iv, Base64.NO_WRAP),
            contenidoBase64 = Base64.encodeToString(
                cifrador.doFinal(valor.toByteArray(StandardCharsets.UTF_8)),
                Base64.NO_WRAP
            )
        )
    }

    override fun descifrar(valor: ValorCifrado): String {
        val entrada = almacenClaves.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry
            ?: error("La clave de la sesión no está disponible")
        val cifrador = Cipher.getInstance(TRANSFORMACION)
        val iv = Base64.decode(valor.ivBase64, Base64.NO_WRAP)
        val contenido = Base64.decode(valor.contenidoBase64, Base64.NO_WRAP)

        cifrador.init(
            Cipher.DECRYPT_MODE,
            entrada.secretKey,
            GCMParameterSpec(BITS_ETIQUETA_GCM, iv)
        )
        cifrador.updateAAD(DATOS_ASOCIADOS)

        return String(cifrador.doFinal(contenido), StandardCharsets.UTF_8)
    }

    override fun eliminarClave() {
        val keyStore = almacenClaves
        if (keyStore.containsAlias(ALIAS)) {
            keyStore.deleteEntry(ALIAS)
        }
    }

    private fun obtenerOCrearClave(): SecretKey {
        val keyStore = almacenClaves
        val existente = keyStore.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry
        if (existente != null) {
            return existente.secretKey
        }

        val generador = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            PROVEEDOR_KEYSTORE
        )
        val especificacion = KeyGenParameterSpec.Builder(
            ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .build()
        generador.init(especificacion)
        return generador.generateKey()
    }

    private companion object {
        const val PROVEEDOR_KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "EVOHKA_TOKEN_SESION_V1"
        const val TRANSFORMACION = "AES/GCM/NoPadding"
        const val BITS_ETIQUETA_GCM = 128
        val DATOS_ASOCIADOS =
            "EVOHKA_SESION_TOKEN_V1".toByteArray(StandardCharsets.UTF_8)
    }
}

internal class RepositorioSesionSegura(
    private val almacenSeguro: AlmacenSesion,
    private val almacenLegado: AlmacenSesion,
    private val protector: ProtectorToken
) {
    @Synchronized
    fun guardarSesion(
        token: String,
        usuario: Usuario,
        tipoIngreso: String
    ) {
        require(token.isNotBlank()) { "El token de sesión es obligatorio" }

        try {
            guardarTokenCifrado(token)
            actualizarIdentidad(usuario, tipoIngreso)
            almacenLegado.aplicar(
                CambiosAlmacenSesion(eliminar = setOf(CLAVE_TOKEN_LEGADO))
            )
        } catch (error: Exception) {
            limpiarInternamente()
            throw error
        }
    }

    @Synchronized
    fun obtenerToken(): String? {
        val iv = almacenSeguro.texto(CLAVE_IV)
        val contenido = almacenSeguro.texto(CLAVE_CONTENIDO)

        if (iv != null || contenido != null) {
            if (iv.isNullOrBlank() || contenido.isNullOrBlank()) {
                limpiarInternamente()
                return null
            }

            return try {
                val token = protector.descifrar(ValorCifrado(iv, contenido))
                if (token.isBlank()) {
                    limpiarInternamente()
                    null
                } else {
                    almacenLegado.aplicar(
                        CambiosAlmacenSesion(eliminar = setOf(CLAVE_TOKEN_LEGADO))
                    )
                    token
                }
            } catch (_: Exception) {
                limpiarInternamente()
                null
            }
        }

        return migrarTokenLegadoUnaVez()
    }

    @Synchronized
    fun actualizarIdentidad(
        usuario: Usuario,
        tipoIngreso: String = tipoIngresoParaRol(usuario.rol)
    ) {
        val rol = usuario.rol.trim().lowercase()
        almacenLegado.aplicar(
            CambiosAlmacenSesion(
                textos = mapOf(
                    CLAVE_NOMBRE to usuario.nombre,
                    CLAVE_ROL to rol,
                    CLAVE_TIPO_INGRESO to tipoIngreso
                ),
                enteros = mapOf(CLAVE_ID_USUARIO to usuario.id_usuario),
                eliminar = setOf(CLAVE_TOKEN_LEGADO)
            )
        )
    }

    @Synchronized
    fun cerrarSesion() {
        limpiarInternamente()
    }

    private fun migrarTokenLegadoUnaVez(): String? {
        val tokenLegado = almacenLegado.texto(CLAVE_TOKEN_LEGADO)
        val migracionRealizada = almacenSeguro.booleano(
            CLAVE_MIGRACION_REALIZADA,
            false
        )

        if (migracionRealizada) {
            if (tokenLegado != null) {
                almacenLegado.aplicar(
                    CambiosAlmacenSesion(eliminar = setOf(CLAVE_TOKEN_LEGADO))
                )
            }
            return null
        }

        if (tokenLegado.isNullOrBlank()) {
            almacenLegado.aplicar(
                CambiosAlmacenSesion(eliminar = setOf(CLAVE_TOKEN_LEGADO))
            )
            almacenSeguro.aplicar(
                CambiosAlmacenSesion(
                    booleanos = mapOf(CLAVE_MIGRACION_REALIZADA to true)
                )
            )
            return null
        }

        return try {
            guardarTokenCifrado(tokenLegado)
            almacenLegado.aplicar(
                CambiosAlmacenSesion(eliminar = setOf(CLAVE_TOKEN_LEGADO))
            )
            tokenLegado
        } catch (_: Exception) {
            limpiarInternamente()
            null
        }
    }

    private fun guardarTokenCifrado(token: String) {
        val cifrado = protector.cifrar(token)
        almacenSeguro.aplicar(
            CambiosAlmacenSesion(
                textos = mapOf(
                    CLAVE_IV to cifrado.ivBase64,
                    CLAVE_CONTENIDO to cifrado.contenidoBase64
                ),
                enteros = mapOf(CLAVE_VERSION to VERSION_ACTUAL),
                booleanos = mapOf(CLAVE_MIGRACION_REALIZADA to true),
                limpiar = true
            )
        )
    }

    private fun limpiarInternamente() {
        runCatching {
            almacenSeguro.aplicar(
                CambiosAlmacenSesion(
                    booleanos = mapOf(CLAVE_MIGRACION_REALIZADA to true),
                    limpiar = true
                )
            )
        }
        runCatching {
            almacenLegado.aplicar(CambiosAlmacenSesion(limpiar = true))
        }
        runCatching { protector.eliminarClave() }
    }

    private fun tipoIngresoParaRol(rol: String): String =
        when (rol.trim().lowercase()) {
            "arrendatario" -> "inquilino"
            "propietario" -> "propietario"
            "administrador" -> "administrador"
            else -> rol.trim().lowercase()
        }

    private companion object {
        const val VERSION_ACTUAL = 1
        const val CLAVE_IV = "token_iv"
        const val CLAVE_CONTENIDO = "token_cifrado"
        const val CLAVE_VERSION = "version"
        const val CLAVE_MIGRACION_REALIZADA = "migracion_legado_realizada"
        const val CLAVE_TOKEN_LEGADO = "token"
        const val CLAVE_ID_USUARIO = "id_usuario"
        const val CLAVE_NOMBRE = "nombre"
        const val CLAVE_ROL = "rol"
        const val CLAVE_TIPO_INGRESO = "tipo_ingreso"
    }
}

object SesionSegura {
    private const val PREFERENCIAS_LEGADAS = "EvohkaSesion"
    private const val PREFERENCIAS_SEGURAS = "EvohkaSesionSegura"

    @Volatile
    private var repositorio: RepositorioSesionSegura? = null

    private val validadaEnEsteProceso = AtomicBoolean(false)

    fun inicializar(contexto: Context) {
        if (repositorio != null) {
            return
        }

        synchronized(this) {
            if (repositorio == null) {
                val contextoAplicacion = contexto.applicationContext
                repositorio = RepositorioSesionSegura(
                    almacenSeguro = AlmacenPreferenciasSesion(
                        contextoAplicacion.getSharedPreferences(
                            PREFERENCIAS_SEGURAS,
                            Context.MODE_PRIVATE
                        )
                    ),
                    almacenLegado = AlmacenPreferenciasSesion(
                        contextoAplicacion.getSharedPreferences(
                            PREFERENCIAS_LEGADAS,
                            Context.MODE_PRIVATE
                        )
                    ),
                    protector = ProtectorTokenAndroidKeystore()
                )
            }
        }
    }

    fun guardarSesion(token: String, usuario: Usuario, tipoIngreso: String) {
        obtenerRepositorio().guardarSesion(token, usuario, tipoIngreso)
        validadaEnEsteProceso.set(true)
    }

    fun obtenerToken(): String? =
        if (validadaEnEsteProceso.get()) {
            obtenerRepositorio().obtenerToken()
        } else {
            null
        }

    fun obtenerTokenParaValidacion(): String? =
        obtenerRepositorio().obtenerToken()

    fun marcarValidadaEnEsteProceso() {
        validadaEnEsteProceso.set(true)
    }

    fun actualizarIdentidad(usuario: Usuario) {
        obtenerRepositorio().actualizarIdentidad(usuario)
    }

    fun cerrarSesion() {
        validadaEnEsteProceso.set(false)
        obtenerRepositorio().cerrarSesion()
    }

    private fun obtenerRepositorio(): RepositorioSesionSegura =
        checkNotNull(repositorio) {
            "SesionSegura debe inicializarse desde Application"
        }
}
