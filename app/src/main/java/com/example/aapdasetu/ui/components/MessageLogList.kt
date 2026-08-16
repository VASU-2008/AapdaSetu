package com.example.aapdasetu.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aapdasetu.model.DeliveryStatus
import com.example.aapdasetu.model.MeshMessage
import com.example.aapdasetu.model.MessageType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MessagesSection(
    messages: List<MeshMessage>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Messages",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.padding(top = 8.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (messages.isEmpty()) {
                    Text(
                        text = "No mesh message activity yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    messages.take(6).forEachIndexed { index, message ->
                        val isLast = index == messages.take(6).size - 1
                        val branchSymbol = if (isLast) "└── " else "├── "

                        MessageTreeItem(
                            branch = branchSymbol,
                            message = message
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MessageTreeItem(
    branch: String,
    message: MeshMessage
) {
    val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(message.timestamp))
    val checkmark = when (message.status) {
        DeliveryStatus.DELIVERED -> "✓"
        DeliveryStatus.SENT -> "↗"
        DeliveryStatus.RELAYED -> "✓✓"
        DeliveryStatus.PENDING -> "..."
        DeliveryStatus.FAILED -> "✗"
    }

    val checkColor = when (message.status) {
        DeliveryStatus.DELIVERED -> Color(0xFF10B981)
        DeliveryStatus.RELAYED -> Color(0xFF3B82F6)
        DeliveryStatus.SENT -> Color(0xFFFBBF24)
        DeliveryStatus.PENDING -> Color.Gray
        DeliveryStatus.FAILED -> Color(0xFFEF4444)
    }

    val typePrefix = when (message.type) {
        MessageType.SOS -> "SOS"
        MessageType.REPORT -> "REPORT"
        MessageType.ACK -> "ACK"
        MessageType.INFO -> "INFO"
        MessageType.PING -> "PING"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = branch,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 15.sp
            )

            Text(
                text = "$typePrefix → ${message.targetNode}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                ),
                color = if (message.type == MessageType.SOS) Color(0xFFF87171) else MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = checkmark,
                fontWeight = FontWeight.Black,
                color = checkColor,
                fontSize = 16.sp
            )
        }

        Text(
            text = timeStr,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            fontFamily = FontFamily.Monospace
        )
    }
}
