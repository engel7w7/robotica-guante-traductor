package bo.edu.lsb.quechua.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bo.edu.lsb.quechua.theme.FingerIndexColor
import bo.edu.lsb.quechua.theme.FingerMiddleColor
import bo.edu.lsb.quechua.theme.FingerPinkyColor
import bo.edu.lsb.quechua.theme.FingerRingColor
import bo.edu.lsb.quechua.theme.FingerThumbColor
import bo.edu.lsb.quechua.theme.TouchActiveBorder
import bo.edu.lsb.quechua.theme.TouchActiveContainer
import bo.edu.lsb.quechua.theme.TouchActiveText
import bo.edu.lsb.quechua.theme.TouchInactiveBorder
import bo.edu.lsb.quechua.theme.TouchInactiveContainer
import bo.edu.lsb.quechua.theme.TouchInactiveText
import java.util.Locale

/**
 * Visualizador Anatómico Biónico del Guante Háptico (HUD de Telemetría).
 * Renderiza la articulación de los 5 dedos Hall SS49E y orientación MPU-6050,
 * adaptado dinámicamente al Modo Claro y Modo Oscuro.
 */
@Composable
fun HandTelemetryVisualizer(
    flexValues: List<Int>,
    touchValues: List<Int>,
    pitch: Double,
    roll: Double,
    modifier: Modifier = Modifier
) {
    val fingerColors = listOf(
        FingerThumbColor,
        FingerIndexColor,
        FingerMiddleColor,
        FingerRingColor,
        FingerPinkyColor
    )

    val fingerLabels = listOf("PUL", "IND", "MED", "ANU", "MEÑ")
    val hasTouchHardware = touchValues.any { it != 0 }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Cabecera del HUD
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Telemetría Háptica • 5 Dedos & IMU",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "I2C 0x48 / 0x68",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Diagrama de Columnas Articulares de los 5 Dedos
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                for (i in 0 until 5) {
                    val rawFlex = flexValues.getOrElse(i) { 0 }.coerceIn(0, 100)
                    val animatedFlex by animateFloatAsState(
                        targetValue = rawFlex / 100f,
                        animationSpec = tween(durationMillis = 140),
                        label = "fingerArtic_$i"
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(54.dp)
                    ) {
                        Text(
                            text = "$rawFlex%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = fingerColors[i],
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Pista adaptativa (claro/oscuro)
                        Box(
                            modifier = Modifier
                                .width(22.dp)
                                .height(90.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(11.dp)),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height((90 * animatedFlex).dp)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                fingerColors[i],
                                                fingerColors[i].copy(alpha = 0.72f)
                                            )
                                        )
                                    )
                            )

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(90.dp),
                                verticalArrangement = Arrangement.SpaceEvenly,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                for (k in 0..2) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (animatedFlex > (k + 1) * 0.25f) {
                                                    Color.White
                                                } else {
                                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
                                                }
                                            )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = fingerLabels[i],
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Subpanel adaptativo: Estado de sensores & Nivel IMU
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (hasTouchHardware) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "TOUCH:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        for (idx in 0 until 3) {
                            val isActive = touchValues.getOrElse(idx) { 0 } == 1
                            Surface(
                                color = if (isActive) TouchActiveContainer else TouchInactiveContainer,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.border(
                                    1.dp,
                                    if (isActive) TouchActiveBorder else TouchInactiveBorder,
                                    RoundedCornerShape(6.dp)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(if (isActive) TouchActiveText else TouchInactiveText)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "T$idx",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (isActive) TouchActiveText else TouchInactiveText
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                    ) {
                        Text(
                            text = "5 Hall SS49E · MPU-6050",
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "ORIENTACIÓN ESP32",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "P: ${String.format(Locale.US, "%.1f", pitch)}°  •  R: ${String.format(Locale.US, "%.1f", roll)}°",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
