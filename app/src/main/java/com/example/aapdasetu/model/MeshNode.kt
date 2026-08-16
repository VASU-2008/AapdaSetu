package com.example.aapdasetu.model

enum class NodeStatus {
    CONNECTED,
    AVAILABLE,
    RELAYING,
    DISCONNECTED
}

data class MeshNode(
    val id: String,
    val name: String,
    val status: NodeStatus,
    val rssi: Int = -60, // dBm
    val hopDistance: Int = 1,
    val batteryLevel: Int = 85,
    val isRelayCapable: Boolean = true,
    val lastSeenTimestamp: Long = System.currentTimeMillis()
)
