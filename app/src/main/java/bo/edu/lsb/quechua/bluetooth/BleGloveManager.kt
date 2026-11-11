package bo.edu.lsb.quechua.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothSocket
import android.bluetooth.BluetoothStatusCodes
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import bo.edu.lsb.quechua.engine.MorphologyEngine
import bo.edu.lsb.quechua.model.ConnectionState
import bo.edu.lsb.quechua.model.GloveCalibrationProfile
import bo.edu.lsb.quechua.model.GloveTelemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Representa un dispositivo BLE detectado en vivo durante el escaneo del selector.
 */
data class ScannedBleDevice(
    val device: BluetoothDevice,
    val name: String,
    val address: String,
    val rssi: Int,
    val hasGloveService: Boolean,
    val lastSeenMs: Long = System.currentTimeMillis()
)

/**
 * Administrador Integral de Conectividad Bluetooth (BLE GATT + Fallback/Simulación Háptica).
 *
 * Opera contra el firmware ESP32 BLE ("Guante_ESP32"):
 * - Service UUID:        4fafc201-1fb5-459e-8fcc-c5c9c331914b
 * - Characteristic UUID: beb5483e-36e1-4688-b7f5-ea07361b26a8
 * - Descriptor CCCD:     00002902-0000-1000-8000-00805f9b34fb
 *
 * Garantiza:
 * 1. Verificación previa de permisos Android (SCAN/CONNECT o Ubicación en Android < 12).
 * 2. Descubrimiento BLE en vivo para seleccionar el ESP32 incluso sin emparejamiento previo.
 * 3. Handshake completo GATT -> MTU -> Servicio -> CCCD antes de reportar Connected.
 * 4. Watchdogs de MTU, CCCD, tiempo de enlace (5.5 s), limpieza de caché GATT y reintento ante Error 133.
 */
class BleGloveManager(private val context: Context) {

    companion object {
        const val TARGET_DEVICE_NAME = "Guante_ESP32"
        const val TARGET_BLE_NAME = "Guante_LSB_Quechua"
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        val SERVICE_UUID: UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
        val CHARACTERISTIC_UUID: UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")
        val CCCD_DESCRIPTOR_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private var bluetoothGatt: BluetoothGatt? = null
    private var sppSocket: BluetoothSocket? = null
    private var sppJob: Job? = null
    private var scanCallback: ScanCallback? = null
    private var discoveryScanCallback: ScanCallback? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _telemetryFlow = MutableSharedFlow<GloveTelemetry>(extraBufferCapacity = 64)
    val telemetryFlow: SharedFlow<GloveTelemetry> = _telemetryFlow.asSharedFlow()

    private val _rawLogFlow = MutableSharedFlow<String>(extraBufferCapacity = 100)
    val rawLogFlow: SharedFlow<String> = _rawLogFlow.asSharedFlow()

    private val discoveredMap = ConcurrentHashMap<String, ScannedBleDevice>()
    private val _scannedDevices = MutableStateFlow<List<ScannedBleDevice>>(emptyList())
    val scannedDevices: StateFlow<List<ScannedBleDevice>> = _scannedDevices.asStateFlow()

    private var simulationJob: Job? = null
    private var isSimulating = false
    private var activeCalibration: GloveCalibrationProfile? = null

    private enum class ConnectOrigin { AUTO_BONDED, AUTO_SCAN, EXPLICIT }

    // Estado interno sincronizado del handshake GATT y reintentos
    private val gattLock = Any()
    private val jsonFramer = BleJsonFramer()
    private var isOpeningGatt = false
    private var isConnectingAttempt = false
    private var discoveredOnce = false
    private var cccdCompleted = false
    private var retriedWithFreshScan = false
    private var scanConnectRetryCount = 0
    private var explicitRetryCount = 0
    private var lastConnectHadServiceError = false
    private var connectOrigin: ConnectOrigin = ConnectOrigin.AUTO_SCAN
    private var lastTargetDevice: BluetoothDevice? = null
    private var lastConnectScope: CoroutineScope? = null

    private var simulateGestureJob: Job? = null
    @Volatile private var isPlayingGestureSimulation = false

    private var mtuDiscoverFallbackRunnable: Runnable? = null
    private var connectionTimeoutRunnable: Runnable? = null
    private var cccdFallbackRunnable: Runnable? = null
    private var scanTimeoutRunnable: Runnable? = null
    private var discoveryTimeoutRunnable: Runnable? = null
    private var pendingScanConnectRunnable: Runnable? = null
    private var pendingRetryRunnable: Runnable? = null

    fun setCalibrationProfile(profile: GloveCalibrationProfile) {
        activeCalibration = profile
    }

    private fun hasScanPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        }
    }

    private fun hasConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) ==
                PackageManager.PERMISSION_GRANTED
        }
    }

    private fun isLocationEnabledOnLegacyAndroid(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) return true
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        return try {
            LocationManagerCompat.isLocationEnabled(lm)
        } catch (_: Exception) {
            true
        }
    }

    private fun isLikelyGloveName(name: String): Boolean {
        if (name.isBlank()) return false
        return name.equals(TARGET_DEVICE_NAME, ignoreCase = true) ||
            name.equals(TARGET_BLE_NAME, ignoreCase = true) ||
            name.contains("Guante", ignoreCase = true) ||
            name.contains("ESP32", ignoreCase = true) ||
            name.contains("S.11", ignoreCase = true) ||
            name.contains("S11", ignoreCase = true) ||
            name.contains("Glove", ignoreCase = true) ||
            name.contains("Rimay", ignoreCase = true) ||
            name.contains("Robotic", ignoreCase = true)
    }

    @SuppressLint("MissingPermission")
    private fun safeDeviceName(device: BluetoothDevice?, fallback: String = TARGET_DEVICE_NAME): String {
        if (device == null) return fallback
        if (!hasConnectPermission()) return fallback
        return try {
            device.name?.takeIf { it.isNotBlank() } ?: fallback
        } catch (_: SecurityException) {
            fallback
        }
    }

    @SuppressLint("MissingPermission")
    fun getBondedDevices(): List<BluetoothDevice> {
        if (!hasConnectPermission()) return emptyList()
        return try {
            bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Inicia un escaneo BLE en vivo para poblar el selector modal con anuncios cercanos
     * (con nombre o con UUID de servicio del guante).
     */
    @SuppressLint("MissingPermission")
    fun startDiscoveryScan() {
        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            _connectionState.value = ConnectionState.Error("Activa el Bluetooth del teléfono para buscar el guante")
            return
        }
        if (!hasScanPermission()) {
            _connectionState.value = ConnectionState.Error("Falta permiso de escaneo Bluetooth / Ubicación")
            return
        }
        if (!isLocationEnabledOnLegacyAndroid()) {
            _connectionState.value = ConnectionState.Error("Activa la Ubicación (GPS) para detectar señales BLE")
            return
        }

        val scanner = adapter.bluetoothLeScanner
        if (scanner == null) {
            _connectionState.value = ConnectionState.Error("Escáner BLE no disponible en este teléfono")
            return
        }

        stopActiveScansOnly()
        discoveredMap.clear()
        _scannedDevices.value = emptyList()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        val cb = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                if (this !== discoveryScanCallback) return
                val r = result ?: return
                val device = r.device ?: return
                val addr = device.address ?: return
                val serviceUuids = r.scanRecord?.serviceUuids?.map { it.uuid } ?: emptyList()
                val matchesUuid = serviceUuids.contains(SERVICE_UUID)
                val recordName = r.scanRecord?.deviceName?.trim().orEmpty()
                val bondedName = if (hasConnectPermission()) {
                    try { device.name?.trim().orEmpty() } catch (_: SecurityException) { "" }
                } else ""

                val existing = discoveredMap[addr]
                val resolvedName = recordName.ifBlank { bondedName }.ifBlank { existing?.name.orEmpty() }
                val hasGlove = matchesUuid || (existing?.hasGloveService == true) || isLikelyGloveName(resolvedName)

                // Filtrar balizas anónimas sin nombre ni UUID del guante para mantener la lista limpia
                if (resolvedName.isBlank() && !hasGlove) return

                val entry = ScannedBleDevice(
                    device = device,
                    name = resolvedName.ifBlank { "Guante BLE ($addr)" },
                    address = addr,
                    rssi = r.rssi,
                    hasGloveService = matchesUuid || (existing?.hasGloveService == true),
                    lastSeenMs = System.currentTimeMillis()
                )
                discoveredMap[addr] = entry
                publishSortedDiscoveredDevices()
            }

            override fun onScanFailed(errorCode: Int) {
                emitLog("[BLE] Error en escaneo de descubrimiento: código $errorCode")
            }
        }
        discoveryScanCallback = cb

        try {
            scanner.startScan(null, settings, cb)
            emitLog("[BLE] Escaneo en vivo abierto para selector de dispositivos...")
            val timeoutRun = Runnable {
                stopDiscoveryScan()
            }
            discoveryTimeoutRunnable = timeoutRun
            mainHandler.postDelayed(timeoutRun, 20_000L)
        } catch (e: Exception) {
            discoveryScanCallback = null
            emitLog("[BLE ERROR] No se pudo iniciar búsqueda en vivo: ${e.localizedMessage}")
        }
    }

    @SuppressLint("MissingPermission")
    fun stopDiscoveryScan() {
        discoveryTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        discoveryTimeoutRunnable = null
        discoveryScanCallback?.let { cb ->
            if (hasScanPermission()) {
                try {
                    bluetoothAdapter?.bluetoothLeScanner?.stopScan(cb)
                } catch (_: Exception) {}
            }
            discoveryScanCallback = null
        }
    }

    private fun publishSortedDiscoveredDevices() {
        val sorted = discoveredMap.values.sortedWith(
            compareByDescending<ScannedBleDevice> { it.hasGloveService }
                .thenByDescending { it.name.equals(TARGET_DEVICE_NAME, ignoreCase = true) || it.name.equals(TARGET_BLE_NAME, ignoreCase = true) }
                .thenByDescending { isLikelyGloveName(it.name) }
                .thenByDescending { it.rssi }
        )
        _scannedDevices.value = sorted
    }

    fun connectExplicitDevice(device: BluetoothDevice, coroutineScope: CoroutineScope) {
        if (isSimulating) {
            stopSimulation()
        }
        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            _connectionState.value = ConnectionState.Error("Bluetooth desactivado")
            return
        }
        if (!hasConnectPermission()) {
            _connectionState.value = ConnectionState.Error("Falta permiso BLUETOOTH_CONNECT")
            return
        }

        disconnectInternal(emitDisconnectedState = false)
        synchronized(gattLock) {
            lastConnectScope = coroutineScope
            connectOrigin = ConnectOrigin.EXPLICIT
            lastTargetDevice = device
            retriedWithFreshScan = true
            scanConnectRetryCount = 0
            explicitRetryCount = 0
        }
        connectToDevice(device)
    }

    /**
     * Inicia conexión automática al Guante ESP32 BLE:
     * 1. Verifica Bluetooth encendido, permisos y caché de vinculados BLE/Dual.
     * 2. Si existe un vinculado BLE compatible, intenta enlace directo con watchdog de 5.5s.
     * 3. Si falla (Error GATT 133 / tiempo agotado) o no hay vinculado, ejecuta escaneo BLE limpio.
     */
    @SuppressLint("MissingPermission")
    fun startScanAndConnect(coroutineScope: CoroutineScope) {
        if (isSimulating) {
            stopSimulation()
        }

        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            _connectionState.value = ConnectionState.Error("Bluetooth no disponible o desactivado")
            return
        }
        if (!hasConnectPermission() || !hasScanPermission()) {
            _connectionState.value = ConnectionState.Error("Concede los permisos de Bluetooth / Ubicación en Ajustes")
            return
        }

        disconnectInternal(emitDisconnectedState = false)
        synchronized(gattLock) {
            lastConnectScope = coroutineScope
            retriedWithFreshScan = false
            scanConnectRetryCount = 0
            explicitRetryCount = 0
            lastTargetDevice = null
        }

        val bondedDevices = try {
            adapter.bondedDevices
        } catch (_: Exception) {
            null
        }
        val bondedGlove = bondedDevices?.firstOrNull { dev ->
            val type = try { dev.type } catch (_: Exception) { BluetoothDevice.DEVICE_TYPE_UNKNOWN }
            if (type == BluetoothDevice.DEVICE_TYPE_CLASSIC) return@firstOrNull false
            val name = try { dev.name.orEmpty() } catch (_: Exception) { "" }
            isLikelyGloveName(name)
        }

        if (bondedGlove != null) {
            val bName = safeDeviceName(bondedGlove)
            synchronized(gattLock) {
                connectOrigin = ConnectOrigin.AUTO_BONDED
                lastTargetDevice = bondedGlove
            }
            emitLog("[BLE] Guante vinculado encontrado: '$bName'. Probando enlace GATT...")
            connectToDevice(bondedGlove)
            return
        }

        synchronized(gattLock) {
            connectOrigin = ConnectOrigin.AUTO_SCAN
        }
        startBleScan(coroutineScope)
    }

    @SuppressLint("MissingPermission")
    private fun startBleScan(coroutineScope: CoroutineScope) {
        if (!hasScanPermission()) {
            _connectionState.value = ConnectionState.Error("Falta permiso de escaneo BLE")
            return
        }
        if (!isLocationEnabledOnLegacyAndroid()) {
            _connectionState.value = ConnectionState.Error("Activa la Ubicación (GPS) para escanear BLE")
            return
        }

        val scanner = bluetoothAdapter?.bluetoothLeScanner
        if (scanner == null) {
            _connectionState.value = ConnectionState.Error("El escáner BLE no se pudo inicializar")
            return
        }

        stopActiveScansOnly()
        synchronized(gattLock) {
            isConnectingAttempt = false
            connectOrigin = ConnectOrigin.AUTO_SCAN
            pendingScanConnectRunnable?.let { mainHandler.removeCallbacks(it) }
            pendingScanConnectRunnable = null
        }
        _connectionState.value = ConnectionState.Scanning
        emitLog("[BLE] Escaneando guante '$TARGET_DEVICE_NAME' (UUID: $SERVICE_UUID)...")

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        val cb = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                val r = result ?: return
                val device = r.device ?: return
                val recordName = r.scanRecord?.deviceName?.trim().orEmpty()
                val deviceName = if (hasConnectPermission()) {
                    try { device.name?.trim().orEmpty() } catch (_: SecurityException) { "" }
                } else ""
                val devName = recordName.ifBlank { deviceName }
                val serviceUuids = r.scanRecord?.serviceUuids?.map { it.uuid } ?: emptyList()

                val matchesUuid = serviceUuids.contains(SERVICE_UUID)
                val matchesName = isLikelyGloveName(devName)
                if (!matchesUuid && !matchesName) return

                val accepted = synchronized(gattLock) {
                    if (this !== scanCallback || isConnectingAttempt || pendingScanConnectRunnable != null) {
                        false
                    } else {
                        isConnectingAttempt = true
                        true
                    }
                }
                if (!accepted) return

                val label = devName.ifBlank { TARGET_DEVICE_NAME }
                emitLog("[BLE OK] Guante detectado: '$label' [${device.address}, ${r.rssi} dBm]. Estabilizando radio (180 ms)...")
                stopAutoScanOnly()
                _connectionState.value = ConnectionState.Connecting(label)

                // Pausa breve de 180 ms entre stopScan() y connectGatt() para evitar Error GATT 133 en Android
                val connectRun = Runnable {
                    val proceed = synchronized(gattLock) {
                        pendingScanConnectRunnable = null
                        isConnectingAttempt
                    }
                    if (proceed) {
                        synchronized(gattLock) {
                            lastConnectScope = coroutineScope
                            connectOrigin = ConnectOrigin.AUTO_SCAN
                            lastTargetDevice = device
                        }
                        connectToDevice(device)
                    }
                }
                synchronized(gattLock) {
                    pendingScanConnectRunnable = connectRun
                }
                mainHandler.postDelayed(connectRun, 180L)
            }

            override fun onScanFailed(errorCode: Int) {
                emitLog("[BLE ERROR] Fallo de escaneo BLE código: $errorCode")
                stopAutoScanOnly()
                synchronized(gattLock) {
                    isConnectingAttempt = false
                }
                _connectionState.value = ConnectionState.Error("Fallo al escanear BLE (código $errorCode)")
            }
        }
        scanCallback = cb

        try {
            scanner.startScan(null, settings, cb)
            val timeoutRun = Runnable {
                val shouldTimeout = synchronized(gattLock) {
                    pendingScanConnectRunnable == null && _connectionState.value is ConnectionState.Scanning
                }
                if (shouldTimeout) {
                    stopAutoScanOnly()
                    emitLog("[BLE] Tiempo de escaneo agotado. Verifica que '$TARGET_DEVICE_NAME' esté encendido.")
                    _connectionState.value = ConnectionState.Error("No se encontró 'Guante_ESP32' cerca (verifica encendido)")
                }
            }
            scanTimeoutRunnable = timeoutRun
            mainHandler.postDelayed(timeoutRun, 12_000L)
        } catch (e: Exception) {
            stopAutoScanOnly()
            _connectionState.value = ConnectionState.Error("Error al iniciar escaneo: ${e.localizedMessage}")
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectToDevice(device: BluetoothDevice) {
        if (!hasConnectPermission()) {
            synchronized(gattLock) { isConnectingAttempt = false }
            _connectionState.value = ConnectionState.Error("Falta permiso BLUETOOTH_CONNECT")
            return
        }

        stopActiveScansOnly()
        cancelWatchdogs()
        closeCurrentGattSilently()
        jsonFramer.clear()

        val devName = safeDeviceName(device)
        synchronized(gattLock) {
            lastTargetDevice = device
            isConnectingAttempt = true
            isOpeningGatt = true
            discoveredOnce = false
            cccdCompleted = false
            lastConnectHadServiceError = false
        }

        _connectionState.value = ConnectionState.Connecting(devName)
        emitLog("[BLE] Estableciendo enlace GATT con $devName [${device.address}]...")

        val gatt = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
            } else {
                device.connectGatt(context, false, gattCallback)
            }
        } catch (e: Exception) {
            emitLog("[BLE ERROR] Excepción al abrir GATT: ${e.localizedMessage}")
            null
        }

        var abortedByUser = false
        synchronized(gattLock) {
            isOpeningGatt = false
            if (gatt == null) {
                isConnectingAttempt = false
            } else if (!isConnectingAttempt) {
                // El usuario canceló o desconectó mientras se invocaba connectGatt()
                abortedByUser = true
            } else {
                bluetoothGatt = gatt
            }
        }

        if (abortedByUser) {
            try { gatt?.disconnect() } catch (_: Exception) {}
            try { gatt?.close() } catch (_: Exception) {}
            return
        }

        if (gatt == null) {
            _connectionState.value = ConnectionState.Error("No se pudo iniciar enlace GATT con $devName")
            return
        }

        scheduleConnectionTimeout(gatt, devName)
    }

    private fun scheduleConnectionTimeout(targetGatt: BluetoothGatt, devName: String) {
        synchronized(gattLock) {
            connectionTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }
            val timeoutRun = Runnable {
                handleConnectionTimeout(targetGatt, devName)
            }
            connectionTimeoutRunnable = timeoutRun
            mainHandler.postDelayed(timeoutRun, 5_500L)
        }
    }

    private fun handleConnectionTimeout(targetGatt: BluetoothGatt, devName: String) {
        var retryAction: (() -> Unit)? = null
        val timedOut = synchronized(gattLock) {
            if (bluetoothGatt !== targetGatt || !isConnectingAttempt) {
                false
            } else {
                when {
                    connectOrigin == ConnectOrigin.AUTO_BONDED && !retriedWithFreshScan && lastConnectScope != null -> {
                        retriedWithFreshScan = true
                        retryAction = { lastConnectScope?.let { startBleScan(it) } }
                    }
                    connectOrigin == ConnectOrigin.AUTO_SCAN && scanConnectRetryCount < 1 && lastConnectScope != null -> {
                        scanConnectRetryCount++
                        retryAction = { lastConnectScope?.let { startBleScan(it) } }
                    }
                    connectOrigin == ConnectOrigin.EXPLICIT && explicitRetryCount < 1 && lastTargetDevice != null -> {
                        explicitRetryCount++
                        val dev = lastTargetDevice!!
                        retryAction = { connectToDevice(dev) }
                    }
                    else -> {
                        isConnectingAttempt = false
                    }
                }
                true
            }
        }
        if (!timedOut) return

        emitLog("[BLE] Tiempo de enlace agotado con $devName. Limpiando caché GATT...")
        refreshGattCache(targetGatt)
        closeTargetGatt(targetGatt)

        val action = retryAction
        if (action != null) {
            emitLog("[BLE] Reintentando conexión BLE limpia...")
            val retryRun = Runnable {
                val stillValid = synchronized(gattLock) {
                    pendingRetryRunnable = null
                    isConnectingAttempt
                }
                if (stillValid) action()
            }
            synchronized(gattLock) {
                pendingRetryRunnable = retryRun
            }
            mainHandler.postDelayed(retryRun, 400L)
        } else {
            _connectionState.value = ConnectionState.Error("Tiempo de espera agotado con $devName")
        }
    }

    @SuppressLint("MissingPermission")
    private fun discoverServicesInternal(gatt: BluetoothGatt?) {
        val shouldRun = synchronized(gattLock) {
            if (gatt == null || gatt !== bluetoothGatt || discoveredOnce) {
                false
            } else {
                discoveredOnce = true
                mtuDiscoverFallbackRunnable?.let { mainHandler.removeCallbacks(it) }
                mtuDiscoverFallbackRunnable = null
                true
            }
        }
        if (!shouldRun || gatt == null) return

        val ok = try {
            gatt.discoverServices()
        } catch (e: Exception) {
            emitLog("[BLE ERROR] discoverServices lanzó excepción: ${e.localizedMessage}")
            false
        }
        if (!ok) {
            emitLog("[BLE ERROR] No se pudo iniciar discoverServices() en GATT")
            synchronized(gattLock) {
                lastConnectHadServiceError = true
            }
            _connectionState.value = ConnectionState.Error("Error al inspeccionar servicios del guante")
            try { gatt.disconnect() } catch (_: Exception) {}
        }
    }

    @SuppressLint("MissingPermission")
    private fun scheduleCccdFallback(targetGatt: BluetoothGatt, devName: String, address: String) {
        synchronized(gattLock) {
            cccdFallbackRunnable?.let { mainHandler.removeCallbacks(it) }
            val run = Runnable {
                val shouldComplete = synchronized(gattLock) {
                    bluetoothGatt === targetGatt && !cccdCompleted
                }
                if (shouldComplete) {
                    completeConnectedHandshake(targetGatt, devName, address, viaFallback = true)
                }
            }
            cccdFallbackRunnable = run
            mainHandler.postDelayed(run, 1_200L)
        }
    }

    private fun completeConnectedHandshake(
        gatt: BluetoothGatt,
        devName: String,
        address: String,
        viaFallback: Boolean
    ) {
        val completedNow = synchronized(gattLock) {
            if (gatt !== bluetoothGatt || cccdCompleted) {
                false
            } else {
                cccdCompleted = true
                isConnectingAttempt = false
                scanConnectRetryCount = 0
                explicitRetryCount = 0
                true
            }
        }
        if (!completedNow) return

        cancelWatchdogs()
        _connectionState.value = ConnectionState.Connected(devName, address)
        val suffix = if (viaFallback) " (CCCD watchdog)" else ""
        emitLog("[BLE OK] Telemetría háptica activa en $devName$suffix (JSON a 100ms).")
    }

    private fun refreshGattCache(gatt: BluetoothGatt?) {
        if (gatt == null) return
        try {
            val refreshMethod = gatt.javaClass.getMethod("refresh")
            refreshMethod.invoke(gatt)
        } catch (_: Exception) {
        }
    }

    @SuppressLint("MissingPermission")
    private fun closeTargetGatt(targetGatt: BluetoothGatt?) {
        if (targetGatt == null) return
        synchronized(gattLock) {
            if (bluetoothGatt === targetGatt) {
                bluetoothGatt = null
            }
        }
        try { targetGatt.disconnect() } catch (_: Exception) {}
        try { targetGatt.close() } catch (_: Exception) {}
    }

    @SuppressLint("MissingPermission")
    private fun closeCurrentGattSilently() {
        val existing = synchronized(gattLock) {
            val g = bluetoothGatt
            bluetoothGatt = null
            isOpeningGatt = false
            g
        }
        if (existing != null) {
            try { existing.disconnect() } catch (_: Exception) {}
            try { existing.close() } catch (_: Exception) {}
        }
    }

    private fun cancelWatchdogs() {
        synchronized(gattLock) {
            mtuDiscoverFallbackRunnable?.let { mainHandler.removeCallbacks(it) }
            mtuDiscoverFallbackRunnable = null
            connectionTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }
            connectionTimeoutRunnable = null
            cccdFallbackRunnable?.let { mainHandler.removeCallbacks(it) }
            cccdFallbackRunnable = null
            pendingScanConnectRunnable?.let { mainHandler.removeCallbacks(it) }
            pendingScanConnectRunnable = null
            pendingRetryRunnable?.let { mainHandler.removeCallbacks(it) }
            pendingRetryRunnable = null
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopAutoScanOnly() {
        scanTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        scanTimeoutRunnable = null
        scanCallback?.let { cb ->
            if (hasScanPermission()) {
                try {
                    bluetoothAdapter?.bluetoothLeScanner?.stopScan(cb)
                } catch (_: Exception) {}
            }
            scanCallback = null
        }
    }

    private fun stopActiveScansOnly() {
        stopAutoScanOnly()
        stopDiscoveryScan()
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (gatt == null) return

            val isCurrent = synchronized(gattLock) {
                if (bluetoothGatt == null && isOpeningGatt && isConnectingAttempt) {
                    bluetoothGatt = gatt
                    true
                } else {
                    gatt === bluetoothGatt
                }
            }
            if (!isCurrent) {
                try { gatt.close() } catch (_: Exception) {}
                return
            }

            val devName = safeDeviceName(gatt.device)
            val address = gatt.device?.address ?: ""

            if (newState == BluetoothProfile.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS) {
                emitLog("[BLE] Enlace GATT establecido con $devName. Verificando servicio y CCCD...")
                _connectionState.value = ConnectionState.Connecting(devName)
                synchronized(gattLock) {
                    discoveredOnce = false
                    cccdCompleted = false
                    val fallbackRun = Runnable { discoverServicesInternal(gatt) }
                    mtuDiscoverFallbackRunnable?.let { mainHandler.removeCallbacks(it) }
                    mtuDiscoverFallbackRunnable = fallbackRun
                    mainHandler.postDelayed(fallbackRun, 1_000L)
                }
                scheduleConnectionTimeout(gatt, devName)

                val mtuReq = try {
                    gatt.requestMtu(512)
                } catch (_: Exception) {
                    false
                }
                if (!mtuReq) {
                    emitLog("[BLE] requestMtu no aceptado, descubriendo servicios directo...")
                    discoverServicesInternal(gatt)
                }
            } else {
                // STATE_DISCONNECTED o error de estado GATT (p. ej. 133, 8, 19, 257)
                var retryAction: (() -> Unit)? = null
                var notifyDisconnect = false
                var hadServiceErr = false
                var wasConnecting = false

                synchronized(gattLock) {
                    mtuDiscoverFallbackRunnable?.let { mainHandler.removeCallbacks(it) }
                    mtuDiscoverFallbackRunnable = null
                    cccdFallbackRunnable?.let { mainHandler.removeCallbacks(it) }
                    cccdFallbackRunnable = null
                    connectionTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }
                    connectionTimeoutRunnable = null

                    if (bluetoothGatt === gatt) {
                        bluetoothGatt = null
                    }
                    hadServiceErr = lastConnectHadServiceError
                    wasConnecting = isConnectingAttempt

                    if (hadServiceErr) {
                        isConnectingAttempt = false
                    } else if (wasConnecting) {
                        val isTransientLinkError = status == 133 || status == 8 || status == 257 || status == 62
                        when {
                            connectOrigin == ConnectOrigin.AUTO_BONDED && !retriedWithFreshScan && lastConnectScope != null -> {
                                retriedWithFreshScan = true
                                retryAction = { lastConnectScope?.let { startBleScan(it) } }
                            }
                            connectOrigin == ConnectOrigin.AUTO_SCAN && scanConnectRetryCount < 1 && lastConnectScope != null -> {
                                scanConnectRetryCount++
                                retryAction = { lastConnectScope?.let { startBleScan(it) } }
                            }
                            connectOrigin == ConnectOrigin.EXPLICIT && isTransientLinkError && explicitRetryCount < 1 && lastTargetDevice != null -> {
                                explicitRetryCount++
                                val dev = lastTargetDevice!!
                                retryAction = { connectToDevice(dev) }
                            }
                            else -> {
                                isConnectingAttempt = false
                            }
                        }
                    } else {
                        cccdCompleted = false
                        discoveredOnce = false
                        notifyDisconnect = true
                    }
                }

                if (status != BluetoothGatt.GATT_SUCCESS) {
                    refreshGattCache(gatt)
                }
                try { gatt.close() } catch (_: Exception) {}

                if (hadServiceErr) return

                if (wasConnecting) {
                    val action = retryAction
                    if (action != null) {
                        emitLog("[BLE] Error GATT $status con $devName. Limpiando caché y reintentando enlace...")
                        val retryRun = Runnable {
                            val stillValid = synchronized(gattLock) {
                                pendingRetryRunnable = null
                                isConnectingAttempt
                            }
                            if (stillValid) action()
                        }
                        synchronized(gattLock) {
                            pendingRetryRunnable = retryRun
                        }
                        mainHandler.postDelayed(retryRun, 450L)
                    } else {
                        emitLog("[BLE ERROR] Fallo conexión GATT con $devName (status: $status)")
                        _connectionState.value = ConnectionState.Error("Error BLE GATT $status al conectar $devName")
                    }
                } else if (notifyDisconnect) {
                    if (status != 0 && status != 19) {
                        emitLog("[BLE] Enlace interrumpido con $devName (status: $status)")
                        _connectionState.value = ConnectionState.Error("Conexión perdida con $devName (código $status)")
                    } else {
                        emitLog("[BLE] Desconectado de $devName (status: $status)")
                        _connectionState.value = ConnectionState.Disconnected
                    }
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            val isCurrent = synchronized(gattLock) { gatt != null && gatt === bluetoothGatt }
            if (!isCurrent) return
            emitLog("[BLE] MTU negociado: $mtu bytes (status: $status). Descubriendo servicios...")
            discoverServicesInternal(gatt)
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            val isCurrent = synchronized(gattLock) {
                if (gatt == null || gatt !== bluetoothGatt) {
                    false
                } else {
                    mtuDiscoverFallbackRunnable?.let { mainHandler.removeCallbacks(it) }
                    mtuDiscoverFallbackRunnable = null
                    true
                }
            }
            if (!isCurrent || gatt == null) return

            if (status != BluetoothGatt.GATT_SUCCESS) {
                emitLog("[BLE ERROR] Error al descubrir servicios GATT (status: $status)")
                synchronized(gattLock) { lastConnectHadServiceError = true }
                _connectionState.value = ConnectionState.Error("Error descubriendo servicios BLE ($status)")
                try { gatt.disconnect() } catch (_: Exception) {}
                return
            }

            emitLog("[BLE] Servicios descubiertos. Verificando UUID $SERVICE_UUID...")
            val service = gatt.getService(SERVICE_UUID)
            if (service == null) {
                val present = gatt.services.joinToString(", ") { it.uuid.toString().take(8) }
                emitLog("[BLE ERROR] Servicio $SERVICE_UUID ausente. Encontrados: [$present]")
                synchronized(gattLock) { lastConnectHadServiceError = true }
                _connectionState.value = ConnectionState.Error("Dispositivo incompatible (no es Guante_ESP32)")
                try { gatt.disconnect() } catch (_: Exception) {}
                return
            }

            val characteristic = service.getCharacteristic(CHARACTERISTIC_UUID)
            if (characteristic == null) {
                emitLog("[BLE ERROR] Característica $CHARACTERISTIC_UUID ausente en el servicio")
                synchronized(gattLock) { lastConnectHadServiceError = true }
                _connectionState.value = ConnectionState.Error("Característica de telemetría no encontrada")
                try { gatt.disconnect() } catch (_: Exception) {}
                return
            }

            val registered = gatt.setCharacteristicNotification(characteristic, true)
            if (!registered) {
                emitLog("[BLE ERROR] setCharacteristicNotification devolvió false")
                synchronized(gattLock) { lastConnectHadServiceError = true }
                _connectionState.value = ConnectionState.Error("No se pudieron activar notificaciones BLE")
                try { gatt.disconnect() } catch (_: Exception) {}
                return
            }

            val devName = safeDeviceName(gatt.device)
            val address = gatt.device?.address ?: ""
            val props = characteristic.properties
            val enableValue = if ((props and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            } else if ((props and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) {
                BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            } else {
                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            }

            val descriptor = characteristic.getDescriptor(CCCD_DESCRIPTOR_UUID)
            if (descriptor != null) {
                val writeInitiated = try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        gatt.writeDescriptor(descriptor, enableValue) == BluetoothStatusCodes.SUCCESS
                    } else {
                        @Suppress("DEPRECATION")
                        descriptor.value = enableValue
                        @Suppress("DEPRECATION")
                        gatt.writeDescriptor(descriptor)
                    }
                } catch (e: Exception) {
                    emitLog("[BLE WARN] Excepción escribiendo CCCD: ${e.localizedMessage}")
                    false
                }
                if (!writeInitiated) {
                    emitLog("[BLE WARN] writeDescriptor no devolvió true; esperando respuesta/tramas...")
                }
                scheduleCccdFallback(gatt, devName, address)
            } else {
                completeConnectedHandshake(gatt, devName, address, viaFallback = false)
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            val isCurrent = synchronized(gattLock) {
                if (gatt == null || gatt !== bluetoothGatt) false else {
                    if (descriptor?.uuid == CCCD_DESCRIPTOR_UUID) {
                        cccdFallbackRunnable?.let { mainHandler.removeCallbacks(it) }
                        cccdFallbackRunnable = null
                    }
                    true
                }
            }
            if (!isCurrent || gatt == null) return

            if (descriptor?.uuid == CCCD_DESCRIPTOR_UUID) {
                val devName = safeDeviceName(gatt.device)
                val address = gatt.device?.address ?: ""
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    completeConnectedHandshake(gatt, devName, address, viaFallback = false)
                } else {
                    emitLog("[BLE ERROR] Error al escribir descriptor CCCD (status: $status)")
                    synchronized(gattLock) { lastConnectHadServiceError = true }
                    _connectionState.value = ConnectionState.Error("Error activando notificaciones CCCD ($status)")
                    try { gatt.disconnect() } catch (_: Exception) {}
                }
            }
        }

        @Deprecated("Deprecated for Android 13+, handled by onCharacteristicChanged(gatt, characteristic, value)")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            var accept = false
            var shouldCompleteHandshake = false
            synchronized(gattLock) {
                if (gatt != null && gatt === bluetoothGatt) {
                    accept = true
                    shouldCompleteHandshake = !cccdCompleted
                }
            }
            if (!accept || gatt == null) return
            if (shouldCompleteHandshake) {
                completeConnectedHandshake(gatt, safeDeviceName(gatt.device), gatt.device?.address ?: "", viaFallback = false)
            }
            @Suppress("DEPRECATION")
            val bytes = characteristic?.value ?: return
            handleIncomingBytes(bytes)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            var accept = false
            var shouldCompleteHandshake = false
            synchronized(gattLock) {
                if (gatt === bluetoothGatt) {
                    accept = true
                    shouldCompleteHandshake = !cccdCompleted
                }
            }
            if (!accept) return
            if (shouldCompleteHandshake) {
                completeConnectedHandshake(gatt, safeDeviceName(gatt.device), gatt.device?.address ?: "", viaFallback = false)
            }
            handleIncomingBytes(value)
        }
    }

    private fun handleIncomingBytes(bytes: ByteArray) {
        try {
            val frames = jsonFramer.appendBytes(bytes)
            for (completeJson in frames) {
                handleIncomingString(completeJson)
            }
        } catch (_: Exception) {}
    }

    private fun handleIncomingString(jsonStr: String) {
        try {
            val telemetry = GloveTelemetry.parseFromJson(jsonStr, activeCalibration)
            if (telemetry != null) {
                _telemetryFlow.tryEmit(telemetry)
            }
            emitLog(jsonStr)
        } catch (_: Exception) {}
    }

    fun disconnect() {
        disconnectInternal(emitDisconnectedState = true)
    }

    @SuppressLint("MissingPermission")
    private fun disconnectInternal(emitDisconnectedState: Boolean) {
        stopSimulation()
        sppJob?.cancel()
        sppJob = null
        jsonFramer.clear()
        try {
            sppSocket?.close()
        } catch (_: Exception) {}
        sppSocket = null

        synchronized(gattLock) {
            isConnectingAttempt = false
            isOpeningGatt = false
            discoveredOnce = false
            cccdCompleted = false
            lastConnectHadServiceError = false
            scanConnectRetryCount = 0
            explicitRetryCount = 0
        }

        cancelWatchdogs()
        stopActiveScansOnly()
        closeCurrentGattSilently()

        if (emitDisconnectedState) {
            _connectionState.value = ConnectionState.Disconnected
            emitLog("[BT] Estado desconectado.")
        }
    }

    // -------------------------------------------------------------
    // MOTOR DE SIMULACIÓN HÁPTICA PARA DESARROLLO Y EVALUACIÓN
    // -------------------------------------------------------------

    /**
     * Activa el modo de simulación de telemetría a 10Hz (100ms, idéntico a INTERVALO_MS del ESP32).
     * Permite probar el 100% de la aplicación cuando no se dispone del guante físico encendido.
     */
    fun startSimulationMode(scope: CoroutineScope) {
        disconnectInternal(emitDisconnectedState = false)
        isSimulating = true
        _connectionState.value = ConnectionState.SimulationMode
        emitLog("[SIMULADOR] Modo simulación activado. Generando tramas BLE virtuales a 10 Hz (100ms)...")

        simulationJob?.cancel()
        simulationJob = scope.launch(Dispatchers.Default) {
            var counter = 0
            while (isActive && isSimulating) {
                if (!isPlayingGestureSimulation) {
                    counter++
                    val f0 = 10 + (counter % 5)
                    val f1 = 12 + ((counter + 2) % 4)
                    val f2 = 8 + (counter % 3)
                    val f3 = 11 + ((counter + 1) % 4)
                    val f4 = 9 + (counter % 4)
                    val p = 2.4 + (counter % 3) * 0.2
                    val r = -1.2 + (counter % 4) * 0.1

                    val baseline = GloveTelemetry(
                        flex = listOf(f0, f1, f2, f3, f4),
                        rawFlex = listOf(f0, f1, f2, f3, f4),
                        imuPitch = p,
                        imuRoll = r,
                        imuYaw = 0.0,
                        touch = listOf(0, 0, 0),
                        gloss = "NINGUNA",
                        rawJson = """{"pulgar":$f0,"indice":$f1,"medio":$f2,"anular":$f3,"menique":$f4,"pitch":${String.format(java.util.Locale.US, "%.1f", p)},"roll":${String.format(java.util.Locale.US, "%.1f", r)}}""",
                        timestamp = System.currentTimeMillis()
                    )
                    _telemetryFlow.emit(baseline)
                }
                delay(100) // 10 Hz = 100 ms (ESP32 INTERVALO_MS)
            }
        }
    }

    /**
     * Simula la ejecución de una seña específica durante 800 ms (8 tramas a 10Hz)
     * para verificar la estabilización del Debouncer (5 muestras) y la traducción morfológica.
     */
    fun simulateSignGesture(scope: CoroutineScope, gloss: String) {
        simulateGestureJob?.cancel()
        if (!isSimulating) {
            startSimulationMode(scope)
        }

        isPlayingGestureSimulation = true
        simulateGestureJob = scope.launch(Dispatchers.Default) {
            try {
                emitLog("[SIMULADOR] Inyectando patrón sensorial Hall de seña: $gloss...")
                val (flexProfile, imuProfile, touchProfile) = getProfilesForGloss(gloss)

                // Emitir 8 muestras consecutivas (800ms > umbral de 5 muestras) sin flag touch (hardware ESP32 real)
                for (i in 0 until 8) {
                    val fList = flexProfile.map { (it + (i % 3)).coerceIn(0, 100) }
                    val p = imuProfile[0]
                    val r = imuProfile[1]
                    val frame = GloveTelemetry(
                        flex = fList,
                        rawFlex = fList,
                        imuPitch = p,
                        imuRoll = r,
                        imuYaw = 0.0,
                        touch = touchProfile,
                        gloss = "NINGUNA",
                        rawJson = """{"pulgar":${fList[0]},"indice":${fList[1]},"medio":${fList[2]},"anular":${fList[3]},"menique":${fList[4]},"pitch":$p,"roll":$r}""",
                        timestamp = System.currentTimeMillis()
                    )
                    _telemetryFlow.emit(frame)
                    delay(100)
                }

                // Retorno a reposo
                for (i in 0 until 4) {
                    val neutral = GloveTelemetry(
                        flex = listOf(10, 12, 10, 11, 10),
                        rawFlex = listOf(10, 12, 10, 11, 10),
                        imuPitch = 0.0,
                        imuRoll = 0.0,
                        touch = listOf(0, 0, 0),
                        gloss = "NINGUNA",
                        rawJson = """{"pulgar":10,"indice":12,"medio":10,"anular":11,"menique":10,"pitch":0.0,"roll":0.0}""",
                        timestamp = System.currentTimeMillis()
                    )
                    _telemetryFlow.emit(neutral)
                    delay(100)
                }
            } finally {
                isPlayingGestureSimulation = false
            }
        }
    }

    private fun getProfilesForGloss(gloss: String): Triple<List<Int>, List<Double>, List<Int>> {
        val item = MorphologyEngine.lexicon[gloss.trim().uppercase()]
        return if (item != null) {
            Triple(item.expectedFlex, item.expectedImu, listOf(0, 0, 0))
        } else {
            Triple(listOf(50, 50, 50, 50, 50), listOf(0.0, 0.0), listOf(0, 0, 0))
        }
    }

    private fun stopSimulation() {
        isPlayingGestureSimulation = false
        simulateGestureJob?.cancel()
        simulateGestureJob = null
        isSimulating = false
        simulationJob?.cancel()
        simulationJob = null
    }

    private fun emitLog(message: String) {
        _rawLogFlow.tryEmit(message)
    }
}
