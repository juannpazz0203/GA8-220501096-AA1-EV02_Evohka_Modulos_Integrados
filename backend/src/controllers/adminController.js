const adminService = require('../services/adminService');
const contrasenaService = require('../services/contrasenaService');

const ROLES_PERMITIDOS = new Set([
  'arrendatario',
  'propietario',
  'administrador'
]);

function esAdministrador(req, res) {
  if (req.usuario?.rol !== 'administrador') {
    res.status(403).json({ mensaje: 'Se requiere el rol administrador' });
    return false;
  }
  return true;
}

function obtenerId(valor) {
  const id = Number(valor);
  return Number.isInteger(id) && id > 0 ? id : null;
}

function correoValido(correo) {
  return correo.length <= 150 && /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(correo);
}

function validarUsuario(datos = {}, incluirContrasena = false) {
  const nombre = typeof datos.nombre === 'string' ? datos.nombre.trim() : '';
  const apellido = typeof datos.apellido === 'string' ? datos.apellido.trim() : '';
  const correo = typeof datos.correo === 'string'
    ? datos.correo.trim().toLowerCase()
    : '';
  const rol = typeof datos.rol === 'string' ? datos.rol.trim().toLowerCase() : '';

  if (!nombre || nombre.length > 100) {
    return { error: 'El nombre es obligatorio y no puede superar 100 caracteres' };
  }
  if (!apellido || apellido.length > 100) {
    return { error: 'El apellido es obligatorio y no puede superar 100 caracteres' };
  }
  if (!correoValido(correo)) {
    return { error: 'El correo no tiene un formato válido' };
  }
  if (!ROLES_PERMITIDOS.has(rol)) {
    return { error: 'El rol debe ser arrendatario, propietario o administrador' };
  }

  const telefono = typeof datos.telefono === 'string' ? datos.telefono.trim() : '';
  if (telefono.length > 20) {
    return { error: 'El teléfono no puede superar 20 caracteres' };
  }

  const normalizado = {
    nombre,
    apellido,
    correo,
    telefono: telefono || null,
    rol
  };
  if (incluirContrasena) {
    const contrasena = typeof datos.contrasena === 'string' ? datos.contrasena : '';
    const validacionContrasena =
      contrasenaService.validarNuevaContrasena(contrasena);
    if (!validacionContrasena.valida) {
      return { error: validacionContrasena.mensaje };
    }
    normalizado.contrasena = contrasena;
  }

  return { datos: normalizado };
}

async function obtenerResumen(req, res) {
  try {
    if (!esAdministrador(req, res)) return;

    const datos = await adminService.obtenerResumen();
    return res.status(200).json({
      mensaje: 'Resumen administrativo recuperado correctamente',
      datos
    });
  } catch (error) {
    console.error('Error al consultar resumen administrativo:', error.message);
    return res.status(500).json({
      mensaje: 'No fue posible consultar el resumen administrativo'
    });
  }
}

async function listarUsuarios(req, res) {
  try {
    if (!esAdministrador(req, res)) return;
    const usuarios = await adminService.listarUsuarios();
    return res.status(200).json({
      mensaje: 'Usuarios recuperados correctamente',
      total: usuarios.length,
      datos: usuarios
    });
  } catch (error) {
    console.error('Error al listar usuarios:', error.message);
    return res.status(500).json({ mensaje: 'No fue posible consultar los usuarios' });
  }
}

async function crearUsuario(req, res) {
  try {
    if (!esAdministrador(req, res)) return;
    const validacion = validarUsuario(req.body, true);
    if (validacion.error) {
      return res.status(400).json({ mensaje: validacion.error });
    }

    const resultado = await adminService.crearUsuario(validacion.datos);
    if (resultado.error === 'correo_duplicado') {
      return res.status(409).json({ mensaje: 'Ya existe un usuario con ese correo' });
    }
    return res.status(201).json({
      mensaje: 'Usuario creado correctamente',
      datos: resultado.datos
    });
  } catch (error) {
    console.error('Error al crear usuario:', error.message);
    return res.status(500).json({ mensaje: 'No fue posible crear el usuario' });
  }
}

async function actualizarUsuario(req, res) {
  try {
    if (!esAdministrador(req, res)) return;
    const idUsuario = obtenerId(req.params.id);
    if (!idUsuario) {
      return res.status(400).json({ mensaje: 'El identificador del usuario no es válido' });
    }
    const validacion = validarUsuario(req.body);
    if (validacion.error) {
      return res.status(400).json({ mensaje: validacion.error });
    }

    const resultado = await adminService.actualizarUsuario(
      idUsuario,
      req.usuario.id_usuario,
      validacion.datos
    );
    const errores = {
      no_existe: [404, 'El usuario no existe'],
      correo_duplicado: [409, 'Ya existe otro usuario con ese correo'],
      ultimo_administrador: [409, 'No se puede cambiar el rol del último administrador'],
      auto_cambio_rol: [409, 'No puede cambiar su propio rol durante la sesión administrativa']
    };
    if (resultado.error) {
      const [codigo, mensaje] = errores[resultado.error] || [409, 'No fue posible actualizar el usuario'];
      return res.status(codigo).json({ mensaje });
    }
    return res.status(200).json({
      mensaje: 'Usuario actualizado correctamente',
      datos: resultado.datos
    });
  } catch (error) {
    console.error('Error al actualizar usuario:', error.message);
    return res.status(500).json({ mensaje: 'No fue posible actualizar el usuario' });
  }
}

async function eliminarUsuario(req, res) {
  try {
    if (!esAdministrador(req, res)) return;
    const idUsuario = obtenerId(req.params.id);
    if (!idUsuario) {
      return res.status(400).json({ mensaje: 'El identificador del usuario no es válido' });
    }

    const resultado = await adminService.eliminarUsuario(
      idUsuario,
      req.usuario.id_usuario
    );
    const errores = {
      no_existe: [404, 'El usuario no existe'],
      ultimo_administrador: [409, 'No se puede eliminar el último administrador'],
      auto_eliminacion: [409, 'No puede eliminar su propia cuenta durante la sesión administrativa'],
      tiene_relaciones: [409, 'El usuario posee información asociada y no puede eliminarse de forma segura']
    };
    if (resultado.error) {
      const [codigo, mensaje] = errores[resultado.error] || [409, 'No fue posible eliminar el usuario'];
      return res.status(codigo).json({ mensaje });
    }
    return res.status(200).json({
      mensaje: 'Usuario eliminado correctamente',
      datos: resultado.datos
    });
  } catch (error) {
    if (error.code === 'ER_ROW_IS_REFERENCED_2' || error.errno === 1451) {
      return res.status(409).json({
        mensaje: 'El usuario posee información asociada y no puede eliminarse de forma segura'
      });
    }
    console.error('Error al eliminar usuario:', error.message);
    return res.status(500).json({ mensaje: 'No fue posible eliminar el usuario' });
  }
}

module.exports = {
  obtenerResumen,
  listarUsuarios,
  crearUsuario,
  actualizarUsuario,
  eliminarUsuario,
  validarUsuario
};
