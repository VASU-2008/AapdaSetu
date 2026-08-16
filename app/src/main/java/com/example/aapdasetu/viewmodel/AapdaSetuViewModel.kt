package com.example.aapdasetu.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.aapdasetu.location.LocationData
import com.example.aapdasetu.location.LocationServiceHelper
import com.example.aapdasetu.mesh.BluetoothMeshManager
import com.example.aapdasetu.model.MeshMessage
import com.example.aapdasetu.model.MeshNode
import com.example.aapdasetu.model.NodeStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isBluetoothOn: Boolean = true,
    val meshStatus: String = "READY",
    val connectedNodesCount: Int = 2,
    val nearbyNodes: List<MeshNode> = emptyList(),
    val messages: List<MeshMessage> = emptyList(),
    val currentLocation: LocationData = LocationData(),
    val isSosActive: Boolean = false
)

class AapdaSetuViewModel(application: Application) : AndroidViewModel(application) {

    val meshManager = BluetoothMeshManager(application)
    val locationHelper = LocationServiceHelper(application)

    private val _isSosActive = MutableStateFlow(false)
    val isSosActive: StateFlow<Boolean> = _isSosActive.asStateFlow()

    val uiState: StateFlow<DashboardUiState> = combine(
        combine(meshManager.isBluetoothEnabled, meshManager.meshStatus, _isSosActive) { bt, mesh, sos ->
            Triple(bt, mesh, sos)
        },
        meshManager.connectedNodes,
        meshManager.messages,
        locationHelper.locationState
    ) { (btOn, meshStatus, sosActive), nodes, msgs, loc ->
        val connectedCount = nodes.count { it.status == NodeStatus.CONNECTED }
        DashboardUiState(
            isBluetoothOn = btOn,
            meshStatus = meshStatus,
            connectedNodesCount = connectedCount,
            nearbyNodes = nodes,
            messages = msgs,
            currentLocation = loc,
            isSosActive = sosActive
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    init {
        meshManager.startMesh()
        locationHelper.requestCurrentLocation()
    }

    fun onPermissionsGranted() {
        meshManager.startMesh()
        locationHelper.requestCurrentLocation()
    }

    fun triggerSos(note: String = "EMERGENCY: Immediate Assistance Needed") {
        val loc = uiState.value.currentLocation
        _isSosActive.value = true
        meshManager.sendSos(
            latitude = loc.latitude,
            longitude = loc.longitude,
            emergencyNote = note
        )
    }

    fun cancelSos() {
        _isSosActive.value = false
    }

    fun connectNode(nodeId: String) {
        meshManager.connectToNode(nodeId)
    }

    fun disconnectNode(nodeId: String) {
        meshManager.disconnectNode(nodeId)
    }

    fun sendReport(title: String, desc: String) {
        val loc = uiState.value.currentLocation
        meshManager.broadcastReport(title, desc, loc.latitude, loc.longitude)
    }

    fun addSimulatedNode(name: String) {
        meshManager.simulateNewNearbyNode(name)
    }
}
