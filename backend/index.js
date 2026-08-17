require('dotenv').config();

const app = require('./src/app');
const { probarConexion } = require('./src/config/db');

const PORT = Number(process.env.PORT) || 3000;

async function iniciarServidor() {
  await probarConexion();

  app.listen(PORT, () => {
    console.log(`Servidor de Evohka ejecutándose en http://localhost:${PORT}`);
    console.log(`Servicio disponible en http://localhost:${PORT}/api/propiedades`);
  });
}

iniciarServidor().catch((error) => {
  console.error('No fue posible iniciar el servidor:', error.message);
  process.exit(1);
});
