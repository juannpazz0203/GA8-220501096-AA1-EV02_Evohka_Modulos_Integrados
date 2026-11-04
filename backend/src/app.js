const express = require('express');
const propiedadRoutes = require('./routes/propiedadRoutes');
const authRoutes = require('./routes/authRoutes');
const solicitudRoutes = require('./routes/solicitudRoutes');
const contratoRoutes = require('./routes/contratoRoutes');
const pagoRoutes = require('./routes/pagoRoutes');
const reporteRoutes = require('./routes/reporteRoutes');
const notificacionRoutes = require('./routes/notificacionRoutes');
const fallaRoutes = require('./routes/fallaRoutes');
const mensajeRoutes = require('./routes/mensajeRoutes');
const adminRoutes = require('./routes/adminRoutes');

const app = express();

// Middleware para recibir información en formato JSON.
app.use(express.json());

// Ruta principal para comprobar que la API está activa.
app.get('/', (req, res) => {
  res.status(200).json({
    mensaje: 'API de Evohka funcionando correctamente',
    evidencia: 'GA8-220501096-AA1-EV01',
    servicioPropiedades: '/api/propiedades',
    servicioAutenticacion: '/api/auth/login',
    servicioSolicitudes: '/api/solicitudes',
    servicioContratos: '/api/contratos',
    servicioPagos: '/api/pagos',
    servicioReportes: '/api/reportes/ingresos',
    servicioNotificaciones: '/api/notificaciones',
    servicioFallas: '/api/fallas',
    servicioMensajes: '/api/mensajes',
    servicioAdministracion: '/api/admin/resumen'
  });
});

// Registro de las rutas.
app.use('/api/auth', authRoutes);
app.use('/api/propiedades', propiedadRoutes);
app.use('/api/solicitudes', solicitudRoutes);
app.use('/api/contratos', contratoRoutes);
app.use('/api/pagos', pagoRoutes);
app.use('/api/reportes', reporteRoutes);
app.use('/api/notificaciones', notificacionRoutes);
app.use('/api/fallas', fallaRoutes);
app.use('/api/mensajes', mensajeRoutes);
app.use('/api/admin', adminRoutes);

module.exports = app;
