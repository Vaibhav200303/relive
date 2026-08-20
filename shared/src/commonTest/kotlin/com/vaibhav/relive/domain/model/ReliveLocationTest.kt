package com.vaibhav.relive.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReliveLocationTest {

    @Test
    fun all_fields_null_is_allowed() {
        val loc = ReliveLocation()
        assertFalse(loc.hasCoordinates)
        assertFalse(loc.hasReadableParts)
    }

    @Test
    fun readable_parts_only_no_coordinates() {
        val loc = ReliveLocation(
            placeName = "NIT Jalandhar",
            region = "Punjab",
            country = "India",
        )
        assertFalse(loc.hasCoordinates)
        assertTrue(loc.hasReadableParts)
    }

    @Test
    fun coordinates_only_no_readable_parts() {
        val loc = ReliveLocation(latitude = 31.396, longitude = 75.535)
        assertTrue(loc.hasCoordinates)
        assertFalse(loc.hasReadableParts)
    }

    @Test
    fun latitude_only_rejected() {
        assertFails { ReliveLocation(latitude = 10.0) }
    }

    @Test
    fun longitude_only_rejected() {
        assertFails { ReliveLocation(longitude = 10.0) }
    }

    @Test
    fun latitude_out_of_range_rejected() {
        assertFails { ReliveLocation(latitude = 91.0, longitude = 0.0) }
        assertFails { ReliveLocation(latitude = -90.5, longitude = 0.0) }
    }

    @Test
    fun longitude_out_of_range_rejected() {
        assertFails { ReliveLocation(latitude = 0.0, longitude = 181.0) }
    }

    @Test
    fun partial_readable_fields_supported() {
        val loc = ReliveLocation(locality = "Amritsar")
        assertEquals("Amritsar", loc.locality)
        assertTrue(loc.hasReadableParts)
    }
}
