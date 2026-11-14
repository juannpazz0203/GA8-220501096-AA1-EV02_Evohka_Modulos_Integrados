class ErrorFotoPropiedad extends Error {
  constructor(codigo, mensaje, estadoHttp = 400, causa) {
    super(mensaje, causa ? { cause: causa } : undefined);
    this.name = 'ErrorFotoPropiedad';
    this.codigo = codigo;
    this.estadoHttp = estadoHttp;
  }
}

module.exports = {
  ErrorFotoPropiedad
};
