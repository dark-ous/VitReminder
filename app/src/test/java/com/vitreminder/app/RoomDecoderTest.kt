package com.vitreminder.app

import com.vitreminder.app.util.RoomDecoder
import org.junit.Assert.*
import org.junit.Test

class RoomDecoderTest {

    @Test
    fun testDecode_standardFourDigitRooms() {
        val r1203 = RoomDecoder.decode("1203")
        assertEquals("Building 1 (Main Admin / Core)", r1203.buildingName)
        assertEquals("2nd Floor", r1203.floorName)
        assertEquals("Room 3", r1203.roomNumber)
        assertTrue(r1203.tips.contains("Central building"))

        val r4108 = RoomDecoder.decode("4108")
        assertEquals("Building 4 (Comp & IT / Library)", r4108.buildingName)
        assertEquals("1st Floor", r4108.floorName)
        assertEquals("Room 8", r4108.roomNumber)

        val r3302 = RoomDecoder.decode("3302")
        assertEquals("Building 3 (DESH / First Year)", r3302.buildingName)
        assertEquals("3rd Floor", r3302.floorName)
        assertEquals("Room 2", r3302.roomNumber)

        val r1005 = RoomDecoder.decode("1005")
        assertEquals("Building 1 (Main Admin / Core)", r1005.buildingName)
        assertEquals("Ground Floor", r1005.floorName)
        assertEquals("Room 5", r1005.roomNumber)
    }

    @Test
    fun testDecode_specialFacilities() {
        val cc = RoomDecoder.decode("CC-3")
        assertTrue(cc.buildingName.contains("Building 4"))
        assertTrue(cc.floorName.contains("Computer Center"))

        val ws = RoomDecoder.decode("WORKSHOP")
        assertTrue(ws.buildingName.contains("Building 2"))
        assertTrue(ws.floorName.contains("Workshop"))
    }
}
