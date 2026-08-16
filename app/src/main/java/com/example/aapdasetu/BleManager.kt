package com.example.aapdasetu

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import java.nio.charset.StandardCharsets
import java.util.ArrayDeque
import java.util.LinkedHashMap
import java.util.UUID

class BleManager(
    private val context: Context
) {

    companion object {

        private val SERVICE_UUID =
            UUID.fromString(
                "12345678-1234-1234-1234-1234567890AB"
            )

        private val NODE_ID_UUID =
            UUID.fromString(
                "12345678-1234-1234-1234-1234567890AC"
            )

        private val MESSAGE_UUID =
            UUID.fromString(
                "12345678-1234-1234-1234-1234567890AD"
            )

        private const val FRAME_START = "<ASSTART>"
        private const val FRAME_END = "<ASEND>"

        private val FRAME_START_BYTES =
            FRAME_START.toByteArray(StandardCharsets.UTF_8)

        private val FRAME_END_BYTES =
            FRAME_END.toByteArray(StandardCharsets.UTF_8)

        private const val DEFAULT_MTU = 23
        private const val REQUESTED_MTU = 247

        private const val MAX_CHUNK_SIZE = 244
        private const val MIN_CHUNK_SIZE = 20

        private const val MAX_TTL = 5
        private const val MAX_MESSAGES = 500

        private const val MAX_FRAME_SIZE =
            1024 * 1024

        private const val MAX_VOICE_PAYLOAD =
            700 * 1024

        private const val RECONNECT_DELAY = 3000L
        private const val RETRY_LOOP = 5000L
        private const val CHUNK_RETRY_DELAY = 350L
        private const val MAX_CHUNK_RETRIES = 5
        private const val CONNECTION_TIMEOUT = 15000L
    }

    private val bluetoothManager =
        context.getSystemService(
            Context.BLUETOOTH_SERVICE
        ) as BluetoothManager

    private val bluetoothAdapter: BluetoothAdapter? =
        bluetoothManager.adapter

    private val advertiser: BluetoothLeAdvertiser?
        get() = bluetoothAdapter?.bluetoothLeAdvertiser

    private val scanner: BluetoothLeScanner?
        get() = bluetoothAdapter?.bluetoothLeScanner

    private var nodeId: String = ""

    private var gattServer:
            BluetoothGattServer? = null

    var onStatusChanged:
            ((String) -> Unit)? = null

    var onNodeDiscovered:
            ((String) -> Unit)? = null

    var onMessageReceived:
            ((HopMessage) -> Unit)? = null

    var onMessageHistoryChanged:
            ((List<HopMessage>) -> Unit)? = null

    // =========================================================
    // MESSAGE
    // =========================================================

    data class HopMessage(

        val messageId: String,

        val senderId: String,

        val message: String,

        val hopCount: Int,

        val ttl: Int,

        val hasLocation: Boolean = false,

        val latitude: Double = 0.0,

        val longitude: Double = 0.0,

        val isVoice: Boolean = false,

        /*
         * Time when the original message was created.
         */
        val timestamp: Long = System.currentTimeMillis(),

        /*
         * Voice recording duration.
         *
         * Zero for text messages.
         */
        val recordingDurationMs: Long = 0L
    ) {

        // =====================================================
        // ENCODE
        // =====================================================

        fun encode(): String {

            return buildString {

                /*
                 * AS4 = new protocol containing timestamp
                 * and recording duration.
                 */
                append("AS4")
                append("|")

                append(messageId)
                append("|")

                append(senderId)
                append("|")

                append(hopCount)
                append("|")

                append(ttl)
                append("|")

                append(
                    if (hasLocation) "1"
                    else "0"
                )
                append("|")

                append(latitude)
                append("|")

                append(longitude)
                append("|")

                append(
                    if (isVoice) "1"
                    else "0"
                )
                append("|")

                append(timestamp)
                append("|")

                append(recordingDurationMs)
                append("|")

                /*
                 * Final field is the actual message.
                 *
                 * limit=12 is used during decoding so any |
                 * inside the message remains in the final field.
                 */
                append(message)
            }
        }

        companion object {

            // =================================================
            // DECODE
            // =================================================

            fun decode(
                data: String
            ): HopMessage? {

                return try {

                    val clean =
                        data.trim(
                            '\u0000',
                            '\r',
                            '\n',
                            ' ',
                            '\t'
                        )

                    // =================================================
                    // NEW AS4 FORMAT
                    // =================================================

                    if (clean.startsWith("AS4|")) {

                        val parts =
                            clean.split(
                                "|",
                                limit = 12
                            )

                        if (parts.size != 12) {
                            return null
                        }

                        val messageId =
                            parts[1].trim()

                        val senderId =
                            parts[2].trim()

                        val hop =
                            parts[3]
                                .trim()
                                .toIntOrNull()
                                ?: return null

                        val ttl =
                            parts[4]
                                .trim()
                                .toIntOrNull()
                                ?: return null

                        val location =
                            parts[5].trim() == "1"

                        val latitude =
                            parts[6]
                                .trim()
                                .toDoubleOrNull()
                                ?: 0.0

                        val longitude =
                            parts[7]
                                .trim()
                                .toDoubleOrNull()
                                ?: 0.0

                        val voice =
                            parts[8].trim() == "1"

                        val timestamp =
                            parts[9]
                                .trim()
                                .toLongOrNull()
                                ?: System.currentTimeMillis()

                        val recordingDuration =
                            parts[10]
                                .trim()
                                .toLongOrNull()
                                ?: 0L

                        val message =
                            parts[11]

                        if (
                            messageId.isBlank()
                        ) {
                            return null
                        }

                        if (
                            senderId.isBlank()
                        ) {
                            return null
                        }

                        if (hop < 0) {
                            return null
                        }

                        if (ttl < 0) {
                            return null
                        }

                        if (
                            voice &&
                            message.length >
                            MAX_VOICE_PAYLOAD
                        ) {
                            return null
                        }

                        return HopMessage(

                            messageId =
                                messageId,

                            senderId =
                                senderId,

                            message =
                                message,

                            hopCount =
                                hop,

                            ttl =
                                ttl,

                            hasLocation =
                                location,

                            latitude =
                                latitude,

                            longitude =
                                longitude,

                            isVoice =
                                voice,

                            timestamp =
                                timestamp,

                            recordingDurationMs =
                                recordingDuration
                        )
                    }

                    // =================================================
                    // OLD AS3 FORMAT
                    // =================================================

                    if (clean.startsWith("AS3|")) {

                        val parts =
                            clean.split(
                                "|",
                                limit = 10
                            )

                        if (parts.size != 10) {
                            return null
                        }

                        val messageId =
                            parts[1].trim()

                        val senderId =
                            parts[2].trim()

                        val hop =
                            parts[3]
                                .trim()
                                .toIntOrNull()
                                ?: return null

                        val ttl =
                            parts[4]
                                .trim()
                                .toIntOrNull()
                                ?: return null

                        val location =
                            parts[5].trim() == "1"

                        val latitude =
                            parts[6]
                                .trim()
                                .toDoubleOrNull()
                                ?: 0.0

                        val longitude =
                            parts[7]
                                .trim()
                                .toDoubleOrNull()
                                ?: 0.0

                        val voice =
                            parts[8].trim() == "1"

                        val message =
                            parts[9]

                        return HopMessage(

                            messageId =
                                messageId,

                            senderId =
                                senderId,

                            message =
                                message,

                            hopCount =
                                hop,

                            ttl =
                                ttl,

                            hasLocation =
                                location,

                            latitude =
                                latitude,

                            longitude =
                                longitude,

                            isVoice =
                                voice,

                            /*
                             * Old messages didn't carry timestamp.
                             */
                            timestamp =
                                System.currentTimeMillis(),

                            recordingDurationMs =
                                0L
                        )
                    }

                    null

                } catch (_: Exception) {

                    null
                }
            }
        }
    }

    // =========================================================
    // PEER
    // =========================================================

    private class Peer(
        val device: BluetoothDevice
    ) {

        var gatt:
                BluetoothGatt? = null

        var nodeId:
                String? = null

        var messageCharacteristic:
                BluetoothGattCharacteristic? = null

        var ready = false

        var connecting = false

        var mtu =
            DEFAULT_MTU

        var receiveBuffer =
            ByteArray(0)

        val queue =
            ArrayDeque<HopMessage>()

        var currentMessage:
                HopMessage? = null

        var chunks:
                List<ByteArray>? = null

        var chunkIndex =
            -1

        var retryCount =
            0

        val sentMessages =
            HashMap<String, Int>()

        var reconnectScheduled =
            false
    }

    private val peers =
        LinkedHashMap<String, Peer>()

    private val storedMessages =
        LinkedHashMap<String, HopMessage>(
            MAX_MESSAGES,
            0.75f,
            true
        )

    private val bestHop =
        HashMap<String, Int>()

    private val handler =
        Handler(Looper.getMainLooper())

    private var retryRunnable:
            Runnable? = null

    private var scanCallback:
            ScanCallback? = null

    // =========================================================
    // START
    // =========================================================

    @SuppressLint("MissingPermission")
    fun start(
        myNodeId: String
    ) {

        nodeId =
            myNodeId.trim()

        if (nodeId.isBlank()) {

            onStatusChanged?.invoke(
                "INVALID NODE ID"
            )

            return
        }

        stopInternal()

        onStatusChanged?.invoke(
            "STARTING AAPDASETU MESH..."
        )

        if (
            bluetoothAdapter == null
        ) {

            onStatusChanged?.invoke(
                "BLUETOOTH NOT AVAILABLE"
            )

            return
        }

        if (
            bluetoothAdapter?.isEnabled != true
        ) {

            onStatusChanged?.invoke(
                "BLUETOOTH IS OFF"
            )

            return
        }

        startGattServer()

        startAdvertising()

        startScanning()

        startRetryLoop()

        notifyHistory()

        onStatusChanged?.invoke(
            "AAPDASETU MESH READY"
        )
    }

    // =========================================================
    // HISTORY
    // =========================================================

    fun getStoredMessages():
            List<HopMessage> {

        synchronized(
            storedMessages
        ) {

            return storedMessages.values.toList()
        }
    }

    fun getConnectedNodeCount():
            Int {

        synchronized(peers) {

            return peers.values.count {
                it.ready
            }
        }
    }

    // =========================================================
    // DELETE
    // =========================================================

    fun deleteMessage(
        messageId: String
    ) {

        synchronized(
            storedMessages
        ) {

            storedMessages.remove(
                messageId
            )
        }

        bestHop.remove(
            messageId
        )

        synchronized(peers) {

            for (peer in peers.values) {

                peer.queue.removeAll {
                    it.messageId ==
                            messageId
                }

                if (
                    peer.currentMessage
                        ?.messageId ==
                    messageId
                ) {

                    peer.currentMessage =
                        null

                    peer.chunks =
                        null

                    peer.chunkIndex =
                        -1

                    peer.retryCount =
                        0
                }

                peer.sentMessages.remove(
                    messageId
                )
            }
        }

        notifyHistory()
    }

    fun deleteAllMessages() {

        synchronized(
            storedMessages
        ) {

            storedMessages.clear()
        }

        bestHop.clear()

        synchronized(peers) {

            for (peer in peers.values) {

                peer.queue.clear()

                peer.currentMessage =
                    null

                peer.chunks =
                    null

                peer.chunkIndex =
                    -1

                peer.retryCount =
                    0

                peer.sentMessages.clear()
            }
        }

        notifyHistory()
    }

    // =========================================================
    // GATT SERVER
    // =========================================================

    @SuppressLint("MissingPermission")
    private fun startGattServer() {

        try {

            gattServer =
                bluetoothManager.openGattServer(
                    context,
                    serverCallback
                )

        } catch (
            e: Exception
        ) {

            onStatusChanged?.invoke(
                "GATT SERVER ERROR: ${e.message}"
            )

            return
        }

        if (
            gattServer == null
        ) {

            onStatusChanged?.invoke(
                "GATT SERVER FAILED"
            )

            return
        }

        val service =
            BluetoothGattService(
                SERVICE_UUID,
                BluetoothGattService
                    .SERVICE_TYPE_PRIMARY
            )

        val nodeCharacteristic =
            BluetoothGattCharacteristic(

                NODE_ID_UUID,

                BluetoothGattCharacteristic
                    .PROPERTY_READ,

                BluetoothGattCharacteristic
                    .PERMISSION_READ
            )

        nodeCharacteristic.value =
            nodeId.toByteArray(
                StandardCharsets.UTF_8
            )

        service.addCharacteristic(
            nodeCharacteristic
        )

        val messageCharacteristic =
            BluetoothGattCharacteristic(

                MESSAGE_UUID,

                BluetoothGattCharacteristic
                    .PROPERTY_WRITE,

                BluetoothGattCharacteristic
                    .PERMISSION_WRITE
            )

        service.addCharacteristic(
            messageCharacteristic
        )

        val added =
            gattServer?.addService(
                service
            )

        if (added == true) {

            onStatusChanged?.invoke(
                "GATT SERVER READY"
            )

        } else {

            onStatusChanged?.invoke(
                "GATT SERVICE FAILED"
            )
        }
    }

    // =========================================================
    // SERVER CALLBACK
    // =========================================================

    private val serverCallback =
        object :
            BluetoothGattServerCallback() {

            override fun onConnectionStateChange(

                device: BluetoothDevice,

                status: Int,

                newState: Int

            ) {

                val address =
                    device.address

                if (
                    newState ==
                    BluetoothProfile.STATE_CONNECTED
                ) {

                    val peer =
                        synchronized(peers) {

                            peers.getOrPut(
                                address
                            ) {
                                Peer(device)
                            }
                        }

                    peer.receiveBuffer =
                        ByteArray(0)

                    onNodeDiscovered?.invoke(
                        "CONNECTED: $address"
                    )

                    onStatusChanged?.invoke(
                        "PEER CONNECTED: $address"
                    )

                    notifyConnectedCount()

                    retryStoredMessages()
                }

                if (
                    newState ==
                    BluetoothProfile.STATE_DISCONNECTED
                ) {

                    val peer =
                        synchronized(peers) {
                            peers[address]
                        }

                    peer?.ready =
                        false

                    peer?.receiveBuffer =
                        ByteArray(0)

                    onStatusChanged?.invoke(
                        "PEER DISCONNECTED: $address"
                    )

                    notifyConnectedCount()
                }
            }

            @SuppressLint("MissingPermission")
            override fun onCharacteristicReadRequest(

                device: BluetoothDevice,

                requestId: Int,

                offset: Int,

                characteristic:
                BluetoothGattCharacteristic

            ) {

                if (
                    characteristic.uuid !=
                    NODE_ID_UUID
                ) {

                    gattServer?.sendResponse(

                        device,

                        requestId,

                        BluetoothGatt
                            .GATT_REQUEST_NOT_SUPPORTED,

                        offset,

                        null
                    )

                    return
                }

                val data =
                    nodeId.toByteArray(
                        StandardCharsets.UTF_8
                    )

                if (
                    offset > data.size
                ) {

                    gattServer?.sendResponse(

                        device,

                        requestId,

                        BluetoothGatt
                            .GATT_INVALID_OFFSET,

                        offset,

                        null
                    )

                    return
                }

                val response =
                    data.copyOfRange(
                        offset,
                        data.size
                    )

                gattServer?.sendResponse(

                    device,

                    requestId,

                    BluetoothGatt
                        .GATT_SUCCESS,

                    offset,

                    response
                )
            }

            @SuppressLint("MissingPermission")
            override fun onCharacteristicWriteRequest(

                device: BluetoothDevice,

                requestId: Int,

                characteristic:
                BluetoothGattCharacteristic,

                preparedWrite: Boolean,

                responseNeeded: Boolean,

                offset: Int,

                value: ByteArray

            ) {

                if (
                    characteristic.uuid !=
                    MESSAGE_UUID
                ) {

                    if (responseNeeded) {

                        gattServer?.sendResponse(

                            device,

                            requestId,

                            BluetoothGatt
                                .GATT_REQUEST_NOT_SUPPORTED,

                            offset,

                            null
                        )
                    }

                    return
                }

                val peer =
                    synchronized(peers) {

                        peers.getOrPut(
                            device.address
                        ) {

                            Peer(device)
                        }
                    }

                if (
                    value.isNotEmpty()
                ) {

                    peer.receiveBuffer =
                        appendBytes(
                            peer.receiveBuffer,
                            value
                        )

                    if (
                        peer.receiveBuffer.size >
                        MAX_FRAME_SIZE
                    ) {

                        peer.receiveBuffer =
                            ByteArray(0)

                        onStatusChanged?.invoke(
                            "RX BUFFER RESET"
                        )

                    } else {

                        processReceiveBuffer(
                            peer
                        )
                    }
                }

                if (responseNeeded) {

                    gattServer?.sendResponse(

                        device,

                        requestId,

                        BluetoothGatt
                            .GATT_SUCCESS,

                        offset,

                        null
                    )
                }
            }
        }

    // =========================================================
    // BYTE HELPERS
    // =========================================================

    private fun appendBytes(
        old: ByteArray,
        new: ByteArray
    ): ByteArray {

        if (old.isEmpty()) {
            return new.copyOf()
        }

        val result =
            ByteArray(
                old.size +
                        new.size
            )

        System.arraycopy(
            old,
            0,
            result,
            0,
            old.size
        )

        System.arraycopy(
            new,
            0,
            result,
            old.size,
            new.size
        )

        return result
    }

    private fun indexOfBytes(

        data: ByteArray,

        target: ByteArray,

        start: Int = 0

    ): Int {

        if (
            target.isEmpty() ||
            data.size < target.size
        ) {

            return -1
        }

        val from =
            maxOf(
                0,
                start
            )

        if (
            from >
            data.size -
            target.size
        ) {

            return -1
        }

        for (
        i in from..
                (data.size -
                        target.size)
        ) {

            var match =
                true

            for (
            j in target.indices
            ) {

                if (
                    data[i + j] !=
                    target[j]
                ) {

                    match =
                        false

                    break
                }
            }

            if (match) {
                return i
            }
        }

        return -1
    }

    // =========================================================
    // RECEIVE BUFFER
    // =========================================================

    private fun processReceiveBuffer(
        peer: Peer
    ) {

        while (true) {

            val buffer =
                peer.receiveBuffer

            val start =
                indexOfBytes(
                    buffer,
                    FRAME_START_BYTES
                )

            if (start < 0) {

                val keep =
                    FRAME_START_BYTES.size -
                            1

                if (
                    buffer.size >
                    keep
                ) {

                    peer.receiveBuffer =
                        buffer.copyOfRange(
                            buffer.size -
                                    keep,
                            buffer.size
                        )
                }

                return
            }

            if (start > 0) {

                peer.receiveBuffer =
                    buffer.copyOfRange(
                        start,
                        buffer.size
                    )
            }

            val current =
                peer.receiveBuffer

            val end =
                indexOfBytes(
                    current,
                    FRAME_END_BYTES,
                    FRAME_START_BYTES.size
                )

            if (end < 0) {
                return
            }

            val payloadBytes =
                current.copyOfRange(
                    FRAME_START_BYTES.size,
                    end
                )

            peer.receiveBuffer =
                current.copyOfRange(
                    end +
                            FRAME_END_BYTES.size,
                    current.size
                )

            val payload =
                String(
                    payloadBytes,
                    StandardCharsets.UTF_8
                )

            val message =
                HopMessage.decode(
                    payload
                )

            if (message == null) {

                onStatusChanged?.invoke(
                    "MESSAGE DECODE FAILED"
                )

                continue
            }

            handleIncomingMessage(
                message,
                peer
            )
        }
    }

    // =========================================================
    // INCOMING
    // =========================================================

    private fun handleIncomingMessage(

        incoming: HopMessage,

        sourcePeer: Peer

    ) {

        if (
            incoming.senderId ==
            nodeId
        ) {
            return
        }

        if (
            incoming.ttl <= 0
        ) {

            onStatusChanged?.invoke(
                "TTL EXPIRED"
            )

            return
        }

        val newHop =
            incoming.hopCount + 1

        if (
            newHop > MAX_TTL
        ) {

            onStatusChanged?.invoke(
                "MAX HOP REACHED"
            )

            return
        }

        val previousBest =
            bestHop[
                incoming.messageId
            ]

        if (
            previousBest != null &&
            newHop >= previousBest
        ) {
            return
        }

        bestHop[
            incoming.messageId
        ] =
            newHop

        /*
         * IMPORTANT:
         *
         * timestamp and recordingDurationMs are retained
         * unchanged when forwarding.
         */
        val updated =
            incoming.copy(
                hopCount =
                    newHop
            )

        synchronized(
            storedMessages
        ) {

            storedMessages[
                updated.messageId
            ] =
                updated

            trimStorage()
        }

        notifyHistory()

        handler.post {

            onMessageReceived?.invoke(
                updated
            )
        }

        onStatusChanged?.invoke(

            if (updated.isVoice) {

                "VOICE RECEIVED"

            } else {

                "MESSAGE RECEIVED"
            }
        )

        if (
            updated.ttl > 1 &&
            newHop < MAX_TTL
        ) {

            relayMessage(
                updated,
                sourcePeer
            )
        }
    }

    // =========================================================
    // RELAY
    // =========================================================

    private fun relayMessage(

        message: HopMessage,

        sourcePeer: Peer

    ) {

        val outgoing =
            message.copy(
                ttl =
                    message.ttl - 1
            )

        if (
            outgoing.ttl <= 0
        ) {
            return
        }

        var count =
            0

        synchronized(peers) {

            for (
            peer in peers.values
            ) {

                if (
                    peer ===
                    sourcePeer
                ) {
                    continue
                }

                if (
                    !peer.ready
                ) {
                    continue
                }

                if (
                    peer.gatt == null
                ) {
                    continue
                }

                if (
                    peer.messageCharacteristic ==
                    null
                ) {
                    continue
                }

                val remoteNode =
                    peer.nodeId
                        ?: continue

                if (
                    remoteNode ==
                    nodeId
                ) {
                    continue
                }

                if (
                    remoteNode ==
                    message.senderId
                ) {
                    continue
                }

                val sentHop =
                    peer.sentMessages[
                        outgoing.messageId
                    ]

                if (
                    sentHop != null &&
                    sentHop <=
                    outgoing.hopCount
                ) {
                    continue
                }

                if (
                    peerHasMessage(
                        peer,
                        outgoing.messageId
                    )
                ) {
                    continue
                }

                peer.queue.addLast(
                    outgoing
                )

                count++

                processPeerQueue(
                    peer
                )
            }
        }

        if (count > 0) {

            onStatusChanged?.invoke(
                "FORWARDING TO $count NODE(S)"
            )
        }
    }

    // =========================================================
    // HISTORY
    // =========================================================

    private fun notifyHistory() {

        val list =
            synchronized(
                storedMessages
            ) {

                storedMessages.values
                    .toList()
            }

        handler.post {

            onMessageHistoryChanged?.invoke(
                list
            )
        }
    }

    private fun notifyConnectedCount() {

        handler.post {

            onStatusChanged?.invoke(
                "CONNECTED NODES: ${
                    getConnectedNodeCount()
                }"
            )
        }
    }

    // =========================================================
    // STORAGE
    // =========================================================

    private fun trimStorage() {

        while (
            storedMessages.size >
            MAX_MESSAGES
        ) {

            val iterator =
                storedMessages
                    .entries
                    .iterator()

            if (
                iterator.hasNext()
            ) {

                val entry =
                    iterator.next()

                iterator.remove()

                bestHop.remove(
                    entry.key
                )
            }
        }
    }

    // =========================================================
    // ADVERTISING
    // =========================================================

    @SuppressLint("MissingPermission")
    private fun startAdvertising() {

        val localAdvertiser =
            advertiser

        if (
            localAdvertiser == null
        ) {

            onStatusChanged?.invoke(
                "BLE ADVERTISER UNAVAILABLE"
            )

            return
        }

        val settings =
            AdvertiseSettings.Builder()

                .setAdvertiseMode(
                    AdvertiseSettings
                        .ADVERTISE_MODE_LOW_LATENCY
                )

                .setTxPowerLevel(
                    AdvertiseSettings
                        .ADVERTISE_TX_POWER_HIGH
                )

                .setConnectable(
                    true
                )

                .setTimeout(
                    0
                )

                .build()

        val data =
            AdvertiseData.Builder()

                .setIncludeDeviceName(
                    false
                )

                .addServiceUuid(
                    ParcelUuid(
                        SERVICE_UUID
                    )
                )

                .build()

        try {

            localAdvertiser.startAdvertising(
                settings,
                data,
                advertiseCallback
            )

        } catch (
            e: Exception
        ) {

            onStatusChanged?.invoke(
                "ADVERTISE ERROR: ${e.message}"
            )
        }
    }

    private val advertiseCallback =
        object :
            AdvertiseCallback() {

            override fun onStartSuccess(
                settingsInEffect:
                AdvertiseSettings?
            ) {

                onStatusChanged?.invoke(
                    "ADVERTISING"
                )
            }

            override fun onStartFailure(
                errorCode: Int
            ) {

                onStatusChanged?.invoke(
                    "ADVERTISE FAILED: $errorCode"
                )
            }
        }

    // =========================================================
    // SCANNING
    // =========================================================

    @SuppressLint("MissingPermission")
    private fun startScanning() {

        val localScanner =
            scanner

        if (
            localScanner == null
        ) {

            onStatusChanged?.invoke(
                "BLE SCANNER UNAVAILABLE"
            )

            return
        }

        val callback =
            object :
                ScanCallback() {

                override fun onScanResult(

                    callbackType: Int,

                    result: ScanResult

                ) {

                    val record =
                        result.scanRecord
                            ?: return

                    val uuids =
                        record.serviceUuids
                            ?: return

                    val isAapdaSetu =
                        uuids.any {

                            it.uuid ==
                                    SERVICE_UUID
                        }

                    if (
                        !isAapdaSetu
                    ) {
                        return
                    }

                    val device =
                        result.device

                    val peer =
                        synchronized(peers) {

                            peers.getOrPut(
                                device.address
                            ) {

                                Peer(
                                    device
                                )
                            }
                        }

                    onNodeDiscovered?.invoke(
                        "AAPDASETU NODE: ${
                            device.address
                        }"
                    )

                    if (
                        peer.ready ||
                        peer.connecting ||
                        peer.gatt != null
                    ) {
                        return
                    }

                    connectToPeer(
                        device
                    )
                }

                override fun onScanFailed(
                    errorCode: Int
                ) {

                    onStatusChanged?.invoke(
                        "SCAN FAILED: $errorCode"
                    )
                }
            }

        scanCallback =
            callback

        try {

            localScanner.startScan(
                callback
            )

            onStatusChanged?.invoke(
                "SCANNING"
            )

        } catch (
            e: Exception
        ) {

            onStatusChanged?.invoke(
                "SCAN ERROR: ${e.message}"
            )
        }
    }

    // =========================================================
    // CONNECT
    // =========================================================

    @SuppressLint("MissingPermission")
    private fun connectToPeer(
        device: BluetoothDevice
    ) {

        if (
            device.address.isBlank()
        ) {
            return
        }

        val peer =
            synchronized(peers) {

                peers.getOrPut(
                    device.address
                ) {

                    Peer(
                        device
                    )
                }
            }

        if (
            peer.ready ||
            peer.connecting ||
            peer.gatt != null
        ) {
            return
        }

        peer.connecting =
            true

        onStatusChanged?.invoke(
            "CONNECTING ${
                device.address
            }"
        )

        val callback =
            createGattCallback(
                peer
            )

        try {

            val gatt =
                if (
                    Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.M
                ) {

                    device.connectGatt(

                        context,

                        false,

                        callback,

                        BluetoothDevice
                            .TRANSPORT_LE
                    )

                } else {

                    @Suppress("DEPRECATION")

                    device.connectGatt(

                        context,

                        false,

                        callback
                    )
                }

            peer.gatt =
                gatt

            handler.postDelayed({

                if (
                    peer.connecting &&
                    !peer.ready
                ) {

                    peer.connecting =
                        false

                    try {
                        peer.gatt?.disconnect()
                    } catch (
                        _: Exception
                    ) {
                    }
                }

            }, CONNECTION_TIMEOUT)

        } catch (
            e: Exception
        ) {

            peer.connecting =
                false

            peer.gatt =
                null

            onStatusChanged?.invoke(
                "CONNECT ERROR: ${e.message}"
            )

            scheduleReconnect(
                peer
            )
        }
    }

    // =========================================================
    // CLIENT CALLBACK
    // =========================================================

    private fun createGattCallback(
        peer: Peer
    ): BluetoothGattCallback {

        return object :
            BluetoothGattCallback() {

            @SuppressLint("MissingPermission")
            override fun onConnectionStateChange(

                gatt: BluetoothGatt,

                status: Int,

                newState: Int

            ) {

                if (
                    newState ==
                    BluetoothProfile.STATE_CONNECTED
                ) {

                    peer.connecting =
                        false

                    peer.gatt =
                        gatt

                    peer.ready =
                        false

                    peer.nodeId =
                        null

                    peer.messageCharacteristic =
                        null

                    peer.mtu =
                        DEFAULT_MTU

                    onStatusChanged?.invoke(
                        "CONNECTED ${
                            peer.device.address
                        }"
                    )

                    notifyConnectedCount()

                    val requested =
                        if (
                            Build.VERSION.SDK_INT >=
                            Build.VERSION_CODES.LOLLIPOP
                        ) {

                            try {

                                gatt.requestMtu(
                                    REQUESTED_MTU
                                )

                            } catch (
                                _: Exception
                            ) {

                                false
                            }

                        } else {

                            false
                        }

                    if (!requested) {

                        try {

                            gatt.discoverServices()

                        } catch (
                            _: Exception
                        ) {
                        }
                    }

                    return
                }

                if (
                    newState ==
                    BluetoothProfile.STATE_DISCONNECTED
                ) {

                    peer.ready =
                        false

                    peer.connecting =
                        false

                    peer.messageCharacteristic =
                        null

                    peer.nodeId =
                        null

                    peer.currentMessage =
                        null

                    peer.chunks =
                        null

                    peer.chunkIndex =
                        -1

                    peer.retryCount =
                        0

                    peer.receiveBuffer =
                        ByteArray(0)

                    peer.gatt =
                        null

                    onStatusChanged?.invoke(
                        "DISCONNECTED ${
                            peer.device.address
                        }"
                    )

                    notifyConnectedCount()

                    try {
                        gatt.close()
                    } catch (
                        _: Exception
                    ) {
                    }

                    scheduleReconnect(
                        peer
                    )
                }
            }

            @SuppressLint("MissingPermission")
            override fun onMtuChanged(

                gatt: BluetoothGatt,

                mtu: Int,

                status: Int

            ) {

                if (
                    status ==
                    BluetoothGatt.GATT_SUCCESS
                ) {

                    peer.mtu =
                        mtu

                    onStatusChanged?.invoke(
                        "MTU $mtu"
                    )
                }

                try {

                    gatt.discoverServices()

                } catch (
                    _: Exception
                ) {
                }
            }

            @SuppressLint("MissingPermission")
            override fun onServicesDiscovered(

                gatt: BluetoothGatt,

                status: Int

            ) {

                if (
                    status !=
                    BluetoothGatt.GATT_SUCCESS
                ) {

                    onStatusChanged?.invoke(
                        "SERVICE DISCOVERY FAILED"
                    )

                    return
                }

                val service =
                    gatt.getService(
                        SERVICE_UUID
                    )

                if (
                    service == null
                ) {

                    onStatusChanged?.invoke(
                        "AAPDASETU SERVICE NOT FOUND"
                    )

                    return
                }

                val nodeCharacteristic =
                    service.getCharacteristic(
                        NODE_ID_UUID
                    )

                val messageCharacteristic =
                    service.getCharacteristic(
                        MESSAGE_UUID
                    )

                if (
                    nodeCharacteristic == null ||
                    messageCharacteristic == null
                ) {

                    onStatusChanged?.invoke(
                        "AAPDASETU CHARACTERISTICS NOT FOUND"
                    )

                    return
                }

                peer.messageCharacteristic =
                    messageCharacteristic

                try {

                    val started =
                        gatt.readCharacteristic(
                            nodeCharacteristic
                        )

                    if (!started) {

                        onStatusChanged?.invoke(
                            "NODE READ DID NOT START"
                        )
                    }

                } catch (
                    _: Exception
                ) {

                    onStatusChanged?.invoke(
                        "NODE READ ERROR"
                    )
                }
            }

            override fun onCharacteristicRead(

                gatt: BluetoothGatt,

                characteristic:
                BluetoothGattCharacteristic,

                status: Int

            ) {

                if (
                    characteristic.uuid !=
                    NODE_ID_UUID
                ) {
                    return
                }

                if (
                    status !=
                    BluetoothGatt.GATT_SUCCESS
                ) {

                    onStatusChanged?.invoke(
                        "NODE ID READ FAILED"
                    )

                    return
                }

                val bytes =
                    characteristic.value
                        ?: ByteArray(0)

                val remoteNode =
                    String(
                        bytes,
                        StandardCharsets.UTF_8
                    )
                        .trim()
                        .trim('\u0000')

                if (
                    remoteNode.isBlank()
                ) {

                    return
                }

                if (
                    remoteNode ==
                    nodeId
                ) {

                    onStatusChanged?.invoke(
                        "SELF NODE IGNORED"
                    )

                    try {
                        gatt.disconnect()
                    } catch (
                        _: Exception
                    ) {
                    }

                    return
                }

                peer.nodeId =
                    remoteNode

                peer.ready =
                    true

                onNodeDiscovered?.invoke(
                    "NODE READY: $remoteNode"
                )

                onStatusChanged?.invoke(
                    "READY: $remoteNode"
                )

                notifyConnectedCount()

                retryStoredMessages()

                processPeerQueue(
                    peer
                )
            }

            @SuppressLint("MissingPermission")
            override fun onCharacteristicWrite(

                gatt: BluetoothGatt,

                characteristic:
                BluetoothGattCharacteristic,

                status: Int

            ) {

                if (
                    characteristic.uuid !=
                    MESSAGE_UUID
                ) {
                    return
                }

                if (
                    status ==
                    BluetoothGatt.GATT_SUCCESS
                ) {

                    peer.retryCount =
                        0

                    handler.post {

                        sendNextChunk(
                            peer
                        )
                    }

                } else {

                    val chunks =
                        peer.chunks

                    val index =
                        peer.chunkIndex

                    if (
                        chunks != null &&
                        index >= 0 &&
                        index < chunks.size
                    ) {

                        retryCurrentChunk(

                            peer,

                            chunks[index],

                            status
                        )

                    } else {

                        finishCurrentMessage(
                            peer
                        )
                    }
                }
            }
        }
    }

    // =========================================================
    // RECONNECT
    // =========================================================

    private fun scheduleReconnect(
        peer: Peer
    ) {

        if (
            peer.reconnectScheduled
        ) {
            return
        }

        peer.reconnectScheduled =
            true

        handler.postDelayed({

            peer.reconnectScheduled =
                false

            if (
                bluetoothAdapter?.isEnabled ==
                true
            ) {

                if (
                    !peer.ready &&
                    !peer.connecting &&
                    peer.gatt == null
                ) {

                    connectToPeer(
                        peer.device
                    )
                }
            }

        }, RECONNECT_DELAY)
    }

    // =========================================================
    // MESSAGE ID
    // =========================================================

    private fun createMessageId():
            String {

        return "AS-" +
                UUID.randomUUID()
                    .toString()
                    .replace(
                        "-",
                        ""
                    )
                    .take(20)
    }

    // =========================================================
    // SEND TEXT
    // =========================================================

    fun createAndSendMessage(

        text: String,

        latitude: Double? = null,

        longitude: Double? = null

    ) {

        if (
            text.isBlank()
        ) {

            onStatusChanged?.invoke(
                "MESSAGE EMPTY"
            )

            return
        }

        val location =
            latitude != null &&
                    longitude != null

        val message =
            HopMessage(

                messageId =
                    createMessageId(),

                senderId =
                    nodeId,

                message =
                    text,

                hopCount =
                    0,

                ttl =
                    MAX_TTL,

                hasLocation =
                    location,

                latitude =
                    latitude ?: 0.0,

                longitude =
                    longitude ?: 0.0,

                isVoice =
                    false,

                timestamp =
                    System.currentTimeMillis(),

                recordingDurationMs =
                    0L
            )

        createOutgoingMessage(
            message
        )
    }

    // =========================================================
    // SEND VOICE
    // =========================================================

    fun createAndSendVoiceMessage(

        base64Audio: String,

        recordingDurationMs: Long,

        latitude: Double? = null,

        longitude: Double? = null

    ) {

        if (
            base64Audio.isBlank()
        ) {

            onStatusChanged?.invoke(
                "VOICE EMPTY"
            )

            return
        }

        if (
            base64Audio.length >
            MAX_VOICE_PAYLOAD
        ) {

            onStatusChanged?.invoke(
                "VOICE TOO LARGE"
            )

            return
        }

        val location =
            latitude != null &&
                    longitude != null

        val message =
            HopMessage(

                messageId =
                    createMessageId(),

                senderId =
                    nodeId,

                message =
                    base64Audio,

                hopCount =
                    0,

                ttl =
                    MAX_TTL,

                hasLocation =
                    location,

                latitude =
                    latitude ?: 0.0,

                longitude =
                    longitude ?: 0.0,

                isVoice =
                    true,

                timestamp =
                    System.currentTimeMillis(),

                recordingDurationMs =
                    recordingDurationMs
            )

        createOutgoingMessage(
            message
        )
    }

    // =========================================================
    // CREATE OUTGOING
    // =========================================================

    private fun createOutgoingMessage(
        message: HopMessage
    ) {

        synchronized(
            storedMessages
        ) {

            storedMessages[
                message.messageId
            ] =
                message

            bestHop[
                message.messageId
            ] =
                0

            trimStorage()
        }

        notifyHistory()

        var count =
            0

        synchronized(peers) {

            for (
            peer in peers.values
            ) {

                if (!peer.ready) {
                    continue
                }

                if (peer.gatt == null) {
                    continue
                }

                if (
                    peer.messageCharacteristic ==
                    null
                ) {
                    continue
                }

                val remoteNode =
                    peer.nodeId
                        ?: continue

                if (
                    remoteNode ==
                    nodeId
                ) {
                    continue
                }

                if (
                    remoteNode ==
                    message.senderId
                ) {
                    continue
                }

                if (
                    peerHasMessage(
                        peer,
                        message.messageId
                    )
                ) {
                    continue
                }

                peer.queue.addLast(
                    message
                )

                count++

                processPeerQueue(
                    peer
                )
            }
        }

        onStatusChanged?.invoke(

            if (count > 0) {

                if (message.isVoice) {

                    "VOICE QUEUED TO $count NODE(S)"

                } else {

                    "MESSAGE QUEUED TO $count NODE(S)"
                }

            } else {

                "MESSAGE STORED - WAITING FOR NODES"
            }
        )
    }

    // =========================================================
    // RETRY STORED
    // =========================================================

    private fun retryStoredMessages() {

        val messages =
            synchronized(
                storedMessages
            ) {

                storedMessages
                    .values
                    .toList()
            }

        synchronized(peers) {

            for (
            peer in peers.values
            ) {

                if (!peer.ready) {
                    continue
                }

                if (peer.gatt == null) {
                    continue
                }

                if (
                    peer.messageCharacteristic ==
                    null
                ) {
                    continue
                }

                val remoteNode =
                    peer.nodeId
                        ?: continue

                if (
                    remoteNode ==
                    nodeId
                ) {
                    continue
                }

                for (
                original
                in messages
                ) {

                    val outgoing =
                        if (
                            original.senderId ==
                            nodeId
                        ) {

                            original

                        } else {

                            if (
                                original.ttl <=
                                1
                            ) {
                                continue
                            }

                            original.copy(
                                ttl =
                                    original.ttl - 1
                            )
                        }

                    if (
                        outgoing.ttl <=
                        0
                    ) {
                        continue
                    }

                    if (
                        remoteNode ==
                        outgoing.senderId
                    ) {
                        continue
                    }

                    val previous =
                        peer.sentMessages[
                            outgoing.messageId
                        ]

                    if (
                        previous != null &&
                        previous <=
                        outgoing.hopCount
                    ) {
                        continue
                    }

                    if (
                        peerHasMessage(
                            peer,
                            outgoing.messageId
                        )
                    ) {
                        continue
                    }

                    peer.queue.addLast(
                        outgoing
                    )
                }

                processPeerQueue(
                    peer
                )
            }
        }
    }

    // =========================================================
    // QUEUE
    // =========================================================

    @SuppressLint("MissingPermission")
    private fun processPeerQueue(
        peer: Peer
    ) {

        if (
            !peer.ready ||
            peer.gatt == null ||
            peer.messageCharacteristic == null
        ) {
            return
        }

        if (
            peer.currentMessage != null
        ) {
            return
        }

        if (
            peer.queue.isEmpty()
        ) {
            return
        }

        val message =
            peer.queue.removeFirst()

        val previous =
            peer.sentMessages[
                message.messageId
            ]

        if (
            previous != null &&
            previous <=
            message.hopCount
        ) {

            processPeerQueue(
                peer
            )

            return
        }

        if (
            message.ttl <= 0
        ) {

            processPeerQueue(
                peer
            )

            return
        }

        peer.currentMessage =
            message

        peer.retryCount =
            0

        val frame =
            (
                    FRAME_START +
                            message.encode() +
                            FRAME_END
                    )
                .toByteArray(
                    StandardCharsets.UTF_8
                )

        if (
            frame.size >
            MAX_FRAME_SIZE
        ) {

            onStatusChanged?.invoke(
                "MESSAGE FRAME TOO LARGE"
            )

            finishCurrentMessage(
                peer
            )

            return
        }

        val chunkSize =
            calculateChunkSize(
                peer.mtu
            )

        peer.chunks =
            createChunks(
                frame,
                chunkSize
            )

        peer.chunkIndex =
            -1

        onStatusChanged?.invoke(

            if (
                message.isVoice
            ) {

                "SENDING VOICE -> ${
                    peer.nodeId
                }"

            } else {

                "SENDING MESSAGE -> ${
                    peer.nodeId
                }"
            }
        )

        sendNextChunk(
            peer
        )
    }

    // =========================================================
    // CHUNK SIZE
    // =========================================================

    private fun calculateChunkSize(
        mtu: Int
    ): Int {

        if (
            mtu <= DEFAULT_MTU
        ) {

            return MIN_CHUNK_SIZE
        }

        return (
                mtu - 3
                )
            .coerceIn(
                MIN_CHUNK_SIZE,
                MAX_CHUNK_SIZE
            )
    }

    // =========================================================
    // SEND CHUNK
    // =========================================================

    @SuppressLint("MissingPermission")
    private fun sendNextChunk(
        peer: Peer
    ) {

        val message =
            peer.currentMessage
                ?: return

        val chunks =
            peer.chunks
                ?: run {

                    finishCurrentMessage(
                        peer
                    )

                    return
                }

        val gatt =
            peer.gatt
                ?: run {

                    finishCurrentMessage(
                        peer
                    )

                    return
                }

        val characteristic =
            peer.messageCharacteristic
                ?: run {

                    finishCurrentMessage(
                        peer
                    )

                    return
                }

        if (
            !peer.ready
        ) {
            return
        }

        val next =
            peer.chunkIndex + 1

        if (
            next >= chunks.size
        ) {

            peer.sentMessages[
                message.messageId
            ] =
                message.hopCount

            onStatusChanged?.invoke(

                if (message.isVoice) {

                    "VOICE DELIVERED"

                } else {

                    "MESSAGE DELIVERED"
                }
            )

            finishCurrentMessage(
                peer
            )

            return
        }

        peer.chunkIndex =
            next

        peer.retryCount =
            0

        writeChunk(

            peer,

            chunks[next],

            gatt,

            characteristic
        )
    }

    // =========================================================
    // WRITE
    // =========================================================

    @SuppressLint("MissingPermission")
    private fun writeChunk(

        peer: Peer,

        chunk: ByteArray,

        gatt: BluetoothGatt,

        characteristic:
        BluetoothGattCharacteristic

    ) {

        try {

            characteristic.writeType =
                BluetoothGattCharacteristic
                    .WRITE_TYPE_DEFAULT

            characteristic.value =
                chunk

            val started =
                gatt.writeCharacteristic(
                    characteristic
                )

            if (!started) {

                retryCurrentChunk(
                    peer,
                    chunk,
                    -1
                )
            }

        } catch (
            _: Exception
        ) {

            retryCurrentChunk(
                peer,
                chunk,
                -2
            )
        }
    }

    // =========================================================
    // RETRY
    // =========================================================

    private fun retryCurrentChunk(

        peer: Peer,

        chunk: ByteArray,

        status: Int

    ) {

        if (
            peer.currentMessage ==
            null
        ) {
            return
        }

        if (
            peer.retryCount <
            MAX_CHUNK_RETRIES
        ) {

            peer.retryCount++

            onStatusChanged?.invoke(
                "CHUNK RETRY ${
                    peer.retryCount
                }/$MAX_CHUNK_RETRIES"
            )

            handler.postDelayed({

                if (
                    peer.currentMessage !=
                    null &&
                    peer.ready
                ) {

                    val gatt =
                        peer.gatt

                    val characteristic =
                        peer.messageCharacteristic

                    if (
                        gatt != null &&
                        characteristic != null
                    ) {

                        writeChunk(

                            peer,

                            chunk,

                            gatt,

                            characteristic
                        )
                    }
                }

            }, CHUNK_RETRY_DELAY)

        } else {

            onStatusChanged?.invoke(
                "WRITE FAILED $status"
            )

            val failed =
                peer.currentMessage

            peer.currentMessage =
                null

            peer.chunks =
                null

            peer.chunkIndex =
                -1

            peer.retryCount =
                0

            if (
                failed != null
            ) {

                peer.queue.addFirst(
                    failed
                )
            }

            try {

                peer.gatt?.disconnect()

            } catch (
                _: Exception
            ) {
            }
        }
    }

    // =========================================================
    // FINISH
    // =========================================================

    private fun finishCurrentMessage(
        peer: Peer
    ) {

        peer.currentMessage =
            null

        peer.chunks =
            null

        peer.chunkIndex =
            -1

        peer.retryCount =
            0

        handler.post {

            processPeerQueue(
                peer
            )
        }
    }

    // =========================================================
    // DUPLICATE
    // =========================================================

    private fun peerHasMessage(

        peer: Peer,

        messageId: String

    ): Boolean {

        if (
            peer.currentMessage
                ?.messageId ==
            messageId
        ) {
            return true
        }

        for (
        queued in peer.queue
        ) {

            if (
                queued.messageId ==
                messageId
            ) {

                return true
            }
        }

        return false
    }

    // =========================================================
    // CHUNKS
    // =========================================================

    private fun createChunks(

        data: ByteArray,

        size: Int

    ): List<ByteArray> {

        val safeSize =
            size.coerceAtLeast(
                MIN_CHUNK_SIZE
            )

        val result =
            ArrayList<ByteArray>()

        var position =
            0

        while (
            position <
            data.size
        ) {

            val end =
                minOf(
                    position +
                            safeSize,
                    data.size
                )

            result.add(
                data.copyOfRange(
                    position,
                    end
                )
            )

            position =
                end
        }

        return result
    }

    // =========================================================
    // RETRY LOOP
    // =========================================================

    private fun startRetryLoop() {

        retryRunnable =
            object :
                Runnable {

                override fun run() {

                    try {

                        retryStoredMessages()

                    } catch (
                        _: Exception
                    ) {
                    }

                    handler.postDelayed(
                        this,
                        RETRY_LOOP
                    )
                }
            }

        handler.postDelayed(
            retryRunnable!!,
            RETRY_LOOP
        )
    }

    // =========================================================
    // STOP
    // =========================================================

    @SuppressLint("MissingPermission")
    fun stop() {

        stopInternal()

        onStatusChanged?.invoke(
            "AAPDASETU MESH STOPPED"
        )
    }

    // =========================================================
    // INTERNAL STOP
    // =========================================================

    @SuppressLint("MissingPermission")
    private fun stopInternal() {

        retryRunnable?.let {

            handler.removeCallbacks(
                it
            )
        }

        retryRunnable =
            null

        try {

            scanCallback?.let {

                scanner?.stopScan(
                    it
                )
            }

        } catch (
            _: Exception
        ) {
        }

        scanCallback =
            null

        try {

            advertiser?.stopAdvertising(
                advertiseCallback
            )

        } catch (
            _: Exception
        ) {
        }

        synchronized(peers) {

            for (
            peer in peers.values
            ) {

                try {
                    peer.gatt?.disconnect()
                } catch (
                    _: Exception
                ) {
                }

                try {
                    peer.gatt?.close()
                } catch (
                    _: Exception
                ) {
                }
            }

            peers.clear()
        }

        try {

            gattServer?.close()

        } catch (
            _: Exception
        ) {
        }

        gattServer =
            null
    }
}