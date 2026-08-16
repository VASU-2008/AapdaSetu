package com.example.aapdasetu.mesh

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import androidx.core.content.ContextCompat
import com.example.aapdasetu.model.DeliveryStatus
import com.example.aapdasetu.model.MeshMessage
import com.example.aapdasetu.model.MeshNode
import com.example.aapdasetu.model.MessageType
import com.example.aapdasetu.model.NodeStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class BluetoothMeshManager(private val context: Context) {

    companion object {
        val SERVICE_UUID: UUID = UUID.fromString("0000A1D0-0000-1000-8000-00805F9B34FB")
        val LOCAL_NODE_ID: String = "NODE_A_LOCAL"
        val LOCAL_NODE_NAME: String = "AapdaSetu Hub (You)"
    }

    private val scope = CoroutineScope(Dispatchers.Default + Job())

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
    private var advertiser: BluetoothLeAdvertiser? = null
    private var scanner: BluetoothLeScanner? = null

    private val _isBluetoothEnabled = MutableStateFlow(bluetoothAdapter?.isEnabled == true)
    val isBluetoothEnabled: StateFlow<Boolean> = _isBluetoothEnabled.asStateFlow()

    private val _meshStatus = MutableStateFlow("READY")
    val meshStatus: StateFlow<String> = _meshStatus.asStateFlow()

    private val _connectedNodes = MutableStateFlow<List<MeshNode>>(emptyList())
    val connectedNodes: StateFlow<List<MeshNode>> = _connectedNodes.asStateFlow()

    private val _messages = MutableStateFlow<List<MeshMessage>>(emptyList())
    val messages: StateFlow<List<MeshMessage>> = _messages.asStateFlow()

    private var isAdvertising = false
    private var isScanning = false

    init {
        initializeDefaultNodes()
    }

    private fun initializeDefaultNodes() {
        val initialNodes = listOf(
            MeshNode(
                id = "NODE_B",
                name = "Node B",
                status = NodeStatus.CONNECTED,
                rssi = -58,
                hopDistance = 1,
                batteryLevel = 92
            ),
            MeshNode(
                id = "NODE_C",
                name = "Node C",
                status = NodeStatus.AVAILABLE,
                rssi = -74,
                hopDistance = 2,
                batteryLevel = 78
            )
        )
        _connectedNodes.value = initialNodes

        val initialMessages = listOf(
            MeshMessage(
                id = "INIT_SOS_1",
                senderId = LOCAL_NODE_ID,
                senderName = "You",
                targetNode = "Node B",
                content = "SOS Broadcast Active",
                type = MessageType.SOS,
                hopCount = 1,
                status = DeliveryStatus.DELIVERED
            )
        )
        _messages.value = initialMessages
    }

    fun hasRequiredPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val scan = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
            val adv = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED
            val conn = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
            scan && adv && conn
        } else {
            val bt = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
            val loc = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            bt && loc
        }
    }

    fun startMesh() {
        _isBluetoothEnabled.value = bluetoothAdapter?.isEnabled == true || true // fallback true for testing
        _meshStatus.value = "READY"

        if (hasRequiredPermissions() && bluetoothAdapter?.isEnabled == true) {
            startBleScanning()
            startBleAdvertising()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startBleAdvertising() {
        if (isAdvertising || !hasRequiredPermissions()) return
        advertiser = bluetoothAdapter?.bluetoothLeAdvertiser
        if (advertiser == null) return

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(false)
            .build()

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addServiceUuid(ParcelUuid(SERVICE_UUID))
            .build()

        try {
            advertiser?.startAdvertising(settings, data, advertiseCallback)
            isAdvertising = true
        } catch (_: Exception) {
            // Ignored on unsupported hardware / emulators
        }
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            isAdvertising = true
        }

        override fun onStartFailure(errorCode: Int) {
            isAdvertising = false
        }
    }

    @SuppressLint("MissingPermission")
    private fun startBleScanning() {
        if (isScanning || !hasRequiredPermissions()) return
        scanner = bluetoothAdapter?.bluetoothLeScanner
        if (scanner == null) return

        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(SERVICE_UUID))
            .build()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        try {
            scanner?.startScan(listOf(filter), settings, scanCallback)
            isScanning = true
        } catch (_: Exception) {
            // Fallback gracefully
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.let {
                val serviceData = it.scanRecord?.getServiceData(ParcelUuid(SERVICE_UUID))
                if (serviceData != null) {
                    val decoded = PacketCodec.decode(serviceData)
                    if (decoded != null) {
                        onMessageReceived(decoded)
                    }
                }
            }
        }
    }

    fun sendSos(
        latitude: Double? = null,
        longitude: Double? = null,
        emergencyNote: String = "EMERGENCY: Immediate Assistance Needed"
    ) {
        val targetNodeName = _connectedNodes.value.firstOrNull { it.status == NodeStatus.CONNECTED }?.name ?: "Broadcast Mesh"
        val messageId = "SOS_${System.currentTimeMillis()}"

        val message = MeshMessage(
            id = messageId,
            senderId = LOCAL_NODE_ID,
            senderName = "You",
            targetNode = targetNodeName,
            content = emergencyNote,
            type = MessageType.SOS,
            latitude = latitude,
            longitude = longitude,
            hopCount = 1,
            status = DeliveryStatus.SENT
        )

        _messages.value = listOf(message) + _messages.value
        _meshStatus.value = "RELAYING"

        // Simulate multi-hop mesh propagation & acknowledgment
        scope.launch {
            delay(1200)
            // Update status to DELIVERED / RELAYED with checkmark
            _messages.value = _messages.value.map {
                if (it.id == messageId) it.copy(status = DeliveryStatus.DELIVERED) else it
            }
            _meshStatus.value = "READY"

            // Node B acknowledges and relays to Node C
            delay(1800)
            val ackMessage = MeshMessage(
                id = "ACK_${System.currentTimeMillis()}",
                senderId = "NODE_B",
                senderName = "Node B",
                targetNode = "Node C",
                content = "Relayed SOS to Node C (Disaster Sector 4)",
                type = MessageType.ACK,
                hopCount = 2,
                relayedBy = "Node B",
                status = DeliveryStatus.DELIVERED
            )
            _messages.value = listOf(ackMessage) + _messages.value
        }
    }

    fun broadcastReport(reportTitle: String, details: String, lat: Double?, lon: Double?) {
        val message = MeshMessage(
            id = "RPT_${System.currentTimeMillis()}",
            senderId = LOCAL_NODE_ID,
            senderName = "You",
            targetNode = "All Nodes",
            content = "$reportTitle: $details",
            type = MessageType.REPORT,
            latitude = lat,
            longitude = lon,
            hopCount = 1,
            status = DeliveryStatus.DELIVERED
        )
        _messages.value = listOf(message) + _messages.value
    }

    fun connectToNode(nodeId: String) {
        _connectedNodes.value = _connectedNodes.value.map {
            if (it.id == nodeId) it.copy(status = NodeStatus.CONNECTED) else it
        }
    }

    fun disconnectNode(nodeId: String) {
        _connectedNodes.value = _connectedNodes.value.map {
            if (it.id == nodeId) it.copy(status = NodeStatus.AVAILABLE) else it
        }
    }

    fun simulateNewNearbyNode(name: String) {
        val newNode = MeshNode(
            id = "NODE_${System.currentTimeMillis()}",
            name = name,
            status = NodeStatus.AVAILABLE,
            rssi = -68,
            hopDistance = 1,
            batteryLevel = 88
        )
        _connectedNodes.value = _connectedNodes.value + newNode
    }

    private fun onMessageReceived(message: MeshMessage) {
        if (_messages.value.none { it.id == message.id }) {
            _messages.value = listOf(message) + _messages.value
        }
    }
}
