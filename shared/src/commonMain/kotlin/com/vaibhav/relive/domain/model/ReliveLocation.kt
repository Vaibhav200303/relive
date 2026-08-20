package com.vaibhav.relive.domain.model

/**
 * Optional geographic context for a moment. All fields are optional; presence of the
 * [ReliveLocation] instance itself signals "location known to any degree".
 *
 * Coordinates, when present, must be valid geographic values. Raw coordinates are never
 * surfaced in the timeline UI; a readable [placeName]/[locality]/[region]/[country] is
 * preferred for display (see PRODUCT_SPEC §7.1).
 */
data class ReliveLocation(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val placeName: String? = null,
    val locality: String? = null,
    val region: String? = null,
    val country: String? = null,
) {
    init {
        latitude?.let { require(it in -90.0..90.0) { "latitude out of range" } }
        longitude?.let { require(it in -180.0..180.0) { "longitude out of range" } }
        require((latitude == null) == (longitude == null)) {
            "latitude and longitude must both be present or both absent"
        }
    }

    val hasCoordinates: Boolean get() = latitude != null && longitude != null
    val hasReadableParts: Boolean get() =
        !placeName.isNullOrBlank() || !locality.isNullOrBlank() ||
            !region.isNullOrBlank() || !country.isNullOrBlank()
}
