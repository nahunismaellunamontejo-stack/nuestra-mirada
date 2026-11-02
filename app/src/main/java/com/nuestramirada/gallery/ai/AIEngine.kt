package com.nuestramirada.gallery.ai

/**
 * Punto de integración del futuro motor de superresolución.
 * No se presenta ningún filtro básico como si fuera IA.
 *
 * La implementación real se añadirá después de validar un modelo
 * compatible con LiteRT/Android y el hardware del dispositivo.
 */
interface AIEngine {
    suspend fun enhance(
        inputPath: String,
        outputPath: String,
        scale: Int
    ): Result<String>
}
