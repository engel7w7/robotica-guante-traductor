package bo.edu.lsb.quechua.bluetooth

import java.nio.charset.StandardCharsets

/**
 * Reensamblador incremental de tramas JSON provenientes de notificaciones BLE (GATT).
 *
 * Maneja de forma segura fragmentación MTU (chunks de 20 bytes), múltiples objetos JSON
 * pegados en una misma notificación (`{...}{...}`), ruido previo al inicio `{`, cadenas
 * con llaves escapadas y prevención de desbordamiento si una trama queda truncada.
 */
internal class BleJsonFramer(
    private val maxBufferChars: Int = 2048
) {
    private val jsonBuffer = StringBuilder()

    @Synchronized
    fun appendBytes(bytes: ByteArray): List<String> {
        if (bytes.isEmpty()) return emptyList()
        return appendChunk(String(bytes, StandardCharsets.UTF_8))
    }

    @Synchronized
    fun appendChunk(chunk: String): List<String> {
        if (chunk.isEmpty()) return emptyList()
        jsonBuffer.append(chunk)

        val completed = mutableListOf<String>()
        while (true) {
            val startIdx = jsonBuffer.indexOf("{")
            if (startIdx == -1) {
                if (jsonBuffer.length > 512) jsonBuffer.clear()
                break
            }
            if (startIdx > 0) {
                jsonBuffer.delete(0, startIdx)
            }

            var depth = 0
            var inString = false
            var escaped = false
            var endIdx = -1

            for (i in 0 until jsonBuffer.length) {
                val c = jsonBuffer[i]
                if (escaped) {
                    escaped = false
                } else if (c == '\\') {
                    escaped = true
                } else if (c == '"') {
                    inString = !inString
                } else if (!inString) {
                    if (c == '{') depth++
                    else if (c == '}') {
                        depth--
                        if (depth == 0) {
                            endIdx = i
                            break
                        }
                    }
                }
            }

            if (endIdx != -1) {
                val completeJson = jsonBuffer.substring(0, endIdx + 1).trim()
                jsonBuffer.delete(0, endIdx + 1)
                if (completeJson.isNotEmpty()) {
                    completed.add(completeJson)
                }
            } else {
                if (jsonBuffer.length > maxBufferChars) {
                    jsonBuffer.clear()
                }
                break
            }
        }
        return completed
    }

    @Synchronized
    fun clear() {
        jsonBuffer.clear()
    }
}

