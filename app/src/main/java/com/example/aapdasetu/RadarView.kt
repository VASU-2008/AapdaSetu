package com.example.aapdasetu

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

data class RadarContact(
    val label: String,
    val distanceMeters: Double,
    val bearingDegrees: Double
)

// Simple offline "radar": places each contact as a dot around a center "you"
// marker based on raw distance + bearing computed from GPS coordinates.
// No map tiles, no internet - works anywhere there's a GPS fix.
@Composable
fun RadarView(
    contacts: List<RadarContact>,
    maxRangeMeters: Double = 2000.0,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {

        Text(
            text = "Range shown: up to ${maxRangeMeters.toInt()} m (further = pinned to edge)",
            style = MaterialTheme.typography.bodySmall
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val maxRadius = min(size.width, size.height) / 2f * 0.9f

                // Range rings
                for (fraction in listOf(0.33f, 0.66f, 1f)) {
                    drawCircle(
                        color = Color.Gray.copy(alpha = 0.4f),
                        radius = maxRadius * fraction,
                        center = center,
                        style = Stroke(width = 2f)
                    )
                }

                // "You" marker at center
                drawCircle(
                    color = Color.Blue,
                    radius = 12f,
                    center = center
                )

                // Contacts, placed by distance (clamped to edge) + bearing.
                // bearing - 90 converts compass bearing (0 = North = up on
                // screen) into standard screen angle (0 = right, clockwise
                // as y increases downward).
                for (contact in contacts) {
                    val clamped = min(contact.distanceMeters, maxRangeMeters)
                    val radiusFraction = (clamped / maxRangeMeters).toFloat()
                    val angleRad = Math.toRadians(contact.bearingDegrees - 90.0)

                    val dotX = center.x + (maxRadius * radiusFraction) * cos(angleRad).toFloat()
                    val dotY = center.y + (maxRadius * radiusFraction) * sin(angleRad).toFloat()

                    drawCircle(
                        color = Color.Red,
                        radius = 14f,
                        center = Offset(dotX, dotY)
                    )
                }
            }
        }

        if (contacts.isEmpty()) {
            Text(
                text = "No location-tagged messages yet.",
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            Column {
                for (contact in contacts) {
                    Text(
                        text = "${contact.label} - " +
                                "${LocationUtils.formatDistance(contact.distanceMeters)} " +
                                "(${LocationUtils.compassLabel(contact.bearingDegrees)})",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}