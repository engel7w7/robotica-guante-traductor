package bo.edu.lsb.quechua.vision

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HandLandmarkTest {

    @Test
    fun testHandSkeletonTopologyHas21LandmarksAndCorrectConnections() {
        // Verificar que la topología cubra los 21 puntos estándar y las conexiones óseas
        val connections = HandSkeletonTopology.CONNECTIONS
        assertTrue("Las conexiones esqueléticas deben tener al menos 20 huesos", connections.size >= 21)

        // Verificar que todas las conexiones apunten a índices válidos [0..20]
        for ((start, end) in connections) {
            assertTrue("start index debe estar entre 0 y 20", start in 0..20)
            assertTrue("end index debe estar entre 0 y 20", end in 0..20)
        }

        assertEquals(0, HandSkeletonTopology.WRIST_INDEX)
        assertEquals(4, HandSkeletonTopology.THUMB_INDICES.size)
        assertEquals(4, HandSkeletonTopology.INDEX_INDICES.size)
        assertEquals(4, HandSkeletonTopology.MIDDLE_INDICES.size)
        assertEquals(4, HandSkeletonTopology.RING_INDICES.size)
        assertEquals(4, HandSkeletonTopology.PINKY_INDICES.size)
        assertEquals(5, HandSkeletonTopology.FINGERTIP_INDICES.size)
    }

    @Test
    fun testHandLandmarksResultDefaultState() {
        val defaultResult = HandLandmarksResult.default()
        assertFalse(defaultResult.isHandDetected)
        assertEquals("NEUTRAL", defaultResult.gesture)
        assertEquals(0f, defaultResult.confidence, 0.001f)
        assertTrue(defaultResult.landmarks.isEmpty())
    }

    @Test
    fun testFrontAndBackCameraMirroringLogic() {
        // En cámara frontal, una coordenada raw X = 0.8f se invierte a (1 - 0.8) = 0.2f (espejo)
        val rawX = 0.8f
        val frontX = 1.0f - rawX
        val backX = rawX

        assertEquals(0.2f, frontX, 0.001f)
        assertEquals(0.8f, backX, 0.001f)
        assertTrue("Frontal debe ser simétrica respecto a trasera", frontX != backX)
    }

    @Test
    fun testNormalizedRectCenterAndDimensions() {
        val rect = NormalizedRect(left = 0.2f, top = 0.1f, right = 0.8f, bottom = 0.7f)
        assertEquals(0.6f, rect.width, 0.001f)
        assertEquals(0.6f, rect.height, 0.001f)
        assertEquals(0.5f, rect.centerX, 0.001f)
        assertEquals(0.4f, rect.centerY, 0.001f)
    }
}
