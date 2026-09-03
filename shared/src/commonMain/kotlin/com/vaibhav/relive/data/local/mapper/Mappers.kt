package com.vaibhav.relive.data.local.mapper

import com.vaibhav.relive.data.PersistenceMappingException
import com.vaibhav.relive.data.local.db.Custom_timelines
import com.vaibhav.relive.data.local.db.Media_attachments
import com.vaibhav.relive.data.local.db.Moments
import com.vaibhav.relive.domain.model.MediaAttachment
import com.vaibhav.relive.domain.model.MediaAttachmentId
import com.vaibhav.relive.domain.model.MediaStorageRef
import com.vaibhav.relive.domain.model.MediaType
import com.vaibhav.relive.domain.model.Moment
import com.vaibhav.relive.domain.model.MomentFeeling
import com.vaibhav.relive.domain.model.MomentId
import com.vaibhav.relive.domain.model.MomentTheme
import com.vaibhav.relive.domain.model.ReliveLocation
import com.vaibhav.relive.domain.model.Tag
import com.vaibhav.relive.domain.model.Timeline
import com.vaibhav.relive.domain.model.TimelineAppearance
import com.vaibhav.relive.domain.model.TimelineId
import com.vaibhav.relive.domain.model.TimelineWallpaper
import com.vaibhav.relive.domain.time.Instant

/**
 * Row ↔ domain mappers. The domain types own their invariants; mappers only shepherd
 * primitive values across the boundary and translate persistence-shaped nulls back
 * into optional domain fields. Any structurally invalid row surfaces as a
 * [PersistenceMappingException] rather than degrading to a fake default.
 */
internal fun Moments.toDomain(tags: List<Tag>, attachments: List<MediaAttachment>): Moment =
    try {
        Moment(
            id = MomentId(id),
            createdAt = Instant(created_at),
            updatedAt = updated_at?.let { Instant(it) },
            title = title,
            content = content,
            isFavorite = is_favorite != 0L,
            feeling = feeling?.let(::decodeFeeling),
            location = decodeLocation(
                lat = location_lat,
                lon = location_lon,
                displayName = location_display_name,
                locality = location_locality,
                region = location_region,
                country = location_country,
            ),
            tags = tags,
            attachments = attachments,
        )
    } catch (e: IllegalArgumentException) {
        throw PersistenceMappingException("Corrupt moment row id=$id: ${e.message}", e)
    }

private fun decodeLocation(
    lat: Double?,
    lon: Double?,
    displayName: String?,
    locality: String?,
    region: String?,
    country: String?,
): ReliveLocation? {
    val hasCoords = lat != null || lon != null
    val hasReadable = !displayName.isNullOrBlank() ||
        !locality.isNullOrBlank() ||
        !region.isNullOrBlank() ||
        !country.isNullOrBlank()
    if (!hasCoords && !hasReadable) return null
    return ReliveLocation(
        latitude = lat,
        longitude = lon,
        placeName = displayName,
        locality = locality,
        region = region,
        country = country,
    )
}

internal fun Media_attachments.toDomain(): MediaAttachment =
    try {
        MediaAttachment(
            id = MediaAttachmentId(id),
            type = decodeMediaType(media_type),
            storageRef = MediaStorageRef(storage_ref),
            sortIndex = sort_index.toInt(),
        )
    } catch (e: IllegalArgumentException) {
        throw PersistenceMappingException("Corrupt media_attachments row id=$id: ${e.message}", e)
    }

private fun decodeMediaType(raw: String): MediaType =
    MediaType.entries.firstOrNull { it.name == raw }
        ?: throw PersistenceMappingException("Unknown media_type='$raw'")

internal fun Custom_timelines.toDomain(): Timeline.Custom =
    try {
        Timeline.Custom(
            id = TimelineId(id),
            name = name,
            appearance = TimelineAppearance(
                wallpaper = decodeTimelineWallpaper(wallpaper),
                momentTheme = decodeMomentTheme(moment_theme),
            ),
            coverPhotoRef = cover_photo_ref?.let(::MediaStorageRef),
        )
    } catch (e: IllegalArgumentException) {
        throw PersistenceMappingException("Corrupt custom_timelines row id=$id: ${e.message}", e)
    }

internal fun decodeTimelineWallpaper(raw: String): TimelineWallpaper =
    TimelineWallpaper.entries.firstOrNull { it.name == raw }
        ?: throw PersistenceMappingException("Unknown timeline wallpaper='$raw'")

internal fun decodeMomentTheme(raw: String): MomentTheme =
    MomentTheme.entries.firstOrNull { it.name == raw }
        ?: when (raw) {
            // Values persisted during Part 1 remain readable after the placeholder
            // names were aligned with the Timeline theme screen.
            "Evergreen" -> MomentTheme.Sage
            "LilacDusk" -> MomentTheme.Lavender
            "CrimsonKeepsake" -> MomentTheme.Rose
            "BlueHour" -> MomentTheme.Ocean
            "Rosewood" -> MomentTheme.Monochrome
            else -> null
        }
        ?: throw PersistenceMappingException("Unknown moment theme='$raw'")

internal fun decodeTag(canonicalRow: String, label: String): Tag {
    val restored = Tag.ofOrNull(label)
        ?: throw PersistenceMappingException("Corrupt tag row canonical=$canonicalRow label=$label")
    return restored
}

/** Encoders (domain → row scalars) live here so both write and read use the same shape. */

internal fun encodeTimelineWallpaper(wallpaper: TimelineWallpaper): String = wallpaper.name

internal fun encodeMomentTheme(theme: MomentTheme): String = theme.name

internal fun encodeMediaTypeName(type: MediaType): String = type.name

internal fun encodeFavorite(isFavorite: Boolean): Long = if (isFavorite) 1L else 0L

internal fun encodeFeeling(feeling: MomentFeeling?): String? = feeling?.name

internal fun decodeFeeling(raw: String): MomentFeeling =
    MomentFeeling.entries.firstOrNull { it.name == raw }
        ?: throw PersistenceMappingException("Unknown feeling='$raw'")
