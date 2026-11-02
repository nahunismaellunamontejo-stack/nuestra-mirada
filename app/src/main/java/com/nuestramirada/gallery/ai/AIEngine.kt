package com.nuestramirada.gallery.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest

/** First on-device Real-ESRGAN x4plus integration experiment.
 * The public model is downloaded on demand, checksum-verified, then executed with TFLite.
 * This initial proof-of-concept feeds one 128x128 tile; tiled full-resolution processing
 * is a subsequent optimization once the device/runtime compatibility is measured.
 */
class AIEngine(private val context: Context) {
    companion object {
        private const val MODEL_URL = "https://huggingface.co/Mogfbkx/pixellift-models/resolve/main/models/real_esrgan_x4plus_f32.tflite"
        private const val MODEL_BYTES = 67_024_900L
        private const val MODEL_SHA256 = "026fcdd456423335fee6a7d7a624722e286e7d98adce2d651491819eb41bad87"
    }

    suspend fun enhance(uri: Uri, progress: (String) -> Unit = {}): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            progress("Preparando modelo Real-ESRGAN x4plus…")
            val model = ensureModel(progress)
            progress("Cargando fotografía…")
            val source = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                ?: error("No se pudo leer la fotografía seleccionada.")
            val inputBitmap = Bitmap.createScaledBitmap(source, 128, 128, true)
            val input = ByteBuffer.allocateDirect(1 * 128 * 128 * 3 * 4).order(ByteOrder.nativeOrder())
            val pixels = IntArray(128 * 128)
            inputBitmap.getPixels(pixels, 0, 128, 0, 0, 128, 128)
            for (pixel in pixels) {
                input.putFloat(((pixel shr 16) and 255) / 255f)
                input.putFloat(((pixel shr 8) and 255) / 255f)
                input.putFloat((pixel and 255) / 255f)
            }
            input.rewind()
            progress("Ejecutando IA en el teléfono…")
            val output = Array(1) { Array(512) { Array(512) { FloatArray(3) } } }
            val options = Interpreter.Options().setNumThreads(4)
            Interpreter(model, options).use { interpreter ->
                val inShape = interpreter.getInputTensor(0).shape()
                val outShape = interpreter.getOutputTensor(0).shape()
                require(inShape.contentEquals(intArrayOf(1, 128, 128, 3))) { "Entrada del modelo no compatible: ${inShape.contentToString()}" }
                require(outShape.contentEquals(intArrayOf(1, 512, 512, 3))) { "Salida del modelo no compatible: ${outShape.contentToString()}" }
                interpreter.run(input, output)
            }
            progress("Guardando resultado 4×…")
            val outPixels = IntArray(512 * 512)
            var index = 0
            for (y in 0 until 512) for (x in 0 until 512) {
                val rgb = output[0][y][x]
                val r = (rgb[0].coerceIn(0f, 1f) * 255).toInt()
                val g = (rgb[1].coerceIn(0f, 1f) * 255).toInt()
                val b = (rgb[2].coerceIn(0f, 1f) * 255).toInt()
                outPixels[index++] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
            val result = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
            result.setPixels(outPixels, 0, 512, 0, 0, 512, 512)
            val file = File(context.cacheDir, "nuestra_mirada_esrgan_${System.currentTimeMillis()}.png")
            file.outputStream().use { result.compress(Bitmap.CompressFormat.PNG, 100, it) }
            source.recycle(); inputBitmap.recycle(); result.recycle()
            file
        }
    }

    private fun ensureModel(progress: (String) -> Unit): ByteBuffer {
        val dir = File(context.filesDir, "models").apply { mkdirs() }
        val modelFile = File(dir, "real_esrgan_x4plus_f32.tflite")
        if (!modelFile.exists() || modelFile.length() != MODEL_BYTES || sha256(modelFile) != MODEL_SHA256) {
            val temp = File(dir, "model.download")
            progress("Descargando modelo (aprox. 64 MB)…")
            val connection = (URL(MODEL_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = 20000; readTimeout = 120000; instanceFollowRedirects = true
            }
            try {
                connection.connect()
                if (connection.responseCode !in 200..299) error("No se pudo descargar el modelo. HTTP ${connection.responseCode}")
                connection.inputStream.use { input -> temp.outputStream().use { output ->
                    val buffer = ByteArray(1024 * 1024); var total = 0L
                    while (true) { val n = input.read(buffer); if (n < 0) break; output.write(buffer, 0, n); total += n }
                } }
            } finally { connection.disconnect() }
            if (temp.length() != MODEL_BYTES || sha256(temp) != MODEL_SHA256) {
                temp.delete(); error("El archivo del modelo no pasó la verificación de integridad.")
            }
            if (!temp.renameTo(modelFile)) { temp.copyTo(modelFile, overwrite = true); temp.delete() }
        }
        val bytes = modelFile.readBytes()
        return ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder()).apply { put(bytes); rewind() }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input -> val buffer = ByteArray(1024 * 1024); while (true) { val n = input.read(buffer); if (n < 0) break; digest.update(buffer, 0, n) } }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
