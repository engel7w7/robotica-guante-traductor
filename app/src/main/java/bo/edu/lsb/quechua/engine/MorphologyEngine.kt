package bo.edu.lsb.quechua.engine

import bo.edu.lsb.quechua.model.MorphologyItem
import bo.edu.lsb.quechua.model.TranslationResult

/**
 * Motor Morfológico y Sintáctico Integral de Quechua Boliviano (Collao/Sureño) y Español.
 *
 * Características lingüísticas formales:
 * 1. Sintaxis SVO en Español <-> Sintaxis SOV en Quechua:
 *    [Adverbio Tiempo] + [Sujeto] + [Objeto + Sufijo Declinación] + [Negación 'mana'] + [Verbo Conjugado]
 * 2. Sistema Completo de Sufijación Andina:
 *    - Acusativo (-ta): marca de objeto directo en verbos transitivos (yaku -> yakuta)
 *    - Alativo / Direccional (-man): destino de desplazamiento (wasi -> wasiman)
 *    - Locativo (-pi): permanencia espacial (llaqta -> llaqtapi)
 *    - Instrumental / Sociativo (-wan): compañía o instrumento (pay -> paywan)
 *    - Posesivos (-y, -yki, -n): 1ra, 2da y 3ra persona (uma -> umay)
 *    - Afección corporal (nanay): raíz corporal + -y + nanawan ("umay nanawan" = "Me duele la cabeza")
 *    - Conjugaciones Verbales: -ni (1SG), -nki (2SG), -n (3SG), -nchik (1PL.INC), -yku (1PL.EXC), -nkichik (2PL), -nku (3PL)
 *    - Morfología de Negación: Mana ... -chu ("Ñuqa yakuta mana munanichu" = "Yo no quiero agua")
 * 3. Adaptador Fonético para Síntesis de Voz (TTS Android) en dialecto auténtico quechua boliviano.
 */
object MorphologyEngine {

    val lexicon: Map<String, MorphologyItem> = mapOf(
        // ==========================================
        // PRONOMBRES Y PERSONAS (PRON)
        // ==========================================
        "YO" to MorphologyItem(
            gloss = "YO",
            type = "PRON",
            quechuaStem = "Ñuqa",
            spanishDefault = "Yo",
            person = "1SG",
            categoryLabel = "Pronombre",
            description = "Pronombre de 1ra persona singular",
            expectedFlex = listOf(52, 12, 88, 92, 92),
            expectedImu = listOf(15.0, 0.0),
            expectedTouch = listOf(1, 0, 0),
            visionDescriptor = "TOUCH_CHEST"
        ),
        "TU" to MorphologyItem(
            gloss = "TU",
            type = "PRON",
            quechuaStem = "Qam",
            spanishDefault = "Tú",
            person = "2SG",
            categoryLabel = "Pronombre",
            description = "Pronombre de 2da persona singular",
            expectedFlex = listOf(90, 10, 88, 92, 92),
            expectedImu = listOf(0.0, -50.0),
            expectedTouch = listOf(0, 0, 0),
            visionDescriptor = "INDEX_POINT"
        ),
        "EL" to MorphologyItem(
            gloss = "EL",
            type = "PRON",
            quechuaStem = "Pay",
            spanishDefault = "Él",
            person = "3SG",
            categoryLabel = "Pronombre",
            description = "Pronombre de 3ra persona singular",
            expectedFlex = listOf(90, 10, 88, 92, 92),
            expectedImu = listOf(0.0, 75.0),
            expectedTouch = listOf(0, 0, 0),
            visionDescriptor = "INDEX_POINT_RIGHT"
        ),
        "ELLA" to MorphologyItem(
            gloss = "ELLA",
            type = "PRON",
            quechuaStem = "Pay",
            spanishDefault = "Ella",
            person = "3SG",
            categoryLabel = "Pronombre",
            description = "Pronombre de 3ra persona femenina singular",
            expectedFlex = listOf(48, 12, 88, 92, 92),
            expectedImu = listOf(50.0, 70.0),
            expectedTouch = listOf(0, 0, 0),
            visionDescriptor = "INDEX_POINT_RIGHT"
        ),
        "NOSOTROS" to MorphologyItem(
            gloss = "NOSOTROS",
            type = "PRON",
            quechuaStem = "Ñuqanchik",
            spanishDefault = "Nosotros",
            person = "1PL_INC",
            categoryLabel = "Pronombre",
            description = "Pronombre de 1ra persona plural inclusivo",
            expectedFlex = listOf(14, 14, 14, 14, 14),
            expectedImu = listOf(15.0, 75.0),
            expectedTouch = listOf(1, 0, 0),
            visionDescriptor = "OPEN_PALM"
        ),
        "USTEDES" to MorphologyItem(
            gloss = "USTEDES",
            type = "PRON",
            quechuaStem = "Qamkuna",
            spanishDefault = "Ustedes",
            person = "2PL",
            categoryLabel = "Pronombre",
            description = "Pronombre de 2da persona plural",
            expectedFlex = listOf(90, 10, 10, 92, 92),
            expectedImu = listOf(0.0, -75.0),
            expectedTouch = listOf(0, 0, 0),
            visionDescriptor = "TWO_POINTS_FORWARD"
        ),
        "ELLOS" to MorphologyItem(
            gloss = "ELLOS",
            type = "PRON",
            quechuaStem = "Paykuna",
            spanishDefault = "Ellos",
            person = "3PL",
            categoryLabel = "Pronombre",
            description = "Pronombre de 3ra persona plural",
            expectedFlex = listOf(90, 10, 10, 92, 92),
            expectedImu = listOf(0.0, 75.0),
            expectedTouch = listOf(0, 0, 0),
            visionDescriptor = "TWO_POINTS_RIGHT"
        ),

        // ==========================================
        // FAMILIA Y PERSONAS (SUST)
        // ==========================================
        "MAMA" to MorphologyItem(
            gloss = "MAMA",
            type = "SUST",
            quechuaStem = "mama",
            spanishDefault = "a mamá",
            categoryLabel = "Familia",
            description = "Madre de familia",
            expectedFlex = listOf(12, 12, 12, 90, 92),
            expectedImu = listOf(58.0, 75.0),
            expectedTouch = listOf(1, 1, 0),
            visionDescriptor = "CHIN_TAP"
        ),
        "PAPA" to MorphologyItem(
            gloss = "PAPA",
            type = "SUST",
            quechuaStem = "tata",
            spanishDefault = "a papá",
            categoryLabel = "Familia",
            description = "Padre de familia",
            expectedFlex = listOf(12, 12, 12, 90, 92),
            expectedImu = listOf(58.0, -75.0),
            expectedTouch = listOf(1, 0, 1),
            visionDescriptor = "FOREHEAD_TAP"
        ),
        "HERMANO" to MorphologyItem(
            gloss = "HERMANO",
            type = "SUST",
            quechuaStem = "wawqi",
            spanishDefault = "al hermano",
            categoryLabel = "Familia",
            description = "Hermano varón",
            expectedFlex = listOf(12, 10, 88, 92, 12),
            expectedImu = listOf(58.0, 0.0),
            expectedTouch = listOf(0, 1, 0),
            visionDescriptor = "HORNS_SIGN"
        ),
        "HERMANA" to MorphologyItem(
            gloss = "HERMANA",
            type = "SUST",
            quechuaStem = "pana",
            spanishDefault = "a la hermana",
            categoryLabel = "Familia",
            description = "Hermana mujer",
            expectedFlex = listOf(88, 88, 90, 12, 12),
            expectedImu = listOf(0.0, -80.0),
            expectedTouch = listOf(0, 1, 1),
            visionDescriptor = "FINGERS_CROSSED_TILT"
        ),
        "HIJO" to MorphologyItem(
            gloss = "HIJO",
            type = "SUST",
            quechuaStem = "churi",
            spanishDefault = "al hijo",
            categoryLabel = "Familia",
            description = "Hijo o descendiente",
            expectedFlex = listOf(88, 10, 88, 92, 12),
            expectedImu = listOf(58.0, -75.0),
            expectedTouch = listOf(0, 0, 0),
            visionDescriptor = "CRADLE_HAND"
        ),
        "AMIGO" to MorphologyItem(
            gloss = "AMIGO",
            type = "SUST",
            quechuaStem = "masi",
            spanishDefault = "al amigo",
            categoryLabel = "Relaciones",
            description = "Compañero o amigo",
            expectedFlex = listOf(88, 10, 88, 12, 12),
            expectedImu = listOf(0.0, 0.0),
            expectedTouch = listOf(1, 0, 1),
            visionDescriptor = "CLASPED_HANDS"
        ),
        "MEDICO" to MorphologyItem(
            gloss = "MEDICO",
            type = "SUST",
            quechuaStem = "hampiq",
            spanishDefault = "al médico",
            categoryLabel = "Profesión",
            description = "Profesional de la salud",
            expectedFlex = listOf(12, 12, 90, 92, 12),
            expectedImu = listOf(0.0, 35.0),
            expectedTouch = listOf(0, 0, 1),
            visionDescriptor = "CROSS_SHOULDER"
        ),
        "PROFESOR" to MorphologyItem(
            gloss = "PROFESOR",
            type = "SUST",
            quechuaStem = "yachachiq",
            spanishDefault = "al profesor",
            categoryLabel = "Profesión",
            description = "Docente o educador",
            expectedFlex = listOf(88, 10, 12, 90, 12),
            expectedImu = listOf(0.0, 0.0),
            expectedTouch = listOf(1, 0, 0),
            visionDescriptor = "BOOK_OPEN"
        ),
        "PERSONA" to MorphologyItem(
            gloss = "PERSONA",
            type = "SUST",
            quechuaStem = "runa",
            spanishDefault = "a la persona",
            categoryLabel = "Sustantivo",
            description = "Ser humano o prójimo",
            expectedFlex = listOf(12, 88, 90, 12, 12),
            expectedImu = listOf(0.0, 0.0),
            expectedTouch = listOf(0, 0, 0),
            visionDescriptor = "PERSON_SIGN"
        ),

        // ==========================================
        // ALIMENTOS Y BEBIDAS (SUST)
        // ==========================================
        "AGUA" to MorphologyItem(
            gloss = "AGUA",
            type = "SUST",
            quechuaStem = "yaku",
            spanishDefault = "agua",
            categoryLabel = "Alimento",
            description = "Líquido vital objeto directo",
            expectedFlex = listOf(30, 25, 20, 75, 80),
            expectedImu = listOf(15.0, 10.0),
            expectedTouch = listOf(0, 1, 0),
            visionDescriptor = "CUP_HAND"
        ),
        "PAN" to MorphologyItem(
            gloss = "PAN",
            type = "SUST",
            quechuaStem = "t'anta",
            spanishDefault = "pan",
            categoryLabel = "Alimento",
            description = "Alimento básico objeto directo",
            expectedFlex = listOf(14, 72, 76, 80, 82),
            expectedImu = listOf(0.0, 45.0),
            expectedTouch = listOf(0, 0, 0),
            visionDescriptor = "BREAKING_BREAD"
        ),
        "PAPA_ALIMENTO" to MorphologyItem(
            gloss = "PAPA_ALIMENTO",
            type = "SUST",
            quechuaStem = "papa",
            spanishDefault = "papa",
            categoryLabel = "Alimento",
            description = "Tubérculo andino objeto directo",
            expectedFlex = listOf(45, 12, 88, 12, 92),
            expectedImu = listOf(58.0, -75.0),
            expectedTouch = listOf(1, 1, 0),
            visionDescriptor = "HOLD_SPHERE"
        ),
        "CARNE" to MorphologyItem(
            gloss = "CARNE",
            type = "SUST",
            quechuaStem = "aycha",
            spanishDefault = "carne",
            categoryLabel = "Alimento",
            description = "Proteína cárnica objeto directo",
            expectedFlex = listOf(12, 12, 12, 12, 88),
            expectedImu = listOf(-52.0, 75.0),
            expectedTouch = listOf(1, 0, 0),
            visionDescriptor = "PINCH_CHEEK"
        ),
        "LECHE" to MorphologyItem(
            gloss = "LECHE",
            type = "SUST",
            quechuaStem = "ñuñu",
            spanishDefault = "leche",
            categoryLabel = "Alimento",
            description = "Bebida láctea nutritiva",
            expectedFlex = listOf(72, 74, 12, 12, 12),
            expectedImu = listOf(45.0, 0.0),
            expectedTouch = listOf(0, 0, 0),
            visionDescriptor = "OK_PINCH"
        ),
        "COMIDA" to MorphologyItem(
            gloss = "COMIDA",
            type = "SUST",
            quechuaStem = "mikhuna",
            spanishDefault = "comida",
            categoryLabel = "Alimento",
            description = "Alimento preparado o almuerzo",
            expectedFlex = listOf(58, 60, 62, 90, 92),
            expectedImu = listOf(-45.0, 0.0),
            expectedTouch = listOf(1, 1, 1),
            visionDescriptor = "HAND_TO_MOUTH"
        ),

        // ==========================================
        // CUERPO HUMANO Y SALUD (CORP)
        // ==========================================
        "CABEZA" to MorphologyItem(
            gloss = "CABEZA",
            type = "CORP",
            quechuaStem = "uma",
            spanishDefault = "la cabeza",
            categoryLabel = "Cuerpo Humano",
            description = "Zona craneal",
            expectedFlex = listOf(12, 12, 88, 90, 92),
            expectedImu = listOf(65.0, 0.0),
            expectedTouch = listOf(1, 0, 0),
            visionDescriptor = "TOUCH_HEAD"
        ),
        "MANO" to MorphologyItem(
            gloss = "MANO",
            type = "CORP",
            quechuaStem = "maki",
            spanishDefault = "la mano",
            categoryLabel = "Cuerpo Humano",
            description = "Extremidad superior manual",
            expectedFlex = listOf(88, 88, 12, 90, 92),
            expectedImu = listOf(58.0, 75.0),
            expectedTouch = listOf(1, 0, 0),
            visionDescriptor = "PALM_TAP"
        ),
        "OJO" to MorphologyItem(
            gloss = "OJO",
            type = "CORP",
            quechuaStem = "ñawi",
            spanishDefault = "el ojo",
            categoryLabel = "Cuerpo Humano",
            description = "Órgano de la visión",
            expectedFlex = listOf(12, 90, 12, 90, 92),
            expectedImu = listOf(-52.0, -75.0),
            expectedTouch = listOf(0, 1, 0),
            visionDescriptor = "POINT_TO_EYE"
        ),
        "CORAZON" to MorphologyItem(
            gloss = "CORAZON",
            type = "CORP",
            quechuaStem = "sunqu",
            spanishDefault = "el corazón",
            categoryLabel = "Cuerpo Humano",
            description = "Centro cardiovascular y anímico",
            expectedFlex = listOf(88, 88, 12, 12, 12),
            expectedImu = listOf(-52.0, 75.0),
            expectedTouch = listOf(1, 1, 0),
            visionDescriptor = "BEATING_HEART"
        ),
        "ESTOMAGO" to MorphologyItem(
            gloss = "ESTOMAGO",
            type = "CORP",
            quechuaStem = "wisa",
            spanishDefault = "el estómago",
            categoryLabel = "Cuerpo Humano",
            description = "Región abdominal digestiva",
            expectedFlex = listOf(36, 40, 40, 42, 42),
            expectedImu = listOf(-55.0, 0.0),
            expectedTouch = listOf(1, 0, 0),
            visionDescriptor = "CIRCLE_STOMACH"
        ),
        "PIE" to MorphologyItem(
            gloss = "PIE",
            type = "CORP",
            quechuaStem = "chaki",
            spanishDefault = "el pie",
            categoryLabel = "Cuerpo Humano",
            description = "Extremidad inferior podálica",
            expectedFlex = listOf(90, 10, 90, 92, 92),
            expectedImu = listOf(-65.0, 0.0),
            expectedTouch = listOf(0, 0, 0),
            visionDescriptor = "POINT_DOWN"
        ),
        "REMEDIO" to MorphologyItem(
            gloss = "REMEDIO",
            type = "SUST",
            quechuaStem = "hampi",
            spanishDefault = "remedio",
            categoryLabel = "Medicina",
            description = "Fármaco o medicina terapéutica",
            expectedFlex = listOf(12, 12, 55, 90, 92),
            expectedImu = listOf(-52.0, 75.0),
            expectedTouch = listOf(1, 0, 1),
            visionDescriptor = "PILL_SWALLOW"
        ),

        // ==========================================
        // VERBOS TRANSITIVOS (V_TR) -> Acusativo (-ta)
        // ==========================================
        "QUERER" to MorphologyItem(
            gloss = "QUERER",
            type = "V_TR",
            quechuaStem = "muna",
            spanishDefault = "querer",
            spanish1Sg = "quiero",
            spanish2Sg = "quieres",
            spanish3Sg = "quiere",
            spanish1Pl = "queremos",
            categoryLabel = "Verbo Transitivo",
            description = "Expresa deseo o necesidad hacia un objeto (-ta)",
            expectedFlex = listOf(48, 60, 64, 68, 64),
            expectedImu = listOf(15.0, 0.0),
            expectedTouch = listOf(1, 1, 0),
            visionDescriptor = "PULL_TO_SELF"
        ),
        "COMER" to MorphologyItem(
            gloss = "COMER",
            type = "V_TR",
            quechuaStem = "mikhu",
            spanishDefault = "comer",
            spanish1Sg = "como",
            spanish2Sg = "comes",
            spanish3Sg = "come",
            spanish1Pl = "comemos",
            categoryLabel = "Verbo Transitivo",
            description = "Acción de ingerir alimentos (-ta)",
            expectedFlex = listOf(58, 60, 62, 90, 92),
            expectedImu = listOf(0.0, 80.0),
            expectedTouch = listOf(1, 1, 1),
            visionDescriptor = "HAND_TO_MOUTH"
        ),
        "BEBER" to MorphologyItem(
            gloss = "BEBER",
            type = "V_TR",
            quechuaStem = "ubya",
            spanishDefault = "beber",
            spanish1Sg = "bebo",
            spanish2Sg = "bebes",
            spanish3Sg = "bebe",
            spanish1Pl = "bebemos",
            categoryLabel = "Verbo Transitivo",
            description = "Acción de tomar líquidos (-ta)",
            expectedFlex = listOf(12, 55, 58, 62, 62),
            expectedImu = listOf(55.0, 65.0),
            expectedTouch = listOf(1, 0, 0),
            visionDescriptor = "DRINK_CUP"
        ),
        "VER" to MorphologyItem(
            gloss = "VER",
            type = "V_TR",
            quechuaStem = "qhawa",
            spanishDefault = "ver",
            spanish1Sg = "veo",
            spanish2Sg = "ves",
            spanish3Sg = "ve",
            spanish1Pl = "vemos",
            categoryLabel = "Verbo Transitivo",
            description = "Acción de observar o mirar (-ta)",
            expectedFlex = listOf(88, 10, 88, 92, 12),
            expectedImu = listOf(-52.0, 75.0),
            expectedTouch = listOf(0, 0, 0),
            visionDescriptor = "TWO_FINGERS_EYES"
        ),
        "ESCUCHAR" to MorphologyItem(
            gloss = "ESCUCHAR",
            type = "V_TR",
            quechuaStem = "uyari",
            spanishDefault = "escuchar",
            spanish1Sg = "escucho",
            spanish2Sg = "escuchas",
            spanish3Sg = "escucha",
            spanish1Pl = "escuchamos",
            categoryLabel = "Verbo Transitivo",
            description = "Acción de oír atentamente",
            expectedFlex = listOf(88, 88, 12, 90, 92),
            expectedImu = listOf(-52.0, -75.0),
            expectedTouch = listOf(0, 1, 0),
            visionDescriptor = "CUP_EAR"
        ),
        "SABER" to MorphologyItem(
            gloss = "SABER",
            type = "V_TR",
            quechuaStem = "yacha",
            spanishDefault = "saber",
            spanish1Sg = "sé",
            spanish2Sg = "sabes",
            spanish3Sg = "sabe",
            spanish1Pl = "sabemos",
            categoryLabel = "Verbo Transitivo",
            description = "Conocimiento o entendimiento",
            expectedFlex = listOf(45, 12, 88, 12, 92),
            expectedImu = listOf(-52.0, 75.0),
            expectedTouch = listOf(1, 0, 0),
            visionDescriptor = "TAP_TEMPLE"
        ),

        // ==========================================
        // VERBOS DE MOVIMIENTO (V_MV) -> Alativo (-man)
        // ==========================================
        "IR" to MorphologyItem(
            gloss = "IR",
            type = "V_MV",
            quechuaStem = "ri",
            spanishDefault = "ir",
            spanish1Sg = "voy",
            spanish2Sg = "vas",
            spanish3Sg = "va",
            spanish1Pl = "vamos",
            categoryLabel = "Verbo de Movimiento",
            description = "Expresa desplazamiento hacia un destino (-man)",
            expectedFlex = listOf(88, 10, 10, 92, 92),
            expectedImu = listOf(-50.0, 0.0),
            expectedTouch = listOf(0, 0, 0),
            visionDescriptor = "TWO_FINGERS_WALK"
        ),
        "VENIR" to MorphologyItem(
            gloss = "VENIR",
            type = "V_MV",
            quechuaStem = "hamu",
            spanishDefault = "venir",
            spanish1Sg = "vengo",
            spanish2Sg = "vienes",
            spanish3Sg = "viene",
            spanish1Pl = "venimos",
            categoryLabel = "Verbo de Movimiento",
            description = "Aproximación hacia el hablante",
            expectedFlex = listOf(12, 52, 90, 92, 92),
            expectedImu = listOf(58.0, 0.0),
            expectedTouch = listOf(1, 0, 0),
            visionDescriptor = "BECKON_COME"
        ),
        "VOLVER" to MorphologyItem(
            gloss = "VOLVER",
            type = "V_MV",
            quechuaStem = "kuti",
            spanishDefault = "volver",
            spanish1Sg = "vuelvo",
            spanish2Sg = "vuelves",
            spanish3Sg = "vuelve",
            spanish1Pl = "volvemos",
            categoryLabel = "Verbo de Movimiento",
            description = "Regreso al punto de origen",
            expectedFlex = listOf(12, 12, 12, 12, 88),
            expectedImu = listOf(58.0, -75.0),
            expectedTouch = listOf(0, 1, 0),
            visionDescriptor = "CIRCULAR_RETURN"
        ),

        // ==========================================
        // VERBOS INTRANSITIVOS Y AFECTIVOS (V_IT / V_AF)
        // ==========================================
        "TRABAJAR" to MorphologyItem(
            gloss = "TRABAJAR",
            type = "V_IT",
            quechuaStem = "llank'a",
            spanishDefault = "trabajar",
            spanish1Sg = "trabajo",
            spanish2Sg = "trabajas",
            spanish3Sg = "trabaja",
            spanish1Pl = "trabajamos",
            categoryLabel = "Verbo Intransitivo",
            description = "Labor o trabajo productivo",
            expectedFlex = listOf(88, 55, 55, 55, 55),
            expectedImu = listOf(58.0, -75.0),
            expectedTouch = listOf(0, 0, 0),
            visionDescriptor = "HAMMER_FISTS"
        ),
        "HABLAR" to MorphologyItem(
            gloss = "HABLAR",
            type = "V_IT",
            quechuaStem = "rima",
            spanishDefault = "hablar",
            spanish1Sg = "hablo",
            spanish2Sg = "hablas",
            spanish3Sg = "habla",
            spanish1Pl = "hablamos",
            categoryLabel = "Verbo Intransitivo",
            description = "Comunicación oral o en lengua de señas",
            expectedFlex = listOf(12, 90, 12, 90, 92),
            expectedImu = listOf(58.0, 75.0),
            expectedTouch = listOf(1, 1, 0),
            visionDescriptor = "TALKING_HAND"
        ),
        "DOLER" to MorphologyItem(
            gloss = "DOLER",
            type = "V_AF",
            quechuaStem = "nana",
            spanishDefault = "duele",
            categoryLabel = "Verbo Afectivo",
            description = "Afección corporal con sufijo afectivo -wan",
            expectedFlex = listOf(30, 80, 82, 85, 85),
            expectedImu = listOf(-42.0, 42.0),
            expectedTouch = listOf(0, 1, 1),
            visionDescriptor = "PAIN_TWIST"
        ),
        "ESTAR" to MorphologyItem(
            gloss = "ESTAR",
            type = "V_IT",
            quechuaStem = "ka",
            spanishDefault = "estar",
            spanish1Sg = "estoy",
            spanish2Sg = "estás",
            spanish3Sg = "está",
            spanish1Pl = "estamos",
            categoryLabel = "Verbo Copulativo",
            description = "Estado o existencia en un lugar",
            expectedFlex = listOf(52, 55, 55, 55, 12),
            expectedImu = listOf(0.0, -80.0),
            expectedTouch = listOf(1, 0, 0),
            visionDescriptor = "RESTING_PALM"
        ),

        // ==========================================
        // LUGARES Y ESPACIO (LOC) -> Locativo (-pi) / Alativo (-man)
        // ==========================================
        "CASA" to MorphologyItem(
            gloss = "CASA",
            type = "LOC",
            quechuaStem = "wasi",
            spanishDefault = "casa",
            categoryLabel = "Lugar",
            description = "Hogar o vivienda",
            expectedFlex = listOf(48, 12, 12, 12, 12),
            expectedImu = listOf(45.0, -45.0),
            expectedTouch = listOf(1, 1, 0),
            visionDescriptor = "ROOF_SHAPE"
        ),
        "HOSPITAL" to MorphologyItem(
            gloss = "HOSPITAL",
            type = "LOC",
            quechuaStem = "hampina wasi",
            spanishDefault = "hospital",
            categoryLabel = "Lugar",
            description = "Centro médico de atención",
            expectedFlex = listOf(88, 12, 12, 90, 12),
            expectedImu = listOf(-35.0, 50.0),
            expectedTouch = listOf(0, 0, 1),
            visionDescriptor = "HOSPITAL_CROSS"
        ),
        "ESCUELA" to MorphologyItem(
            gloss = "ESCUELA",
            type = "LOC",
            quechuaStem = "yachay wasi",
            spanishDefault = "escuela",
            categoryLabel = "Lugar",
            description = "Centro educativo",
            expectedFlex = listOf(48, 12, 12, 12, 12),
            expectedImu = listOf(0.0, 80.0),
            expectedTouch = listOf(1, 0, 0),
            visionDescriptor = "BOOK_STUDY"
        ),
        "CIUDAD" to MorphologyItem(
            gloss = "CIUDAD",
            type = "LOC",
            quechuaStem = "llaqta",
            spanishDefault = "ciudad",
            categoryLabel = "Lugar",
            description = "Población o urbe",
            expectedFlex = listOf(48, 88, 90, 92, 92),
            expectedImu = listOf(0.0, -80.0),
            expectedTouch = listOf(0, 1, 0),
            visionDescriptor = "BUILDING_OUTLINE"
        ),

        // ==========================================
        // TIEMPO Y ADVERBIOS (ADV)
        // ==========================================
        "HOY" to MorphologyItem(
            gloss = "HOY",
            type = "ADV",
            quechuaStem = "Kunan",
            spanishDefault = "Hoy",
            categoryLabel = "Tiempo",
            description = "Día presente",
            expectedFlex = listOf(86, 52, 52, 52, 92),
            expectedImu = listOf(0.0, 0.0),
            expectedTouch = listOf(0, 0, 0),
            visionDescriptor = "POINT_DOWN_DOUBLE"
        ),
        "MANANA" to MorphologyItem(
            gloss = "MANANA",
            type = "ADV",
            quechuaStem = "Q'aya",
            spanishDefault = "Mañana",
            categoryLabel = "Tiempo",
            description = "Día posterior",
            expectedFlex = listOf(12, 10, 88, 90, 92),
            expectedImu = listOf(25.0, -65.0),
            expectedTouch = listOf(0, 1, 0),
            visionDescriptor = "L_SHAPE"
        ),
        "AYER" to MorphologyItem(
            gloss = "AYER",
            type = "ADV",
            quechuaStem = "Qayna",
            spanishDefault = "Ayer",
            categoryLabel = "Tiempo",
            description = "Día anterior",
            expectedFlex = listOf(12, 55, 58, 62, 62),
            expectedImu = listOf(-52.0, -75.0),
            expectedTouch = listOf(0, 1, 0),
            visionDescriptor = "FLICK_BACKWARD"
        ),
        "AHORA" to MorphologyItem(
            gloss = "AHORA",
            type = "ADV",
            quechuaStem = "Kunanpacha",
            spanishDefault = "Ahora",
            categoryLabel = "Tiempo",
            description = "Momento actual inmediato",
            expectedFlex = listOf(90, 10, 10, 10, 10),
            expectedImu = listOf(-52.0, 75.0),
            expectedTouch = listOf(1, 0, 0),
            visionDescriptor = "PULL_DOWN_FIRM"
        ),
        "SIEMPRE" to MorphologyItem(
            gloss = "SIEMPRE",
            type = "ADV",
            quechuaStem = "Wiñay",
            spanishDefault = "Siempre",
            categoryLabel = "Adverbio",
            description = "Continuidad permanente",
            expectedFlex = listOf(88, 88, 90, 92, 10),
            expectedImu = listOf(-52.0, 75.0),
            expectedTouch = listOf(0, 0, 0),
            visionDescriptor = "INFINITY_CIRCLE"
        ),

        // ==========================================
        // FÓRMULAS SOCIALES Y CORTESÍA (SAL)
        // ==========================================
        "HOLA" to MorphologyItem(
            gloss = "HOLA",
            type = "SAL",
            quechuaStem = "Allillanchu",
            spanishDefault = "Hola, ¿cómo estás?",
            categoryLabel = "Fórmula Social",
            description = "Saludo protocolar bilingüe",
            expectedFlex = listOf(10, 10, 10, 10, 10),
            expectedImu = listOf(45.0, -55.0),
            expectedTouch = listOf(0, 0, 0),
            visionDescriptor = "WAVE_HELLO"
        ),
        "BIEN" to MorphologyItem(
            gloss = "BIEN",
            type = "SAL",
            quechuaStem = "Allillanmi",
            spanishDefault = "Estoy muy bien",
            categoryLabel = "Fórmula Social",
            description = "Respuesta afirmativa de bienestar",
            expectedFlex = listOf(12, 88, 90, 92, 92),
            expectedImu = listOf(45.0, 0.0),
            expectedTouch = listOf(0, 0, 0),
            visionDescriptor = "THUMB_UP"
        ),
        "GRACIAS" to MorphologyItem(
            gloss = "GRACIAS",
            type = "SAL",
            quechuaStem = "Yusulpayki",
            spanishDefault = "Muchas gracias",
            categoryLabel = "Fórmula Social",
            description = "Expresión de gratitud y cortesía",
            expectedFlex = listOf(14, 14, 14, 14, 14),
            expectedImu = listOf(0.0, -55.0),
            expectedTouch = listOf(1, 0, 1),
            visionDescriptor = "CHIN_FORWARD_PALM"
        ),
        "AYUDA" to MorphologyItem(
            gloss = "AYUDA",
            type = "SAL",
            quechuaStem = "Yanapaway",
            spanishDefault = "¡Ayúdame, por favor!",
            categoryLabel = "Solicitud Urgente",
            description = "Petición imperativa de auxilio",
            expectedFlex = listOf(88, 92, 92, 92, 92),
            expectedImu = listOf(58.0, 0.0),
            expectedTouch = listOf(1, 1, 1),
            visionDescriptor = "FIST"
        ),
        "POR_FAVOR" to MorphologyItem(
            gloss = "POR_FAVOR",
            type = "SAL",
            quechuaStem = "Allichu",
            spanishDefault = "Por favor",
            categoryLabel = "Fórmula Social",
            description = "Cortesía o ruego",
            expectedFlex = listOf(14, 14, 14, 14, 14),
            expectedImu = listOf(-55.0, 0.0),
            expectedTouch = listOf(1, 1, 0),
            visionDescriptor = "PRAY_HANDS"
        ),
        "PERDON" to MorphologyItem(
            gloss = "PERDON",
            type = "SAL",
            quechuaStem = "Pampachaway",
            spanishDefault = "Perdón / Discúlpame",
            categoryLabel = "Fórmula Social",
            description = "Petición de disculpa",
            expectedFlex = listOf(38, 42, 42, 42, 42),
            expectedImu = listOf(55.0, 0.0),
            expectedTouch = listOf(1, 0, 0),
            visionDescriptor = "CIRCULAR_CHEST"
        ),
        "ADIOS" to MorphologyItem(
            gloss = "ADIOS",
            type = "SAL",
            quechuaStem = "Tupananchiskama",
            spanishDefault = "Hasta luego / Adiós",
            categoryLabel = "Fórmula Social",
            description = "Despedida tradicional andina",
            expectedFlex = listOf(10, 10, 10, 10, 10),
            expectedImu = listOf(58.0, 75.0),
            expectedTouch = listOf(0, 0, 0),
            visionDescriptor = "WAVE_GOODBYE"
        ),
        "CHAU" to MorphologyItem(
            gloss = "CHAU",
            type = "SAL",
            quechuaStem = "Tupananchiskama",
            spanishDefault = "Hasta luego / Adiós",
            categoryLabel = "Fórmula Social",
            description = "Despedida tradicional andina",
            expectedFlex = listOf(14, 45, 45, 45, 45),
            expectedImu = listOf(52.0, 25.0),
            expectedTouch = listOf(0, 0, 0),
            visionDescriptor = "WAVE_GOODBYE"
        ),
        "SI" to MorphologyItem(
            gloss = "SI",
            type = "SAL",
            quechuaStem = "Arí",
            spanishDefault = "Sí",
            categoryLabel = "Respuesta",
            description = "Afirmación rotunda",
            expectedFlex = listOf(88, 92, 92, 92, 92),
            expectedImu = listOf(-45.0, 0.0),
            expectedTouch = listOf(0, 0, 0),
            visionDescriptor = "NOD_FIST"
        ),
        "NO" to MorphologyItem(
            gloss = "NO",
            type = "NEG",
            quechuaStem = "Mana",
            spanishDefault = "No",
            categoryLabel = "Negación",
            description = "Negación gramatical que rige sufijo verbal -chu",
            expectedFlex = listOf(55, 58, 58, 90, 92),
            expectedImu = listOf(45.0, 0.0),
            expectedTouch = listOf(0, 0, 0),
            visionDescriptor = "SHAKE_FINGER"
        ),

        // ==========================================
        // INTERROGATIVOS (INT)
        // ==========================================
        "QUE" to MorphologyItem(
            gloss = "QUE",
            type = "INT",
            quechuaStem = "Ima",
            spanishDefault = "¿Qué?",
            categoryLabel = "Pregunta",
            description = "Pronombre interrogativo qué cosa",
            expectedFlex = listOf(12, 12, 12, 90, 92),
            expectedImu = listOf(0.0, 80.0),
            expectedTouch = listOf(0, 0, 0),
            visionDescriptor = "PALMS_UP_SIDE"
        ),
        "DONDE" to MorphologyItem(
            gloss = "DONDE",
            type = "INT",
            quechuaStem = "Maypi",
            spanishDefault = "¿Dónde?",
            categoryLabel = "Pregunta",
            description = "Interrogativo de ubicación espacial",
            expectedFlex = listOf(12, 12, 12, 90, 92),
            expectedImu = listOf(0.0, -80.0),
            expectedTouch = listOf(0, 0, 0),
            visionDescriptor = "PALMS_UP_SIDE"
        ),
        "QUIEN" to MorphologyItem(
            gloss = "QUIEN",
            type = "INT",
            quechuaStem = "Pi",
            spanishDefault = "¿Quién?",
            categoryLabel = "Pregunta",
            description = "Interrogativo de persona",
            expectedFlex = listOf(88, 52, 90, 92, 92),
            expectedImu = listOf(58.0, 0.0),
            expectedTouch = listOf(0, 0, 0),
            visionDescriptor = "CIRCLE_CHIN"
        ),

        // ==========================================
        // CALIFICADORES Y NÚMEROS (ADJ / NUM)
        // ==========================================
        "GRANDE" to MorphologyItem(
            gloss = "GRANDE",
            type = "ADJ",
            quechuaStem = "hatun",
            spanishDefault = "grande",
            categoryLabel = "Calificador",
            description = "Tamaño extenso",
            expectedFlex = listOf(12, 10, 88, 92, 12),
            expectedImu = listOf(-52.0, 75.0),
            expectedTouch = listOf(0, 0, 0),
            visionDescriptor = "SPREAD_ARMS"
        ),
        "PEQUENO" to MorphologyItem(
            gloss = "PEQUENO",
            type = "ADJ",
            quechuaStem = "huch'uy",
            spanishDefault = "pequeño",
            categoryLabel = "Calificador",
            description = "Tamaño reducido",
            expectedFlex = listOf(70, 72, 90, 92, 92),
            expectedImu = listOf(35.0, 0.0),
            expectedTouch = listOf(1, 0, 0),
            visionDescriptor = "PINCH_SMALL"
        ),
        "BUENO" to MorphologyItem(
            gloss = "BUENO",
            type = "ADJ",
            quechuaStem = "allin",
            spanishDefault = "bueno",
            categoryLabel = "Calificador",
            description = "Cualidad positiva",
            expectedFlex = listOf(12, 88, 90, 92, 92),
            expectedImu = listOf(-45.0, 0.0),
            expectedTouch = listOf(0, 0, 0),
            visionDescriptor = "THUMB_UP"
        ),
        "UNO" to MorphologyItem(
            gloss = "UNO",
            type = "NUM",
            quechuaStem = "huk",
            spanishDefault = "uno",
            categoryLabel = "Número",
            description = "Cifra numérica 1",
            expectedFlex = listOf(90, 10, 90, 92, 92),
            expectedImu = listOf(65.0, 0.0),
            expectedTouch = listOf(0, 0, 0),
            visionDescriptor = "INDEX_ONE"
        ),
        "DOS" to MorphologyItem(
            gloss = "DOS",
            type = "NUM",
            quechuaStem = "iskay",
            spanishDefault = "dos",
            categoryLabel = "Número",
            description = "Cifra numérica 2",
            expectedFlex = listOf(90, 10, 10, 92, 92),
            expectedImu = listOf(65.0, 0.0),
            expectedTouch = listOf(0, 0, 0),
            visionDescriptor = "TWO_VICTORY"
        ),
        "TRES" to MorphologyItem(
            gloss = "TRES",
            type = "NUM",
            quechuaStem = "kinsa",
            spanishDefault = "tres",
            categoryLabel = "Número",
            description = "Cifra numérica 3",
            expectedFlex = listOf(90, 10, 10, 10, 92),
            expectedImu = listOf(65.0, 0.0),
            expectedTouch = listOf(0, 0, 0),
            visionDescriptor = "THREE_FINGERS"
        ),
        "CUATRO" to MorphologyItem(
            gloss = "CUATRO",
            type = "NUM",
            quechuaStem = "tawa",
            spanishDefault = "cuatro",
            categoryLabel = "Número",
            description = "Cifra numérica 4",
            expectedFlex = listOf(90, 10, 10, 10, 10),
            expectedImu = listOf(65.0, 0.0),
            expectedTouch = listOf(0, 0, 0),
            visionDescriptor = "FOUR_FINGERS"
        ),
        "CINCO" to MorphologyItem(
            gloss = "CINCO",
            type = "NUM",
            quechuaStem = "phichqa",
            spanishDefault = "cinco",
            categoryLabel = "Número",
            description = "Cifra numérica 5",
            expectedFlex = listOf(12, 12, 12, 12, 12),
            expectedImu = listOf(58.0, 0.0),
            expectedTouch = listOf(0, 0, 0),
            visionDescriptor = "OPEN_HAND_FIVE"
        ),
        "DIEZ" to MorphologyItem(
            gloss = "DIEZ",
            type = "NUM",
            quechuaStem = "chunka",
            spanishDefault = "diez",
            categoryLabel = "Número",
            description = "Cifra numérica 10",
            expectedFlex = listOf(12, 88, 90, 92, 92),
            expectedImu = listOf(58.0, 75.0),
            expectedTouch = listOf(1, 0, 0),
            visionDescriptor = "TEN_SHAKE"
        ),

        // ==========================================
        // SALUD, ENFERMEDADES Y SÍNTOMAS (SUST / ADJ / V_TR)
        // ==========================================
        "FIEBRE" to MorphologyItem(
            gloss = "FIEBRE",
            type = "SUST",
            quechuaStem = "ruphay",
            spanishDefault = "fiebre",
            categoryLabel = "Salud & Dolor",
            description = "Temperatura corporal elevada",
            expectedFlex = listOf(88, 55, 55, 55, 55),
            expectedImu = listOf(-58.0, 0.0),
            expectedTouch = listOf(1, 0, 0),
            visionDescriptor = "FEEL_FOREHEAD"
        ),
        "TOS" to MorphologyItem(
            gloss = "TOS",
            type = "SUST",
            quechuaStem = "ch'uju",
            spanishDefault = "tos",
            categoryLabel = "Salud & Dolor",
            description = "Afección respiratoria espasmódica",
            expectedFlex = listOf(12, 50, 50, 90, 92),
            expectedImu = listOf(-58.0, 0.0),
            expectedTouch = listOf(1, 1, 0),
            visionDescriptor = "FIST_TO_CHEST"
        ),
        "HERIDA" to MorphologyItem(
            gloss = "HERIDA",
            type = "SUST",
            quechuaStem = "k'iri",
            spanishDefault = "la herida",
            categoryLabel = "Salud & Dolor",
            description = "Lesión o corte en el cuerpo",
            expectedFlex = listOf(88, 88, 90, 12, 12),
            expectedImu = listOf(58.0, 75.0),
            expectedTouch = listOf(0, 1, 0),
            visionDescriptor = "POINT_TO_WOUND"
        ),
        "ENFERMO" to MorphologyItem(
            gloss = "ENFERMO",
            type = "ADJ",
            quechuaStem = "onqosqa",
            spanishDefault = "enfermo",
            categoryLabel = "Salud & Dolor",
            description = "Persona que padece enfermedad",
            expectedFlex = listOf(52, 55, 55, 55, 12),
            expectedImu = listOf(58.0, 75.0),
            expectedTouch = listOf(1, 0, 1),
            visionDescriptor = "SICK_GESTURE"
        ),
        "SANO" to MorphologyItem(
            gloss = "SANO",
            type = "ADJ",
            quechuaStem = "qhali",
            spanishDefault = "sano",
            categoryLabel = "Salud & Dolor",
            description = "Buen estado de vigor y salud",
            expectedFlex = listOf(52, 55, 88, 90, 92),
            expectedImu = listOf(58.0, -75.0),
            expectedTouch = listOf(0, 0, 0),
            visionDescriptor = "HEALTHY_CHEST"
        ),
        "FRIO" to MorphologyItem(
            gloss = "FRIO",
            type = "SUST",
            quechuaStem = "chiri",
            spanishDefault = "frío",
            categoryLabel = "Sensación",
            description = "Sensación térmica baja",
            expectedFlex = listOf(86, 52, 52, 90, 92),
            expectedImu = listOf(0.0, -80.0),
            expectedTouch = listOf(1, 1, 1),
            visionDescriptor = "SHIVER_CROSS"
        ),
        "CALOR" to MorphologyItem(
            gloss = "CALOR",
            type = "SUST",
            quechuaStem = "ruphay",
            spanishDefault = "calor",
            categoryLabel = "Sensación",
            description = "Sensación térmica elevada",
            expectedFlex = listOf(48, 12, 88, 92, 92),
            expectedImu = listOf(-52.0, -75.0),
            expectedTouch = listOf(0, 0, 0),
            visionDescriptor = "FAN_FACE"
        ),
        "CANSADO" to MorphologyItem(
            gloss = "CANSADO",
            type = "ADJ",
            quechuaStem = "sayk'usqa",
            spanishDefault = "cansado",
            categoryLabel = "Estado",
            description = "Fatiga física o agotamiento",
            expectedFlex = listOf(12, 88, 90, 92, 12),
            expectedImu = listOf(-52.0, -75.0),
            expectedTouch = listOf(1, 0, 0),
            visionDescriptor = "TIRED_SLUMP"
        ),
        "CURAR" to MorphologyItem(
            gloss = "CURAR",
            type = "V_TR",
            quechuaStem = "alliyachi",
            spanishDefault = "curar",
            spanish1Sg = "curo",
            spanish2Sg = "curas",
            spanish3Sg = "cura",
            spanish1Pl = "curamos",
            categoryLabel = "Verbo Transitivo",
            description = "Restablecer la salud (-ta)",
            expectedFlex = listOf(88, 88, 12, 12, 12),
            expectedImu = listOf(0.0, -80.0),
            expectedTouch = listOf(1, 0, 1),
            visionDescriptor = "HEAL_PAT"
        ),

        // ==========================================
        // FAMILIA Y COMUNIDAD ADICIONAL (SUST)
        // ==========================================
        "ABUELO" to MorphologyItem(
            gloss = "ABUELO",
            type = "SUST",
            quechuaStem = "jatun tata",
            spanishDefault = "al abuelo",
            categoryLabel = "Familia",
            description = "Abuelo o patriarca familiar",
            expectedFlex = listOf(86, 52, 52, 90, 92),
            expectedImu = listOf(58.0, 75.0),
            expectedTouch = listOf(1, 0, 0),
            visionDescriptor = "STROKE_BEARD"
        ),
        "ABUELA" to MorphologyItem(
            gloss = "ABUELA",
            type = "SUST",
            quechuaStem = "jatun mama",
            spanishDefault = "a la abuela",
            categoryLabel = "Familia",
            description = "Abuela o matriarca familiar",
            expectedFlex = listOf(12, 52, 90, 92, 92),
            expectedImu = listOf(-52.0, 75.0),
            expectedTouch = listOf(1, 1, 0),
            visionDescriptor = "GRANDMOTHER_CHIN"
        ),
        "HIJA" to MorphologyItem(
            gloss = "HIJA",
            type = "SUST",
            quechuaStem = "ususi",
            spanishDefault = "a la hija",
            categoryLabel = "Familia",
            description = "Hija mujer de la familia",
            expectedFlex = listOf(90, 10, 10, 10, 10),
            expectedImu = listOf(0.0, -80.0),
            expectedTouch = listOf(0, 1, 0),
            visionDescriptor = "CRADLE_RIGHT"
        ),
        "BEBE" to MorphologyItem(
            gloss = "BEBE",
            type = "SUST",
            quechuaStem = "wawa",
            spanishDefault = "al bebé",
            categoryLabel = "Familia",
            description = "Infante o niño pequeño",
            expectedFlex = listOf(12, 50, 50, 90, 92),
            expectedImu = listOf(58.0, -75.0),
            expectedTouch = listOf(1, 1, 0),
            visionDescriptor = "ROCK_BABY"
        ),
        "COMUNIDAD" to MorphologyItem(
            gloss = "COMUNIDAD",
            type = "SUST",
            quechuaStem = "ayllu",
            spanishDefault = "a la comunidad",
            categoryLabel = "Sociedad",
            description = "Comunidad andina solidaria",
            expectedFlex = listOf(36, 40, 40, 42, 40),
            expectedImu = listOf(0.0, 80.0),
            expectedTouch = listOf(0, 0, 0),
            visionDescriptor = "CIRCLE_GROUP"
        ),
        "MUJER" to MorphologyItem(
            gloss = "MUJER",
            type = "SUST",
            quechuaStem = "warmi",
            spanishDefault = "a la mujer",
            categoryLabel = "Persona",
            description = "Persona de sexo femenino",
            expectedFlex = listOf(12, 12, 12, 90, 92),
            expectedImu = listOf(-58.0, 0.0),
            expectedTouch = listOf(0, 1, 1),
            visionDescriptor = "BRAID_SIGN"
        ),
        "HOMBRE" to MorphologyItem(
            gloss = "HOMBRE",
            type = "SUST",
            quechuaStem = "qhari",
            spanishDefault = "al hombre",
            categoryLabel = "Persona",
            description = "Persona de sexo masculino",
            expectedFlex = listOf(88, 88, 90, 92, 10),
            expectedImu = listOf(0.0, -80.0),
            expectedTouch = listOf(1, 0, 1),
            visionDescriptor = "HAT_BRIM"
        ),
        "JOVEN" to MorphologyItem(
            gloss = "JOVEN",
            type = "SUST",
            quechuaStem = "wayna",
            spanishDefault = "al joven",
            categoryLabel = "Persona",
            description = "Persona en etapa juvenil",
            expectedFlex = listOf(12, 10, 88, 92, 12),
            expectedImu = listOf(0.0, -80.0),
            expectedTouch = listOf(1, 0, 0),
            visionDescriptor = "YOUTH_STRIDE"
        ),

        // ==========================================
        // ALIMENTOS Y COCINA ADICIONAL (SUST)
        // ==========================================
        "SOPA" to MorphologyItem(
            gloss = "SOPA",
            type = "SUST",
            quechuaStem = "lawa",
            spanishDefault = "sopa",
            categoryLabel = "Alimento",
            description = "Caldo o sopa tradicional andina",
            expectedFlex = listOf(88, 88, 90, 12, 12),
            expectedImu = listOf(-58.0, 0.0),
            expectedTouch = listOf(1, 1, 0),
            visionDescriptor = "SPOON_SOUP"
        ),
        "CHICHA" to MorphologyItem(
            gloss = "CHICHA",
            type = "SUST",
            quechuaStem = "aqha",
            spanishDefault = "chicha",
            categoryLabel = "Alimento",
            description = "Bebida fermentada de maíz",
            expectedFlex = listOf(12, 50, 50, 90, 92),
            expectedImu = listOf(0.0, 80.0),
            expectedTouch = listOf(0, 1, 0),
            visionDescriptor = "BOWL_DRINK"
        ),
        "QUESO" to MorphologyItem(
            gloss = "QUESO",
            type = "SUST",
            quechuaStem = "kesu",
            spanishDefault = "queso",
            categoryLabel = "Alimento",
            description = "Lácteo cuajado artesanal",
            expectedFlex = listOf(12, 52, 90, 92, 92),
            expectedImu = listOf(0.0, -80.0),
            expectedTouch = listOf(1, 1, 0),
            visionDescriptor = "SLICE_CHEESE"
        ),
        "SAL" to MorphologyItem(
            gloss = "SAL",
            type = "SUST",
            quechuaStem = "kachi",
            spanishDefault = "sal",
            categoryLabel = "Alimento",
            description = "Condimento mineral",
            expectedFlex = listOf(12, 12, 55, 90, 92),
            expectedImu = listOf(0.0, -80.0),
            expectedTouch = listOf(1, 0, 0),
            visionDescriptor = "PINCH_SALT"
        ),
        "HUEVO" to MorphologyItem(
            gloss = "HUEVO",
            type = "SUST",
            quechuaStem = "runtu",
            spanishDefault = "huevo",
            categoryLabel = "Alimento",
            description = "Alimento avícola proteico",
            expectedFlex = listOf(12, 88, 90, 12, 12),
            expectedImu = listOf(58.0, 75.0),
            expectedTouch = listOf(1, 1, 0),
            visionDescriptor = "HOLD_EGG"
        ),
        "FRUTA" to MorphologyItem(
            gloss = "FRUTA",
            type = "SUST",
            quechuaStem = "puquy",
            spanishDefault = "fruta",
            categoryLabel = "Alimento",
            description = "Fruto cosechado",
            expectedFlex = listOf(88, 10, 88, 12, 12),
            expectedImu = listOf(58.0, 75.0),
            expectedTouch = listOf(0, 1, 0),
            visionDescriptor = "PLUCK_FRUIT"
        ),

        // ==========================================
        // VERBOS COTIDIANOS ADICIONALES (V_TR / V_IT)
        // ==========================================
        "AYUDAR" to MorphologyItem(
            gloss = "AYUDAR",
            type = "V_TR",
            quechuaStem = "yanapa",
            spanishDefault = "ayudar",
            spanish1Sg = "ayudo",
            spanish2Sg = "ayudas",
            spanish3Sg = "ayuda",
            spanish1Pl = "ayudamos",
            categoryLabel = "Verbo Transitivo",
            description = "Acción de cooperar o socorrer (-ta)",
            expectedFlex = listOf(88, 10, 12, 90, 12),
            expectedImu = listOf(58.0, 75.0),
            expectedTouch = listOf(1, 1, 1),
            visionDescriptor = "SUPPORT_ARM"
        ),
        "CAMINAR" to MorphologyItem(
            gloss = "CAMINAR",
            type = "V_IT",
            quechuaStem = "puri",
            spanishDefault = "caminar",
            spanish1Sg = "camino",
            spanish2Sg = "caminas",
            spanish3Sg = "camina",
            spanish1Pl = "caminamos",
            categoryLabel = "Verbo Intransitivo",
            description = "Desplazamiento a pie",
            expectedFlex = listOf(88, 52, 90, 92, 92),
            expectedImu = listOf(-52.0, -75.0),
            expectedTouch = listOf(0, 0, 0),
            visionDescriptor = "TWO_FINGERS_WALK"
        ),
        "DESCANSAR" to MorphologyItem(
            gloss = "DESCANSAR",
            type = "V_IT",
            quechuaStem = "samari",
            spanishDefault = "descansar",
            spanish1Sg = "descanso",
            spanish2Sg = "descansas",
            spanish3Sg = "descansa",
            spanish1Pl = "descansamos",
            categoryLabel = "Verbo Intransitivo",
            description = "Tomar reposo o tregua",
            expectedFlex = listOf(86, 52, 52, 52, 92),
            expectedImu = listOf(-52.0, -75.0),
            expectedTouch = listOf(1, 0, 0),
            visionDescriptor = "EXHALE_HANDS"
        ),
        "BUSCAR" to MorphologyItem(
            gloss = "BUSCAR",
            type = "V_TR",
            quechuaStem = "mask'a",
            spanishDefault = "buscar",
            spanish1Sg = "busco",
            spanish2Sg = "buscas",
            spanish3Sg = "busca",
            spanish1Pl = "buscamos",
            categoryLabel = "Verbo Transitivo",
            description = "Indagar o rastrear un objeto (-ta)",
            expectedFlex = listOf(88, 10, 88, 92, 12),
            expectedImu = listOf(0.0, 0.0),
            expectedTouch = listOf(1, 0, 0),
            visionDescriptor = "SEARCH_CIRCLE"
        ),
        "DAR" to MorphologyItem(
            gloss = "DAR",
            type = "V_TR",
            quechuaStem = "qu",
            spanishDefault = "dar",
            spanish1Sg = "doy",
            spanish2Sg = "das",
            spanish3Sg = "da",
            spanish1Pl = "damos",
            categoryLabel = "Verbo Transitivo",
            description = "Ceder u ofrecer algo (-ta)",
            expectedFlex = listOf(88, 88, 12, 90, 92),
            expectedImu = listOf(0.0, 0.0),
            expectedTouch = listOf(0, 0, 0),
            visionDescriptor = "GIVE_OUTWARD"
        ),
        "LLEVAR" to MorphologyItem(
            gloss = "LLEVAR",
            type = "V_TR",
            quechuaStem = "apa",
            spanishDefault = "llevar",
            spanish1Sg = "llevo",
            spanish2Sg = "llevas",
            spanish3Sg = "lleva",
            spanish1Pl = "llevamos",
            categoryLabel = "Verbo Transitivo",
            description = "Transportar algo hacia fuera (-ta)",
            expectedFlex = listOf(12, 88, 90, 12, 12),
            expectedImu = listOf(-52.0, -75.0),
            expectedTouch = listOf(1, 1, 0),
            visionDescriptor = "CARRY_FORWARD"
        ),
        "TRAER" to MorphologyItem(
            gloss = "TRAER",
            type = "V_TR",
            quechuaStem = "apamu",
            spanishDefault = "traer",
            spanish1Sg = "traigo",
            spanish2Sg = "traes",
            spanish3Sg = "trae",
            spanish1Pl = "traemos",
            categoryLabel = "Verbo Transitivo",
            description = "Traer algo hacia el emisor (-ta)",
            expectedFlex = listOf(45, 12, 88, 12, 92),
            expectedImu = listOf(0.0, 0.0),
            expectedTouch = listOf(1, 0, 0),
            visionDescriptor = "PULL_TOWARD"
        ),
        "LAVAR" to MorphologyItem(
            gloss = "LAVAR",
            type = "V_TR",
            quechuaStem = "t'aqsa",
            spanishDefault = "lavar",
            spanish1Sg = "lavo",
            spanish2Sg = "lavas",
            spanish3Sg = "lava",
            spanish1Pl = "lavamos",
            categoryLabel = "Verbo Transitivo",
            description = "Asear o lavar ropa o cosas (-ta)",
            expectedFlex = listOf(88, 10, 88, 12, 12),
            expectedImu = listOf(-52.0, -75.0),
            expectedTouch = listOf(1, 1, 1),
            visionDescriptor = "RUB_PALMS"
        ),
        "ENTENDER" to MorphologyItem(
            gloss = "ENTENDER",
            type = "V_TR",
            quechuaStem = "jap'iqa",
            spanishDefault = "entender",
            spanish1Sg = "entiendo",
            spanish2Sg = "entiendes",
            spanish3Sg = "entiende",
            spanish1Pl = "entendemos",
            categoryLabel = "Verbo Transitivo",
            description = "Comprender con claridad",
            expectedFlex = listOf(88, 10, 12, 90, 12),
            expectedImu = listOf(-52.0, -75.0),
            expectedTouch = listOf(1, 0, 0),
            visionDescriptor = "GRASP_FOREHEAD"
        ),

        // ==========================================
        // LUGARES Y ENTORNO ADICIONAL (LOC)
        // ==========================================
        "MERCADO" to MorphologyItem(
            gloss = "MERCADO",
            type = "LOC",
            quechuaStem = "qhatu",
            spanishDefault = "al mercado",
            categoryLabel = "Lugar",
            description = "Plaza de feria o comercio",
            expectedFlex = listOf(12, 90, 12, 90, 92),
            expectedImu = listOf(0.0, 0.0),
            expectedTouch = listOf(1, 0, 1),
            visionDescriptor = "MARKET_EXCHANGE"
        ),
        "CAMPO" to MorphologyItem(
            gloss = "CAMPO",
            type = "LOC",
            quechuaStem = "chajra",
            spanishDefault = "al campo",
            categoryLabel = "Lugar",
            description = "Chacra o terreno rural cultivado",
            expectedFlex = listOf(12, 12, 12, 12, 88),
            expectedImu = listOf(0.0, 0.0),
            expectedTouch = listOf(0, 0, 0),
            visionDescriptor = "SWEEP_LAND"
        ),
        "CAMINO" to MorphologyItem(
            gloss = "CAMINO",
            type = "LOC",
            quechuaStem = "ñan",
            spanishDefault = "al camino",
            categoryLabel = "Lugar",
            description = "Vía, sendero o ruta",
            expectedFlex = listOf(88, 92, 92, 92, 92),
            expectedImu = listOf(0.0, -80.0),
            expectedTouch = listOf(0, 0, 0),
            visionDescriptor = "ROAD_PARALLEL"
        ),
        "RIO" to MorphologyItem(
            gloss = "RIO",
            type = "LOC",
            quechuaStem = "mayu",
            spanishDefault = "al río",
            categoryLabel = "Lugar",
            description = "Corriente natural de agua",
            expectedFlex = listOf(48, 88, 90, 92, 92),
            expectedImu = listOf(-58.0, 0.0),
            expectedTouch = listOf(0, 1, 0),
            visionDescriptor = "WAVE_WATER"
        ),

        // ==========================================
        // ESTADOS Y EMOCIONES (ADJ)
        // ==========================================
        "FELIZ" to MorphologyItem(
            gloss = "FELIZ",
            type = "ADJ",
            quechuaStem = "kusisqa",
            spanishDefault = "feliz",
            categoryLabel = "Emoción",
            description = "Alegría y contento",
            expectedFlex = listOf(74, 74, 12, 12, 12),
            expectedImu = listOf(0.0, 80.0),
            expectedTouch = listOf(1, 1, 0),
            visionDescriptor = "SMILE_CHEEKS"
        ),
        "TRISTE" to MorphologyItem(
            gloss = "TRISTE",
            type = "ADJ",
            quechuaStem = "llakisqa",
            spanishDefault = "triste",
            categoryLabel = "Emoción",
            description = "Pesar o aflicción",
            expectedFlex = listOf(74, 74, 12, 12, 12),
            expectedImu = listOf(-52.0, -75.0),
            expectedTouch = listOf(0, 0, 0),
            visionDescriptor = "TEAR_DOWN"
        ),
        "MIEDO" to MorphologyItem(
            gloss = "MIEDO",
            type = "ADJ",
            quechuaStem = "mancharisqa",
            spanishDefault = "asustado",
            categoryLabel = "Emoción",
            description = "Temor o susto repentino",
            expectedFlex = listOf(12, 10, 88, 90, 92),
            expectedImu = listOf(0.0, 80.0),
            expectedTouch = listOf(1, 0, 0),
            visionDescriptor = "SHAKE_CHEST"
        ),
        "MUCHO" to MorphologyItem(
            gloss = "MUCHO",
            type = "ADJ",
            quechuaStem = "achkha",
            spanishDefault = "mucho",
            categoryLabel = "Calificador",
            description = "Gran cantidad o magnitud",
            expectedFlex = listOf(12, 55, 58, 62, 62),
            expectedImu = listOf(0.0, 0.0),
            expectedTouch = listOf(1, 1, 1),
            visionDescriptor = "HEAP_HANDS"
        ),
        "POCO" to MorphologyItem(
            gloss = "POCO",
            type = "ADJ",
            quechuaStem = "pisi",
            spanishDefault = "poco",
            categoryLabel = "Calificador",
            description = "Escasa cantidad",
            expectedFlex = listOf(86, 52, 52, 52, 92),
            expectedImu = listOf(58.0, 75.0),
            expectedTouch = listOf(1, 0, 0),
            visionDescriptor = "PINCH_LITTLE"
        ),

        // ==========================================
        // PARTES DEL CUERPO ADICIONALES (CORP)
        // ==========================================
        "PECHO" to MorphologyItem(
            gloss = "PECHO",
            type = "CORP",
            quechuaStem = "qhasqu",
            spanishDefault = "el pecho",
            categoryLabel = "Cuerpo Humano",
            description = "Zona torácica pectoral",
            expectedFlex = listOf(52, 55, 55, 55, 12),
            expectedImu = listOf(-58.0, 0.0),
            expectedTouch = listOf(1, 0, 0),
            visionDescriptor = "TOUCH_CHEST_FLAT"
        ),
        "ESPALDA" to MorphologyItem(
            gloss = "ESPALDA",
            type = "CORP",
            quechuaStem = "wasa",
            spanishDefault = "la espalda",
            categoryLabel = "Cuerpo Humano",
            description = "Zona dorsal del tronco",
            expectedFlex = listOf(12, 10, 88, 90, 92),
            expectedImu = listOf(-58.0, 0.0),
            expectedTouch = listOf(0, 0, 0),
            visionDescriptor = "REACH_BACK"
        ),
        "DIENTE" to MorphologyItem(
            gloss = "DIENTE",
            type = "CORP",
            quechuaStem = "kiru",
            spanishDefault = "el diente",
            categoryLabel = "Cuerpo Humano",
            description = "Pieza dental bucal",
            expectedFlex = listOf(88, 88, 90, 92, 10),
            expectedImu = listOf(58.0, 0.0),
            expectedTouch = listOf(0, 1, 0),
            visionDescriptor = "PINKY_ONLY"
        ),
        "GARGANTA" to MorphologyItem(
            gloss = "GARGANTA",
            type = "CORP",
            quechuaStem = "kunka",
            spanishDefault = "la garganta",
            categoryLabel = "Cuerpo Humano",
            description = "Zona faríngea del cuello",
            expectedFlex = listOf(88, 52, 90, 92, 92),
            expectedImu = listOf(0.0, 80.0),
            expectedTouch = listOf(1, 1, 0),
            visionDescriptor = "TOUCH_THROAT"
        ),

        // ==========================================
        // ALIMENTOS Y BEBIDAS ANDINAS (SUST)
        // ==========================================
        "MAIZ" to MorphologyItem(
            gloss = "MAIZ",
            type = "SUST",
            quechuaStem = "sara",
            spanishDefault = "maíz / choclo",
            categoryLabel = "Alimento",
            description = "Grano sagrado andino de maíz",
            expectedFlex = listOf(12, 88, 90, 92, 92),
            expectedImu = listOf(0.0, -80.0),
            expectedTouch = listOf(1, 0, 0),
            visionDescriptor = "CORN_COB"
        ),
        "PAPA_DULCE" to MorphologyItem(
            gloss = "PAPA_DULCE",
            type = "SUST",
            quechuaStem = "apichu",
            spanishDefault = "camote",
            categoryLabel = "Alimento",
            description = "Tubérculo dulce andino",
            expectedFlex = listOf(48, 88, 90, 92, 92),
            expectedImu = listOf(58.0, 75.0),
            expectedTouch = listOf(1, 1, 0),
            visionDescriptor = "TUBER_HOLD"
        ),
        "CHARQUE" to MorphologyItem(
            gloss = "CHARQUE",
            type = "SUST",
            quechuaStem = "ch'arki",
            spanishDefault = "charque",
            categoryLabel = "Alimento",
            description = "Carne deshidratada al sol con sal",
            expectedFlex = listOf(88, 55, 55, 55, 55),
            expectedImu = listOf(0.0, 80.0),
            expectedTouch = listOf(0, 1, 1),
            visionDescriptor = "DRIED_MEAT"
        ),
        "MATE" to MorphologyItem(
            gloss = "MATE",
            type = "SUST",
            quechuaStem = "hampi yaku",
            spanishDefault = "mate de hierbas",
            categoryLabel = "Alimento",
            description = "Infusión caliente de hierbas medicinales",
            expectedFlex = listOf(62, 68, 70, 72, 68),
            expectedImu = listOf(60.0, 0.0),
            expectedTouch = listOf(1, 0, 0),
            visionDescriptor = "DRINK_CUP"
        ),

        // ==========================================
        // FAMILIA Y ATENCIÓN SOCIAL (SUST)
        // ==========================================
        "TIO" to MorphologyItem(
            gloss = "TIO",
            type = "SUST",
            quechuaStem = "tiyu",
            spanishDefault = "al tío",
            categoryLabel = "Familia",
            description = "Hermano de padre o madre",
            expectedFlex = listOf(86, 52, 52, 90, 92),
            expectedImu = listOf(-52.0, 75.0),
            expectedTouch = listOf(1, 0, 0),
            visionDescriptor = "TIO_CHEEK"
        ),
        "TIA" to MorphologyItem(
            gloss = "TIA",
            type = "SUST",
            quechuaStem = "tiya",
            spanishDefault = "a la tía",
            categoryLabel = "Familia",
            description = "Hermana de padre o madre",
            expectedFlex = listOf(90, 10, 10, 10, 92),
            expectedImu = listOf(-58.0, 0.0),
            expectedTouch = listOf(0, 1, 0),
            visionDescriptor = "TIA_CHIN"
        ),
        "ENFERMERA" to MorphologyItem(
            gloss = "ENFERMERA",
            type = "SUST",
            quechuaStem = "hampiq warmi",
            spanishDefault = "a la enfermera",
            categoryLabel = "Salud & Dolor",
            description = "Personal de enfermería de salud",
            expectedFlex = listOf(52, 55, 88, 90, 92),
            expectedImu = listOf(-52.0, -75.0),
            expectedTouch = listOf(1, 1, 0),
            visionDescriptor = "CROSS_ARM"
        ),
        "INYECCION" to MorphologyItem(
            gloss = "INYECCION",
            type = "SUST",
            quechuaStem = "t'uksina",
            spanishDefault = "la inyección",
            categoryLabel = "Salud & Dolor",
            description = "Punción terapéutica con jeringa",
            expectedFlex = listOf(88, 88, 12, 12, 12),
            expectedImu = listOf(58.0, 75.0),
            expectedTouch = listOf(0, 1, 0),
            visionDescriptor = "INJECT_ARM"
        ),
        "MAREO" to MorphologyItem(
            gloss = "MAREO",
            type = "SUST",
            quechuaStem = "umach'uspiy",
            spanishDefault = "mareo",
            categoryLabel = "Salud & Dolor",
            description = "Sensación de inestabilidad y vértigo",
            expectedFlex = listOf(48, 12, 12, 12, 12),
            expectedImu = listOf(-52.0, -75.0),
            expectedTouch = listOf(1, 0, 0),
            visionDescriptor = "DIZZY_SPIRAL"
        ),

        // ==========================================
        // ACCIONES Y VERBOS COMPLEMENTARIOS (V_TR / V_IT)
        // ==========================================
        "DORMIR" to MorphologyItem(
            gloss = "DORMIR",
            type = "V_IT",
            quechuaStem = "puñu",
            spanishDefault = "dormir",
            spanish1Sg = "duermo",
            spanish2Sg = "duermes",
            spanish3Sg = "duerme",
            spanish1Pl = "dormimos",
            categoryLabel = "Verbo Intransitivo",
            description = "Reposo del sueño",
            expectedFlex = listOf(12, 12, 55, 90, 92),
            expectedImu = listOf(58.0, 75.0),
            expectedTouch = listOf(1, 1, 0),
            visionDescriptor = "SLEEP_CHEEK"
        ),
        "DESPERTAR" to MorphologyItem(
            gloss = "DESPERTAR",
            type = "V_IT",
            quechuaStem = "rikch'ari",
            spanishDefault = "despertar",
            spanish1Sg = "despierto",
            spanish2Sg = "despiertas",
            spanish3Sg = "despierta",
            spanish1Pl = "despertamos",
            categoryLabel = "Verbo Intransitivo",
            description = "Abrir los ojos y levantarse",
            expectedFlex = listOf(52, 55, 88, 90, 92),
            expectedImu = listOf(0.0, 0.0),
            expectedTouch = listOf(0, 0, 0),
            visionDescriptor = "EYES_OPEN_WIDE"
        ),
        "JUGAR" to MorphologyItem(
            gloss = "JUGAR",
            type = "V_IT",
            quechuaStem = "puklla",
            spanishDefault = "jugar",
            spanish1Sg = "juego",
            spanish2Sg = "juegas",
            spanish3Sg = "juega",
            spanish1Pl = "jugamos",
            categoryLabel = "Verbo Intransitivo",
            description = "Actividad recreativa lúdica",
            expectedFlex = listOf(12, 88, 90, 92, 12),
            expectedImu = listOf(55.0, 0.0),
            expectedTouch = listOf(0, 0, 0),
            visionDescriptor = "PLAY_SHAKE"
        ),
        "CANTAR" to MorphologyItem(
            gloss = "CANTAR",
            type = "V_TR",
            quechuaStem = "taki",
            spanishDefault = "cantar",
            spanish1Sg = "canto",
            spanish2Sg = "cantas",
            spanish3Sg = "canta",
            spanish1Pl = "cantamos",
            categoryLabel = "Verbo Transitivo",
            description = "Entonar coplas o melodías",
            expectedFlex = listOf(48, 12, 88, 92, 92),
            expectedImu = listOf(58.0, -75.0),
            expectedTouch = listOf(1, 1, 0),
            visionDescriptor = "SING_MOUTH"
        ),
        "ESCRIBIR" to MorphologyItem(
            gloss = "ESCRIBIR",
            type = "V_TR",
            quechuaStem = "qillqa",
            spanishDefault = "escribir",
            spanish1Sg = "escribo",
            spanish2Sg = "escribes",
            spanish3Sg = "escribe",
            spanish1Pl = "escribimos",
            categoryLabel = "Verbo Transitivo",
            description = "Trazar signos sobre papel o pantalla (-ta)",
            expectedFlex = listOf(55, 58, 58, 90, 92),
            expectedImu = listOf(0.0, -80.0),
            expectedTouch = listOf(1, 0, 0),
            visionDescriptor = "WRITE_PALM"
        ),
        "LEER" to MorphologyItem(
            gloss = "LEER",
            type = "V_TR",
            quechuaStem = "ñawincha",
            spanishDefault = "leer",
            spanish1Sg = "leo",
            spanish2Sg = "lees",
            spanish3Sg = "lee",
            spanish1Pl = "leemos",
            categoryLabel = "Verbo Transitivo",
            description = "Decodificar textos con los ojos (-ta)",
            expectedFlex = listOf(12, 55, 58, 62, 62),
            expectedImu = listOf(58.0, -75.0),
            expectedTouch = listOf(0, 0, 0),
            visionDescriptor = "OPEN_BOOK"
        ),
        "COMPRAR" to MorphologyItem(
            gloss = "COMPRAR",
            type = "V_TR",
            quechuaStem = "ranti",
            spanishDefault = "comprar",
            spanish1Sg = "compro",
            spanish2Sg = "compras",
            spanish3Sg = "compra",
            spanish1Pl = "compramos",
            categoryLabel = "Verbo Transitivo",
            description = "Adquirir un bien mediante dinero (-ta)",
            expectedFlex = listOf(36, 40, 40, 42, 40),
            expectedImu = listOf(0.0, -80.0),
            expectedTouch = listOf(1, 1, 0),
            visionDescriptor = "BUY_COIN"
        ),
        "VENDER" to MorphologyItem(
            gloss = "VENDER",
            type = "V_TR",
            quechuaStem = "ranqha",
            spanishDefault = "vender",
            spanish1Sg = "vendo",
            spanish2Sg = "vendes",
            spanish3Sg = "vende",
            spanish1Pl = "vendemos",
            categoryLabel = "Verbo Transitivo",
            description = "Ofrecer bienes a cambio de dinero (-ta)",
            expectedFlex = listOf(88, 92, 92, 92, 92),
            expectedImu = listOf(0.0, 80.0),
            expectedTouch = listOf(0, 0, 0),
            visionDescriptor = "SELL_FORWARD"
        ),

        // ==========================================
        // NATURALEZA Y TIEMPO (SUST / ADV)
        // ==========================================
        "LLUVIA" to MorphologyItem(
            gloss = "LLUVIA",
            type = "SUST",
            quechuaStem = "para",
            spanishDefault = "la lluvia",
            categoryLabel = "Naturaleza",
            description = "Precipitación fluvial de gotas",
            expectedFlex = listOf(48, 12, 88, 92, 92),
            expectedImu = listOf(-52.0, 75.0),
            expectedTouch = listOf(0, 0, 0),
            visionDescriptor = "RAIN_FALL"
        ),
        "SOL" to MorphologyItem(
            gloss = "SOL",
            type = "SUST",
            quechuaStem = "inti",
            spanishDefault = "el sol",
            categoryLabel = "Naturaleza",
            description = "Astro solar dador de vida",
            expectedFlex = listOf(86, 52, 52, 52, 92),
            expectedImu = listOf(58.0, -75.0),
            expectedTouch = listOf(0, 0, 0),
            visionDescriptor = "SUN_RAY"
        ),
        "LUNA" to MorphologyItem(
            gloss = "LUNA",
            type = "SUST",
            quechuaStem = "killa",
            spanishDefault = "la luna",
            categoryLabel = "Naturaleza",
            description = "Astro lunar nocturno",
            expectedFlex = listOf(86, 52, 52, 52, 92),
            expectedImu = listOf(-52.0, 75.0),
            expectedTouch = listOf(1, 0, 0),
            visionDescriptor = "CRESCENT_MOON"
        ),
        "NOCHE" to MorphologyItem(
            gloss = "NOCHE",
            type = "ADV",
            quechuaStem = "Tuta",
            spanishDefault = "la noche",
            categoryLabel = "Tiempo",
            description = "Periodo nocturno oscuro",
            expectedFlex = listOf(88, 10, 88, 92, 12),
            expectedImu = listOf(58.0, 75.0),
            expectedTouch = listOf(1, 1, 1),
            visionDescriptor = "NIGHT_COVER"
        ),
        "DIA" to MorphologyItem(
            gloss = "DIA",
            type = "ADV",
            quechuaStem = "P'unchaw",
            spanishDefault = "el día",
            categoryLabel = "Tiempo",
            description = "Jornada diurna iluminada",
            expectedFlex = listOf(88, 10, 88, 92, 12),
            expectedImu = listOf(-52.0, -75.0),
            expectedTouch = listOf(0, 0, 0),
            visionDescriptor = "DAY_SUNRISE"
        ),

        // ==========================================
        // COLORES ANDINOS TRADICIONALES (ADJ)
        // ==========================================
        "ROJO" to MorphologyItem(
            gloss = "ROJO",
            type = "ADJ",
            quechuaStem = "puka",
            spanishDefault = "rojo",
            categoryLabel = "Color",
            description = "Color rojo carmín tradicional",
            expectedFlex = listOf(52, 55, 88, 90, 92),
            expectedImu = listOf(58.0, 75.0),
            expectedTouch = listOf(0, 1, 0),
            visionDescriptor = "RED_LIP"
        ),
        "BLANCO" to MorphologyItem(
            gloss = "BLANCO",
            type = "ADJ",
            quechuaStem = "yuraq",
            spanishDefault = "blanco",
            categoryLabel = "Color",
            description = "Color blanco puro nevado",
            expectedFlex = listOf(52, 55, 88, 90, 92),
            expectedImu = listOf(-52.0, 75.0),
            expectedTouch = listOf(1, 0, 0),
            visionDescriptor = "WHITE_CHEST"
        ),
        "NEGRO" to MorphologyItem(
            gloss = "NEGRO",
            type = "ADJ",
            quechuaStem = "yana",
            spanishDefault = "negro",
            categoryLabel = "Color",
            description = "Color oscuro obsidiana",
            expectedFlex = listOf(88, 88, 12, 90, 92),
            expectedImu = listOf(58.0, -75.0),
            expectedTouch = listOf(0, 1, 0),
            visionDescriptor = "BLACK_EYEBROW"
        ),
        "AMARILLO" to MorphologyItem(
            gloss = "AMARILLO",
            type = "ADJ",
            quechuaStem = "q'illu",
            spanishDefault = "amarillo",
            categoryLabel = "Color",
            description = "Color amarillo oro solar",
            expectedFlex = listOf(12, 88, 90, 92, 12),
            expectedImu = listOf(0.0, 75.0),
            expectedTouch = listOf(1, 0, 0),
            visionDescriptor = "PLAY_SHAKE"
        ),
        "VERDE" to MorphologyItem(
            gloss = "VERDE",
            type = "ADJ",
            quechuaStem = "q'umir",
            spanishDefault = "verde",
            categoryLabel = "Color",
            description = "Color verde hoja de la naturaleza",
            expectedFlex = listOf(88, 88, 12, 90, 92),
            expectedImu = listOf(-52.0, 75.0),
            expectedTouch = listOf(0, 1, 0),
            visionDescriptor = "GREEN_TWIST"
        ),
        "AZUL" to MorphologyItem(
            gloss = "AZUL",
            type = "ADJ",
            quechuaStem = "anqas",
            spanishDefault = "azul",
            categoryLabel = "Color",
            description = "Color azul cielo y lago andino",
            expectedFlex = listOf(12, 88, 90, 12, 12),
            expectedImu = listOf(58.0, -75.0),
            expectedTouch = listOf(0, 0, 0),
            visionDescriptor = "BLUE_WAVE"
        )
    )

    /**
     * Traduce una secuencia acumulada de glosas LSB a Español (SVO) y Quechua Boliviano (SOV).
     * Aplica el motor morfosintáctico de concordancia, declinaciones y negación.
     */
    fun translateSequence(glossSequence: List<String>): TranslationResult {
        val cleanSequence = glossSequence
            .map { it.trim().uppercase() }
            .filter { it.isNotBlank() && it != "NINGUNA" }
        if (cleanSequence.isEmpty()) {
            return TranslationResult()
        }

        // Caso 1: Saludo, fórmula social, respuesta unitaria o negación directa
        if (cleanSequence.size == 1) {
            val item = lexicon[cleanSequence[0]]
            if (item != null && (item.type == "SAL" || item.type == "INT" || item.type == "NEG")) {
                val es = item.spanishDefault
                val qu = item.quechuaStem
                val phon = adaptQuechuaPhonetics(qu)
                return TranslationResult(
                    sequenceGlosses = cleanSequence,
                    spanishSentence = es,
                    quechuaSentence = qu,
                    quechuaPhonetics = phon,
                    ruleExplanation = "Fórmula social, respuesta o partícula (${item.categoryLabel})"
                )
            }
        }

        var personCode = "1SG"
        var subjQu = "Ñuqa"
        var subjEs = "Yo"
        var hasExplicitSubj = false

        var objQu = ""
        var objEs = ""
        var isLocation = false
        var isBodyPart = false

        var timeAdvQu = ""
        var timeAdvEs = ""

        var isNegated = false
        var isQuestion = false

        var verbItem: MorphologyItem? = null
        val modifiers = mutableListOf<String>()
        val preModifiersEs = mutableListOf<String>()
        val postModifiersEs = mutableListOf<String>()

        for (gloss in cleanSequence) {
            val info = lexicon[gloss] ?: continue

            when (info.type) {
                "PRON" -> {
                    personCode = info.person ?: "1SG"
                    subjQu = info.quechuaStem
                    subjEs = info.spanishDefault
                    hasExplicitSubj = true
                }
                "SUST" -> {
                    objQu = info.quechuaStem
                    objEs = info.spanishDefault
                    isLocation = false
                    isBodyPart = false
                }
                "LOC" -> {
                    objQu = info.quechuaStem
                    objEs = info.spanishDefault
                    isLocation = true
                }
                "CORP" -> {
                    objQu = info.quechuaStem
                    objEs = info.spanishDefault
                    isBodyPart = true
                }
                "ADV" -> {
                    timeAdvQu = info.quechuaStem
                    timeAdvEs = info.spanishDefault
                }
                "NEG" -> {
                    isNegated = true
                }
                "INT" -> {
                    isQuestion = true
                    timeAdvQu = info.quechuaStem
                    timeAdvEs = info.spanishDefault.trimEnd('?')
                }
                "V_TR", "V_MV", "V_IT", "V_AF" -> {
                    verbItem = info
                }
                "ADJ", "NUM" -> {
                    modifiers.add(info.quechuaStem)
                    if (info.type == "NUM" || gloss == "MUCHO" || gloss == "POCO") {
                        preModifiersEs.add(info.spanishDefault)
                    } else {
                        postModifiersEs.add(info.spanishDefault)
                    }
                }
            }
        }

        fun formatModifiedSpanishNoun(nounEs: String): String {
            if (nounEs.isBlank()) return ""
            val lower = nounEs.trim().lowercase()
            val startsWithPrepOrArt = lower.startsWith("a ") ||
                lower.startsWith("al ") ||
                lower.startsWith("la ") ||
                lower.startsWith("el ") ||
                lower.startsWith("en ")
            return if (startsWithPrepOrArt) {
                listOf(nounEs.trim(), (preModifiersEs + postModifiersEs).joinToString(" "))
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
            } else {
                listOf(
                    preModifiersEs.joinToString(" "),
                    nounEs.trim(),
                    postModifiersEs.joinToString(" ")
                ).filter { it.isNotBlank() }.joinToString(" ")
            }
        }

        // Sufijos verbales quechuas y conjugación en español
        val quechuaVerbSuffix = when (personCode) {
            "2SG" -> "nki"
            "3SG" -> "n"
            "1PL_INC" -> "nchik"
            "1PL_EXC" -> "yku"
            "2PL" -> "nkichik"
            "3PL" -> "nku"
            else -> "ni" // 1SG
        }

        val spanishVerbConjugated = when (personCode) {
            "2SG" -> verbItem?.spanish2Sg ?: verbItem?.spanishDefault ?: ""
            "3SG" -> verbItem?.spanish3Sg ?: verbItem?.spanishDefault ?: ""
            "1PL_INC", "1PL_EXC" -> verbItem?.spanish1Pl ?: verbItem?.spanishDefault ?: ""
            "2PL", "3PL" -> verbItem?.spanish3Sg?.let {
                if (it.endsWith("e") || it.endsWith("a") || it.endsWith("á")) "${it}n" else it
            } ?: verbItem?.spanishDefault ?: ""
            else -> verbItem?.spanish1Sg ?: verbItem?.spanishDefault ?: "" // 1SG
        }

        val finalSpanishRaw: String
        val finalQuechua: String
        val ruleExplanation: String

        if (verbItem != null) {
            when (verbItem.type) {
                "V_TR" -> {
                    // Objeto directo con sufijo acusativo -ta
                    val modPrefixQu = if (modifiers.isNotEmpty()) "${modifiers.joinToString(" ")} " else ""
                    val objDeclinado = if (objQu.isNotEmpty()) {
                        "$modPrefixQu${objQu}ta"
                    } else {
                        modifiers.joinToString(" ")
                    }

                    val baseVerbQu = "${verbItem.quechuaStem}$quechuaVerbSuffix"
                    val finalVerbQu = if (isNegated) "${baseVerbQu}chu" else baseVerbQu
                    val negPrefixQu = if (isNegated) "mana " else ""

                    finalQuechua = listOf(timeAdvQu, subjQu, objDeclinado, "$negPrefixQu$finalVerbQu")
                        .filter { it.isNotBlank() }
                        .joinToString(" ")

                    val negEs = if (isNegated) "no " else ""
                    val objPhraseEs = if (objEs.isNotBlank()) {
                        formatModifiedSpanishNoun(objEs)
                    } else {
                        (preModifiersEs + postModifiersEs).joinToString(" ")
                    }
                    finalSpanishRaw = listOf(timeAdvEs, subjEs, "$negEs$spanishVerbConjugated", objPhraseEs)
                        .filter { it.isNotBlank() }
                        .joinToString(" ")

                    ruleExplanation = "SOV Quechua con Acusativo (-ta), Verbalizador (-$quechuaVerbSuffix)" + if (isNegated) " y Negación (mana ... -chu)" else ""
                }

                "V_MV" -> {
                    // Destino con sufijo alativo -man
                    val destDeclinado = if (objQu.isNotEmpty()) {
                        val modPrefix = if (modifiers.isNotEmpty()) "${modifiers.joinToString(" ")} " else ""
                        "$modPrefix${objQu}man"
                    } else ""

                    val baseVerbQu = "${verbItem.quechuaStem}$quechuaVerbSuffix"
                    val finalVerbQu = if (isNegated) "${baseVerbQu}chu" else baseVerbQu
                    val negPrefixQu = if (isNegated) "mana " else ""

                    finalQuechua = listOf(timeAdvQu, subjQu, destDeclinado, "$negPrefixQu$finalVerbQu")
                        .filter { it.isNotBlank() }
                        .joinToString(" ")

                    val negEs = if (isNegated) "no " else ""
                    val objEsTrimmed = objEs.trim()
                    val objEsLower = objEsTrimmed.lowercase()
                    val destEs = if (objEsTrimmed.isBlank()) {
                        ""
                    } else if (objEsLower.startsWith("al ") || objEsLower.startsWith("a la ") || objEsLower.startsWith("a ")) {
                        listOf(objEsTrimmed, (preModifiersEs + postModifiersEs).joinToString(" "))
                            .filter { it.isNotBlank() }
                            .joinToString(" ")
                    } else {
                        val prepEs = when (objEsLower) {
                            "hospital", "médico", "medico" -> "al "
                            "casa", "escuela", "ciudad" -> "a la "
                            else -> if (isLocation) "a " else ""
                        }
                        val modifiedLoc = listOf(
                            preModifiersEs.joinToString(" "),
                            objEsTrimmed,
                            postModifiersEs.joinToString(" ")
                        ).filter { it.isNotBlank() }.joinToString(" ")
                        "$prepEs$modifiedLoc"
                    }
                    finalSpanishRaw = listOf(timeAdvEs, subjEs, "$negEs$spanishVerbConjugated", destEs)
                        .filter { it.isNotBlank() }
                        .joinToString(" ")

                    ruleExplanation = "SOV Quechua con Alativo Direccional (-man) y Desplazamiento (-$quechuaVerbSuffix)"
                }

                "V_AF" -> {
                    // Verbo de afección corporal (dolor): raíz corporal + posesivo (-y/-yki/-n...) + afectivo (-wan/-sunki/-n)
                    val raizCorp = if (objQu.isNotEmpty()) objQu else "uma"
                    val cuerpoEs = if (objEs.isNotBlank()) objEs else "la cabeza"
                    val endsWithVowel = raizCorp.last().lowercaseChar() in "aeiouáéíóú"

                    val (possSuffix, affSuffix, pronounEs) = when (personCode) {
                        "2SG" -> Triple(if (endsWithVowel) "yki" else "niyki", "sunki", "te")
                        "3SG" -> Triple(if (endsWithVowel) "n" else "nin", "n", "le")
                        "1PL_INC" -> Triple(if (endsWithVowel) "nchik" else "ninchik", "wanchik", "nos")
                        "1PL_EXC" -> Triple(if (endsWithVowel) "yku" else "niyku", "wayku", "nos")
                        "2PL" -> Triple(if (endsWithVowel) "ykichik" else "niykichik", "sunkichik", "les")
                        "3PL" -> Triple(if (endsWithVowel) "nku" else "ninku", "n", "les")
                        else -> Triple(if (endsWithVowel) "y" else "niy", "wan", "me") // 1SG
                    }

                    val verbAfQu = if (isNegated) {
                        "mana ${verbItem.quechuaStem}${affSuffix}chu"
                    } else {
                        "${verbItem.quechuaStem}$affSuffix"
                    }
                    finalQuechua = listOf(timeAdvQu, "$raizCorp$possSuffix", verbAfQu)
                        .filter { it.isNotBlank() }
                        .joinToString(" ")

                    val coreEs = if (isNegated) {
                        "no $pronounEs duele $cuerpoEs"
                    } else {
                        "$pronounEs duele $cuerpoEs"
                    }
                    finalSpanishRaw = if (timeAdvEs.isNotBlank()) {
                        "$timeAdvEs $coreEs"
                    } else {
                        coreEs.replaceFirstChar { it.uppercase() }
                    }
                    ruleExplanation = "Afección corporal Quechua con Posesivo (-$possSuffix) y Afectivo (-$affSuffix)"
                }

                "V_IT" -> {
                    // Verbo intransitivo / copulativo con locación (-pi) o predicado adjetival
                    val modPrefixQu = if (modifiers.isNotEmpty()) "${modifiers.joinToString(" ")} " else ""
                    val locDeclinado = if (objQu.isNotEmpty() && isLocation) {
                        "$modPrefixQu${objQu}pi"
                    } else if (objQu.isNotEmpty()) {
                        "$modPrefixQu$objQu"
                    } else {
                        modifiers.joinToString(" ")
                    }

                    val baseVerbQu = "${verbItem.quechuaStem}$quechuaVerbSuffix"
                    val finalVerbQu = if (isNegated) "${baseVerbQu}chu" else baseVerbQu
                    val negPrefixQu = if (isNegated) "mana " else ""

                    finalQuechua = listOf(timeAdvQu, subjQu, locDeclinado, "$negPrefixQu$finalVerbQu")
                        .filter { it.isNotBlank() }
                        .joinToString(" ")

                    val negEs = if (isNegated) "no " else ""
                    val objEsTrimmed = objEs.trim()
                    val complementoEs = if (objEsTrimmed.isNotBlank() && isLocation) {
                        val bareLoc = when {
                            objEsTrimmed.lowercase().startsWith("al ") -> objEsTrimmed.substring(3).trim()
                            objEsTrimmed.lowercase().startsWith("a la ") -> objEsTrimmed.substring(5).trim()
                            objEsTrimmed.lowercase().startsWith("a ") -> objEsTrimmed.substring(2).trim()
                            else -> objEsTrimmed
                        }
                        val prepEs = when (bareLoc.lowercase()) {
                            "hospital", "mercado", "campo", "camino", "río", "rio" -> "en el "
                            else -> "en la "
                        }
                        val modifiedLoc = listOf(
                            preModifiersEs.joinToString(" "),
                            bareLoc,
                            postModifiersEs.joinToString(" ")
                        ).filter { it.isNotBlank() }.joinToString(" ")
                        "$prepEs$modifiedLoc"
                    } else if (objEsTrimmed.isNotBlank()) {
                        formatModifiedSpanishNoun(objEsTrimmed)
                    } else {
                        (preModifiersEs + postModifiersEs).joinToString(" ")
                    }
                    finalSpanishRaw = listOf(timeAdvEs, subjEs, "$negEs$spanishVerbConjugated", complementoEs)
                        .filter { it.isNotBlank() }
                        .joinToString(" ")

                    ruleExplanation = "Sintaxis Quechua con Locativo (-pi) y Persona (-$quechuaVerbSuffix)"
                }

                else -> {
                    finalQuechua = cleanSequence.map { lexicon[it]?.quechuaStem ?: it }.joinToString(" ")
                    finalSpanishRaw = cleanSequence.map { lexicon[it]?.spanishDefault ?: it }.joinToString(" ")
                    ruleExplanation = "Yuxtaposición morfológica directa"
                }
            }
        } else {
            // Frase nominal acumulativa sin verbo
            val nominalSequence = cleanSequence.filter { lexicon[it]?.type != "NEG" }
            val rawQu = nominalSequence.map { lexicon[it]?.quechuaStem ?: it }.joinToString(" ")
            val rawEs = nominalSequence.map { g ->
                val es = lexicon[g]?.spanishDefault ?: g
                if (isQuestion) es.trim('¿', '?') else es
            }.joinToString(" ")

            finalQuechua = when {
                isNegated && rawQu.isNotBlank() -> "Mana $rawQu"
                isNegated -> "Mana"
                else -> rawQu
            }
            finalSpanishRaw = when {
                isNegated && rawEs.isNotBlank() -> "No $rawEs"
                isNegated -> "No"
                else -> rawEs
            }
            ruleExplanation = "Sintagma nominal bilingüe acumulativo"
        }

        val finalSpanish = if (isQuestion && finalSpanishRaw.isNotBlank() && !finalSpanishRaw.endsWith("?")) {
            val cleaned = finalSpanishRaw.trim()
            if (cleaned.startsWith("¿")) "$cleaned?" else "¿$cleaned?"
        } else {
            finalSpanishRaw
        }

        val phonetics = adaptQuechuaPhonetics(finalQuechua)

        return TranslationResult(
            sequenceGlosses = cleanSequence,
            spanishSentence = finalSpanish,
            quechuaSentence = finalQuechua,
            quechuaPhonetics = phonetics,
            ruleExplanation = ruleExplanation
        )
    }

    /**
     * Motor Fonético Avanzado para Quechua Boliviano (Collao/Sureño / Charka).
     *
     * Adapta la ortografía unificada oficial a la pronunciación fonética nativa requerida
     * por sintetizadores Text-To-Speech (TTS de Android es-BO / es-ES y qu-BO).
     *
     * Reglas fonológicas aplicadas:
     * 1. Aspirada Glotal /h/: Quechua 'h' es aspirada [h], se transcribe como 'j' para que el motor
     *    español no la ignore como 'h' muda (hampi -> jampi, hamuy -> jamuy, hatun -> jatun).
     * 2. Alofonía Uvular /q, qh, q'/: Provoca apertura de vocales altas /i, u/ a medias [e, o]
     *    (sunqu -> soncjo, qucha -> cjocha, uqyana -> ocyana, allillanchu -> allillancho).
     * 3. Oclusivas Eyectivas /t', k', p', ch', q'/: Se adaptan con estallido plosivo acústico
     *    (t'anta -> tjanta, k'acha -> kjacha, ch'arki -> chjarki, p'acha -> pjacha).
     * 4. Oclusivas Aspiradas /qh, kh, th, ph, chh/: Se transcriben con fricación laríngea
     *    (qhawa -> cjhagua, mikhuna -> micjuna, phuku -> pjuku).
     * 5. Fricativa Post-Velar en Coda Silábica: 'q' al final de sílaba suena como [χ]/[x]
     *    (llaqta -> lyacjta, hampiq -> jampicj, yachachiq -> yachachicj).
     * 6. Semiconsonante Bilabial /w/: Se vocaliza como [w]/[gu] (wasi -> guasi, wawa -> guagua).
     * 7. Lateral Palatal /ll/: Se preserva como [ʎ] (ly) evitando yeísmo plano.
     */
    fun adaptQuechuaPhonetics(quechuaText: String): String {
        if (quechuaText.isBlank()) return ""

        // 1. Reemplazos directos de palabras de alta frecuencia del léxico LSB
        var text = quechuaText
            .replace("Ñuqa", "Ñocja")
            .replace("ñuqa", "ñocja")
            .replace("Qam", "Cjam")
            .replace("qam", "cjam")
            .replace("Q'aya", "Cjaya")
            .replace("q'aya", "cjaya")
            .replace("Qayna", "Cjayna")
            .replace("qayna", "cjayna")
            .replace("t'anta", "tjanta")
            .replace("T'anta", "Tjanta")
            .replace("hampiq", "jampicj")
            .replace("Hampiq", "Jampicj")
            .replace("hampi", "jampi")
            .replace("Hampi", "Jampi")
            .replace("llank'a", "lyankja")
            .replace("Llank'a", "Lyankja")
            .replace("qhawa", "cjhagua")
            .replace("Qhawa", "Cjhagua")
            .replace("mikhuna", "micjuna")
            .replace("Mikhuna", "Micjuna")
            .replace("mikhu", "micju")
            .replace("Mikhu", "Micju")
            .replace("yachachiq", "yachachicj")
            .replace("Yachachiq", "Yachachicj")
            .replace("llaqta", "lyacjta")
            .replace("Llaqta", "Lyacjta")
            .replace("sunqu", "soncjo")
            .replace("Sunqu", "Soncjo")
            .replace("churi", "churi")
            .replace("wawqi", "guajqe")
            .replace("Wawqi", "Guajqe")
            .replace("pana", "pana")
            .replace("wasi", "guasi")
            .replace("Wasi", "Guasi")
            .replace("yaku", "yacu")
            .replace("Yaku", "Yacu")

        // 2. Fórmulas conversacionales andinas
        text = text
            .replace(Regex("\\b([Aa]llillan)chu\\b"), "$1cho")
            .replace(Regex("\\b([Mm]ana)chu\\b"), "$1cho")
            .replace("sullpayki", "solpayqui")
            .replace("Sullpayki", "Solpayqui")
            .replace("qullqi", "cjollqe")
            .replace("Qullqi", "Cjollqe")
            .replace("qucha", "cjocha")
            .replace("Qucha", "Cjocha")

        // 3. Reglas fonológicas generales para oclusivas eyectivas y aspiradas
        // Alofonía uvular en eyectivas /q'i/, /q'u/ antes de reemplazar q' -> cj
        text = text
            .replace("q'i", "cje")
            .replace("Q'i", "Cje")
            .replace("q'u", "cjo")
            .replace("Q'u", "Cjo")
            .replace("t'", "tj")
            .replace("T'", "Tj")
            .replace("k'", "kj")
            .replace("K'", "Kj")
            .replace("p'", "pj")
            .replace("P'", "Pj")
            .replace("ch'", "chj")
            .replace("Ch'", "Chj")
            .replace("q'", "cj")
            .replace("Q'", "Cj")
            .replace("qh", "cjh")
            .replace("Qh", "Cjh")
            .replace("kh", "cjh")
            .replace("Kh", "Cjh")
            .replace("th", "tj")
            .replace("Th", "Tj")
            .replace("ph", "pj")
            .replace("Ph", "Pj")
            .replace("chh", "chjh")
            .replace("Chh", "Chjh")

        // 4. Aspirada 'h' al inicio de palabra o sílaba ([h] -> 'j')
        text = text.replace(Regex("\\b[Hh]([aeiouáéíóú])")) { match ->
            val first = match.value[0]
            val vowel = match.groupValues[1]
            if (first.isUpperCase()) "J$vowel" else "j$vowel"
        }

        // 5. Alofonía uvular post-velar: 'qu' -> 'cjo', 'uq' -> 'ocj', 'qi' -> 'cje', 'iq' -> 'ecj', 'qo' -> 'cjo', 'oq' -> 'ocj'
        text = text
            .replace("qu", "cjo")
            .replace("Qu", "Cjo")
            .replace("uq", "ocj")
            .replace("Uq", "Ocj")
            .replace("qo", "cjo")
            .replace("Qo", "Cjo")
            .replace("oq", "ocj")
            .replace("Oq", "Ocj")
            .replace("qi", "cje")
            .replace("Qi", "Cje")
            .replace("iq", "ecj")
            .replace("Iq", "Ecj")

        // 6. Post-velar 'q' restante en posición de coda silábica o intervocálica
        text = text
            .replace(Regex("[Qq](?=[bcdfghjklmnpqrstvwxyz\\s.,!?]|$)"), "cj")
            .replace(Regex("\\b[Qq]a"), "Cja")
            .replace(Regex("\\bqa"), "cja")
            .replace("qa", "cja")
            .replace("Qa", "Cja")

        // 7. Semiconsonante bilabial 'w' (coda vocálica '-w' -> '-u'; wi/we -> güi/güe; resto -> gu) y lateral palatal 'll'
        text = text
            .replace("ll", "ly")
            .replace("Ll", "Ly")
            .replace(Regex("([aeiouáéíóú])[Ww](?=[^aeiouáéíóú\\s]|$|\\s|[.,!?])")) { m ->
                "${m.groupValues[1]}u"
            }
            .replace(Regex("([Ww])([iíeé])")) { m ->
                val lead = if (m.groupValues[1].first().isUpperCase()) "G" else "g"
                "${lead}ü${m.groupValues[2]}"
            }
            .replace("w", "gu")
            .replace("W", "Gu")
            .replace("Alyilyancho", "Allillancho")
            .replace("alyilyancho", "allillancho")

        return text
    }

    /**
     * Genera la cadena fonética optimizada específicamente para los motores Text-To-Speech (TTS)
     * de Android en Español Latinoamericano/Andino (es-BO, es-PE, es-419, es-US, es-ES).
     *
     * A diferencia de [adaptQuechuaPhonetics] (que conserva notaciones visuales como 'cj', 'tj', 'ly'),
     * esta función produce ortografía española 100% pronunciable sin deletreos ni cortes robóticos:
     * 1. Mantiene 'll' nativo andino (palatal lateral) en lugar de 'ly'.
     * 2. Usa ortografía natural 'k' / 'j' para las post-velares y aspiradas en lugar de dígrafos impronunciables ('cj', 'cjh', 'tj', 'pj').
     * 3. Corrige sílabas 'wi/we' -> 'güi/güe' para evitar 'u' muda en español, y coda '-w' -> '-u' (P'unchaw -> Púnchau).
     * 4. Aplica acentuación paroxítona automática del Quechua (grave en penúltima sílaba) a palabras terminadas
     *    en -y, -j, -k, -r (Jámuy, Yanapáguay, Ñokánchik, Jámpij, Yacháchij, Púnchau, Kómer) respetando excepciones como Arí.
     */
    fun toTtsPhoneticSpeech(quechuaText: String): String {
        if (quechuaText.isBlank()) return ""

        var text = quechuaText

        // 1. Reemplazos léxicos exactos (palabra completa) con acentuación andina natural
        val exactWordLexMap = listOf(
            "Allillanchu" to "Allillancho",
            "allillanchu" to "allillancho",
            "Allillanmi" to "Allillanmi",
            "allillanmi" to "allillanmi",
            "Yusulpayki" to "Yusulpáyki",
            "yusulpayki" to "yusulpáyki",
            "Sullpayki" to "Solpáyki",
            "sullpayki" to "solpáyki",
            "Tupananchiskama" to "Tupananchiskama",
            "tupananchiskama" to "tupananchiskama",
            "Yanapaway" to "Yanapáguay",
            "yanapaway" to "yanapáguay",
            "Pampachaway" to "Pampacháguay",
            "pampachaway" to "pampacháguay",
            "Ñuqanchik" to "Ñokánchik",
            "ñuqanchik" to "ñokánchik",
            "Q'umir" to "Kómer",
            "q'umir" to "kómer",
            "Hampiq" to "Jámpij",
            "hampiq" to "jámpij",
            "Yachachiq" to "Yacháchij",
            "yachachiq" to "yacháchij",
            "P'unchaw" to "Púnchau",
            "p'unchaw" to "púnchau",
            "Huch'uy" to "Júchuy",
            "huch'uy" to "júchuy"
        )
        for ((k, v) in exactWordLexMap) {
            text = text.replace(Regex("(?<![\\p{L}'’])${Regex.escape(k)}(?![\\p{L}'’])"), v)
        }

        // Reemplazos de raíces que también aplican cuando llevan sufijos (-ta, -man, -pi, etc.)
        val stemLexMap = listOf(
            "Ñuqanchik" to "Ñokanchik",
            "ñuqanchik" to "ñokanchik",
            "Ñuqa" to "Ñoka",
            "ñuqa" to "ñoka",
            "Qam" to "Kam",
            "qam" to "kam",
            "Q'aya" to "Kaya",
            "q'aya" to "kaya",
            "Qayna" to "Kayna",
            "qayna" to "kayna",
            "Qullqi" to "Kollke",
            "qullqi" to "kollke",
            "Qucha" to "Kocha",
            "qucha" to "kocha",
            "Sunqu" to "Sonko",
            "sunqu" to "sonko",
            "Onqosqa" to "Onkoska",
            "onqosqa" to "onkoska",
            "Qhasqu" to "Jasko",
            "qhasqu" to "jasko",
            "Qillqa" to "Kellka",
            "qillqa" to "kellka",
            "Q'illu" to "Kellu",
            "q'illu" to "kellu",
            "Q'umir" to "Komer",
            "q'umir" to "komer",
            "Wawqi" to "Guajke",
            "wawqi" to "guajke",
            "Llaqta" to "Llajta",
            "llaqta" to "llajta",
            "Hampiq" to "Jampij",
            "hampiq" to "jampij",
            "Yachachiq" to "Yachachij",
            "yachachiq" to "yachachij",
            "P'unchaw" to "Punchau",
            "p'unchaw" to "punchau",
            "Mikhuna" to "Mijuna",
            "mikhuna" to "mijuna",
            "Mikhu" to "Miju",
            "mikhu" to "miju",
            "Qhawa" to "Jagua",
            "qhawa" to "jagua",
            "Uqyana" to "Ojyana",
            "uqyana" to "ojyana",
            "Huch'uy" to "Juchuy",
            "huch'uy" to "juchuy",
            "Jap'iqay" to "Japékay",
            "jap'iqay" to "japékay",
            "Jap'iqa" to "Japeka",
            "jap'iqa" to "japeka",
            "Puquy" to "Pokoy",
            "puquy" to "pokoy",
            "Aqha" to "Aja",
            "aqha" to "aja",
            "Achkha" to "Achka",
            "achkha" to "achka",
            "Ranqha" to "Ranja",
            "ranqha" to "ranja",
            "Phichqa" to "Pichka",
            "phichqa" to "pichka"
        )
        for ((k, v) in stemLexMap) {
            text = text.replace(k, v)
        }

        // 2. Terminación interrogativa/afirmativa -chu
        text = text.replace(Regex("\\b([Mm]ana)chu\\b"), "$1cho")

        // 3. Eyectivas y aspiradas limpias para síntesis de voz española
        val consonantMap = listOf(
            "Chh" to "Ch", "chh" to "ch",
            "Ch'" to "Ch", "ch'" to "ch",
            "Qh" to "J", "qh" to "j",
            "Q'" to "K", "q'" to "k",
            "Kh" to "K", "kh" to "k",
            "K'" to "K", "k'" to "k",
            "Th" to "T", "th" to "t",
            "T'" to "T", "t'" to "t",
            "Ph" to "P", "ph" to "p",
            "P'" to "P", "p'" to "p"
        )
        for ((k, v) in consonantMap) {
            text = text.replace(k, v)
        }

        // 4. 'h' aspirada en inicio de palabra -> 'j' audible (no muda)
        text = text.replace(Regex("\\b([Hh])([aeiouáéíóú])")) { match ->
            val lead = if (match.groupValues[1].first().isUpperCase()) "J" else "j"
            "$lead${match.groupValues[2]}"
        }

        // 5. Alofonía uvular con vocales /i, u/ y coda /q/
        val uvularMap = listOf(
            "Qu" to "Ko", "qu" to "ko",
            "Uq" to "Oj", "uq" to "oj",
            "Qi" to "Ke", "qi" to "ke",
            "Iq" to "Ej", "iq" to "ej",
            "Qo" to "Ko", "qo" to "ko",
            "Oq" to "Oj", "oq" to "oj",
            "Qe" to "Ke", "qe" to "ke",
            "Eq" to "Ej", "eq" to "ej"
        )
        for ((k, v) in uvularMap) {
            text = text.replace(k, v)
        }
        text = text
            .replace(Regex("[Qq](?=[bcdfghjklmnpqrstvwxyz\\s.,!?]|$)"), "j")
            .replace("Qa", "Ka")
            .replace("qa", "ka")

        // 6. Semiconsonante /w/: coda vocálica -> 'u'; w+i/e -> güi/güe; resto -> gu
        text = text
            .replace(Regex("([aeiouáéíóú])[Ww](?=[^aeiouáéíóú\\s]|$|\\s|[.,!?])")) { m ->
                "${m.groupValues[1]}u"
            }
            .replace(Regex("([Ww])([iíeé])")) { m ->
                val lead = if (m.groupValues[1].first().isUpperCase()) "G" else "g"
                "${lead}ü${m.groupValues[2]}"
            }
            .replace("W", "Gu")
            .replace("w", "gu")

        // 7. Acentuación paroxítona automática palabra por palabra (para -y, -j, -k, -r, etc.)
        val tokens = Regex("(\\s+|[.,!?¿¡]+)").splitToTokensAndDelimiters(text)
        val accented = tokens.joinToString("") { token ->
            if (token.isNotEmpty() && token[0].isLetter()) {
                applyQuechuaPenultimateStress(token)
            } else {
                token
            }
        }

        // 8. Contorno entonativo interrogativo natural en fórmulas como Allillanchu (sin afectar negaciones mana)
        val trimmedOut = accented.trim()
        val isGreetingQuestion = (trimmedOut.equals("Allillancho", ignoreCase = true) ||
            trimmedOut.equals("Ima", ignoreCase = true) ||
            trimmedOut.equals("Maypi", ignoreCase = true)) &&
            !trimmedOut.contains("mana", ignoreCase = true)
        return if (isGreetingQuestion && !trimmedOut.endsWith("?")) "¿$trimmedOut?" else trimmedOut
    }

    private fun Regex.splitToTokensAndDelimiters(input: String): List<String> {
        val result = mutableListOf<String>()
        var lastIndex = 0
        for (match in findAll(input)) {
            if (match.range.first > lastIndex) {
                result.add(input.substring(lastIndex, match.range.first))
            }
            result.add(match.value)
            lastIndex = match.range.last + 1
        }
        if (lastIndex < input.length) {
            result.add(input.substring(lastIndex))
        }
        return result
    }

    private fun applyQuechuaPenultimateStress(word: String): String {
        if (word.any { it in "áéíóúÁÉÍÓÚ" }) return word
        val lettersOnly = word.filter { it.isLetter() }
        if (lettersOnly.length < 2) return word

        // El español ya acentúa la penúltima sílaba de manera natural cuando la palabra termina en vocal, 'n' o 's'
        val lastChar = lettersOnly.last().lowercaseChar()
        if (lastChar in "aeiouns") return word

        val groups = findSpanishVowelGroups(word)
        if (groups.size < 2) return word

        val penult = groups[groups.size - 2]
        // En grupos vocálicos tipo 'güi', la 'ü' es deslizamiento labial [w], nunca núcleo tónico: buscar primero vocal fuerte, sino la primera vocal no-'ü'
        var targetIdx: Int? = null
        for (idx in penult.first until penult.second) {
            if (word[idx].lowercaseChar() in "aeo") {
                targetIdx = idx
                break
            }
        }
        if (targetIdx == null) {
            targetIdx = (penult.first until penult.second).firstOrNull { word[it].lowercaseChar() != 'ü' }
                ?: penult.first
        }

        val accentedChar = when (word[targetIdx]) {
            'a' -> 'á'
            'e' -> 'é'
            'i' -> 'í'
            'o' -> 'ó'
            'u' -> 'ú'
            'A' -> 'Á'
            'E' -> 'É'
            'I' -> 'Í'
            'O' -> 'Ó'
            'U' -> 'Ú'
            else -> word[targetIdx]
        }
        return word.substring(0, targetIdx) + accentedChar + word.substring(targetIdx + 1)
    }

    private fun findSpanishVowelGroups(word: String): List<Pair<Int, Int>> {
        val strong = setOf('a', 'e', 'o', 'á', 'é', 'ó')
        val allVowels = setOf('a', 'e', 'i', 'o', 'u', 'á', 'é', 'í', 'ó', 'ú', 'ü')
        val lower = word.lowercase()
        val groups = mutableListOf<Pair<Int, Int>>()
        var i = 0
        while (i < lower.length) {
            val c = lower[i]
            // Ignorar 'u' muda en dígrafos 'qu' / 'gu' (pero NO en 'gü')
            if (c == 'u' && i > 0 && (lower[i - 1] == 'q' || lower[i - 1] == 'g') &&
                i + 1 < lower.length && lower[i + 1] in setOf('e', 'i', 'é', 'í')
            ) {
                i++
                continue
            }
            if (c in allVowels) {
                val start = i
                i++
                while (i < lower.length && lower[i] in allVowels) {
                    val prev = lower[i - 1]
                    val cur = lower[i]
                    if ((prev in strong && cur in strong) || prev in setOf('í', 'ú') || cur in setOf('í', 'ú')) {
                        break
                    }
                    i++
                }
                groups.add(Pair(start, i))
            } else {
                i++
            }
        }
        return groups
    }
}
