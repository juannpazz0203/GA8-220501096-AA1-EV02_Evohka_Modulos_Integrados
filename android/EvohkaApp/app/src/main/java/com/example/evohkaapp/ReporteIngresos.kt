package com.example.evohkaapp

data class RespuestaReporteIngresos(
    val mensaje: String,
    val datos: ReporteIngresos?
)

data class ReporteIngresos(
    val resumen: ResumenIngresos,
    val por_propiedad: List<IngresoPorPropiedad>,
    val detalle: List<DetalleIngreso>
)

data class ResumenIngresos(
    val cantidad_pagos: Int,
    val total_ingresos: Double
)

data class IngresoPorPropiedad(
    val id_propiedad: Int,
    val titulo_propiedad: String?,
    val cantidad_pagos: Int,
    val total_ingresos: Double
)

data class DetalleIngreso(
    val id_pago: Int,
    val id_contrato: Int,
    val fecha_pago: String?,
    val monto: Double?,
    val metodo_pago: String?,
    val estado: String?,
    val id_propiedad: Int,
    val titulo_propiedad: String?
)
