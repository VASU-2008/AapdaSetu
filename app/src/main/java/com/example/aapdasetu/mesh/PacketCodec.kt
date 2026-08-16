package com.example.aapdasetu.mesh

import com.example.aapdasetu.model.DeliveryStatus
import com.example.aapdasetu.model.MeshMessage
import com.example.aapdasetu.model.MessageType
import java.nio.charset.StandardCharsets

object PacketCodec {
    private const val PREFIX = "AAPDA:"
    private const val DELIMITER = "|"

    /**
     * Compact serialization for BLE service data / advertising packet
     * Format: AAPDA:ID|TYPE|SENDER_ID|SENDER_NAME|LAT|LON|HOPS|CONTENT
     */
    fun encode(message: MeshMessage): ByteArray {
        val raw = StringBuilder().apply {
            append(PREFIX)
            append(message.id).append(DELIMITER)
            append(message.type.name).append(DELIMITER)
            append(message.senderId).append(DELIMITER)
            append(message.senderName).append(DELIMITER)
            append(message.latitude ?: 0.0).append(DELIMITER)
            append(message.longitude ?: 0.0).append(DELIMITER)
            append(message.hopCount).append(DELIMITER)
            append(message.content)
        }.toString()

        return raw.toByteArray(StandardCharsets.UTF_8)
    }

    fun decode(bytes: ByteArray): MeshMessage? {
        return try {
            val raw = String(bytes, StandardCharsets.UTF_8)
            if (!raw.startsWith(PREFIX)) return null
            val payload = raw.removePrefix(PREFIX)
            val parts = payload.split(DELIMITER)
            if (parts.size < 8) return null

            val id = parts[0]
            val type = runCatching { MessageType.valueOf(parts[1]) }.getOrDefault(MessageType.SOS)
            val senderId = parts[2]
            val senderName = parts[3]
            val lat = parts[4].toDoubleOrNull()
            val lon = parts[5].toDoubleOrNull()
            val hops = parts[6].toIntOrNull() ?: 1
            val content = parts.subList(7, parts.size).joinToString(DELIMITER)

            MeshMessage(
                id = id,
                senderId = senderId,
                senderName = senderName,
                type = type,
                latitude = lat,
                longitude = lon,
                hopCount = hops,
                content = content,
                status = DeliveryStatus.DELIVERED
            )
        } catch (_: Exception) {
            null
        }
    }
}
