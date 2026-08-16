package com.example.aapdasetu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import com.example.aapdasetu.model.MeshNode
import com.example.aapdasetu.model.NodeStatus

@Composable
fun NearbyNodesSection(
    nodes: List<MeshNode>,
    onNodeClick: (MeshNode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Nearby Nodes",
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
                if (nodes.isEmpty()) {
                    Text(
                        text = "Scanning for nearby mesh nodes...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    nodes.forEachIndexed { index, node ->
                        val isLast = index == nodes.size - 1
                        val branchSymbol = if (isLast) "└── " else "├── "

                        NodeTreeItem(
                            branch = branchSymbol,
                            node = node,
                            onClick = { onNodeClick(node) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NodeTreeItem(
    branch: String,
    node: MeshNode,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
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
                text = node.name,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "(${node.rssi} dBm · ${node.hopDistance} hop)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }

        NodeStatusPill(status = node.status)
    }
}

@Composable
fun NodeStatusPill(status: NodeStatus) {
    val (backgroundColor, textColor, label) = when (status) {
        NodeStatus.CONNECTED -> Triple(Color(0xFF065F46), Color(0xFF34D399), "Connected")
        NodeStatus.AVAILABLE -> Triple(Color(0xFF1E293B), Color(0xFF94A3B8), "Available")
        NodeStatus.RELAYING -> Triple(Color(0xFF78350F), Color(0xFFFBBF24), "Relaying")
        NodeStatus.DISCONNECTED -> Triple(Color(0xFF450A0A), Color(0xFFF87171), "Offline")
    }

    Box(
        modifier = Modifier
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            fontFamily = FontFamily.Monospace
        )
    }
}
