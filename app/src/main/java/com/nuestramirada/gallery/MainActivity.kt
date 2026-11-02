package com.nuestramirada.gallery

import android.content.ContentValues
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import com.nuestramirada.gallery.ai.AIEngine
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs

private val Blue = Color(0xFF7560E8)
private val Ink = Color(0xFF171522)
private val Soft = Color(0xFFF3F1FA)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NuestraMiradaApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NuestraMiradaApp() {
    val context = LocalContext.current
    val photos = remember { mutableStateListOf<Uri>() }
    var selected by remember { mutableStateOf<Uri?>(null) }
    var enhanceTarget by remember { mutableStateOf<Uri?>(null) }
    var enhancedFile by remember { mutableStateOf<File?>(null) }
    var progress by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var advanced by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { chosen ->
        chosen.forEach { if (!photos.contains(it)) photos.add(it) }
    }
    val engine = remember { AIEngine(context.applicationContext) }

    MaterialTheme(colorScheme = lightColorScheme(primary = Blue, onPrimary = Color.White, background = Color.White, surface = Color.White)) {
        when {
            enhanceTarget != null -> {
                Scaffold(topBar = { TopAppBar(title = { Text("Nuestra Mirada AI", fontWeight = FontWeight.Bold) }, navigationIcon = {
                    IconButton(onClick = { enhanceTarget = null; enhancedFile = null; error = null }) { Icon(Icons.Default.ArrowBack, "Volver") }
                }) }) { padding ->
                    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (advanced) "Modo avanzado" else "Mejora automática", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("Real-ESRGAN x4plus · procesamiento local", color = Color.Gray, fontSize = 12.sp)
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = !advanced, onClick = { advanced = false }, label = { Text("Automático") })
                            FilterChip(selected = advanced, onClick = { advanced = true }, label = { Text("Avanzado") })
                        }
                        Spacer(Modifier.height(12.dp))
                        Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(22.dp)).background(Color(0xFF171522)), contentAlignment = Alignment.Center) {
                            AsyncImage(model = enhancedFile ?: enhanceTarget, contentDescription = "Vista previa", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                        }
                        Spacer(Modifier.height(14.dp))
                        if (advanced) {
                            Text("Primera prueba: superresolución 4× con el modelo Real-ESRGAN. Los controles adicionales se habilitarán cuando validemos el modelo.", color = Color.Gray, fontSize = 12.sp)
                            Spacer(Modifier.height(8.dp))
                        }
                        if (progress.isNotBlank()) Text(progress, color = Blue, fontSize = 13.sp)
                        error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
                        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                        Spacer(Modifier.height(10.dp))
                        Button(onClick = {
                            val uri = enhanceTarget ?: return@Button
                            busy = true; error = null; enhancedFile = null
                            scope.launch {
                                val result = engine.enhance(uri) { stage -> progress = stage }
                                result.onSuccess { enhancedFile = it; progress = "IA terminada: salida de prueba 4×" }
                                    .onFailure { error = it.message ?: "Falló el procesamiento del modelo."; progress = "Prueba fallida" }
                                busy = false
                            }
                        }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.AutoAwesome, null); Spacer(Modifier.width(8.dp)); Text(if (busy) "Procesando…" else if (enhancedFile == null) "Ejecutar IA" else "Volver a procesar")
                        }
                        if (enhancedFile != null) {
                            OutlinedButton(onClick = {
                                scope.launch {
                                    runCatching {
                                        val values = ContentValues().apply { put(MediaStore.Images.Media.DISPLAY_NAME, "NuestraMirada_IA_${System.currentTimeMillis()}.png"); put(MediaStore.Images.Media.MIME_TYPE, "image/png"); put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/NuestraMirada") }
                                        val dest = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: error("No se pudo crear el archivo en Galería")
                                        context.contentResolver.openOutputStream(dest)?.use { out -> enhancedFile!!.inputStream().use { it.copyTo(out) } } ?: error("No se pudo guardar")
                                    }.onSuccess { progress = "Fotografía guardada en Pictures/NuestraMirada" }.onFailure { error = it.message }
                                }
                            }, modifier = Modifier.fillMaxWidth()) { Text("Guardar resultado en Galería") }
                        }
                    }
                }
            }
            selected != null -> PhotoViewer(photos = photos.toList(), uri = selected!!, onSelect = { selected = it }, onBack = { selected = null }, onEnhance = { enhanceTarget = it })
            else -> Scaffold(
                topBar = { TopAppBar(title = { Column { Text("Nuestra Mirada", fontWeight = FontWeight.Bold, fontSize = 21.sp); Text("Captura. Explora. Mejora.", color = Color.Gray, fontSize = 12.sp) } }, actions = {
                    IconButton(onClick = { picker.launch("image/*") }) { Icon(Icons.Default.AddPhotoAlternate, "Añadir fotografías") }
                }) },
                floatingActionButton = { ExtendedFloatingActionButton(onClick = { picker.launch("image/*") }, containerColor = Blue, contentColor = Color.White, icon = { Icon(Icons.Default.AddPhotoAlternate, null) }, text = { Text("Añadir fotos") }) },
                bottomBar = { NavigationBar { NavigationBarItem(selected = true, onClick = {}, icon = { Icon(Icons.Default.PhotoLibrary, null) }, label = { Text("Galería") }); NavigationBarItem(selected = false, onClick = { photos.lastOrNull()?.let { enhanceTarget = it } }, icon = { Icon(Icons.Default.AutoAwesome, null) }, label = { Text("Mejorar") }) } }
            ) { padding ->
                Column(Modifier.fillMaxSize().padding(padding).background(Color.White)) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Fotografías", fontSize = 19.sp, fontWeight = FontWeight.SemiBold); Text("${photos.size} cargadas", color = Color.Gray, fontSize = 12.sp)
                    }
                    if (photos.isEmpty()) EmptyGallery { picker.launch("image/*") }
                    else LazyVerticalGrid(columns = GridCells.Fixed(3), contentPadding = PaddingValues(3.dp), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        items(photos) { uri -> AsyncImage(model = uri, contentDescription = "Fotografía", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(7.dp)).clickable { selected = uri }) }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyGallery(onAdd: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.Collections, null, tint = Blue, modifier = Modifier.size(64.dp)); Spacer(Modifier.height(16.dp))
        Text("Tu galería comienza aquí", fontSize = 20.sp, fontWeight = FontWeight.Bold); Text("Selecciona fotografías para probar la mejora con IA.", color = Color.Gray)
        Spacer(Modifier.height(18.dp)); Button(onClick = onAdd) { Text("Seleccionar fotografías") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhotoViewer(
    photos: List<Uri>,
    uri: Uri,
    onSelect: (Uri) -> Unit,
    onBack: () -> Unit,
    onEnhance: (Uri) -> Unit
) {
    val currentIndex = photos.indexOf(uri).coerceAtLeast(0)
    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { 64.dp.toPx() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fotografía ${currentIndex + 1} de ${photos.size}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 6.dp) {
                Row(
                    Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(onClick = {}, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.FavoriteBorder, null)
                        Text(" Favorito")
                    }
                    Button(onClick = { onEnhance(uri) }, modifier = Modifier.weight(1.4f)) {
                        Icon(Icons.Default.AutoAwesome, null)
                        Text(" Mejorar IA")
                    }
                }
            }
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF101018))
                .pointerInput(uri, photos.size) {
                    var dragX = 0f
                    var dragY = 0f
                    detectDragGestures(
                        onDragStart = {
                            dragX = 0f
                            dragY = 0f
                        },
                        onDragEnd = {
                            when {
                                dragY > swipeThresholdPx && dragY > abs(dragX) -> onBack()
                                abs(dragX) > swipeThresholdPx && abs(dragX) > abs(dragY) -> {
                                    if (dragX < 0 && currentIndex < photos.lastIndex) {
                                        onSelect(photos[currentIndex + 1])
                                    } else if (dragX > 0 && currentIndex > 0) {
                                        onSelect(photos[currentIndex - 1])
                                    }
                                }
                            }
                        },
                        onDrag = { change, amount ->
                            change.consume()
                            dragX += amount.x
                            dragY += amount.y
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = uri,
                contentDescription = "Fotografía",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
            Text(
                "Desliza a los lados para cambiar de foto · hacia abajo para salir",
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 11.sp,
                modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp)
            )
        }
    }
}
