package com.example.aapdasetu

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class MainActivity : ComponentActivity() {

    // =====================================================
    // BLUETOOTH
    // =====================================================

    private lateinit var bleManager: BleManager

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

    // =====================================================
    // VOICE
    // =====================================================

    private val isRecording =
        mutableStateOf(false)

    /*
     * Live recording duration.
     *
     * UI updates every 100 ms while recording.
     */
    private val recordingDurationMs =
        mutableStateOf(0L)

    private var recordingStartTime =
        0L

    /*
     * Used if recording is stopped automatically.
     */
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

        private const val PREFS_NAME =
            "aapdasetu_prefs"

        private const val KEY_NODE_NAME =
            "node_name"

        private const val MAX_NAME_LENGTH =
            24

        private const val MAX_VOICE_DURATION_MS =
            15000L

        private const val LOCATION_FRESH_WINDOW_MS =
            2 * 60 * 1000L

        private const val LOCATION_FIX_TIMEOUT_MS =
            8000L
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

    // =====================================================
    // FORMAT TIME
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

            val formatter =
                SimpleDateFormat(
                    "HH:mm",
                    Locale.getDefault()
                )

            formatter.format(
                Date(timestamp)
            )

        } catch (_: Exception) {

            "--:--"
        }
    }

    // =====================================================
    // PERMISSIONS
    // =====================================================

    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            if (
                permissions.values.all {
                    it
                }
            ) {

                startBluetoothHop()

            } else {

                // User can press Start again.
            }
        }

    private val locationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {

                refreshLocation()

            } else {

                locationStatus.value =
                    "Location permission denied"
            }
        }

    private val recordPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {

                startRecording()
            }
        }

    private fun hasLocationPermission():
            Boolean {

        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun hasRecordAudioPermission():
            Boolean {

        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) ==
                PackageManager.PERMISSION_GRANTED
    }

    // =====================================================
    // LOCATION
    // =====================================================

    private fun refreshLocation() {

        if (
            !hasLocationPermission()
        ) {

            locationPermissionLauncher.launch(
                Manifest.permission.ACCESS_FINE_LOCATION
            )

            return
        }

        locationStatus.value =
            "Fetching GPS fix..."

        fetchLocation { location ->

            if (
                location != null
            ) {

                myLocation.value =
                    location

                locationStatus.value =
                    "Lat ${
                        "%.5f".format(
                            location.latitude
                        )
                    }, Lon ${
                        "%.5f".format(
                            location.longitude
                        )
                    }"

            } else {

                locationStatus.value =
                    "Location unavailable. Check that GPS is ON."
            }
        }
    }

    private fun fetchLocation(
        onResult: (Location?) -> Unit
    ) {

        if (
            !hasLocationPermission()
        ) {

            onResult(null)

            return
        }

        val locationManager =
            getSystemService(
                LOCATION_SERVICE
            ) as LocationManager

        if (
            !locationManager.isProviderEnabled(
                LocationManager.GPS_PROVIDER
            )
        ) {

            onResult(null)

            return
        }

        val last =
            try {

                locationManager
                    .getLastKnownLocation(
                        LocationManager.GPS_PROVIDER
                    )

            } catch (
                _: SecurityException
            ) {

                null
            }

        if (
            last != null &&
            System.currentTimeMillis() -
            last.time <
            LOCATION_FRESH_WINDOW_MS
        ) {

            onResult(
                last
            )

            return
        }

        var settled =
            false

        val listener =
            object : LocationListener {

                override fun onLocationChanged(
                    location: Location
                ) {

                    if (
                        settled
                    ) {
                        return
                    }

                    settled =
                        true

                    locationManager
                        .removeUpdates(
                            this
                        )

                    onResult(
                        location
                    )
                }

                @Deprecated(
                    "Deprecated in Java"
                )
                override fun onStatusChanged(
                    provider: String?,
                    status: Int,
                    extras: Bundle?
                ) {
                }

                override fun onProviderEnabled(
                    provider: String
                ) {
                }

                override fun onProviderDisabled(
                    provider: String
                ) {
                }
            }

        try {

            locationManager
                .requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    0L,
                    0f,
                    listener,
                    Looper.getMainLooper()
                )

        } catch (
            _: SecurityException
        ) {

            onResult(null)

            return
        }

        Handler(
            Looper.getMainLooper()
        ).postDelayed({

            if (
                !settled
            ) {

                settled =
                    true

                locationManager
                    .removeUpdates(
                        listener
                    )

                onResult(
                    last
                )
            }

        }, LOCATION_FIX_TIMEOUT_MS)
    }

    // =====================================================
    // START RECORDING
    // =====================================================

    private fun startRecording() {

        if (
            !hasRecordAudioPermission()
        ) {

            recordPermissionLauncher.launch(
                Manifest.permission.RECORD_AUDIO
            )

            return
        }

        /*
         * Do not start a second recorder.
         */
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

                @Suppress(
                    "DEPRECATION"
                )

                MediaRecorder()
            }

        try {

            recorder.setAudioSource(
                MediaRecorder.AudioSource.MIC
            )

            recorder.setOutputFormat(
                MediaRecorder.OutputFormat.AMR_NB
            )

            recorder.setAudioEncoder(
                MediaRecorder.AudioEncoder.AMR_NB
            )

            /*
             * Maximum recording duration.
             */
            recorder.setMaxDuration(
                MAX_VOICE_DURATION_MS.toInt()
            )

            /*
             * IMPORTANT:
             *
             * When Android reaches the maximum duration,
             * automatically stop and send the voice message.
             */
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

            recordingHandler.removeCallbacks(
                recordingTimerRunnable
            )

            recordingHandler.post(
                recordingTimerRunnable
            )

        } catch (
            e: Exception
        ) {

            try {
                recorder.release()
            } catch (_: Exception) {
            }

            mediaRecorder =
                null

            isRecording.value =
                false

            currentRecordingFile =
                null

            recordingDurationMs.value =
                0L
        }
    }

    // =====================================================
    // STOP RECORDING + SEND
    // =====================================================

    private fun stopRecordingAndSend(
        attachLocation: Boolean
    ) {

        if (
            !isRecording.value &&
            mediaRecorder == null
        ) {
            return
        }

        /*
         * Stop timer first.
         */
        recordingHandler.removeCallbacks(
            recordingTimerRunnable
        )

        val finalDuration =
            if (
                recordingStartTime > 0L
            ) {

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

            } else {

                recordingDurationMs.value
            }

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

        /*
         * Compress + Base64 encode through AudioUtils.
         */
        val base64Audio =
            AudioUtils.fileToBase64(
                file
            )

        /*
         * Delete temporary recording.
         */
        try {
            file.delete()
        } catch (_: Exception) {
        }

        if (
            base64Audio.isBlank()
        ) {

            return
        }

        /*
         * Send duration along with the voice message.
         */
        if (
            attachLocation &&
            myLocation.value != null
        ) {

            val loc =
                myLocation.value!!

            bleManager.createAndSendVoiceMessage(

                base64Audio,

                finalDuration,

                loc.latitude,

                loc.longitude
            )

        } else {

            bleManager.createAndSendVoiceMessage(

                base64Audio,

                finalDuration
            )
        }
    }

    // =====================================================
    // CANCEL RECORDING
    // =====================================================

    private fun cancelRecording() {

        recordingHandler.removeCallbacks(
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

        currentRecordingFile
            ?.delete()

        currentRecordingFile =
            null
    }

    // =====================================================
    // VOICE PLAYBACK
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
                } catch (_: Exception) {
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
            } catch (_: Exception) {
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

            // =================================================
            // STATE
            // =================================================

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
            // CALLBACKS
            // =================================================

            bleManager.onStatusChanged =
                { text ->

                    runOnUiThread {

                        status.value =
                            text
                    }
                }

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
            // DELETE DIALOG
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

                    // =========================================
                    // TITLE
                    // =========================================

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

                    // =========================================
                    // NODE NAME
                    // =========================================

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

                                        bleManager
                                            .start(
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

                    // =========================================
                    // STATUS
                    // =========================================

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

                    // =========================================
                    // START BLUETOOTH
                    // =========================================

                    item {

                        Button(

                            onClick = {
                                requestPermissions()
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

                    // =========================================
                    // NEARBY NODES
                    // =========================================

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

                    // =========================================
                    // LOCATION
                    // =========================================

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

                                if (
                                    myLocation.value !=
                                    null
                                ) {

                                    Text(
                                        text =
                                            "Latitude: ${
                                                "%.6f".format(
                                                    myLocation
                                                        .value!!
                                                        .latitude
                                                )
                                            }"
                                    )

                                    Text(
                                        text =
                                            "Longitude: ${
                                                "%.6f".format(
                                                    myLocation
                                                        .value!!
                                                        .longitude
                                                )
                                            }"
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
                                        "REFRESH LOCATION"
                                    )
                                }
                            }
                        }
                    }

                    // =========================================
                    // ATTACH LOCATION
                    // =========================================

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

                    // =========================================
                    // MESSAGE INPUT
                    // =========================================

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

                    // =========================================
                    // SOS + VOICE
                    // =========================================

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

                                            val loc =
                                                myLocation.value!!

                                            bleManager
                                                .createAndSendMessage(

                                                    text,

                                                    loc.latitude,

                                                    loc.longitude
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

                                        /*
                                         * Remember the location
                                         * choice for possible
                                         * auto-stop at 15 seconds.
                                         */
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

                    // =========================================
                    // RECORDING STATUS
                    // =========================================

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

                                    verticalArrangement =
                                        Arrangement.spacedBy(
                                            8.dp
                                        ),

                                    horizontalAlignment =
                                        Alignment.CenterHorizontally

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
                                            "Maximum ${
                                                MAX_VOICE_DURATION_MS /
                                                        1000
                                            } seconds"
                                    )
                                }
                            }
                        }
                    }

                    // =========================================
                    // MESSAGE HEADER
                    // =========================================

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

                    // =========================================
                    // CHAT / RADAR
                    // =========================================

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

                    // =========================================
                    // RADAR
                    // =========================================

                    if (
                        viewMode.value ==
                        "RADAR"
                    ) {

                        item {

                            val loc =
                                myLocation.value

                            val contacts =
                                if (
                                    loc == null
                                ) {

                                    emptyList()

                                } else {

                                    messages
                                        .filter {

                                            it.hasLocation &&
                                                    it.senderId !=
                                                    nodeIdState.value
                                        }
                                        .map { message ->

                                            RadarContact(

                                                label =
                                                    message.senderId,

                                                distanceMeters =
                                                    LocationUtils
                                                        .distanceMeters(

                                                            loc.latitude,
                                                            loc.longitude,

                                                            message.latitude,
                                                            message.longitude
                                                        ),

                                                bearingDegrees =
                                                    LocationUtils
                                                        .bearingDegrees(

                                                            loc.latitude,
                                                            loc.longitude,

                                                            message.latitude,
                                                            message.longitude
                                                        )
                                            )
                                        }
                                }

                            if (
                                loc == null
                            ) {

                                Text(
                                    text =
                                        "Fetch your own location first to see the radar."
                                )
                            }

                            RadarView(

                                contacts =
                                    contacts,

                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                            )
                        }

                    } else {

                        // =====================================
                        // CHAT
                        // =====================================

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

                            ) { hopMessage ->

                                MessageCard(

                                    message =
                                        hopMessage,

                                    myNodeId =
                                        nodeIdState.value,

                                    isPlaying =
                                        playingMessageId.value ==
                                                hopMessage.messageId,

                                    onDelete = {

                                        bleManager
                                            .deleteMessage(
                                                hopMessage.messageId
                                            )
                                    },

                                    onTogglePlayback = {

                                        togglePlayback(
                                            hopMessage
                                        )
                                    }
                                )
                            }
                        }
                    }

                    // =========================================
                    // BOTTOM
                    // =========================================

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
                                    "Bluetooth messages can travel through multiple hops."
                            )

                            Text(
                                text =
                                    "Scroll up/down to access all features."
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
                            "FROM: " +
                                    message.senderId
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
                        "HOP: " +
                                message.hopCount
                )

                Text(
                    text =
                        "TTL: " +
                                message.ttl
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
    // REQUEST PERMISSIONS
    // =====================================================

    private fun requestPermissions() {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.S
        ) {

            permissionLauncher.launch(

                arrayOf(

                    Manifest.permission.BLUETOOTH_SCAN,

                    Manifest.permission.BLUETOOTH_CONNECT,

                    Manifest.permission.BLUETOOTH_ADVERTISE,

                    Manifest.permission.ACCESS_FINE_LOCATION,

                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )

        } else {

            permissionLauncher.launch(

                arrayOf(

                    Manifest.permission.ACCESS_FINE_LOCATION,

                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
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
    }

    private fun statusBluetoothDisabled() {

        // User must enable Bluetooth.
    }

    // =====================================================
    // DESTROY
    // =====================================================

    override fun onDestroy() {

        recordingHandler.removeCallbacks(
            recordingTimerRunnable
        )

        stopPlayback()

        cancelRecording()

        try {
            bleManager.stop()
        } catch (_: Exception) {
        }

        super.onDestroy()
    }
}