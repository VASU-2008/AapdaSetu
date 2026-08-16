package com.example.aapdasetu

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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

// =========================================================
// RADAR CONTACT
// =========================================================

data class RadarContact(

    val nodeId: String,

    val label: String,

    val distanceMeters: Double,

    val bearingDegrees: Double,

    val accuracyMeters: Float = 0f,

    val rssi: Int? = null
)

// =========================================================
// STABLE COLOR
// =========================================================

private fun stableNodeColor(
    nodeId: String
): Color {

    val colors =
        listOf(

            Color(0xFF1565C0),

            Color(0xFF2E7D32),

            Color(0xFFC62828),

            Color(0xFF6A1B9A),

            Color(0xFFEF6C00),

            Color(0xFF00838F),

            Color(0xFFAD1457),

            Color(0xFF4527A0),

            Color(0xFF558B2F),

            Color(0xFF00695C)
        )

    val index =
        (
                nodeId.hashCode()
                        and Int.MAX_VALUE
                ) % colors.size

    return colors[index]
}

// =========================================================
// RSSI LABEL
// =========================================================

private fun radarRssiLabel(
    rssi: Int?
): String {

    if (
        rssi == null
    ) {
        return "BLE ?"
    }

    return when {

        rssi >= -55 ->
            "VERY NEAR"

        rssi >= -67 ->
            "NEAR"

        rssi >= -80 ->
            "MODERATE"

        else ->
            "WEAK"
    }
}

// =========================================================
// RADAR
// =========================================================

@Composable
fun RadarView(

    contacts: List<RadarContact>,

    maxRangeMeters: Double = 2000.0,

    modifier: Modifier =
        Modifier

) {

    Column(

        modifier =
            modifier
                .fillMaxWidth(),

        verticalArrangement =
            Arrangement.spacedBy(
                8.dp
            )
    ) {

        Text(

            text =
                "GPS range: up to ${
                    maxRangeMeters.toInt()
                } m",

            style =
                MaterialTheme
                    .typography
                    .bodySmall
        )

        Text(

            text =
                "Colored markers identify each node. BLE shows proximity.",

            style =
                MaterialTheme
                    .typography
                    .bodySmall
        )

        // =====================================================
        // RADAR CANVAS
        // =====================================================

        Box(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(8.dp),

            contentAlignment =
                Alignment.Center
        ) {

            Canvas(

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
            ) {

                val center =
                    Offset(
                        size.width / 2f,
                        size.height / 2f
                    )

                val maxRadius =
                    min(
                        size.width,
                        size.height
                    ) / 2f * 0.9f

                // =============================================
                // RANGE RINGS
                // =============================================

                for (
                fraction in
                listOf(
                    0.33f,
                    0.66f,
                    1f
                )
                ) {

                    drawCircle(

                        color =
                            Color.Gray.copy(
                                alpha = 0.4f
                            ),

                        radius =
                            maxRadius *
                                    fraction,

                        center =
                            center,

                        style =
                            Stroke(
                                width = 2f
                            )
                    )
                }

                // =============================================
                // YOU
                // =============================================

                drawCircle(

                    color =
                        Color.Black,

                    radius =
                        13f,

                    center =
                        center
                )

                /*
                 * Small white center.
                 */
                drawCircle(

                    color =
                        Color.White,

                    radius =
                        5f,

                    center =
                        center
                )

                // =============================================
                // CONTACTS
                // =============================================

                for (
                contact in contacts
                ) {

                    val clampedDistance =
                        min(
                            contact.distanceMeters,
                            maxRangeMeters
                        )

                    val radiusFraction =
                        (
                                clampedDistance /
                                        maxRangeMeters
                                )
                            .toFloat()
                            .coerceIn(
                                0f,
                                1f
                            )

                    /*
                     * Bearing:
                     *
                     * 0 = North
                     * 90 = East
                     *
                     * Subtracting 90 converts it to the
                     * normal screen coordinate system.
                     */
                    val angleRad =
                        Math.toRadians(
                            contact.bearingDegrees -
                                    90.0
                        )

                    val dotX =
                        center.x +
                                (
                                        maxRadius *
                                                radiusFraction
                                        ) *
                                cos(
                                    angleRad
                                ).toFloat()

                    val dotY =
                        center.y +
                                (
                                        maxRadius *
                                                radiusFraction
                                        ) *
                                sin(
                                    angleRad
                                ).toFloat()

                    val nodeColor =
                        stableNodeColor(
                            contact.nodeId
                        )

                    /*
                     * If GPS accuracy is poor, draw an
                     * uncertainty ring around the marker.
                     *
                     * This prevents the radar from pretending
                     * that a 40 m ±35 m reading is exact.
                     */
                    val accuracyRatio =
                        (
                                contact.accuracyMeters /
                                        maxRangeMeters
                                )
                            .toFloat()
                            .coerceIn(
                                0f,
                                1f
                            )

                    if (
                        contact.accuracyMeters >
                        0f
                    ) {

                        drawCircle(

                            color =
                                nodeColor.copy(
                                    alpha =
                                        0.18f
                                ),

                            radius =
                                maxRadius *
                                        accuracyRatio,

                            center =
                                Offset(
                                    dotX,
                                    dotY
                                ),

                            style =
                                Stroke(
                                    width = 3f
                                )
                        )
                    }

                    /*
                     * Node marker.
                     */
                    drawCircle(

                        color =
                            nodeColor,

                        radius =
                            14f,

                        center =
                            Offset(
                                dotX,
                                dotY
                            )
                    )

                    /*
                     * White center makes markers easier
                     * to distinguish.
                     */
                    drawCircle(

                        color =
                            Color.White,

                        radius =
                            4f,

                        center =
                            Offset(
                                dotX,
                                dotY
                            )
                    )
                }
            }
        }

        // =====================================================
        // CONTACT LIST
        // =====================================================

        if (
            contacts.isEmpty()
        ) {

            Text(

                text =
                    "No live nodes yet.",

                style =
                    MaterialTheme
                        .typography
                        .bodyMedium
            )

        } else {

            Column(

                verticalArrangement =
                    Arrangement.spacedBy(
                        6.dp
                    )
            ) {

                for (
                contact in contacts
                ) {

                    val nodeColor =
                        stableNodeColor(
                            contact.nodeId
                        )

                    Row(

                        modifier =
                            Modifier
                                .fillMaxWidth(),

                        verticalAlignment =
                            Alignment
                                .CenterVertically,

                        horizontalArrangement =
                            Arrangement
                                .spacedBy(
                                    8.dp
                                )
                    ) {

                        Text(

                            text =
                                "●",

                            color =
                                nodeColor
                        )

                        Column {

                            Text(

                                text =
                                    contact.label,

                                style =
                                    MaterialTheme
                                        .typography
                                        .titleSmall
                            )

                            Text(

                                text =
                                    "${
                                        LocationUtils
                                            .formatDistance(
                                                contact.distanceMeters
                                            )
                                    } • ${
                                        LocationUtils
                                            .compassLabel(
                                                contact.bearingDegrees
                                            )
                                    }"
                            )

                            Text(

                                text =
                                    "GPS accuracy: ±${
                                        contact.accuracyMeters
                                            .toInt()
                                    } m • ${
                                        radarRssiLabel(
                                            contact.rssi
                                        )
                                    }"
                            )
                        }
                    }
                }
            }
        }
    }
}