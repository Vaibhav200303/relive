package com.vaibhav.relive.presentation.timeline

import com.vaibhav.relive.domain.model.Moment
import com.vaibhav.relive.domain.model.MomentId
import com.vaibhav.relive.domain.model.ReliveLocation
import com.vaibhav.relive.domain.time.Instant
import java.util.TimeZone
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MomentPresentationMapperTest {

    private var previousDefault: TimeZone? = null

    @BeforeTest
    fun captureDefault() {
        previousDefault = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @AfterTest
    fun restoreDefault() {
        previousDefault?.let { TimeZone.setDefault(it) }
    }

    @Test
    fun mapsMomentToPresentationWithFormattedDateAndTime() {
        val moment = Moment(
            id = MomentId("m-1"),
            createdAt = Instant(1_695_913_200_000L),
            title = "Quiet morning light",
            content = "Peaceful moments before the day begins.",
            isFavorite = true,
        )
        val presentation = moment.toPresentation()
        assertEquals("SEPTEMBER 28, 2023", presentation.formattedDate)
        assertEquals("3:00 PM", presentation.formattedTime)
        assertEquals("Quiet morning light", presentation.title)
        assertTrue(presentation.hasTitle)
        assertTrue(presentation.hasContent)
        assertTrue(presentation.isFavorite)
        assertNull(presentation.locationLabel)
    }

    @Test
    fun sameCreatedAtDrivesBothFormattedDateAndTime() {
        val createdAt = Instant(1_695_913_200_000L)
        val moment = Moment(id = MomentId("m-x"), createdAt = createdAt, content = "n")
        val presentation = moment.toPresentation()
        assertEquals(
            com.vaibhav.relive.presentation.date.EditorialDateFormatter.format(createdAt),
            presentation.formattedDate,
        )
        assertEquals(
            com.vaibhav.relive.presentation.date.EditorialTimeFormatter.format(createdAt),
            presentation.formattedTime,
        )
    }

    @Test
    fun preservesReadableLocationLabel() {
        val moment = Moment(
            id = MomentId("m-2"),
            createdAt = Instant(1_695_913_200_000L),
            location = ReliveLocation(placeName = "Livraria Lello", locality = "Porto"),
            content = "note",
        )
        val presentation = moment.toPresentation()

        assertEquals("Livraria Lello", presentation.locationLabel)
        assertEquals(moment.location, presentation.location)
    }

    @Test
    fun blankPersistedLocationMapsToNoMetadataLabel() {
        val moment = Moment(
            id = MomentId("m-blank-location"),
            createdAt = Instant(1_695_913_200_000L),
            location = ReliveLocation(placeName = "   ", locality = "\t"),
            content = "note",
        )

        assertNull(moment.toPresentation().locationLabel)
    }

    @Test
    fun persistedLocationIsTrimmedAndOnlyItsFirstCharacterIsCapitalizedForDisplay() {
        val location = ReliveLocation(placeName = "  pune  ")
        val moment = Moment(
            id = MomentId("m-lowercase-location"),
            createdAt = Instant(1_695_913_200_000L),
            location = location,
            content = "note",
        )
        val presentation = moment.toPresentation()

        assertEquals("Pune", presentation.locationLabel)
        assertEquals(location, presentation.location)
    }

    @Test
    fun locationWithOnlyCoordinatesHasNoReadableLabel() {
        val moment = Moment(
            id = MomentId("m-3"),
            createdAt = Instant(1_695_913_200_000L),
            location = ReliveLocation(latitude = 41.0, longitude = -8.0),
            content = "note",
        )
        assertNull(moment.toPresentation().locationLabel)
    }
}
