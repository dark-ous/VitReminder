package com.vitreminder.app.util

import com.vitreminder.app.data.model.RoomLocation

object RoomDecoder {

    fun decode(rawRoom: String?): RoomLocation {
        val clean = rawRoom?.trim()?.uppercase() ?: ""
        if (clean.isBlank()) {
            return RoomLocation(
                rawRoom = "",
                buildingName = "Classroom",
                floorName = "Check schedule",
                roomNumber = "",
                tips = "Room number not specified in timetable."
            )
        }

        // Special named facilities
        when {
            clean.startsWith("CC") || clean.contains("COMPUTER") -> {
                return RoomLocation(
                    rawRoom = clean,
                    buildingName = "Building 4 (Computer & IT)",
                    floorName = "Computer Center (Ground / 1st Floor)",
                    roomNumber = clean,
                    tips = "Head to Building 4 labs. Check individual lab door board."
                )
            }
            clean.startsWith("WS") || clean.contains("WORKSHOP") -> {
                return RoomLocation(
                    rawRoom = clean,
                    buildingName = "Building 2 (Mechanical)",
                    floorName = "Ground Floor (Workshop Bay)",
                    roomNumber = clean,
                    tips = "Located on the ground floor of Building 2 towards the rear."
                )
            }
            clean.startsWith("AUD") || clean.contains("ARENA") -> {
                return RoomLocation(
                    rawRoom = clean,
                    buildingName = "Building 4 / Sharad Arena",
                    floorName = "Auditorium Level",
                    roomNumber = clean,
                    tips = "Main auditorium inside Building 4 complex."
                )
            }
            clean.contains("LIB") -> {
                return RoomLocation(
                    rawRoom = clean,
                    buildingName = "Building 4 (Central Library)",
                    floorName = "1st & 2nd Floor",
                    roomNumber = clean,
                    tips = "Enter Building 4; library spans the first and second floors."
                )
            }
            clean.contains("DH") || clean.contains("DRAWING") -> {
                return RoomLocation(
                    rawRoom = clean,
                    buildingName = "Building 2",
                    floorName = "4th Floor (Drawing Hall)",
                    roomNumber = clean,
                    tips = "Take the Building 2 staircase to the top drawing halls."
                )
            }
        }

        // Standard 4-digit room code: e.g. 1203, 4108, 3302
        val digitMatch = Regex("""^[1-5][0-5][0-9]{2}$""").find(clean)
        if (digitMatch != null) {
            val bldgDigit = clean[0]
            val floorDigit = clean[1]
            val roomNum = clean.substring(2).trimStart('0')

            val bldgName = when (bldgDigit) {
                '1' -> "Building 1 (Main Admin / Core)"
                '2' -> "Building 2 (Mechanical / Production)"
                '3' -> "Building 3 (DESH / First Year)"
                '4' -> "Building 4 (Comp & IT / Library)"
                '5' -> "Building 5 (Instrumentation / Chem)"
                else -> "Building $bldgDigit"
            }

            val floorName = when (floorDigit) {
                '0' -> "Ground Floor"
                '1' -> "1st Floor"
                '2' -> "2nd Floor"
                '3' -> "3rd Floor"
                '4' -> "4th Floor"
                '5' -> "5th Floor"
                else -> "Floor $floorDigit"
            }

            val tip = when (bldgDigit) {
                '1' -> "Central building near the main gate. Central staircase and lift available."
                '2' -> "Mechanical building behind Building 1. Use the northern staircase."
                '3' -> "DESH building directly facing the open ground and cafeteria."
                '4' -> "Computer & IT building near the library. Lift available near lobby."
                else -> "Follow the building floor signage."
            }

            return RoomLocation(
                rawRoom = clean,
                buildingName = bldgName,
                floorName = floorName,
                roomNumber = "Room $roomNum",
                tips = tip
            )
        }

        // 3-digit room code fallback (e.g. 203 -> Floor 2, Room 3)
        val threeDigitMatch = Regex("""^[0-5][0-9]{2}$""").find(clean)
        if (threeDigitMatch != null) {
            val floorDigit = clean[0]
            val roomNum = clean.substring(1).trimStart('0')
            val floorName = when (floorDigit) {
                '0' -> "Ground Floor"
                '1' -> "1st Floor"
                '2' -> "2nd Floor"
                '3' -> "3rd Floor"
                '4' -> "4th Floor"
                else -> "Floor $floorDigit"
            }
            return RoomLocation(
                rawRoom = clean,
                buildingName = "Campus Building",
                floorName = floorName,
                roomNumber = "Room $roomNum",
                tips = "Check your department building directory."
            )
        }

        // Generic fallback
        return RoomLocation(
            rawRoom = clean,
            buildingName = "Room $clean",
            floorName = "Campus",
            roomNumber = clean,
            tips = "Check department notice boards or lab entrance doors."
        )
    }
}
