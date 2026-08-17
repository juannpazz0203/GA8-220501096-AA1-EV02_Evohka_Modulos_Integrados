const express = require('express');
const propiedadRoutes = require('./routes/propiedadRoutes');
const authRoutes = require('./routes/authRoutes');

const app = express();

// Middleware para recibir información en formato JSON.
app.use(express.json());

// Ruta principal para comprobar que la API está activa.
app.get('/', (req, res) => {
  res.status(200).json({
    mensaje: 'API de Evohka funcionando correctamente',
    evidencia: 'GA8-220501096-AA1-EV01',
    servicioPropiedades: '/api/propiedades',
    servicioAutenticacion: '/api/auth/login'
  });
});

// Registro de las rutas.
app.use('/api/auth', authRoutes);
app.use('/api/propiedades', propiedadRoutes);

module.exports = app;