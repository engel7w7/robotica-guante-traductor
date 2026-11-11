package bo.edu.lsb.quechua.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import bo.edu.lsb.quechua.engine.MultimodalMode
import bo.edu.lsb.quechua.model.ConnectionState
import bo.edu.lsb.quechua.theme.AppThemeMode
import bo.edu.lsb.quechua.theme.FingerIndexColor
import bo.edu.lsb.quechua.theme.FingerMiddleColor
import bo.edu.lsb.quechua.theme.FingerPinkyColor
import bo.edu.lsb.quechua.theme.FingerRingColor
import bo.edu.lsb.quechua.theme.FingerThumbColor
import bo.edu.lsb.quechua.viewmodel.MainViewModel
import bo.edu.lsb.quechua.viewmodel.SpeechMode
import bo.edu.lsb.quechua.vision.HandSkeletonTopology
import bo.edu.lsb.quechua.vision.MediaPipeHandLandmarkerHelper
import java.util.concurrent.Executors

/**
 * Pantalla de Visión Computacional por Cámara LSB con Reconocimiento Real MediaPipe 3D.
 * Incluye conmutador instantáneo entre Cámara Frontal/Trasera, cambio fluido entre Guante BLE / Cámara IA / Híbrido,
 * barras anatómicas de 5 dedos en vivo y selector de idioma bilingüe en Teleprompter.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraVisionScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onSwitchToGlove: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    val themeMode by viewModel.themeMode.collectAsState()
    val multimodalMode by viewModel.multimodalMode.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val speechMode by viewModel.speechMode.collectAsState()
    val translation by viewModel.translation.collectAsState()
    val recognitionResult by viewModel.recognitionResult.collectAsState()
    val debouncerProgress by viewModel.debouncerProgress.collectAsState()
    val handLandmarksResult by viewModel.handLandmarksResult.collectAsState()

    var showLandmarksOverlay by remember { mutableStateOf(true) }
    var cameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }
    var showSimulationStrip by remember { mutableStateOf(false) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    val handLandmarkerHelper = remember(context, cameraSelector) {
        MediaPipeHandLandmarkerHelper(
            context = context,
            isFrontCamera = (cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA),
            onResult = { result ->
                viewModel.updateHandLandmarks(result)
            },
            onError = { error ->
                viewModel.logSystem("[VISION IA] $error")
            }
        )
    }

    DisposableEffect(handLandmarkerHelper) {
        onDispose {
            handLandmarkerHelper.close()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetCameraState()
            cameraExecutor.shutdown()
        }
    }

    val previewView = remember(context) {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    // Vincular CameraX únicamente cuando cambie el permiso, lente o helper (sin re-vincular por frame)
    LaunchedEffect(hasCameraPermission, cameraSelector, lifecycleOwner, handLandmarkerHelper, previewView) {
        if (!hasCameraPermission) return@LaunchedEffect
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, handLandmarkerHelper)
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                viewModel.logSystem("[CÁMARA] Error al iniciar lente: ${e.localizedMessage}")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    val testGestures = listOf(
        Triple("WAVE_HELLO", "Hola (Saludo)", 0.9f),
        Triple("CUP_HAND", "Agua (Copa)", 0.65f),
        Triple("THUMB_UP", "Bien (Pulgar)", 0.35f),
        Triple("INDEX_POINT", "Tú / Señalar", 0.3f),
        Triple("TOUCH_CHEST", "Yo (Pecho)", 0.45f),
        Triple("TOUCH_HEAD", "Cabeza", 0.45f),
        Triple("FIST", "Ayuda (Puño)", 0.05f)
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (multimodalMode == MultimodalMode.HYBRID_FUSION) {
                                "Fusión Híbrida • Cámara IA"
                            } else {
                                "Cámara MediaPipe 3D"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FiberManualRecord,
                                contentDescription = null,
                                tint = if (handLandmarksResult.isHandDetected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(9.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (handLandmarksResult.isHandDetected) {
                                    "${if (cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA) "Frontal" else "Trasera"} · ${handLandmarksResult.fps.toInt()} FPS"
                                } else {
                                    "${if (cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA) "Frontal" else "Trasera"} · Enfoca la mano"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = if (handLandmarksResult.isHandDetected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showLandmarksOverlay = !showLandmarksOverlay }) {
                        Icon(
                            imageVector = if (showLandmarksOverlay) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Mostrar u ocultar landmarks",
                            tint = if (showLandmarksOverlay) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = { viewModel.toggleThemeMode() }) {
                        Icon(
                            imageVector = if (themeMode == AppThemeMode.DARK) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Cambiar tema",
                            tint = if (themeMode == AppThemeMode.DARK) Color(0xFFFFD54F) else Color(0xFF4F46E5)
                        )
                    }

                    if (hasCameraPermission) {
                        IconButton(onClick = {
                            cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                                CameraSelector.DEFAULT_FRONT_CAMERA
                            } else {
                                CameraSelector.DEFAULT_BACK_CAMERA
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Cameraswitch,
                                contentDescription = "Cambiar cámara frontal / trasera",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (hasCameraPermission) {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize()
                )

                // Capa de Dibujo de los 21 Landmarks reales
                if (showLandmarksOverlay) {
                    val primaryAccent = MaterialTheme.colorScheme.primary
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        val landmarks = handLandmarksResult.landmarks

                        fun projectPoint(normX: Float, normY: Float): Offset {
                            val imgW = handLandmarksResult.imageWidth.toFloat()
                            val imgH = handLandmarksResult.imageHeight.toFloat()
                            if (imgW <= 0f || imgH <= 0f) {
                                return Offset(normX * canvasWidth, normY * canvasHeight)
                            }
                            val scale = maxOf(canvasWidth / imgW, canvasHeight / imgH)
                            val offsetX = (canvasWidth - imgW * scale) / 2f
                            val offsetY = (canvasHeight - imgH * scale) / 2f
                            return Offset((normX * imgW) * scale + offsetX, (normY * imgH) * scale + offsetY)
                        }

                        if (handLandmarksResult.isHandDetected && landmarks.size >= 21) {
                            val box = handLandmarksResult.boundingBox
                            if (box != null) {
                                val pTopLeft = projectPoint(box.left, box.top)
                                val pBottomRight = projectPoint(box.right, box.bottom)
                                val bx = pTopLeft.x
                                val by = pTopLeft.y
                                val bw = (pBottomRight.x - pTopLeft.x).coerceAtLeast(10f)
                                val bh = (pBottomRight.y - pTopLeft.y).coerceAtLeast(10f)

                                drawRoundRect(
                                    color = primaryAccent.copy(alpha = 0.35f),
                                    topLeft = Offset(bx, by),
                                    size = Size(bw, bh),
                                    cornerRadius = CornerRadius(18f, 18f),
                                    style = Stroke(width = 2.5f)
                                )

                                val cLen = 22f
                                val sW = 4.5f
                                drawLine(primaryAccent, Offset(bx, by), Offset(bx + cLen, by), sW)
                                drawLine(primaryAccent, Offset(bx, by), Offset(bx, by + cLen), sW)
                                drawLine(primaryAccent, Offset(bx + bw, by), Offset(bx + bw - cLen, by), sW)
                                drawLine(primaryAccent, Offset(bx + bw, by), Offset(bx + bw, by + cLen), sW)
                                drawLine(primaryAccent, Offset(bx, by + bh), Offset(bx + cLen, by + bh), sW)
                                drawLine(primaryAccent, Offset(bx, by + bh), Offset(bx, by + bh - cLen), sW)
                                drawLine(primaryAccent, Offset(bx + bw, by + bh), Offset(bx + bw - cLen, by + bh), sW)
                                drawLine(primaryAccent, Offset(bx + bw, by + bh), Offset(bx + bw, by + bh - cLen), sW)
                            }

                            for ((startIdx, endIdx) in HandSkeletonTopology.CONNECTIONS) {
                                if (startIdx < landmarks.size && endIdx < landmarks.size) {
                                    val p1 = projectPoint(landmarks[startIdx].x, landmarks[startIdx].y)
                                    val p2 = projectPoint(landmarks[endIdx].x, landmarks[endIdx].y)

                                    val boneColor = when {
                                        startIdx in HandSkeletonTopology.THUMB_INDICES && endIdx in HandSkeletonTopology.THUMB_INDICES -> FingerThumbColor
                                        startIdx in HandSkeletonTopology.INDEX_INDICES && endIdx in HandSkeletonTopology.INDEX_INDICES -> FingerIndexColor
                                        startIdx in HandSkeletonTopology.MIDDLE_INDICES && endIdx in HandSkeletonTopology.MIDDLE_INDICES -> FingerMiddleColor
                                        startIdx in HandSkeletonTopology.RING_INDICES && endIdx in HandSkeletonTopology.RING_INDICES -> FingerRingColor
                                        startIdx in HandSkeletonTopology.PINKY_INDICES && endIdx in HandSkeletonTopology.PINKY_INDICES -> FingerPinkyColor
                                        else -> Color(0xFF38BDF8).copy(alpha = 0.85f)
                                    }

                                    drawLine(color = boneColor, start = p1, end = p2, strokeWidth = 5.5f)
                                }
                            }

                            for (i in landmarks.indices) {
                                val center = projectPoint(landmarks[i].x, landmarks[i].y)
                                val nodeColor = when (i) {
                                    HandSkeletonTopology.WRIST_INDEX -> Color.White
                                    in HandSkeletonTopology.THUMB_INDICES -> FingerThumbColor
                                    in HandSkeletonTopology.INDEX_INDICES -> FingerIndexColor
                                    in HandSkeletonTopology.MIDDLE_INDICES -> FingerMiddleColor
                                    in HandSkeletonTopology.RING_INDICES -> FingerRingColor
                                    in HandSkeletonTopology.PINKY_INDICES -> FingerPinkyColor
                                    else -> Color.Cyan
                                }
                                val isTip = i in HandSkeletonTopology.FINGERTIP_INDICES
                                val radius = if (isTip) 8.5f else (if (i == 0) 9.5f else 6.5f)

                                drawCircle(color = nodeColor.copy(alpha = 0.35f), radius = radius * 2.1f, center = center)
                                drawCircle(color = nodeColor, radius = radius, center = center)
                                drawCircle(color = Color.White, radius = radius * 0.42f, center = center)
                            }
                        } else {
                            val guideWidth = canvasWidth * 0.68f
                            val guideHeight = canvasHeight * 0.42f
                            val gLeft = (canvasWidth - guideWidth) / 2f
                            val gTop = (canvasHeight - guideHeight) / 3.2f
                            drawRoundRect(
                                color = Color.White.copy(alpha = 0.25f),
                                topLeft = Offset(gLeft, gTop),
                                size = Size(guideWidth, guideHeight),
                                cornerRadius = CornerRadius(20f, 20f),
                                style = Stroke(width = 2f)
                            )
                        }
                    }
                }

                // HUD Superior Minimalista
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Selector de modo rápido y reversible
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val modes = listOf(
                            Triple(MultimodalMode.GLOVE_ONLY, "Guante BLE ⇄", true),
                            Triple(MultimodalMode.CAMERA_ONLY, "Cámara IA", false),
                            Triple(MultimodalMode.HYBRID_FUSION, "Híbrido", false)
                        )
                        modes.forEach { (mode, label, openGlove) ->
                            val isSelected = multimodalMode == mode
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.28f),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        if (openGlove) {
                                            onSwitchToGlove()
                                        } else {
                                            viewModel.setMultimodalMode(mode)
                                        }
                                    },
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.65f)
                            ) {
                                Text(
                                    text = label,
                                    modifier = Modifier.padding(vertical = 7.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.White,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // Badge Compacto de Reconocimiento + Progreso Debouncer
                    Surface(
                        color = Color.Black.copy(alpha = 0.72f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Psychology,
                                    contentDescription = null,
                                    tint = if (recognitionResult.gloss != "NINGUNA") Color(0xFF34D399) else Color(0xFF94A3B8),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (recognitionResult.gloss != "NINGUNA") {
                                        "Seña: ${recognitionResult.gloss} (${(recognitionResult.confidence * 100).toInt()}%)"
                                    } else if (handLandmarksResult.isHandDetected) {
                                        "Gesto: ${handLandmarksResult.gesture} · Mantén la seña"
                                    } else {
                                        if (multimodalMode == MultimodalMode.HYBRID_FUSION && connectionState is ConnectionState.Connected) {
                                            "BLE conectado · Esperando mano en cuadro"
                                        } else {
                                            "MediaPipe IA activo · Esperando mano"
                                        }
                                    },
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )

                                if (debouncerProgress > 0f) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    LinearProgressIndicator(
                                        progress = { debouncerProgress },
                                        modifier = Modifier
                                            .width(42.dp)
                                            .height(5.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = Color(0xFFFBBF24),
                                        trackColor = Color.White.copy(alpha = 0.22f)
                                    )
                                }
                            }

                            // Mini barras en vivo de los 5 dedos (3D MediaPipe) cuando hay mano
                            if (handLandmarksResult.isHandDetected) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val names = listOf("P", "Í", "M", "A", "Me")
                                    val colors = listOf(FingerThumbColor, FingerIndexColor, FingerMiddleColor, FingerRingColor, FingerPinkyColor)
                                    for (i in 0 until 5) {
                                        val pct = handLandmarksResult.fingerFlex.getOrElse(i) { 0 }.coerceIn(0, 100)
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = names[i],
                                                color = Color.White.copy(alpha = 0.85f),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(4.dp)
                                                    .clip(RoundedCornerShape(2.dp))
                                                    .background(Color.White.copy(alpha = 0.18f))
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxHeight()
                                                        .fillMaxWidth(pct / 100f)
                                                        .background(colors[i])
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Desplegable opcional de pruebas rápidas
                    OutlinedButton(
                        onClick = { showSimulationStrip = !showSimulationStrip },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White,
                            containerColor = Color.Black.copy(alpha = 0.55f)
                        ),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Text(
                            text = if (showSimulationStrip) "Ocultar pruebas manuales" else "Pruebas manuales",
                            fontSize = 10.sp
                        )
                    }

                    AnimatedVisibility(visible = showSimulationStrip) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            testGestures.forEach { (descriptor, label, openness) ->
                                OutlinedButton(
                                    onClick = { viewModel.updateCameraVision(descriptor, openness, 0f) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color.White,
                                        containerColor = Color.Black.copy(alpha = 0.72f)
                                    ),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text(text = label, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }

                // Tarjeta Inferior: Teleprompter Minimalista Bilingüe
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        .align(Alignment.BottomCenter)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Subtitles,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "TELEPROMPTER",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 0.8.sp
                                )
                            }

                            // Selector compacto de idioma TTS: ES | QU | ES+QU
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(
                                    SpeechMode.SPANISH to "ES",
                                    SpeechMode.QUECHUA to "QU",
                                    SpeechMode.BILINGUAL to "ES+QU"
                                ).forEach { (mode, chipText) ->
                                    val selected = speechMode == mode
                                    Surface(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .border(
                                                1.dp,
                                                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                                RoundedCornerShape(6.dp)
                                            )
                                            .clickable { viewModel.setSpeechMode(mode) },
                                        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                    ) {
                                        Text(
                                            text = chipText,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = if (translation.isEmpty) {
                                if (handLandmarksResult.isHandDetected) {
                                    "Mano en foco. Realiza una seña sostenida (~0.5 s)..."
                                } else {
                                    "Apunta la cámara hacia la mano de quien realiza señas..."
                                }
                            } else {
                                "ES: ${translation.spanishSentence}"
                            },
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = if (translation.isEmpty) "Suyakuchkan makita..." else "QU: ${translation.quechuaSentence}",
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (translation.quechuaPhonetics.isNotBlank() && translation.quechuaPhonetics != translation.quechuaSentence) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "[ ${translation.quechuaPhonetics} ]",
                                color = MaterialTheme.colorScheme.tertiary,
                                style = MaterialTheme.typography.bodySmall,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!translation.isEmpty) {
                                OutlinedButton(
                                    onClick = { viewModel.removeLastGloss() },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(36.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Deshacer", modifier = Modifier.size(16.dp))
                                }

                                OutlinedButton(
                                    onClick = { viewModel.clearSequence() },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(36.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Icon(Icons.Default.Clear, contentDescription = "Limpiar", modifier = Modifier.size(16.dp))
                                }
                            }

                            Button(
                                onClick = { viewModel.speakCurrentSentence() },
                                enabled = !translation.isEmpty,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Escuchar (${speechMode.badge})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Permiso de Cámara Requerido",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Rimay requiere acceso a la cámara frontal y trasera para el seguimiento 3D de los 21 landmarks con MediaPipe IA.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(text = "Conceder Permiso", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
