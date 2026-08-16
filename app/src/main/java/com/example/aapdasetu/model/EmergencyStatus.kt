package com.example.aapdasetu.model

enum class EmergencyType(val label: String, val code: String) {
    MEDICAL("Medical Emergency", "MED"),
    TRAPPED("Trapped / Structural Collapse", "TRAP"),
    FIRE("Fire / Hazard", "FIRE"),
    FLOOD("Flood / Water Rising", "FLD"),
    GENERAL("General SOS", "GEN")
}

data class EmergencyPayload(
    val senderId: String,
    val senderName: String,
    val emergencyType: EmergencyType = EmergencyType.GENERAL,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null,
    val batteryPercent: Int = 100,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val peopleCount: Int = 1
)
