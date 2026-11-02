# Nuestra Mirada V2 — prueba de IA integrada

Proyecto Android Kotlin + Jetpack Compose. Esta versión incorpora una primera integración experimental de **Real-ESRGAN x4plus** mediante TensorFlow Lite.

## Prueba de IA

- Al pulsar **Mejorar IA**, la aplicación descarga bajo demanda el modelo (aprox. 64 MB) desde el repositorio público de modelos PixelLift en Hugging Face.
- Verifica tamaño y SHA-256 antes de usarlo.
- Ejecuta TensorFlow Lite localmente en el teléfono; la fotografía seleccionada no se envía a un servidor.
- Esta primera prueba procesa un recorte cuadrado de 128×128 y genera una salida de 512×512. Es una prueba de integración del modelo, no todavía el procesamiento por mosaicos de fotografías completas.
- El modo automático y el avanzado ya abren el flujo de inferencia. Los controles avanzados adicionales se implementarán después de validar la inferencia.
- El resultado puede guardarse en `Pictures/NuestraMirada`.

Modelo: `real_esrgan_x4plus_f32.tflite` (exportación float32, entrada 1×128×128×3, salida 1×512×512×3). El archivo se descarga en el primer uso, no está incluido dentro del APK.

Repositorio del modelo y metadatos: https://huggingface.co/Mogfbkx/pixellift-models

Licencia indicada para el modelo: BSD-3-Clause. Revisar los avisos de licencia del repositorio antes de distribuir públicamente la aplicación.

## Limitaciones conocidas de esta primera prueba

- El Snapdragon 865 del Galaxy S20 FE no aparece entre los dispositivos oficialmente validados por Qualcomm para este modelo. La inferencia usa CPU TensorFlow Lite inicialmente; rendimiento, memoria y estabilidad deben medirse en el dispositivo real.
- La primera prueba no conserva la relación de aspecto completa ni procesa toda la imagen por mosaicos; se usa para confirmar descarga, carga del modelo e inferencia 4×.
- Si la descarga, la validación, la forma de tensores o la inferencia fallan, la aplicación mostrará el error para poder corregirlo.
- La galería de esta compilación todavía usa el selector del sistema; la consulta automática completa a MediaStore y la persistencia de la biblioteca siguen pendientes de integrar.

## Compilación

GitHub Actions ejecuta `gradle assembleDebug` con JDK 17 y Gradle 8.13. El artefacto se publica como `nuestra-mirada-v2-debug-apk`.
