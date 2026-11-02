package com.nuestramirada.gallery

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

private val Ink = Color(0xFF17191C)
private val Blue = Color(0xFF2864DC)
private val SurfaceSoft = Color(0xFFF3F5F8)

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
    var showInfo by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { chosen ->
        chosen.forEach { uri -> if (!photos.contains(uri)) photos.add(uri) }
    }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Blue,
            onPrimary = Color.White,
            background = Color.White,
            surface = Color.White,
            onSurface = Ink
        )
    ) {
        if (selected != null) {
            PhotoViewer(
                uri = selected!!,
                onBack = { selected = null },
                onEnhance = { showInfo = true }
            )
        } else {
            Scaffold(
                containerColor = Color.White,
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text("Nuestra Mirada", fontWeight = FontWeight.Bold, fontSize = 21.sp)
                                Text("Tus recuerdos, en un solo lugar", color = Color.Gray, fontSize = 12.sp)
                            }
                        },
                        actions = {
                            IconButton(onClick = { picker.launch("image/*") }) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Añadir fotos")
                            }
                            IconButton(onClick = { showInfo = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Más información")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                    )
                },
                floatingActionButton = {
                    ExtendedFloatingActionButton(
                        onClick = { picker.launch("image/*") },
                        containerColor = Blue,
                        contentColor = Color.White,
                        icon = { Icon(Icons.Default.AddPhotoAlternate, null) },
                        text = { Text("Añadir fotos") }
                    )
                },
                bottomBar = {
                    NavigationBar(containerColor = Color.White) {
                        NavigationBarItem(
                            selected = true,
                            onClick = {},
                            icon = { Icon(Icons.Default.PhotoLibrary, null) },
                            label = { Text("Galería") }
                        )
                        NavigationBarItem(
                            selected = false,
                            onClick = { showInfo = true },
                            icon = { Icon(Icons.Default.AutoAwesome, null) },
                            label = { Text("Mejorar") }
                        )
                    }
                }
            ) { padding ->
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding)
                        .background(Color.White)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Fotografías", fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                        Text("${photos.size} seleccionadas", color = Color.Gray, fontSize = 12.sp)
                    }

                    if (photos.isEmpty()) {
                        EmptyGallery(onAdd = { picker.launch("image/*") })
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            contentPadding = PaddingValues(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            items(photos) { uri ->
                                AsyncImage(
                                    model = uri,
                                    contentDescription = "Fotografía",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(5.dp))
                                        .clickable { selected = uri }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showInfo) {
            AlertDialog(
                onDismissRequest = { showInfo = false },
                title = { Text("Mejora con inteligencia artificial") },
                text = {
                    Text(
                        "El motor de superresolución local está en desarrollo. " +
                        "Esta versión permite seleccionar y visualizar fotografías; " +
                        "todavía no aplica una mejora por IA."
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showInfo = false }) { Text("Entendido") }
                }
            )
        }
    }
}

@Composable
private fun EmptyGallery(onAdd: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = SurfaceSoft,
            modifier = Modifier.size(104.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Collections, null, tint = Blue, modifier = Modifier.size(48.dp))
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("Tu galería comienza aquí", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Selecciona fotografías de tu teléfono para verlas en Nuestra Mirada.",
            color = Color.Gray,
            fontSize = 14.sp
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onAdd) { Text("Seleccionar fotografías") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhotoViewer(uri: Uri, onBack: () -> Unit, onEnhance: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fotografía") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 6.dp, color = Color.White) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(onClick = onEnhance, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.AutoAwesome, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Mejorar con IA")
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding).background(Color(0xFFF4F4F4)),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = uri,
                contentDescription = "Fotografía seleccionada",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().fillMaxHeight()
            )
        }
    }
}
