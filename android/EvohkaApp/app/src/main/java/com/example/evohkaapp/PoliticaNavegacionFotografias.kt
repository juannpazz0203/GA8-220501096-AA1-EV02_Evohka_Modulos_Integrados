package com.example.evohkaapp

internal enum class DestinoPostGuardadoFotografias {
    MIS_INMUEBLES,
    REINTENTAR_FOTOGRAFIAS
}

internal object PoliticaNavegacionFotografias {
    fun decidir(
        fotografiasPendientes: Boolean,
        cargaFotografiasExitosa: Boolean
    ): DestinoPostGuardadoFotografias =
        if (
            !fotografiasPendientes ||
            cargaFotografiasExitosa
        ) {
            DestinoPostGuardadoFotografias.MIS_INMUEBLES
        } else {
            DestinoPostGuardadoFotografias.REINTENTAR_FOTOGRAFIAS
        }
}
