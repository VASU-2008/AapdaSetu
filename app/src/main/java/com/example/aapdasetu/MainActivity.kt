package com.example.aapdasetu

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.pm.PackageManager
import android.location.Location
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

// =========================================================
// LIVE RADAR NODE
// =========================================================

data class LiveRadarNode(
    val nodeId: String,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val sourceTimestamp: Long,
    val receivedAt: Long,
    val hopCount: Int,
    val rssi: Int? = null
)

class MainActivity : ComponentActivity() {

    // =====================================================
    // BLE
    // =====================================================

    private lateinit var bleManager: BleManager

    // =====================================================
    // FUSED LOCATION
    // =====================================================

    private lateinit var fusedLocationClient:
            FusedLocationProviderClient

    private var fusedLocationCallback:
            LocationCallback? = null

    // =====================================================
    // NODE
    // =====================================================

    private val nodeIdState =
        mutableStateOf("")

    private val isStarted =
        mutableStateOf(false)

    // =====================================================
    // LOCATION
    // =====================================================

    private val myLocation =
        mutableStateOf<Location?>(null)

    private val locationStatus =
        mutableStateOf(
            "Location not fetched yet"
        )

    private val liveRadarNodes =
        mutableStateOf(
            emptyMap<String, LiveRadarNode>()
        )

    private val radarHandler =
        Handler(
            Looper.getMainLooper()
        )

    private val radarCleanupRunnable =
        object : Runnable {

            override fun run() {

                cleanStaleRadarNodes()

                radarHandler.postDelayed(
                    this,
                    RADAR_CLEANUP_INTERVAL_MS
                )
            }
        }

    // =====================================================
    // VOICE
    // =====================================================

    private val isRecording =
        mutableStateOf(false)

    private val recordingDurationMs =
        mutableStateOf(0L)

    private var recordingStartTime =
        0L

    private var currentAttachLocation =
        false

    private val playingMessageId =
        mutableStateOf<String?>(null)

    private var mediaRecorder:
            MediaRecorder? = null

    private var currentRecordingFile:
            File? = null

    private var mediaPlayer:
            MediaPlayer? = null

    private val recordingHandler =
        Handler(
            Looper.getMainLooper()
        )

    private val recordingTimerRunnable =
        object : Runnable {

            override fun run() {

                if (
                    isRecording.value
                ) {

                    recordingDurationMs.value =
                        System.currentTimeMillis() -
                                recordingStartTime

                    recordingHandler.postDelayed(
                        this,
                        100L
                    )
                }
            }
        }

    // =====================================================
    // CONSTANTS
    // =====================================================

    companion object {

        private const val REQUEST_BLUETOOTH = 1001
        private const val REQUEST_LOCATION = 1002
        private const val REQUEST_RECORD_AUDIO = 1003

        private const val PREFS_NAME =
            "aapdasetu_prefs"

        private const val KEY_NODE_NAME =
            "node_name"

        private const val MAX_NAME_LENGTH =
            24

        private const val MAX_VOICE_DURATION_MS =
            15000L

        private const val LIVE_LOCATION_INTERVAL_MS =
            5000L

        private const val LIVE_LOCATION_FASTEST_MS =
            2000L

        private const val RADAR_NODE_TIMEOUT_MS =
            30000L

        private const val RADAR_CLEANUP_INTERVAL_MS =
            5000L
    }

    // =====================================================
    // CREATE
    // =====================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        bleManager =
            BleManager(this)

        fusedLocationClient =
            LocationServices
                .getFusedLocationProviderClient(
                    this
                )

        val savedName =
            loadSavedNodeName()

        if (
            savedName != null
        ) {

            nodeIdState.value =
                savedName

        } else {

            val generated =
                generateDefaultName()

            saveNodeName(
                generated
            )

            nodeIdState.value =
                generated
        }

        setContent {

            val status =
                remember {
                    mutableStateOf(
                        "Not started"
                    )
                }

            val messageText =
                remember {
                    mutableStateOf(
                        ""
                    )
                }

            val nearbyNodes =
                remember {
                    mutableStateListOf<String>()
                }

            val messages =
                remember {
                    mutableStateListOf<
                            BleManager.HopMessage
                            >()
                }

            val showDeleteAllConfirm =
                remember {
                    mutableStateOf(
                        false
                    )
                }

            val nameInput =
                remember {
                    mutableStateOf(
                        nodeIdState.value
                    )
                }

            val nameError =
                remember {
                    mutableStateOf<String?>(
                        null
                    )
                }

            val attachLocation =
                remember {
                    mutableStateOf(
                        false
                    )
                }

            val viewMode =
                remember {
                    mutableStateOf(
                        "CHAT"
                    )
                }

            // =================================================
            // STATUS
            // =================================================

            bleManager.onStatusChanged =
                { text ->

                    runOnUiThread {

                        status.value =
                            text
                    }
                }

            // =================================================
            // DISCOVERY
            // =================================================

            bleManager.onNodeDiscovered =
                { node ->

                    runOnUiThread {

                        if (
                            !nearbyNodes.contains(
                                node
                            )
                        ) {

                            nearbyNodes.add(
                                node
                            )
                        }
                    }
                }

            // =================================================
            // MESSAGE HISTORY
            // =================================================

            bleManager.onMessageHistoryChanged =
                { history ->

                    runOnUiThread {

                        messages.clear()

                        messages.addAll(
                            history
                        )
                    }
                }

            // =================================================
            // LOCATION RECEIVED
            // =================================================

            bleManager.onLocationReceived =
                { location ->

                    runOnUiThread {

                        if (
                            location.nodeId ==
                            nodeIdState.value
                        ) {

                            return@runOnUiThread
                        }

                        val existing =
                            liveRadarNodes.value[
                                location.nodeId
                            ]

                        if (
                            existing != null &&
                            location.timestamp <=
                            existing.sourceTimestamp
                        ) {

                            return@runOnUiThread
                        }

                        val updated =
                            liveRadarNodes.value
                                .toMutableMap()

                        updated[
                            location.nodeId
                        ] =
                            LiveRadarNode(

                                nodeId =
                                    stableNodeId(
                                        location.nodeId
                                    ),

                                latitude =
                                    location.latitude,

                                longitude =
                                    location.longitude,

                                accuracyMeters =
                                    location.accuracyMeters,

                                sourceTimestamp =
                                    location.timestamp,

                                receivedAt =
                                    System.currentTimeMillis(),

                                hopCount =
                                    location.hopCount,

                                rssi =
                                    existing?.rssi
                            )

                        liveRadarNodes.value =
                            updated
                    }
                }

            // =================================================
            // RSSI RECEIVED
            // =================================================

            bleManager.onNodeSignalChanged =
                { node, rssi ->

                    runOnUiThread {

                        val existing =
                            liveRadarNodes.value[
                                node
                            ]

                        if (
                            existing != null
                        ) {

                            val updated =
                                liveRadarNodes.value
                                    .toMutableMap()

                            updated[node] =
                                existing.copy(
                                    rssi = rssi
                                )

                            liveRadarNodes.value =
                                updated
                        }
                    }
                }

            // =================================================
            // DELETE ALL DIALOG
            // =================================================

            if (
                showDeleteAllConfirm.value
            ) {

                AlertDialog(

                    onDismissRequest = {

                        showDeleteAllConfirm.value =
                            false
                    },

                    title = {

                        Text(
                            "Delete all messages?"
                        )
                    },

                    text = {

                        Text(
                            "This will permanently remove the entire local chat history on this device."
                        )
                    },

                    confirmButton = {

                        TextButton(

                            onClick = {

                                bleManager
                                    .deleteAllMessages()

                                showDeleteAllConfirm.value =
                                    false
                            }
                        ) {

                            Text(
                                "Delete All"
                            )
                        }
                    },

                    dismissButton = {

                        TextButton(

                            onClick = {

                                showDeleteAllConfirm.value =
                                    false
                            }
                        ) {

                            Text(
                                "Cancel"
                            )
                        }
                    }
                )
            }

            // =================================================
            // MAIN SCREEN
            // =================================================

            MaterialTheme {

                LazyColumn(

                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(
                                20.dp
                            ),

                    verticalArrangement =
                        Arrangement.spacedBy(
                            10.dp
                        )
                ) {

                    // =================================================
                    // TITLE
                    // =================================================

                    item {

                        Text(
                            text =
                                "AapdaSetu",

                            style =
                                MaterialTheme
                                    .typography
                                    .headlineMedium
                        )

                        Text(
                            text =
                                "Offline Bluetooth Emergency Network"
                        )
                    }

                    // =================================================
                    // NODE NAME
                    // =================================================

                    item {

                        Text(
                            text =
                                "MY NODE NAME"
                        )

                        OutlinedTextField(

                            value =
                                nameInput.value,

                            onValueChange = {

                                nameInput.value =
                                    it

                                nameError.value =
                                    null
                            },

                            modifier =
                                Modifier
                                    .fillMaxWidth(),

                            label = {

                                Text(
                                    "Visible name, e.g. Rahul-Team2"
                                )
                            },

                            singleLine = true
                        )

                        if (
                            nameError.value != null
                        ) {

                            Text(
                                text =
                                    nameError.value
                                        ?: ""
                            )
                        }

                        Button(

                            onClick = {

                                val sanitized =
                                    sanitizeName(
                                        nameInput.value
                                    )

                                if (
                                    sanitized == null
                                ) {

                                    nameError.value =
                                        "Name can't be empty."

                                } else {

                                    saveNodeName(
                                        sanitized
                                    )

                                    nodeIdState.value =
                                        sanitized

                                    nameInput.value =
                                        sanitized

                                    nameError.value =
                                        null

                                    if (
                                        isStarted.value
                                    ) {

                                        bleManager.start(
                                            sanitized
                                        )
                                    }
                                }
                            },

                            modifier =
                                Modifier
                                    .fillMaxWidth()

                        ) {

                            Text(
                                "SAVE NAME"
                            )
                        }
                    }

                    // =================================================
                    // STATUS
                    // =================================================

                    item {

                        Card(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                        ) {

                            Column(

                                modifier =
                                    Modifier
                                        .padding(
                                            16.dp
                                        ),

                                verticalArrangement =
                                    Arrangement.spacedBy(
                                        6.dp
                                    )
                            ) {

                                Text(
                                    text =
                                        "STATUS",

                                    style =
                                        MaterialTheme
                                            .typography
                                            .titleMedium
                                )

                                Text(
                                    text =
                                        status.value
                                )

                                Text(
                                    text =
                                        if (
                                            isStarted.value
                                        ) {

                                            "BluetoothHop: RUNNING"

                                        } else {

                                            "BluetoothHop: NOT RUNNING"
                                        }
                                )

                                Text(
                                    text =
                                        "Connected nodes: ${
                                            bleManager
                                                .getConnectedNodeCount()
                                        }"
                                )
                            }
                        }
                    }

                    // =================================================
                    // START
                    // =================================================

                    item {

                        Button(

                            onClick = {

                                requestAllPermissions()
                            },

                            modifier =
                                Modifier
                                    .fillMaxWidth()

                        ) {

                            Text(

                                if (
                                    isStarted.value
                                ) {

                                    "BLUETOOTHHOP RUNNING"

                                } else {

                                    "START BLUETOOTHHOP"
                                }
                            )
                        }
                    }

                    // =================================================
                    // NEARBY
                    // =================================================

                    item {

                        Text(
                            text =
                                "NEARBY NODES",

                            style =
                                MaterialTheme
                                    .typography
                                    .titleMedium
                        )
                    }

                    if (
                        nearbyNodes.isEmpty()
                    ) {

                        item {

                            Text(
                                text =
                                    "No nodes discovered"
                            )
                        }

                    } else {

                        items(
                            items =
                                nearbyNodes
                        ) { node ->

                            Card(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                            ) {

                                Text(

                                    text =
                                        "\uD83D\uDCF1 $node",

                                    modifier =
                                        Modifier
                                            .padding(
                                                12.dp
                                            )
                                )
                            }
                        }
                    }

                    // =================================================
                    // LOCATION
                    // =================================================

                    item {

                        Card(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                        ) {

                            Column(

                                modifier =
                                    Modifier
                                        .padding(
                                            16.dp
                                        ),

                                verticalArrangement =
                                    Arrangement.spacedBy(
                                        8.dp
                                    )
                            ) {

                                Text(
                                    text =
                                        "MY LOCATION",

                                    style =
                                        MaterialTheme
                                            .typography
                                            .titleMedium
                                )

                                Text(
                                    text =
                                        locationStatus.value
                                )

                                myLocation.value?.let {
                                        location ->

                                    Text(
                                        text =
                                            "Latitude: ${
                                                "%.6f".format(
                                                    location.latitude
                                                )
                                            }"
                                    )

                                    Text(
                                        text =
                                            "Longitude: ${
                                                "%.6f".format(
                                                    location.longitude
                                                )
                                            }"
                                    )

                                    Text(
                                        text =
                                            "GPS accuracy: ${
                                                location.accuracy.toInt()
                                            } m"
                                    )
                                }

                                OutlinedButton(

                                    onClick = {

                                        refreshLocation()
                                    },

                                    modifier =
                                        Modifier
                                            .fillMaxWidth()

                                ) {

                                    Text(
                                        "REFRESH NOW"
                                    )
                                }
                            }
                        }
                    }

                    // =================================================
                    // ATTACH LOCATION
                    // =================================================

                    item {

                        Row(

                            modifier =
                                Modifier
                                    .fillMaxWidth(),

                            verticalAlignment =
                                Alignment.CenterVertically,

                            horizontalArrangement =
                                Arrangement.SpaceBetween
                        ) {

                            Text(
                                text =
                                    "Attach location to next message"
                            )

                            Switch(

                                checked =
                                    attachLocation.value,

                                onCheckedChange = {
                                        checked ->

                                    attachLocation.value =
                                        checked

                                    if (
                                        checked &&
                                        myLocation.value ==
                                        null
                                    ) {

                                        refreshLocation()
                                    }
                                }
                            )
                        }
                    }

                    // =================================================
                    // MESSAGE
                    // =================================================

                    item {

                        Text(
                            text =
                                "EMERGENCY MESSAGE",

                            style =
                                MaterialTheme
                                    .typography
                                    .titleMedium
                        )

                        OutlinedTextField(

                            value =
                                messageText.value,

                            onValueChange = {

                                messageText.value =
                                    it
                            },

                            modifier =
                                Modifier
                                    .fillMaxWidth(),

                            label = {

                                Text(
                                    "Emergency message"
                                )
                            },

                            minLines = 3
                        )
                    }

                    // =================================================
                    // SEND
                    // =================================================

                    item {

                        Row(

                            modifier =
                                Modifier
                                    .fillMaxWidth(),

                            horizontalArrangement =
                                Arrangement.spacedBy(
                                    10.dp
                                )
                        ) {

                            Button(

                                onClick = {

                                    val text =
                                        messageText
                                            .value
                                            .trim()

                                    if (
                                        text.isNotEmpty()
                                    ) {

                                        if (
                                            attachLocation.value &&
                                            myLocation.value !=
                                            null
                                        ) {

                                            val location =
                                                myLocation.value!!

                                            bleManager
                                                .createAndSendMessage(

                                                    text,

                                                    location.latitude,

                                                    location.longitude
                                                )

                                        } else {

                                            bleManager
                                                .createAndSendMessage(
                                                    text
                                                )
                                        }

                                        messageText.value =
                                            ""
                                    }
                                },

                                modifier =
                                    Modifier
                                        .weight(1f)
                            ) {

                                Text(
                                    "SEND SOS"
                                )
                            }

                            Button(

                                onClick = {

                                    if (
                                        isRecording.value
                                    ) {

                                        stopRecordingAndSend(
                                            attachLocation.value
                                        )

                                    } else {

                                        currentAttachLocation =
                                            attachLocation.value

                                        startRecording()
                                    }
                                },

                                modifier =
                                    Modifier
                                        .weight(1f)
                            ) {

                                Text(

                                    if (
                                        isRecording.value
                                    ) {

                                        "\u23F9 STOP & SEND"

                                    } else {

                                        "\uD83C\uDFA4 VOICE"
                                    }
                                )
                            }
                        }
                    }

                    // =================================================
                    // RECORDING
                    // =================================================

                    if (
                        isRecording.value
                    ) {

                        item {

                            Card(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                            ) {

                                Column(

                                    modifier =
                                        Modifier
                                            .padding(
                                                16.dp
                                            ),

                                    horizontalAlignment =
                                        Alignment.CenterHorizontally,

                                    verticalArrangement =
                                        Arrangement.spacedBy(
                                            8.dp
                                        )
                                ) {

                                    Text(
                                        text =
                                            "\uD83C\uDFA4 RECORDING",

                                        style =
                                            MaterialTheme
                                                .typography
                                                .titleMedium
                                    )

                                    Text(

                                        text =
                                            formatDuration(
                                                recordingDurationMs.value
                                            ),

                                        style =
                                            MaterialTheme
                                                .typography
                                                .headlineMedium
                                    )

                                    Text(
                                        text =
                                            "Maximum 15 seconds"
                                    )
                                }
                            }
                        }
                    }

                    // =================================================
                    // MESSAGE HEADER
                    // =================================================

                    item {

                        Row(

                            modifier =
                                Modifier
                                    .fillMaxWidth(),

                            horizontalArrangement =
                                Arrangement.SpaceBetween,

                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Text(

                                text =
                                    "MESSAGES (${messages.size})",

                                style =
                                    MaterialTheme
                                        .typography
                                        .titleMedium
                            )

                            if (
                                messages.isNotEmpty()
                            ) {

                                OutlinedButton(

                                    onClick = {

                                        showDeleteAllConfirm.value =
                                            true
                                    }
                                ) {

                                    Text(
                                        "DELETE CHAT"
                                    )
                                }
                            }
                        }
                    }

                    // =================================================
                    // CHAT / RADAR
                    // =================================================

                    item {

                        Row(

                            modifier =
                                Modifier
                                    .fillMaxWidth(),

                            horizontalArrangement =
                                Arrangement.spacedBy(
                                    6.dp
                                )
                        ) {

                            OutlinedButton(

                                onClick = {

                                    viewMode.value =
                                        "CHAT"
                                },

                                modifier =
                                    Modifier
                                        .weight(1f)
                            ) {

                                Text(
                                    "CHAT"
                                )
                            }

                            OutlinedButton(

                                onClick = {

                                    viewMode.value =
                                        "RADAR"
                                },

                                modifier =
                                    Modifier
                                        .weight(1f)
                            ) {

                                Text(
                                    "RADAR"
                                )
                            }
                        }
                    }

                    // =================================================
                    // RADAR
                    // =================================================

                    if (
                        viewMode.value ==
                        "RADAR"
                    ) {

                        item {

                            val myLoc =
                                myLocation.value

                            val now =
                                System.currentTimeMillis()

                            val liveNodes =
                                liveRadarNodes
                                    .value
                                    .values
                                    .filter {

                                        it.nodeId !=
                                                nodeIdState.value
                                    }
                                    .filter {

                                        now -
                                                it.receivedAt <
                                                RADAR_NODE_TIMEOUT_MS
                                    }

                            if (
                                myLoc == null
                            ) {

                                Text(
                                    text =
                                        "Waiting for automatic GPS location..."
                                )
                            }

                            val contacts =
                                if (
                                    myLoc == null
                                ) {

                                    emptyList()

                                } else {

                                    liveNodes.map {
                                            node ->

                                        val distance =
                                            LocationUtils
                                                .distanceMeters(

                                                    myLoc.latitude,
                                                    myLoc.longitude,

                                                    node.latitude,
                                                    node.longitude
                                                )

                                        val bearing =
                                            LocationUtils
                                                .bearingDegrees(

                                                    myLoc.latitude,
                                                    myLoc.longitude,

                                                    node.latitude,
                                                    node.longitude
                                                )

                                        RadarContact(

                                            nodeId =
                                                node.nodeId,

                                            label =
                                                node.nodeId,

                                            distanceMeters =
                                                distance,

                                            bearingDegrees =
                                                bearing,

                                            accuracyMeters =
                                                node.accuracyMeters,

                                            rssi =
                                                node.rssi
                                        )
                                    }
                                }

                            RadarView(

                                contacts =
                                    contacts,

                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                            )

                            if (
                                liveNodes.isNotEmpty() &&
                                myLoc != null
                            ) {

                                Column(

                                    verticalArrangement =
                                        Arrangement.spacedBy(
                                            8.dp
                                        )
                                ) {

                                    Text(

                                        text =
                                            "LIVE NODE DETAILS",

                                        style =
                                            MaterialTheme
                                                .typography
                                                .titleMedium
                                    )

                                    liveNodes.forEach {
                                            node ->

                                        val distance =
                                            LocationUtils
                                                .distanceMeters(

                                                    myLoc.latitude,
                                                    myLoc.longitude,

                                                    node.latitude,
                                                    node.longitude
                                                )

                                        val bearing =
                                            LocationUtils
                                                .bearingDegrees(

                                                    myLoc.latitude,
                                                    myLoc.longitude,

                                                    node.latitude,
                                                    node.longitude
                                                )

                                        Card(

                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                        ) {

                                            Column(

                                                modifier =
                                                    Modifier
                                                        .padding(
                                                            12.dp
                                                        ),

                                                verticalArrangement =
                                                    Arrangement.spacedBy(
                                                        4.dp
                                                    )
                                            ) {

                                                Text(

                                                    text =
                                                        "● ${node.nodeId}",

                                                    style =
                                                        MaterialTheme
                                                            .typography
                                                            .titleSmall
                                                )

                                                Text(
                                                    text =
                                                        "GPS distance: ${
                                                            LocationUtils
                                                                .formatDistance(
                                                                    distance
                                                                )
                                                        }"
                                                )

                                                Text(
                                                    text =
                                                        "GPS accuracy: ±${
                                                            node.accuracyMeters.toInt()
                                                        } m"
                                                )

                                                Text(
                                                    text =
                                                        "Direction: ${
                                                            LocationUtils
                                                                .compassLabel(
                                                                    bearing
                                                                )
                                                        } ${
                                                            "%.1f".format(
                                                                bearing
                                                            )
                                                        }°"
                                                )

                                                Text(
                                                    text =
                                                        "BLE: ${
                                                            getRssiDescription(
                                                                node.rssi
                                                            )
                                                        }"
                                                )

                                                Text(
                                                    text =
                                                        "Hop: ${node.hopCount}"
                                                )

                                                Text(
                                                    text =
                                                        "Last seen: ${
                                                            formatLastSeen(
                                                                node.receivedAt
                                                            )
                                                        }"
                                                )

                                                Text(
                                                    text =
                                                        "Coordinates: ${
                                                            "%.6f".format(
                                                                node.latitude
                                                            )
                                                        }, ${
                                                            "%.6f".format(
                                                                node.longitude
                                                            )
                                                        }"
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                    } else {

                        // =================================================
                        // CHAT
                        // =================================================

                        if (
                            messages.isEmpty()
                        ) {

                            item {

                                Card(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                ) {

                                    Text(

                                        text =
                                            "No messages yet",

                                        modifier =
                                            Modifier
                                                .padding(
                                                    16.dp
                                                )
                                    )
                                }
                            }

                        } else {

                            items(

                                items =
                                    messages,

                                key = {
                                    it.messageId
                                }

                            ) { message ->

                                MessageCard(

                                    message =
                                        message,

                                    myNodeId =
                                        nodeIdState.value,

                                    isPlaying =
                                        playingMessageId.value ==
                                                message.messageId,

                                    onDelete = {

                                        bleManager
                                            .deleteMessage(
                                                message.messageId
                                            )
                                    },

                                    onTogglePlayback = {

                                        togglePlayback(
                                            message
                                        )
                                    }
                                )
                            }
                        }
                    }

                    // =================================================
                    // FOOTER
                    // =================================================

                    item {

                        Column(

                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        top = 20.dp,
                                        bottom = 40.dp
                                    ),

                            verticalArrangement =
                                Arrangement.spacedBy(
                                    5.dp
                                )
                        ) {

                            Text(
                                text =
                                    "AapdaSetu • Offline Emergency Mesh"
                            )

                            Text(
                                text =
                                    "Automatic GPS location is active while the mesh is running."
                            )

                            Text(
                                text =
                                    "GPS distance is shown with the sender's GPS accuracy."
                            )

                            Text(
                                text =
                                    "BLE RSSI indicates proximity and is not an exact distance."
                            )
                        }
                    }
                }
            }
        }
    }

    // =====================================================
    // MESSAGE CARD
    // =====================================================

    @Composable
    private fun MessageCard(

        message:
        BleManager.HopMessage,

        myNodeId:
        String,

        isPlaying:
        Boolean,

        onDelete:
            () -> Unit,

        onTogglePlayback:
            () -> Unit

    ) {

        Card(

            modifier =
                Modifier
                    .fillMaxWidth()
        ) {

            Column(

                modifier =
                    Modifier
                        .padding(
                            16.dp
                        ),

                verticalArrangement =
                    Arrangement.spacedBy(
                        8.dp
                    )
            ) {

                Text(

                    text =

                        if (
                            message.senderId ==
                            myNodeId
                        ) {

                            "\uD83D\uDCE4 SENT"

                        } else {

                            "\uD83D\uDCE9 RECEIVED"
                        },

                    style =
                        MaterialTheme
                            .typography
                            .titleSmall
                )

                Row(

                    modifier =
                        Modifier
                            .fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.SpaceBetween,

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text(
                        text =
                            "FROM: ${message.senderId}"
                    )

                    Text(
                        text =
                            formatMessageTime(
                                message.timestamp
                            )
                    )
                }

                if (
                    message.isVoice
                ) {

                    Text(
                        text =
                            "\uD83C\uDFA4 Voice • ${
                                formatDuration(
                                    message.recordingDurationMs
                                )
                            }"
                    )

                } else {

                    Text(

                        text =
                            message.message,

                        style =
                            MaterialTheme
                                .typography
                                .bodyLarge
                    )
                }

                Text(
                    text =
                        "HOP: ${message.hopCount}"
                )

                Text(
                    text =
                        "TTL: ${message.ttl}"
                )

                if (
                    message.hasLocation
                ) {

                    Text(
                        text =
                            "\uD83D\uDCCD ${
                                "%.5f".format(
                                    message.latitude
                                )
                            }, ${
                                "%.5f".format(
                                    message.longitude
                                )
                            }"
                    )
                }

                if (
                    message.isVoice
                ) {

                    OutlinedButton(

                        onClick = {
                            onTogglePlayback()
                        },

                        modifier =
                            Modifier
                                .fillMaxWidth()
                    ) {

                        Text(

                            if (
                                isPlaying
                            ) {

                                "\u23F8 STOP VOICE"

                            } else {

                                "\u25B6 PLAY VOICE"
                            }
                        )
                    }
                }

                OutlinedButton(

                    onClick = {
                        onDelete()
                    },

                    modifier =
                        Modifier
                            .fillMaxWidth()
                ) {

                    Text(
                        "\uD83D\uDDD1 DELETE MESSAGE"
                    )
                }
            }
        }
    }

    // =====================================================
    // PERMISSIONS
    // =====================================================

    private fun requestAllPermissions() {

        /*
         * Android 12+
         */
        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.S
        ) {

            val bluetoothMissing =
                !hasPermission(
                    Manifest.permission
                        .BLUETOOTH_SCAN
                ) ||
                        !hasPermission(
                            Manifest.permission
                                .BLUETOOTH_CONNECT
                        ) ||
                        !hasPermission(
                            Manifest.permission
                                .BLUETOOTH_ADVERTISE
                        )

            if (
                bluetoothMissing
            ) {

                requestPermissions(

                    arrayOf(

                        Manifest.permission
                            .BLUETOOTH_SCAN,

                        Manifest.permission
                            .BLUETOOTH_CONNECT,

                        Manifest.permission
                            .BLUETOOTH_ADVERTISE
                    ),

                    REQUEST_BLUETOOTH
                )

                return
            }
        }

        /*
         * Location is required for radar.
         */
        if (
            !hasFineLocationPermission()
        ) {

            requestPermissions(

                arrayOf(

                    Manifest.permission
                        .ACCESS_FINE_LOCATION,

                    Manifest.permission
                        .ACCESS_COARSE_LOCATION
                ),

                REQUEST_LOCATION
            )

            return
        }

        startBluetoothHop()
    }

    private fun hasPermission(
        permission: String
    ): Boolean {

        return ContextCompat.checkSelfPermission(
            this,
            permission
        ) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun hasLocationPermission():
            Boolean {

        return hasPermission(
            Manifest.permission
                .ACCESS_FINE_LOCATION
        ) ||
                hasPermission(
                    Manifest.permission
                        .ACCESS_COARSE_LOCATION
                )
    }

    private fun hasFineLocationPermission():
            Boolean {

        return hasPermission(
            Manifest.permission
                .ACCESS_FINE_LOCATION
        )
    }

    private fun requestLocationOnly() {

        requestPermissions(

            arrayOf(

                Manifest.permission
                    .ACCESS_FINE_LOCATION,

                Manifest.permission
                    .ACCESS_COARSE_LOCATION
            ),

            REQUEST_LOCATION
        )
    }

    private fun requestMicrophone() {

        if (
            !hasPermission(
                Manifest.permission
                    .RECORD_AUDIO
            )
        ) {

            requestPermissions(

                arrayOf(
                    Manifest.permission
                        .RECORD_AUDIO
                ),

                REQUEST_RECORD_AUDIO
            )

            return
        }

        startRecording()
    }

    // =====================================================
    // PERMISSION RESULT
    // =====================================================

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {

        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        when (requestCode) {

            REQUEST_BLUETOOTH -> {

                val granted =
                    grantResults.isNotEmpty() &&
                            grantResults.all {
                                it ==
                                        PackageManager
                                            .PERMISSION_GRANTED
                            }

                if (
                    granted
                ) {

                    requestAllPermissions()
                }
            }

            REQUEST_LOCATION -> {

                val granted =
                    grantResults.isNotEmpty() &&
                            grantResults.any {
                                it ==
                                        PackageManager
                                            .PERMISSION_GRANTED
                            }

                if (
                    granted
                ) {

                    startBluetoothHop()

                } else {

                    locationStatus.value =
                        "Location permission denied"
                }
            }

            REQUEST_RECORD_AUDIO -> {

                val granted =
                    grantResults.isNotEmpty() &&
                            grantResults.all {
                                it ==
                                        PackageManager
                                            .PERMISSION_GRANTED
                            }

                if (
                    granted
                ) {

                    startRecording()
                }
            }
        }
    }

    // =====================================================
    // START BLUETOOTH
    // =====================================================

    private fun startBluetoothHop() {

        val bluetoothManager =
            getSystemService(
                BLUETOOTH_SERVICE
            ) as BluetoothManager

        val adapter =
            bluetoothManager.adapter

        if (
            adapter == null
        ) {
            return
        }

        if (
            !adapter.isEnabled
        ) {

            statusBluetoothDisabled()

            return
        }

        bleManager.start(
            nodeIdState.value
        )

        isStarted.value =
            true

        if (
            hasFineLocationPermission()
        ) {

            startAutomaticLocation()

        } else {

            requestLocationOnly()
        }
    }

    private fun statusBluetoothDisabled() {
        // User must enable Bluetooth.
    }

    // =====================================================
    // LOCATION
    // =====================================================

    private fun startAutomaticLocation() {

        if (
            !hasFineLocationPermission()
        ) {

            requestLocationOnly()

            return
        }

        stopAutomaticLocation()

        val request =
            LocationRequest.Builder(

                Priority
                    .PRIORITY_HIGH_ACCURACY,

                LIVE_LOCATION_INTERVAL_MS

            )
                .setMinUpdateIntervalMillis(
                    LIVE_LOCATION_FASTEST_MS
                )
                .setWaitForAccurateLocation(
                    false
                )
                .build()

        val callback =
            object :
                LocationCallback() {

                override fun onLocationResult(
                    result: LocationResult
                ) {

                    for (
                    location in
                    result.locations
                    ) {

                        handleMyLocation(
                            location
                        )
                    }
                }
            }

        fusedLocationCallback =
            callback

        try {

            fusedLocationClient
                .requestLocationUpdates(

                    request,

                    callback,

                    Looper.getMainLooper()
                )

        } catch (
            _: SecurityException
        ) {

            locationStatus.value =
                "Location permission denied"

            return
        }

        /*
         * Use last known location immediately.
         */
        try {

            fusedLocationClient
                .lastLocation
                .addOnSuccessListener {
                        location ->

                    if (
                        location != null
                    ) {

                        handleMyLocation(
                            location
                        )
                    }
                }

        } catch (
            _: SecurityException
        ) {
        }

        radarHandler
            .removeCallbacks(
                radarCleanupRunnable
            )

        radarHandler
            .post(
                radarCleanupRunnable
            )
    }

    private fun stopAutomaticLocation() {

        fusedLocationCallback?.let {

            try {

                fusedLocationClient
                    .removeLocationUpdates(
                        it
                    )

            } catch (
                _: SecurityException
            ) {
            }
        }

        fusedLocationCallback =
            null

        radarHandler
            .removeCallbacks(
                radarCleanupRunnable
            )
    }

    private fun handleMyLocation(
        location: Location
    ) {

        if (
            !location.latitude.isFinite() ||
            !location.longitude.isFinite()
        ) {
            return
        }

        myLocation.value =
            location

        locationStatus.value =
            buildString {

                append(
                    "AUTO • Lat ${
                        "%.6f".format(
                            location.latitude
                        )
                    }, Lon ${
                        "%.6f".format(
                            location.longitude
                        )
                    }"
                )

                if (
                    location.accuracy > 0f
                ) {

                    append(
                        " • ±${
                            location.accuracy.toInt()
                        }m"
                    )
                }
            }

        if (
            isStarted.value
        ) {

            val accuracy =
                if (
                    location.accuracy > 0f
                ) {

                    location.accuracy

                } else {

                    999f
                }

            bleManager.sendLocationUpdate(

                location.latitude,

                location.longitude,

                accuracy
            )
        }
    }

    private fun refreshLocation() {

        if (
            !hasFineLocationPermission()
        ) {

            requestLocationOnly()

            return
        }

        locationStatus.value =
            "Fetching latest GPS location..."

        startAutomaticLocation()

        try {

            fusedLocationClient
                .lastLocation
                .addOnSuccessListener {
                        location ->

                    if (
                        location != null
                    ) {

                        handleMyLocation(
                            location
                        )
                    } else {

                        locationStatus.value =
                            "Waiting for a new GPS fix..."
                    }
                }

        } catch (
            _: SecurityException
        ) {
        }
    }

    private fun cleanStaleRadarNodes() {

        val now =
            System.currentTimeMillis()

        val filtered =
            liveRadarNodes.value
                .filterValues {

                    now -
                            it.receivedAt <
                            RADAR_NODE_TIMEOUT_MS
                }

        if (
            filtered.size !=
            liveRadarNodes.value.size
        ) {

            liveRadarNodes.value =
                filtered
        }
    }

    // =====================================================
    // NODE NAME
    // =====================================================

    private fun generateDefaultName(): String {

        return "NODE-" +
                UUID.randomUUID()
                    .toString()
                    .substring(
                        0,
                        4
                    )
                    .uppercase()
    }

    private fun sanitizeName(
        raw: String
    ): String? {

        val cleaned =
            raw
                .trim()
                .replace(
                    ";",
                    ""
                )
                .replace(
                    "\n",
                    " "
                )
                .replace(
                    "\r",
                    " "
                )
                .take(
                    MAX_NAME_LENGTH
                )
                .trim()

        return if (
            cleaned.isEmpty()
        ) {
            null
        } else {
            cleaned
        }
    }

    private fun loadSavedNodeName():
            String? {

        return getSharedPreferences(
            PREFS_NAME,
            MODE_PRIVATE
        )
            .getString(
                KEY_NODE_NAME,
                null
            )
    }

    private fun saveNodeName(
        name: String
    ) {

        getSharedPreferences(
            PREFS_NAME,
            MODE_PRIVATE
        )
            .edit()
            .putString(
                KEY_NODE_NAME,
                name
            )
            .apply()
    }

    private fun stableNodeId(
        raw: String
    ): String {

        return raw
            .trim()
            .ifBlank {
                "UNKNOWN"
            }
    }

    // =====================================================
    // TIME
    // =====================================================

    private fun formatDuration(
        durationMs: Long
    ): String {

        val totalSeconds =
            durationMs / 1000L

        val minutes =
            totalSeconds / 60L

        val seconds =
            totalSeconds % 60L

        return String.format(
            Locale.getDefault(),
            "%02d:%02d",
            minutes,
            seconds
        )
    }

    private fun formatMessageTime(
        timestamp: Long
    ): String {

        return try {

            SimpleDateFormat(
                "HH:mm",
                Locale.getDefault()
            ).format(
                Date(timestamp)
            )

        } catch (
            _: Exception
        ) {

            "--:--"
        }
    }

    private fun formatLastSeen(
        timestamp: Long
    ): String {

        val elapsed =
            (
                    System.currentTimeMillis() -
                            timestamp
                    )
                .coerceAtLeast(
                    0L
                )

        val seconds =
            elapsed / 1000L

        return when {

            seconds <= 1L ->
                "just now"

            seconds < 60L ->
                "$seconds sec ago"

            else -> {

                val minutes =
                    seconds / 60L

                if (
                    minutes == 1L
                ) {

                    "1 min ago"

                } else {

                    "$minutes min ago"
                }
            }
        }
    }

    // =====================================================
    // RSSI
    // =====================================================

    private fun getRssiDescription(
        rssi: Int?
    ): String {

        if (
            rssi == null
        ) {

            return "Unknown"
        }

        return when {

            rssi >= -55 ->
                "$rssi dBm • VERY NEAR"

            rssi >= -67 ->
                "$rssi dBm • NEAR"

            rssi >= -80 ->
                "$rssi dBm • MODERATE"

            else ->
                "$rssi dBm • WEAK"
        }
    }

    // =====================================================
    // VOICE
    // =====================================================

    private fun startRecording() {

        if (
            !hasPermission(
                Manifest.permission.RECORD_AUDIO
            )
        ) {

            requestMicrophone()

            return
        }

        if (
            isRecording.value
        ) {
            return
        }

        val file =
            File(
                cacheDir,
                "recording_${
                    System.currentTimeMillis()
                }.amr"
            )

        currentRecordingFile =
            file

        val recorder =
            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.S
            ) {

                MediaRecorder(this)

            } else {

                @Suppress("DEPRECATION")
                MediaRecorder()
            }

        try {

            recorder.setAudioSource(
                MediaRecorder.AudioSource.MIC
            )

            recorder.setOutputFormat(
                MediaRecorder.OutputFormat
                    .AMR_NB
            )

            recorder.setAudioEncoder(
                MediaRecorder.AudioEncoder
                    .AMR_NB
            )

            recorder.setMaxDuration(
                MAX_VOICE_DURATION_MS.toInt()
            )

            recorder.setOnInfoListener {
                    _,
                    what,
                    _ ->

                if (
                    what ==
                    MediaRecorder
                        .MEDIA_RECORDER_INFO_MAX_DURATION_REACHED
                ) {

                    runOnUiThread {

                        if (
                            isRecording.value
                        ) {

                            stopRecordingAndSend(
                                currentAttachLocation
                            )
                        }
                    }
                }
            }

            recorder.setOutputFile(
                file.absolutePath
            )

            recorder.prepare()
            recorder.start()

            mediaRecorder =
                recorder

            isRecording.value =
                true

            recordingStartTime =
                System.currentTimeMillis()

            recordingDurationMs.value =
                0L

            recordingHandler
                .removeCallbacks(
                    recordingTimerRunnable
                )

            recordingHandler.post(
                recordingTimerRunnable
            )

        } catch (
            _: Exception
        ) {

            try {
                recorder.release()
            } catch (
                _: Exception
            ) {
            }

            mediaRecorder =
                null

            isRecording.value =
                false

            currentRecordingFile =
                null
        }
    }

    private fun stopRecordingAndSend(
        attachLocation: Boolean
    ) {

        if (
            !isRecording.value &&
            mediaRecorder == null
        ) {
            return
        }

        recordingHandler
            .removeCallbacks(
                recordingTimerRunnable
            )

        val finalDuration =
            (
                    System.currentTimeMillis() -
                            recordingStartTime
                    )
                .coerceAtLeast(
                    0L
                )
                .coerceAtMost(
                    MAX_VOICE_DURATION_MS
                )

        recordingDurationMs.value =
            finalDuration

        val recorder =
            mediaRecorder

        isRecording.value =
            false

        if (
            recorder == null
        ) {

            recordingStartTime =
                0L

            return
        }

        try {
            recorder.stop()
        } catch (
            _: Exception
        ) {
        }

        try {
            recorder.release()
        } catch (
            _: Exception
        ) {
        }

        mediaRecorder =
            null

        recordingStartTime =
            0L

        val file =
            currentRecordingFile

        currentRecordingFile =
            null

        if (
            file == null ||
            !file.exists() ||
            file.length() == 0L
        ) {
            return
        }

        val base64Audio =
            AudioUtils.fileToBase64(
                file
            )

        try {
            file.delete()
        } catch (
            _: Exception
        ) {
        }

        if (
            base64Audio.isBlank()
        ) {
            return
        }

        if (
            attachLocation &&
            myLocation.value != null
        ) {

            val location =
                myLocation.value!!

            bleManager
                .createAndSendVoiceMessage(

                    base64Audio,

                    finalDuration,

                    location.latitude,

                    location.longitude
                )

        } else {

            bleManager
                .createAndSendVoiceMessage(

                    base64Audio,

                    finalDuration
                )
        }
    }

    private fun cancelRecording() {

        recordingHandler
            .removeCallbacks(
                recordingTimerRunnable
            )

        val recorder =
            mediaRecorder

        isRecording.value =
            false

        mediaRecorder =
            null

        recordingStartTime =
            0L

        recordingDurationMs.value =
            0L

        if (
            recorder != null
        ) {

            try {
                recorder.stop()
            } catch (
                _: Exception
            ) {
            }

            try {
                recorder.release()
            } catch (
                _: Exception
            ) {
            }
        }

        currentRecordingFile?.delete()

        currentRecordingFile =
            null
    }

    // =====================================================
    // PLAYBACK
    // =====================================================

    private fun togglePlayback(
        message:
        BleManager.HopMessage
    ) {

        if (
            playingMessageId.value ==
            message.messageId
        ) {

            stopPlayback()

            return
        }

        stopPlayback()

        val file =
            AudioUtils
                .base64ToPlaybackFile(

                    cacheDir,

                    message.message,

                    message.messageId
                )
                ?: return

        try {

            val player =
                MediaPlayer()

            player.setDataSource(
                file.absolutePath
            )

            player.setOnCompletionListener {

                try {
                    it.release()
                } catch (
                    _: Exception
                ) {
                }

                mediaPlayer =
                    null

                playingMessageId.value =
                    null
            }

            player.setOnErrorListener {
                    mp,
                    _,
                    _ ->

                try {
                    mp.release()
                } catch (
                    _: Exception
                ) {
                }

                mediaPlayer =
                    null

                playingMessageId.value =
                    null

                true
            }

            player.prepare()
            player.start()

            mediaPlayer =
                player

            playingMessageId.value =
                message.messageId

        } catch (
            _: Exception
        ) {

            try {
                mediaPlayer?.release()
            } catch (
                _: Exception
            ) {
            }

            mediaPlayer =
                null

            playingMessageId.value =
                null
        }
    }

    private fun stopPlayback() {

        try {
            mediaPlayer?.stop()
        } catch (
            _: Exception
        ) {
        }

        try {
            mediaPlayer?.release()
        } catch (
            _: Exception
        ) {
        }

        mediaPlayer =
            null

        playingMessageId.value =
            null
    }

    // =====================================================
    // DESTROY
    // =====================================================

    override fun onDestroy() {

        stopAutomaticLocation()

        recordingHandler
            .removeCallbacks(
                recordingTimerRunnable
            )

        stopPlayback()

        cancelRecording()

        try {
            bleManager.stop()
        } catch (
            _: Exception
        ) {
        }

        super.onDestroy()
    }
}