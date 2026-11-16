package com.example.evohkaapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FiltroPropiedadesTest {

    private val propiedades = listOf(
        propiedad(
            id = 1,
            titulo = "Apartamento central",
            tipo = "Apartamento",
            barrio = "Chapinero",
            ciudad = "Bogotá",
            canon = 1_200_000.0
        ),
        propiedad(
            id = 2,
            titulo = "Casa familiar",
            tipo = "Casa",
            barrio = "Laureles",
            ciudad = "Medellín",
            canon = 2_300_000.0
        ),
        propiedad(
            id = 3,
            titulo = "Estudio económico",
            tipo = "Apartaestudio",
            barrio = "Centro",
            ciudad = "Bogotá",
            canon = 850_000.0
        )
    )

    @Test
    fun filtraPorCiudadSinDistinguirMayusculasOTildes() {
        val resultado = FiltroPropiedades.filtrar(
            propiedades,
            CriteriosFiltroPropiedades(ciudad = "bogota")
        )

        assertEquals(listOf(1, 3), resultado.map { it.id_propiedad })
    }

    @Test
    fun filtraPorTipoDeInmueble() {
        val resultado = FiltroPropiedades.filtrar(
            propiedades,
            CriteriosFiltroPropiedades(tipoInmueble = "casa")
        )

        assertEquals(listOf(2), resultado.map { it.id_propiedad })
    }

    @Test
    fun buscaPorTituloCiudadYBarrio() {
        val porTitulo = FiltroPropiedades.filtrar(
            propiedades,
            CriteriosFiltroPropiedades(texto = "apartamento")
        )
        val porCiudad = FiltroPropiedades.filtrar(
            propiedades,
            CriteriosFiltroPropiedades(texto = "medellin")
        )
        val porBarrio = FiltroPropiedades.filtrar(
            propiedades,
            CriteriosFiltroPropiedades(texto = "centro")
        )

        assertEquals(listOf(1), porTitulo.map { it.id_propiedad })
        assertEquals(listOf(2), porCiudad.map { it.id_propiedad })
        assertEquals(listOf(3), porBarrio.map { it.id_propiedad })
    }

    @Test
    fun filtraPorRangoDePreciosIncluyendoLosLimites() {
        val resultado = FiltroPropiedades.filtrar(
            propiedades,
            CriteriosFiltroPropiedades(
                precioMinimo = 850_000.0,
                precioMaximo = 1_200_000.0
            )
        )

        assertEquals(listOf(1, 3), resultado.map { it.id_propiedad })
    }

    @Test
    fun combinaBusquedaCiudadTipoYPrecio() {
        val resultado = FiltroPropiedades.filtrar(
            propiedades,
            CriteriosFiltroPropiedades(
                texto = "centro",
                ciudad = "Bogotá",
                tipoInmueble = "Apartaestudio",
                precioMinimo = 800_000.0,
                precioMaximo = 900_000.0
            )
        )

        assertEquals(listOf(3), resultado.map { it.id_propiedad })
    }

    @Test
    fun devuelveListaVaciaCuandoNoHayCoincidencias() {
        val resultado = FiltroPropiedades.filtrar(
            propiedades,
            CriteriosFiltroPropiedades(texto = "inexistente")
        )

        assertTrue(resultado.isEmpty())
    }

    @Test
    fun limpiarFiltrosRestauraElCatalogoCompleto() {
        val filtradas = FiltroPropiedades.filtrar(
            propiedades,
            CriteriosFiltroPropiedades(ciudad = "Medellín")
        )
        val restauradas = FiltroPropiedades.filtrar(
            propiedades,
            CriteriosFiltroPropiedades()
        )

        assertEquals(1, filtradas.size)
        assertEquals(propiedades, restauradas)
    }

    @Test
    fun ignoraOpcionesVaciasYNoFallaConDatosFaltantes() {
        val incompletas = listOf(
            propiedad(
                id = 4,
                titulo = null,
                tipo = null,
                barrio = null,
                ciudad = null,
                canon = null
            ),
            propiedad(
                id = 5,
                titulo = "Con ciudad",
                tipo = " ",
                barrio = "",
                ciudad = "Cali",
                canon = Double.NaN
            )
        )

        assertTrue(
            FiltroPropiedades.filtrar(
                incompletas,
                CriteriosFiltroPropiedades(texto = "sin coincidencia")
            ).isEmpty()
        )
        assertEquals(
            listOf("Cali"),
            FiltroPropiedades.ciudadesDisponibles(incompletas)
        )
        assertTrue(
            FiltroPropiedades.tiposDisponibles(incompletas).isEmpty()
        )
    }

    @Test
    fun catalogoSinFiltrosConservaContenidoYOrden() {
        val resultado = FiltroPropiedades.filtrar(
            propiedades,
            CriteriosFiltroPropiedades()
        )

        assertEquals(propiedades, resultado)
        assertFalse(CriteriosFiltroPropiedades().tieneFiltrosActivos())
    }

    @Test
    fun validaPreciosInvalidosYMinimoMayorQueMaximo() {
        val invalido = FiltroPropiedades.validarRangoPrecio(
            "precio",
            "2000000"
        )
        val invertido = FiltroPropiedades.validarRangoPrecio(
            "2000000",
            "1000000"
        )
        val vacio = FiltroPropiedades.validarRangoPrecio("", "")

        assertFalse(invalido.esValido)
        assertFalse(invertido.esValido)
        assertTrue(vacio.esValido)
        assertNull(vacio.minimo)
        assertNull(vacio.maximo)
    }

    private fun propiedad(
        id: Int,
        titulo: String?,
        tipo: String?,
        barrio: String?,
        ciudad: String?,
        canon: Double?
    ): Propiedad = Propiedad(
        id_propiedad = id,
        titulo = titulo,
        tipo_inmueble = tipo,
        direccion = null,
        barrio = barrio,
        ciudad = ciudad,
        canon = canon,
        habitaciones = null,
        banos = null,
        id_propietario = null
    )
}
