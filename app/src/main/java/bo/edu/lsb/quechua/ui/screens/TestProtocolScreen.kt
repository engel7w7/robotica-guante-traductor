package bo.edu.lsb.quechua.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import bo.edu.lsb.quechua.theme.AppThemeMode
import bo.edu.lsb.quechua.viewmodel.MainViewModel
import java.util.Locale

/**
 * Pantalla de Protocolo de Pruebas de 4 Pasos (Fase 7 de Fabricación y Validación).
 * Guía interactiva de laboratorio para comprobar el hardware antes del sellado final de cables.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestProtocolScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val telemetry by viewModel.telemetry.collectAsState()
    val translation by viewModel.translation.collectAsState()
    val logs by viewModel.systemLogs.collectAsState()
    val logListState = rememberLazyListState()
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val testStatus = remember {
        mutableStateMapOf(
            1 to false,
            2 to false,
            3 to false,
            4 to false
        )
    }

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            logListState.animateScrollToItem(logs.size - 1)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Protocolo de Pruebas (Fase 7)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
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
                    IconButton(onClick = { viewModel.toggleThemeMode() }) {
                        Icon(
                            imageVector = if (themeMode == AppThemeMode.DARK) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Alternar Tema",
                            tint = if (themeMode == AppThemeMode.DARK) Color(0xFFFFD54F) else Color(0xFF4F46E5)
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Nota de Precaución de Taller
            item {
                val warnBg = if (isDark) Color(0xFF22180A) else Color(0xFFFEF3C7)
                val warnTitle = if (isDark) Color(0xFFFBBF24) else Color(0xFF92400E)
                val warnBody = if (isDark) Color(0xFFFDE68A) else Color(0xFF78350F)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f), RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = warnBg)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = warnTitle,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Regla Crítica de Taller de Robótica",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = warnTitle
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "No cubras las soldaduras o empalmes con silicona definitiva hasta superar en orden estas 4 verificaciones de 2 minutos.",
                                style = MaterialTheme.typography.bodySmall,
                                color = warnBody,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }

            // Monitor de Trama Viva
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Trama Sensorial BLE en Vivo del Guante (100 ms)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        val consoleBg = if (isDark) Color(0xFF080D14) else Color(0xFF0F172A)
                        Surface(
                            color = consoleBg,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                        ) {
                            Text(
                                text = telemetry.rawJson.ifBlank {
                                    """{"pulgar":${telemetry.thumbFlex},"indice":${telemetry.indexFlex},"medio":${telemetry.middleFlex},"anular":${telemetry.ringFlex},"menique":${telemetry.pinkyFlex},"pitch":${String.format(Locale.US, "%.1f", telemetry.imuPitch)},"roll":${String.format(Locale.US, "%.1f", telemetry.imuRoll)}}"""
                                },
                                modifier = Modifier.padding(10.dp),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = Color(0xFF38BDF8)
                            )
                        }
                    }
                }
            }

            // Prueba 1: Bus I2C (PCF8591 + MPU-6050)
            item {
                ProtocolTestCard(
                    testNumber = 1,
                    title = "Prueba 1: Bus I2C y UUID BLE en ESP32",
                    description = "Conecta el ESP32 y verifica el enlace I2C en pines G21 (SDA) y G22 (SCL) más servicio BLE 'Guante_ESP32':\n• [OK] PCF8591 detectado en 0x48 (Canales CH2..CH0, CH3)\n• [OK] MPU-6050 detectado en 0x68 (Giroscopio/Acelerómetro)\n• [OK] BLE UUID 4fafc201-1fb5-459e-8fcc-c5c9c331914b (100 ms)",
                    troubleshooting = "Si marca -1 en dedos, verifica SDA=G21, SCL=G22 y alimentación común de 3.3V.",
                    isChecked = testStatus[1] == true,
                    onCheckedChange = { testStatus[1] = it },
                    isDark = isDark
                )
            }

            // Prueba 2: Sensores Hall SS49E y Retiro de Jumpers
            item {
                ProtocolTestCard(
                    testNumber = 2,
                    title = "Prueba 2: Sensores Hall SS49E e Imanes (CH2/CH1/CH0/CH3/G34)",
                    description = "1. Retira los 3 jumpers del módulo PCF8591 para liberar los 4 canales analógicos.\n2. Mapeo final: CH2 (AIN2)=Pulgar, CH1 (AIN1)=Índice, CH0 (AIN0)=Medio, CH3 (AIN3)=Anular, G34=Meñique.\n3. El ESP32 escala cada sensor de 0% (estirado) a 100% (doblado) y lo transmite cada 100 ms vía BLE.",
                    troubleshooting = "Flexión actual: [ Pulgar:${telemetry.thumbFlex}%, Índice:${telemetry.indexFlex}%, Medio:${telemetry.middleFlex}%, Anular:${telemetry.ringFlex}%, Meñique:${telemetry.pinkyFlex}% ]",
                    isChecked = testStatus[2] == true,
                    onCheckedChange = { testStatus[2] = it },
                    isDark = isDark
                )
            }

            // Prueba 3: Orientación MPU-6050 en Dorso de Mano
            item {
                ProtocolTestCard(
                    testNumber = 3,
                    title = "Prueba 3: Orientación MPU-6050 en el Dorso",
                    description = "Ubica el MPU-6050 en el centro del dorso (sobre los huesos del metacarpo), 2 a 3 cm debajo de los nudillos con la flecha X hacia los dedos.\n• Inclinación adelante/atrás = Pitch\n• Giro lateral de la palma = Roll\nAl doblar los dedos, el módulo debe mantenerse firme.",
                    troubleshooting = "Ángulos actuales: Pitch = ${String.format(Locale.US, "%.1f", telemetry.imuPitch)}° | Roll = ${String.format(Locale.US, "%.1f", telemetry.imuRoll)}°",
                    isChecked = testStatus[3] == true,
                    onCheckedChange = { testStatus[3] = it },
                    isDark = isDark
                )
            }

            // Prueba 4: Traducción Morfológica y Voz Quechua/Español
            item {
                ProtocolTestCard(
                    testNumber = 4,
                    title = "Prueba 4: Traducción Morfológica y Voz Quechua",
                    description = "Conecta el guante vía BLE ('Guante_ESP32') o usa el Simulador 10 Hz.\nRealiza la secuencia: YO -> AGUA -> QUERER (o CABEZA -> DOLER).\nVerifica la pronunciación en Quechua Sureño con armonía vocálica y español.",
                    troubleshooting = "Oración: ${translation.quechuaSentence.ifBlank { "Esperando señas..." }} [Fonética: ${translation.quechuaPhonetics.ifBlank { "---" }}]",
                    isChecked = testStatus[4] == true,
                    onCheckedChange = { testStatus[4] = it },
                    isDark = isDark
                )
            }

            // Consola Terminal RAW
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Terminal,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Consola de Diagnóstico RAW",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Text(
                                text = "${logs.size} eventos",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val consoleBg = if (isDark) Color(0xFF080D14) else Color(0xFF0F172A)
                        Surface(
                            color = consoleBg,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                        ) {
                            LazyColumn(
                                state = logListState,
                                modifier = Modifier.padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                if (logs.isEmpty()) {
                                    item {
                                        Text(
                                            text = "Esperando eventos del enlace BLE o simulador...",
                                            color = Color(0xFF94A3B8),
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp
                                        )
                                    }
                                } else {
                                    items(logs) { log ->
                                        Text(
                                            text = log,
                                            color = when {
                                                log.contains("ERROR", true) -> Color(0xFFFF8B8B)
                                                log.contains("OK", true) -> Color(0xFF8CE0B0)
                                                log.contains("RECONOCIMIENTO", true) -> Color(0xFFF9D58B)
                                                else -> Color(0xFFD6E4F0)
                                            },
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun ProtocolTestCard(
    testNumber: Int,
    title: String,
    description: String,
    troubleshooting: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    isDark: Boolean
) {
    val checkedBg = if (isDark) Color(0xFF0D251D) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    val accentSuccess = MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (isChecked) accentSuccess.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(14.dp)
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isChecked) checkedBg else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isChecked,
                    onCheckedChange = onCheckedChange,
                    colors = CheckboxDefaults.colors(
                        checkedColor = accentSuccess,
                        checkmarkColor = MaterialTheme.colorScheme.onPrimary
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    color = if (isChecked) accentSuccess.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (isChecked) "SUPERADA" else "PENDIENTE",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isChecked) accentSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp,
                modifier = Modifier.padding(start = 12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.padding(start = 12.dp)
            ) {
                Text(
                    text = troubleshooting,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
