package com.example.aapdasetu.model

enum class MessageType {
    SOS,
    REPORT,
    INFO,
    ACK,
    PING
}

enum class DeliveryStatus {
    PENDING,
    SENT,
    RELAYED,
    DELIVERED,
    FAILED
}

data class MeshMessage(
    val id: String,
    val senderId: String,
    val senderName: String,
    val targetNode: String = "BROADCAST",
    val content: String,
    val type: MessageType = MessageType.SOS,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val hopCount: Int = 1,
    val relayedBy: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val status: DeliveryStatus = DeliveryStatus.SENT
)
