package com.vitreminder.app.data.model

data class RoomLocation(
    val rawRoom: String,
    val buildingName: String,
    val floorName: String,
    val roomNumber: String,
    val tips: String = ""
) {
    val shortLocation: String
        get() = when {
            buildingName.isNotBlank() && floorName.isNotBlank() -> "$buildingName, $floorName"
            buildingName.isNotBlank() -> buildingName
            else -> rawRoom
        }

    val displaySummary: String
        get() = when {
            buildingName.isNotBlank() && floorName.isNotBlank() -> "$buildingName • $floorName"
            else -> "Room $rawRoom"
        }
}
