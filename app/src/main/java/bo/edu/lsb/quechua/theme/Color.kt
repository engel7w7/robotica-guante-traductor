package bo.edu.lsb.quechua.theme

import androidx.compose.ui.graphics.Color

// =========================================================================
// SISTEMA DE DISEÑO DUAL: MODO OSCURO (CYBER-HAPTIC) & MODO CLARO (CLINICAL TECH)
// Optimizado para alta legibilidad, cero recortes de texto y contraste AAA.
// =========================================================================

// =========================================================================
// SISTEMA DE DISEÑO ANDINO-MODERNO: RIMAY LSB & QUECHUA BOLIVIANO
// Inspirado en la dignidad estética andina (Pachamama Esmeralda, Inti Ámbar,
// Cielo Titicaca) con contraste AAA y separación limpia de interfaces.
// =========================================================================

// =========================================================================
// 1. PALETA MODO OSCURO (PIZARRA ANDINA NOCTURNA)
// =========================================================================
val DarkPrimary = Color(0xFF10B981)          // Esmeralda Andino (Pachamama)
val DarkOnPrimary = Color(0xFF022C22)
val DarkPrimaryContainer = Color(0xFF064E3B)
val DarkOnPrimaryContainer = Color(0xFFA7F3D0)

val DarkSecondary = Color(0xFFF59E0B)        // Ámbar Dorado Solar (Inti)
val DarkOnSecondary = Color(0xFF451A03)
val DarkSecondaryContainer = Color(0xFF78350F)
val DarkOnSecondaryContainer = Color(0xFFFDE68A)

val DarkTertiary = Color(0xFF38BDF8)         // Azul Altiplánico (Cielo Titicaca)
val DarkOnTertiary = Color(0xFF082F49)
val DarkTertiaryContainer = Color(0xFF075985)
val DarkOnTertiaryContainer = Color(0xFFBAE6FD)

val DarkBackground = Color(0xFF0B111A)       // Carbón Pizarra Profundo
val DarkOnBackground = Color(0xFFF8FAFC)
val DarkSurface = Color(0xFF131D2A)          // Superficie de Tarjetas Pizarra
val DarkOnSurface = Color(0xFFF8FAFC)
val DarkSurfaceVariant = Color(0xFF1C283A)   // Contenedores Secundarios
val DarkOnSurfaceVariant = Color(0xFF94A3B8)
val DarkOutline = Color(0xFF28384C)          // Bordes Sutiles de 1dp
val DarkOutlineVariant = Color(0xFF1E2B3C)

// Tarjeta de traducción oscura
val DarkTranslationCardBackground = Color(0xFF101926)
val DarkTranslationCardTextQuechua = Color(0xFF34D399) // Esmeralda legible
val DarkTranslationCardTextSpanish = Color(0xFFF8FAFC)
val DarkTranslationCardGlossBadge = Color(0xFF1E2B3D)
val DarkTranslationCardPhonetic = Color(0xFFFBBF24)    // Fonética Ámbar

// =========================================================================
// 2. PALETA MODO CLARO (CANVAS ANDINO DIURNO)
// =========================================================================
val LightPrimary = Color(0xFF047857)         // Verde Esmeralda Profundo
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFD1FAE5)
val LightOnPrimaryContainer = Color(0xFF064E3B)

val LightSecondary = Color(0xFFD97706)       // Ámbar Dorado Inti
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFFEF3C7)
val LightOnSecondaryContainer = Color(0xFF78350F)

val LightTertiary = Color(0xFF0284C7)        // Azul Cielo Titicaca
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFFE0F2FE)
val LightOnTertiaryContainer = Color(0xFF075985)

val LightBackground = Color(0xFFF8FAFC)      // Canvas Blanco Pizarra Cálido
val LightOnBackground = Color(0xFF0F172A)
val LightSurface = Color(0xFFFFFFFF)         // Tarjeta Blanca Pura
val LightOnSurface = Color(0xFF0F172A)
val LightSurfaceVariant = Color(0xFFF1F5F9)  // Gris Pizarra Tenue
val LightOnSurfaceVariant = Color(0xFF475569)
val LightOutline = Color(0xFFCBD5E1)         // Borde Neutro Delicado
val LightOutlineVariant = Color(0xFFE2E8F0)

// Tarjeta de traducción clara
val LightTranslationCardBackground = Color(0xFFFFFFFF)
val LightTranslationCardTextQuechua = Color(0xFF065F46) // Verde Bosque Intenso
val LightTranslationCardTextSpanish = Color(0xFF0F172A)
val LightTranslationCardGlossBadge = Color(0xFFE2E8F0)
val LightTranslationCardPhonetic = Color(0xFFB45309)   // Ámbar Dorado Profundo

// =========================================================================
// 3. SENSORES HALL EFECTO LINEAL Y DEDOS (CALIBRACIÓN SS49E)
// =========================================================================
val FingerThumbColor = Color(0xFF0284C7)   // Pulgar: Celeste Litoral
val FingerIndexColor = Color(0xFF0D9488)   // Índice: Turquesa Jade
val FingerMiddleColor = Color(0xFF10B981)  // Medio: Esmeralda Andino
val FingerRingColor = Color(0xFFF59E0B)    // Anular: Ámbar Inti
val FingerPinkyColor = Color(0xFFE11D48)   // Meñique: Carmesí Aguayo
val FingerBarTrack = Color(0xFF1E293B)

// =========================================================================
// 4. ESTADOS DE CONEXIÓN BLE Y SUBSISTEMAS
// =========================================================================
val StatusConnectedContainer = Color(0xFF064E3B)
val StatusConnectedText = Color(0xFF34D399)
val StatusScanningContainer = Color(0xFF0C2B47)
val StatusScanningText = Color(0xFF38BDF8)
val StatusDisconnectedContainer = Color(0xFF1E293B)
val StatusDisconnectedText = Color(0xFF94A3B8)
val StatusWarningContainer = Color(0xFF451A03)
val StatusWarningText = Color(0xFFFBBF24)

// =========================================================================
// 5. SENSORES TOUCH CAPACITIVOS (T0, T1, T2)
// =========================================================================
val TouchActiveContainer = Color(0xFF064E3B)
val TouchActiveBorder = Color(0xFF10B981)
val TouchActiveText = Color(0xFF34D399)
val TouchInactiveContainer = Color(0xFF151D28)
val TouchInactiveBorder = Color(0xFF243447)
val TouchInactiveText = Color(0xFF64748B)

// =========================================================================
// 6. ALIAS PARA RETROCOMPATIBILIDAD CON VISTAS EXISTENTES
// =========================================================================
val PastelPrimary = DarkPrimary
val PastelOnPrimary = DarkOnPrimary
val PastelPrimaryContainer = DarkPrimaryContainer
val PastelOnPrimaryContainer = DarkOnPrimaryContainer

val PastelSecondary = DarkSecondary
val PastelOnSecondary = DarkOnSecondary
val PastelSecondaryContainer = DarkSecondaryContainer
val PastelOnSecondaryContainer = DarkOnSecondaryContainer

val PastelTertiary = DarkTertiary
val PastelOnTertiary = DarkOnTertiary
val PastelTertiaryContainer = DarkTertiaryContainer
val PastelOnTertiaryContainer = DarkOnTertiaryContainer

val PastelBackground = DarkBackground
val PastelOnBackground = DarkOnBackground
val PastelSurface = DarkSurface
val PastelOnSurface = DarkOnSurface
val PastelSurfaceVariant = DarkSurfaceVariant
val PastelOnSurfaceVariant = DarkOnSurfaceVariant
val PastelOutline = DarkOutline
val PastelOutlineVariant = DarkOutlineVariant

val TranslationCardBackground = DarkTranslationCardBackground
val TranslationCardTextQuechua = DarkTranslationCardTextQuechua
val TranslationCardTextSpanish = DarkTranslationCardTextSpanish
val TranslationCardGlossBadge = DarkTranslationCardGlossBadge
val TranslationCardPhonetic = DarkTranslationCardPhonetic
