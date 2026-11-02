# Nuestra Mirada — Android 0.1.0

Galería Android inicial con interfaz clara inspirada en Samsung One UI.

## Incluye
- Aplicación Android con Kotlin y Jetpack Compose.
- Selector de fotografías del sistema (Photo Picker / GetMultipleContents).
- Cuadrícula de imágenes seleccionadas.
- Visor de fotografía.
- Estructura inicial `AIEngine` para integrar superresolución local más adelante.
- Workflow de GitHub Actions para compilar una APK de depuración en la nube.

## Importante
Esta versión **todavía no incorpora un modelo de IA**. El botón "Mejorar con IA" informa que el motor está en desarrollo. No aplica filtros ni afirma mejorar imágenes.

## Compilar en GitHub Actions
1. Crea un repositorio vacío en GitHub.
2. Sube el contenido de este proyecto a la rama `main`.
3. Abre la pestaña **Actions**.
4. Ejecuta **Build Nuestra Mirada APK** o espera a que termine el flujo tras subir cambios.
5. Descarga el artefacto `nuestra-mirada-debug-apk`.
6. Extrae `app-debug.apk` e instálalo en Android.

## Requisitos
- JDK 17.
- Android SDK Platform 36 en el runner.
- Gradle 8.13 o compatible con Android Gradle Plugin 8.9.1.

## Próxima etapa
Validar un modelo real de superresolución (por ejemplo una conversión compatible de Real-ESRGAN) y medir memoria, tiempo y aceleración en el Galaxy S20 FE. No basta con renombrar un archivo `.pth` a `.tflite`.
