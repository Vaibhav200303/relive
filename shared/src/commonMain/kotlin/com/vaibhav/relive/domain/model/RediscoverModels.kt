package com.vaibhav.relive.domain.model

import com.vaibhav.relive.domain.time.Instant

/** The SQL-backed read model for a Rediscover system collection card. */
data class FavoritesCollectionSummary(
    val momentCount: Long,
    val previewAttachments: List<MediaAttachment>,
) {
    init {
        require(momentCount >= 0) { "momentCount must not be negative" }
        require(previewAttachments.size <= MAX_PREVIEW_ATTACHMENTS) {
            "Favorites previews are bounded to $MAX_PREVIEW_ATTACHMENTS attachments"
        }
        require(previewAttachments.all { it.type == MediaType.Image || it.type == MediaType.Video }) {
            "Favorites previews include visual media only"
        }
    }

    companion object {
        const val MAX_PREVIEW_ATTACHMENTS = 4
    }
}

/** A bounded Rediscover shelf projection for one favorited Moment. */
data class FavoriteMomentPreview(
    val id: MomentId,
    val createdAt: Instant,
    val title: String,
    val content: String,
    val attachments: List<MediaAttachment>,
)

/** A bounded featured-memory projection for the active On This Day shelf. */
data class OnThisDayMomentPreview(
    val id: MomentId,
    val createdAt: Instant,
    val localYear: Int,
    val title: String,
    val content: String,
    val attachments: List<MediaAttachment>,
)

/** A bounded daily-resurfacing projection for a Moment from the older archive. */
data class FromYourPastMomentPreview(
    val id: MomentId,
    val createdAt: Instant,
    val title: String,
    val content: String,
    val attachments: List<MediaAttachment>,
)

/** A device-local Gregorian calendar date used only to form Rediscover reads. */
data class LocalCalendarDate(
    val year: Int,
    val month: Int,
    val day: Int,
)

/** A bounded, persistence-backed memory projection for Rediscover surfaces. */
data class RediscoveredMoment(
    val id: MomentId,
    val createdAt: Instant,
    val localYear: Int,
    val title: String,
    val content: String,
    val attachments: List<MediaAttachment>,
    val tags: List<Tag>,
)

data class RediscoverPlaceSummary(
    val key: String,
    val label: String,
    val momentCount: Long,
)

data class RediscoverTagSummary(
    val canonical: String,
    val momentCount: Long,
)

data class RediscoverQuery(
    val today: LocalCalendarDate,
    val startOfToday: Instant,
    val recentCutoff: Instant,
    val dailySeed: Long,
)

data class RediscoverOverview(
    val totalMomentCount: Long,
    val onThisDay: List<RediscoveredMoment>,
    val fromYourPast: List<RediscoveredMoment>,
    val places: List<RediscoverPlaceSummary>,
    val tags: List<RediscoverTagSummary>,
)
