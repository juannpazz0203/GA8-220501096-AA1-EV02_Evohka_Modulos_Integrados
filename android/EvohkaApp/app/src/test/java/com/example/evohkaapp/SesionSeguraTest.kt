package com.example.evohkaapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets
import java.util.Base64

class SesionSeguraTest {

    @Test
    fun guardarSesionProtegeTokenYEliminaCopiaLegada() {
        val seguro = AlmacenSesionMemoria()
        val legado = AlmacenSesionMemoria()
        val protector = ProtectorTokenPrueba()
        val repositorio = RepositorioSesionSegura(seguro, legado, protector)
        val token = "Bearer token-de-prueba"

        repositorio.guardarSesion(
            token,
            usuario(),
            "inquilino"
        )

        assertNull(legado.texto("token"))
        assertFalse(seguro.todosLosTextos().values.any { it.contains(token) })
        assertEquals(token, repositorio.obtenerToken())
        assertEquals(7, legado.entero("id_usuario", 0))
        assertEquals("arrendatario", legado.texto("rol"))
    }

    @Test
    fun restauraSesionCifrada() {
        val seguro = AlmacenSesionMemoria()
        val legado = AlmacenSesionMemoria()
        val protector = ProtectorTokenPrueba()
        val repositorio = RepositorioSesionSegura(seguro, legado, protector)

        repositorio.guardarSesion(
            "Bearer sesion-restaurada",
            usuario(),
            "inquilino"
        )

        val repositorioTrasReinicio =
            RepositorioSesionSegura(seguro, legado, protector)

        assertEquals(
            "Bearer sesion-restaurada",
            repositorioTrasReinicio.obtenerToken()
        )
    }

    @Test
    fun migraUnaSolaVezTokenLegadoYEliminaTextoPlano() {
        val seguro = AlmacenSesionMemoria()
        val legado = AlmacenSesionMemoria().apply {
            aplicar(
                CambiosAlmacenSesion(
                    textos = mapOf("token" to "Bearer token-legado")
                )
            )
        }
        val repositorio = RepositorioSesionSegura(
            seguro,
            legado,
            ProtectorTokenPrueba()
        )

        assertEquals("Bearer token-legado", repositorio.obtenerToken())
        assertNull(legado.texto("token"))
        assertTrue(seguro.booleano("migracion_legado_realizada", false))
        assertEquals("Bearer token-legado", repositorio.obtenerToken())
    }

    @Test
    fun cifradoCorruptoEliminaSesionYExigeNuevoLogin() {
        val seguro = AlmacenSesionMemoria().apply {
            aplicar(
                CambiosAlmacenSesion(
                    textos = mapOf(
                        "token_iv" to "iv-prueba",
                        "token_cifrado" to "contenido-invalido"
                    )
                )
            )
        }
        val legado = AlmacenSesionMemoria().apply {
            aplicar(
                CambiosAlmacenSesion(
                    enteros = mapOf("id_usuario" to 7),
                    textos = mapOf("rol" to "arrendatario")
                )
            )
        }
        val protector = ProtectorTokenPrueba()
        val repositorio = RepositorioSesionSegura(seguro, legado, protector)

        assertNull(repositorio.obtenerToken())
        assertFalse(seguro.todosLosTextos().containsKey("token_cifrado"))
        assertTrue(seguro.booleano("migracion_legado_realizada", false))
        assertTrue(legado.estaVacio())
        assertTrue(protector.claveEliminada)
    }

    @Test
    fun cerrarSesionEliminaTokenIdentidadYClave() {
        val seguro = AlmacenSesionMemoria()
        val legado = AlmacenSesionMemoria()
        val protector = ProtectorTokenPrueba()
        val repositorio = RepositorioSesionSegura(seguro, legado, protector)
        repositorio.guardarSesion("Bearer temporal", usuario(), "inquilino")

        repositorio.cerrarSesion()

        assertFalse(seguro.todosLosTextos().containsKey("token_cifrado"))
        assertTrue(seguro.booleano("migracion_legado_realizada", false))
        assertTrue(legado.estaVacio())
        assertTrue(protector.claveEliminada)
        assertNull(repositorio.obtenerToken())
    }

    @Test
    fun logoutImpideVolverAMigrarUnTokenLegado() {
        val seguro = AlmacenSesionMemoria()
        val legado = AlmacenSesionMemoria()
        val repositorio = RepositorioSesionSegura(
            seguro,
            legado,
            ProtectorTokenPrueba()
        )
        repositorio.guardarSesion("Bearer inicial", usuario(), "inquilino")
        repositorio.cerrarSesion()
        legado.aplicar(
            CambiosAlmacenSesion(
                textos = mapOf("token" to "Bearer legado-tardio")
            )
        )

        assertNull(repositorio.obtenerToken())
        assertNull(legado.texto("token"))
    }

    private fun usuario() = Usuario(
        id_usuario = 7,
        nombre = "Usuario",
        apellido = "Prueba",
        correo = "usuario@example.test",
        rol = "arrendatario"
    )
}

private class AlmacenSesionMemoria : AlmacenSesion {
    private val valores = mutableMapOf<String, Any>()

    override fun texto(clave: String): String? = valores[clave] as? String

    override fun entero(clave: String, valorPredeterminado: Int): Int =
        valores[clave] as? Int ?: valorPredeterminado

    override fun booleano(clave: String, valorPredeterminado: Boolean): Boolean =
        valores[clave] as? Boolean ?: valorPredeterminado

    override fun aplicar(cambios: CambiosAlmacenSesion) {
        if (cambios.limpiar) {
            valores.clear()
        }
        valores.putAll(cambios.textos)
        valores.putAll(cambios.enteros)
        valores.putAll(cambios.booleanos)
        cambios.eliminar.forEach(valores::remove)
    }

    fun todosLosTextos(): Map<String, String> =
        valores.mapNotNull { (clave, valor) ->
            (valor as? String)?.let { clave to it }
        }.toMap()

    fun estaVacio(): Boolean = valores.isEmpty()
}

private class ProtectorTokenPrueba : ProtectorToken {
    var claveEliminada = false

    override fun cifrar(valor: String): ValorCifrado =
        ValorCifrado(
            ivBase64 = "iv-prueba",
            contenidoBase64 = Base64.getEncoder().encodeToString(
                "protegido:$valor".toByteArray(StandardCharsets.UTF_8)
            )
        )

    override fun descifrar(valor: ValorCifrado): String {
        val contenido = String(
            Base64.getDecoder().decode(valor.contenidoBase64),
            StandardCharsets.UTF_8
        )
        check(contenido.startsWith("protegido:"))
        return contenido.removePrefix("protegido:")
    }

    override fun eliminarClave() {
        claveEliminada = true
    }
}
