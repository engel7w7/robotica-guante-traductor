package bo.edu.lsb.quechua.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bo.edu.lsb.quechua.engine.GloveVocabScope
import bo.edu.lsb.quechua.engine.MorphologyEngine
import bo.edu.lsb.quechua.engine.MultimodalFusionEngine
import bo.edu.lsb.quechua.engine.MultimodalMode
import bo.edu.lsb.quechua.model.ConnectionState
import bo.edu.lsb.quechua.model.GloveTelemetry
import bo.edu.lsb.quechua.model.MorphologyItem
import bo.edu.lsb.quechua.theme.AppThemeMode
import bo.edu.lsb.quechua.theme.FingerIndexColor
import bo.edu.lsb.quechua.theme.FingerMiddleColor
import bo.edu.lsb.quechua.theme.FingerPinkyColor
import bo.edu.lsb.quechua.theme.FingerRingColor
import bo.edu.lsb.quechua.theme.FingerThumbColor
import bo.edu.lsb.quechua.ui.components.BluetoothDevicePickerModal
import bo.edu.lsb.quechua.ui.components.HandTelemetryVisualizer
import bo.edu.lsb.quechua.viewmodel.MainViewModel
import bo.edu.lsb.quechua.viewmodel.SpeechMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Pestañas modulares para separar las vistas de la aplicación.
 * Divide en 3 experiencias dedicadas:
 * 1. Intérprete: Diálogo, traducción SOV/SVO y síntesis de voz.
 * 2. Sensores Hall: Diagnóstico de hardware, visualizador y canales SS49E.
 * 3. Paleta de Señas: Catálogo táctil categorizado para aprendizaje y composición.
 */
enum class TranslatorTab(val title: String, val icon: ImageVector) {
    INTERPRETER("Intérprete", Icons.AutoMirrored.Filled.Chat),
    SENSORS("Sensores Hall", Icons.Default.Sensors),
    PALETTE("Paleta de Señas", Icons.Default.GridOn)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslatorScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onSwitchToCamera: (MultimodalMode) -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(TranslatorTab.INTERPRETER) }
    val themeMode by viewModel.themeMode.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val multimodalMode by viewModel.multimodalMode.collectAsState()
    val isConnected = connectionState is ConnectionState.Connected
    val isConnectingOrScanning = connectionState is ConnectionState.Scanning || connectionState is ConnectionState.Connecting
    val isCalibrationOpen by viewModel.isCalibrationDialogOpen.collectAsState()
    val calibrationProfile by viewModel.calibrationProfile.collectAsState()
    var isDevicePickerOpen by remember { mutableStateOf(false) }

    // Diálogo Modal de Calibración Paso Cero
    if (isCalibrationOpen) {
        bo.edu.lsb.quechua.ui.components.PasoCeroCalibrationDialog(
            viewModel = viewModel,
            onDismissRequest = { viewModel.closeCalibrationDialog() }
        )
    }

    // Selector en vivo de dispositivos BLE
    if (isDevicePickerOpen) {
        BluetoothDevicePickerModal(
            viewModel = viewModel,
            onDismissRequest = { isDevicePickerOpen = false }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Rimay LSB • Intérprete",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = when (selectedTab) {
                                TranslatorTab.INTERPRETER -> "Traducción Bilingüe Quechua / Español"
                                TranslatorTab.SENSORS -> "Sensores Hall SS49E & MPU-6050"
                                TranslatorTab.PALETTE -> "Composición Táctil y Vocabulario"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver al inicio",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.openCalibrationDialog() }) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Paso 0: Calibración",
                            tint = if (calibrationProfile.isCalibrated) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        )
                    }

                    IconButton(onClick = { viewModel.toggleThemeMode() }) {
                        Icon(
                            imageVector = if (themeMode == AppThemeMode.LIGHT) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = "Alternar modo visual",
                            tint = if (themeMode == AppThemeMode.DARK) Color(0xFFFFD54F) else Color(0xFF4F46E5)
                        )
                    }

                    ElevatedButton(
                        onClick = {
                            if (isConnected || isConnectingOrScanning) {
                                viewModel.disconnectGlove()
                            } else {
                                viewModel.connectGlove()
                            }
                        },
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isConnected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.padding(end = 8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (isConnected) Icons.Default.BluetoothConnected else Icons.Default.Bluetooth,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when (connectionState) {
                                is ConnectionState.Connected -> "Conectado"
                                is ConnectionState.Scanning -> "Buscando..."
                                is ConnectionState.Connecting -> "Enlazando..."
                                else -> "Conectar"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // CAMBIO FLUIDO REVERSIBLE DE MODO (Sin perder conexión BLE ni secuencia)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val options = listOf(
                    Triple(MultimodalMode.GLOVE_ONLY, "Guante BLE", false),
                    Triple(MultimodalMode.CAMERA_ONLY, "Cámara IA ⇄", true),
                    Triple(MultimodalMode.HYBRID_FUSION, "Híbrido ⇄", true)
                )
                options.forEach { (mode, label, opensCamera) ->
                    val isSelected = multimodalMode == mode
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                if (opensCamera) {
                                    onSwitchToCamera(mode)
                                } else {
                                    viewModel.setMultimodalMode(mode)
                                }
                            },
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier.padding(vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // ALERTA DE ESTADO BLE Y BOTÓN DIRECTO PARA ELEGIR ESP32
            if (!isConnected && connectionState !is ConnectionState.SimulationMode) {
                val (bannerBg, bannerTint, bannerMsg) = when (val st = connectionState) {
                    is ConnectionState.Scanning -> Triple(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        MaterialTheme.colorScheme.primary,
                        "Escaneando señal BLE 'Guante_ESP32'..."
                    )
                    is ConnectionState.Connecting -> Triple(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        MaterialTheme.colorScheme.primary,
                        "Enlazando GATT/CCCD con ${st.deviceName}..."
                    )
                    is ConnectionState.Error -> Triple(
                        MaterialTheme.colorScheme.errorContainer,
                        MaterialTheme.colorScheme.onErrorContainer,
                        st.message
                    )
                    else -> Triple(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        MaterialTheme.colorScheme.onSurfaceVariant,
                        "Guante BLE sin conectar"
                    )
                }

                Surface(
                    color = bannerBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isConnectingOrScanning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(13.dp),
                                    strokeWidth = 2.dp,
                                    color = bannerTint
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.BluetoothSearching,
                                    contentDescription = null,
                                    tint = bannerTint,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = bannerMsg,
                                fontSize = 11.sp,
                                color = bannerTint,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (connectionState is ConnectionState.Error) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { viewModel.connectGlove() }
                                ) {
                                    Text(
                                        text = "Reintentar",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { isDevicePickerOpen = true }
                            ) {
                                Text(
                                    text = "Elegir ESP32",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ALERTA DE PASO CERO: Si el guante no ha sido calibrado en esta postura
            if (!calibrationProfile.isCalibrated) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.openCalibrationDialog() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Sensors, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Paso 0 Pendiente: Calibra el guante a tu mano",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = "Calibrar ➔",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // SELECTOR DE PESTAÑAS (SEGMENTED CONTROL MODERNO)
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                        color = MaterialTheme.colorScheme.primary,
                        height = 3.dp
                    )
                },
                divider = {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                }
            ) {
                TranslatorTab.values().forEach { tab ->
                    val isSelected = selectedTab == tab
                    Tab(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = tab.title,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )
                }
            }

            // CONTENIDO DE LA PESTAÑA SELECCIONADA
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                when (selectedTab) {
                    TranslatorTab.INTERPRETER -> {
                        InterpreterTabView(
                            viewModel = viewModel,
                            onGoToPalette = { selectedTab = TranslatorTab.PALETTE }
                        )
                    }
                    TranslatorTab.SENSORS -> {
                        HallSensorsTabView(
                            viewModel = viewModel,
                            onOpenDevicePicker = { isDevicePickerOpen = true }
                        )
                    }
                    TranslatorTab.PALETTE -> {
                        SignPaletteTabView(
                            viewModel = viewModel,
                            onGoToInterpreter = { selectedTab = TranslatorTab.INTERPRETER }
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
// PESTAÑA 1: INTÉRPRETE (COMUNICACIÓN BILINGÜE SVO/SOV)
// =========================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InterpreterTabView(
    viewModel: MainViewModel,
    onGoToPalette: () -> Unit
) {
    val translation by viewModel.translation.collectAsState()
    val glossSequence by viewModel.glossSequence.collectAsState()
    val speechMode by viewModel.speechMode.collectAsState()
    val multimodalMode by viewModel.multimodalMode.collectAsState()
    val gloveVocabScope by viewModel.gloveVocabScope.collectAsState()
    val autoSpeakOnPause by viewModel.autoSpeakOnPause.collectAsState()
    val activeTtsVoiceLabel by viewModel.activeTtsVoiceLabel.collectAsState()
    val recognition by viewModel.recognitionResult.collectAsState()
    val debouncerProgress by viewModel.debouncerProgress.collectAsState()
    val candidateGloss by viewModel.candidateGloss.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var hasCopied by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // BARRA DE ESTADO MULTIMODAL & SEÑA ACTIVA
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "MODO: ${multimodalMode.label.uppercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 10.sp
                        )
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (recognition.fusionConfidence > 0f) "${(recognition.fusionConfidence * 100).toInt()}% Certeza" else "En espera",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 10.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Controles rápidos del reconocedor del guante (Esencial vs Completo + Auto-voz)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                val next = if (gloveVocabScope == GloveVocabScope.ESSENTIAL) {
                                    GloveVocabScope.FULL
                                } else {
                                    GloveVocabScope.ESSENTIAL
                                }
                                viewModel.setGloveVocabScope(next)
                            }
                    ) {
                        Text(
                            text = "Guante: ${gloveVocabScope.shortLabel}",
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.secondary,
                            fontSize = 10.sp
                        )
                    }

                    Surface(
                        color = if (autoSpeakOnPause) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .border(
                                1.dp,
                                if (autoSpeakOnPause) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(6.dp)
                            )
                            .clickable { viewModel.setAutoSpeakOnPause(!autoSpeakOnPause) }
                    ) {
                        Text(
                            text = if (autoSpeakOnPause) "Auto-voz: ON" else "Auto-voz: OFF",
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (autoSpeakOnPause) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = "Voz $activeTtsVoiceLabel",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Gesto detectado: ${recognition.gloss}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (candidateGloss != "NINGUNA" && candidateGloss != recognition.gloss) {
                        val stepsDone = (debouncerProgress * viewModel.debouncerThreshold).roundToInt()
                            .coerceIn(0, viewModel.debouncerThreshold)
                        Text(
                            text = "Estabilizando: $candidateGloss ($stepsDone/${viewModel.debouncerThreshold})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontSize = 11.sp
                        )
                    }
                }

                val activeGlossKey = if (candidateGloss != "NINGUNA") candidateGloss else recognition.gloss
                val activePostureGuide = remember(activeGlossKey) {
                    MorphologyEngine.lexicon[activeGlossKey]?.let { MultimodalFusionEngine.describeGlovePosture(it) }
                }
                if (activePostureGuide != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Postura ($activeGlossKey): $activePostureGuide",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (debouncerProgress > 0f && candidateGloss != "NINGUNA") {
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { debouncerProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }

        // SECUENCIA DE GLOSAS ACTIVAS (CON BOTÓN DE BORRADO INDIVIDUAL)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Secuencia de Señas (${glossSequence.size}):",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (glossSequence.isNotEmpty()) {
                            OutlinedButton(
                                onClick = { viewModel.removeLastGloss() },
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Undo,
                                    contentDescription = "Deshacer última seña",
                                    modifier = Modifier.size(13.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(text = "Deshacer", fontSize = 11.sp)
                            }
                        }

                        OutlinedButton(
                            onClick = onGoToPalette,
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(text = "Agregar Seña", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (glossSequence.isEmpty()) {
                    Text(
                        text = "Realiza señas con el guante, enfoca la cámara o toca 'Agregar Seña'...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        glossSequence.forEachIndexed { index, gloss ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.border(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                    RoundedCornerShape(8.dp)
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(start = 8.dp, end = 4.dp, top = 3.dp, bottom = 3.dp)
                                ) {
                                    Text(
                                        text = gloss,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(
                                        onClick = { viewModel.removeGlossAt(index) },
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Quitar seña",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // TARJETA PRINCIPAL DE TRADUCCIÓN BILINGÜE ANDINA
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp)),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Barra superior: Etiqueta y botón Copiar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (translation.isEmpty) "SIN SEÑAS ACTIVAS" else "TRADUCCIÓN EN VIVO",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.5.sp,
                            fontSize = 10.sp
                        )
                    }

                    IconButton(
                        onClick = {
                            val textToCopy = "Español: ${translation.spanishSentence}\nQuechua: ${translation.quechuaSentence}"
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Traducción Rimay LSB", textToCopy))
                            hasCopied = true
                            Toast.makeText(context, "Traducción copiada", Toast.LENGTH_SHORT).show()
                            coroutineScope.launch {
                                delay(2000)
                                hasCopied = false
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (hasCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = "Copiar traducción",
                            tint = if (hasCopied) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ORACIÓN EN ESPAÑOL (SVO)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "ESPAÑOL (SVO)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp,
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = translation.spanishSentence.ifEmpty { "---" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(12.dp))

                // ORACIÓN EN QUECHUA BOLIVIANO (SOV)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "QUECHUA BOLIVIANO (SOV - SUFIJOS)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = translation.quechuaSentence.ifEmpty { "---" },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 22.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    val ttsSpeechText = remember(translation.quechuaSentence) {
                        MorphologyEngine.toTtsPhoneticSpeech(translation.quechuaSentence)
                    }
                    if (ttsSpeechText.isNotBlank() && ttsSpeechText != translation.quechuaSentence) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Pronunciación TTS: [ $ttsSpeechText ]",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                // Visualizador de onda de audio cuando habla
                AnimatedVisibility(visible = isSpeaking) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = 10.dp)
                    ) {
                        AudioWaveformVisualizer()
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Locución activa (${speechMode.label})...",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 10.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Explicación morfosintáctica
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = translation.ruleExplanation,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // CONTROLES DE HABLA & ACCIONES ERGONÓMICAS
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Selector de Modo de Voz
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SpeechMode.values().forEach { mode ->
                        val isSelected = speechMode == mode
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { viewModel.setSpeechMode(mode) },
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = mode.label,
                                modifier = Modifier.padding(vertical = 7.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                // Botones de acción principales
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.clearSequence() },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Limpiar secuencia",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Limpiar", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = { viewModel.speakCurrentSentence() },
                        enabled = !translation.isEmpty,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1.4f)
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = if (isSpeaking) Icons.Default.GraphicEq else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Pronunciar con voz sintetizada",
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isSpeaking) "HABLANDO" else "HABLAR",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
// PESTAÑA 2: SENSORES HALL (LABORATORIO DE TELEMETRÍA Y CALIBRACIÓN)
// =========================================================================
@Composable
private fun HallSensorsTabView(
    viewModel: MainViewModel,
    onOpenDevicePicker: () -> Unit
) {
    val telemetry by viewModel.telemetry.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val isConnected = connectionState is ConnectionState.Connected
    val isSimulating = connectionState is ConnectionState.SimulationMode

    val fingerNames = listOf("Pulgar (CH2)", "Índice (CH1)", "Medio (CH0)", "Anular (CH3)", "Meñique (G34)")
    val fingerColors = listOf(
        FingerThumbColor,
        FingerIndexColor,
        FingerMiddleColor,
        FingerRingColor,
        FingerPinkyColor
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // PANEL DE CONTROL DE HARDWARE ESP32
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Guante LSB • ESP32 + SS49E",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = when (val st = connectionState) {
                                is ConnectionState.Connected -> "Conectado (${st.deviceName}) · 10 Hz"
                                is ConnectionState.Scanning -> "Buscando BLE..."
                                is ConnectionState.Connecting -> "Enlazando ${st.deviceName}..."
                                is ConnectionState.SimulationMode -> "Simulador activo · 10 Hz (100ms)"
                                is ConnectionState.Error -> "Error: ${st.message}"
                                is ConnectionState.Disconnected -> "Desconectado"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            color = when (connectionState) {
                                is ConnectionState.Connected -> MaterialTheme.colorScheme.primary
                                is ConnectionState.Error -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenDevicePicker,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Text(text = "Elegir ESP32", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = { viewModel.toggleSimulationMode() },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Text(
                            text = if (isSimulating) "Parar Sim" else "Simular",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = {
                            when (connectionState) {
                                is ConnectionState.Connected,
                                is ConnectionState.Scanning,
                                is ConnectionState.Connecting -> viewModel.disconnectGlove()
                                else -> viewModel.connectGlove()
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(30.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isConnected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = when (connectionState) {
                                is ConnectionState.Connected -> "Desconectar"
                                is ConnectionState.Scanning -> "Buscando..."
                                is ConnectionState.Connecting -> "Enlazando..."
                                else -> "Conectar"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // ESQUEMA ANATÓMICO 3D INTERACTIVO
        HandTelemetryVisualizer(
            flexValues = telemetry.flex,
            touchValues = telemetry.touch,
            pitch = telemetry.imuPitch,
            roll = telemetry.imuRoll
        )

        // BARRAS DE CALIBRACIÓN DE LOS 5 SENSORES HALL LINEALES
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Calibración Lineal SS49E (0% a 100%)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "G: ${telemetry.gloss}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 10.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                for (i in 0 until 5) {
                    val flexValue = telemetry.flex.getOrElse(i) { 0 }
                    val animatedValue by animateFloatAsState(
                        targetValue = flexValue / 100f,
                        label = "fingerFlexAnim_$i"
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = fingerNames[i],
                            modifier = Modifier.width(96.dp),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        LinearProgressIndicator(
                            progress = { animatedValue },
                            modifier = Modifier
                                .weight(1f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = fingerColors[i],
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "$flexValue%",
                            modifier = Modifier.width(36.dp),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.End,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )

                // IMU Y ESTADO BLE DEL GUANTE (sin chips fantasma T0/T1/T2 si no hay electrodos)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "GIROSCOPIO MPU-6050",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "P: ${String.format(Locale.US, "%.1f", telemetry.imuPitch)}° | R: ${String.format(Locale.US, "%.1f", telemetry.imuRoll)}°",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp
                        )
                    }

                    val hasAnyTouch = telemetry.touch.any { it != 0 }
                    if (hasAnyTouch) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Touch:",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            for (tIndex in 0 until 3) {
                                val isActive = telemetry.touch.getOrElse(tIndex) { 0 } == 1
                                Surface(
                                    color = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.border(
                                        1.dp,
                                        if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                        RoundedCornerShape(4.dp)
                                    )
                                ) {
                                    Text(
                                        text = "T$tIndex",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "ESP32 · BLE 100ms",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// PESTAÑA 3: PALETA DE SEÑAS (CATÁLOGO TÁCTIL Y COMPOSICIÓN)
// =========================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SignPaletteTabView(
    viewModel: MainViewModel,
    onGoToInterpreter: () -> Unit
) {
    val allItems = remember { MorphologyEngine.lexicon.values.toList() }
    val categories = listOf(
        "Todos",
        "Esenciales (Guante)",
        "Personalizadas",
        "Pronombres",
        "Familia",
        "Alimentos",
        "Cuerpo & Salud",
        "Verbos",
        "Lugares",
        "Tiempo & Clima",
        "Colores",
        "Social"
    )
    var selectedCategory by remember { mutableStateOf("Todos") }
    var searchQuery by remember { mutableStateOf("") }
    val glossSequence by viewModel.glossSequence.collectAsState()
    val customSignatures by viewModel.customSignatures.collectAsState()
    val recordingCustomGloss by viewModel.recordingCustomGloss.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val canRecordGlovePose = connectionState is ConnectionState.Connected ||
        connectionState is ConnectionState.SimulationMode

    val filteredItems = remember(selectedCategory, searchQuery, customSignatures) {
        allItems.filter { item ->
            val matchesCat = when (selectedCategory) {
                "Todos" -> true
                "Esenciales (Guante)" -> MultimodalFusionEngine.ESSENTIAL_GLOSSES.contains(item.gloss)
                "Personalizadas" -> customSignatures.containsKey(item.gloss)
                "Pronombres" -> item.categoryLabel.contains("Pronombre", ignoreCase = true)
                "Familia" -> item.categoryLabel.contains("Familia", ignoreCase = true) || item.categoryLabel.contains("Persona", ignoreCase = true)
                "Alimentos" -> item.categoryLabel.contains("Alimento", ignoreCase = true)
                "Cuerpo & Salud" -> item.categoryLabel.contains("Cuerpo", ignoreCase = true) || item.categoryLabel.contains("Salud", ignoreCase = true)
                "Verbos" -> item.categoryLabel.contains("Verbo", ignoreCase = true)
                "Lugares" -> item.categoryLabel.contains("Lugar", ignoreCase = true)
                "Tiempo & Clima" -> item.categoryLabel.contains("Tiempo", ignoreCase = true) || item.categoryLabel.contains("Naturaleza", ignoreCase = true)
                "Colores" -> item.categoryLabel.contains("Color", ignoreCase = true)
                "Social" -> item.categoryLabel.contains("Social", ignoreCase = true) || item.categoryLabel.contains("Respuesta", ignoreCase = true)
                else -> item.categoryLabel.contains(selectedCategory, ignoreCase = true)
            }

            val queryClean = searchQuery.trim().lowercase()
            val matchesQ = if (queryClean.isEmpty()) true else {
                item.gloss.lowercase().contains(queryClean) ||
                        item.quechuaStem.lowercase().contains(queryClean) ||
                        item.spanishDefault.lowercase().contains(queryClean)
            }

            matchesCat && matchesQ
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // BUSCADOR RÁPIDO
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text(
                    text = "Buscar seña por glosa, quechua o español...",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Limpiar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            singleLine = true
        )

        // CHIPS DE CATEGORÍAS
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(categories) { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category },
                    label = {
                        Text(
                            text = category,
                            fontSize = 11.sp,
                            fontWeight = if (selectedCategory == category) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.primary,
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selectedCategory == category,
                        borderColor = if (selectedCategory == category) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                )
            }
        }

        // BARRA INFERIOR DE ACCESO RÁPIDO AL INTÉRPRETE
        if (glossSequence.isNotEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    .clickable { onGoToInterpreter() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Oración actual: ${glossSequence.joinToString(" + ")}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Toca para ver la traducción gramatical completa",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Ir al Intérprete",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // LISTA DE TARJETAS TÁCTILES DE SEÑAS
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredItems, key = { it.gloss }) { item ->
                SignPaletteCard(
                    item = item,
                    isEssential = MultimodalFusionEngine.ESSENTIAL_GLOSSES.contains(item.gloss),
                    isCustom = customSignatures.containsKey(item.gloss),
                    isRecording = recordingCustomGloss == item.gloss,
                    canRecordPose = canRecordGlovePose,
                    onAdd = { viewModel.addGloss(item.gloss) },
                    onSpeak = { viewModel.speakText(item.quechuaStem, isQuechua = true) },
                    onRecordPose = { viewModel.recordCustomSignPose(item.gloss) },
                    onClearPose = { viewModel.clearCustomSignPose(item.gloss) }
                )
            }
        }
    }
}

@Composable
private fun SignPaletteCard(
    item: MorphologyItem,
    isEssential: Boolean,
    isCustom: Boolean,
    isRecording: Boolean,
    canRecordPose: Boolean,
    onAdd: () -> Unit,
    onSpeak: () -> Unit,
    onRecordPose: () -> Unit,
    onClearPose: () -> Unit
) {
    val postureGuide = remember(item.gloss) {
        MultimodalFusionEngine.describeGlovePosture(item)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (isCustom) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline,
                RoundedCornerShape(12.dp)
            )
            .clickable { onAdd() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = item.gloss,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = item.categoryLabel,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (isEssential) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "Guante",
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        if (isCustom) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "Personalizada",
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Qu: ${item.quechuaStem}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.secondary,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "• Es: ${item.spanishDefault}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onSpeak,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Escuchar pronunciación",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    OutlinedButton(
                        onClick = onAdd,
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(text = "Usar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Guía de postura del guante + grabación personalizada con la mano real
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = postureGuide,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (canRecordPose) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (isCustom) {
                            IconButton(
                                onClick = onClearPose,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RestartAlt,
                                    contentDescription = "Restaurar postura por defecto",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Surface(
                            color = if (isRecording) {
                                MaterialTheme.colorScheme.errorContainer
                            } else {
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                            },
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable(enabled = !isRecording) { onRecordPose() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FiberManualRecord,
                                    contentDescription = null,
                                    tint = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(9.dp)
                                )
                                Text(
                                    text = if (isRecording) "Grabando..." else "Grabar",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Visualizador de ondas de audio tipo ecualizador con 5 barras oscilantes.
 */
@Composable
private fun AudioWaveformVisualizer() {
    val transition = rememberInfiniteTransition(label = "audioWave")

    val heights = (0 until 5).map { i ->
        val duration = 300 + i * 80
        transition.animateFloat(
            initialValue = 4f,
            targetValue = 22f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = duration, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "waveHeight_$i"
        )
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(26.dp)
    ) {
        heights.forEachIndexed { _, animH ->
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(animH.value.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}
