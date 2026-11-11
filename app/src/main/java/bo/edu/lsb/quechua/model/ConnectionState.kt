package bo.edu.lsb.quechua.model

/**
 * Estados del ciclo de vida de la conexión Bluetooth Low Energy (BLE)
 * con el Guante Inteligente LSB.
 */
sealed interface ConnectionState {
    data object Disconnected : ConnectionState
    data object Scanning : ConnectionState
    data class Connecting(val deviceName: String) : ConnectionState
    data class Connected(val deviceName: String, val address: String) : ConnectionState
    data object SimulationMode : ConnectionState
    data class Error(val message: String) : ConnectionState
}
