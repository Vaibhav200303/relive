package com.vaibhav.relive.data.exporting

import com.vaibhav.relive.domain.exporting.PortableArchiveEntry
import com.vaibhav.relive.domain.exporting.PortableArchiveManifest
import com.vaibhav.relive.domain.exporting.PortableArchiveSnapshot
import com.vaibhav.relive.domain.exporting.PortableTimelineIdentity
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

object PortableArchiveJson {
    fun encodeSnapshot(snapshot: PortableArchiveSnapshot): String {
        val tags = snapshot.moments.flatMap { it.tags }.distinctBy { it.canonical }
        val root = linkedMapOf<String, JsonValue>(
            "exportedAt" to number(snapshot.exportedAtEpochMilliseconds),
            "allAppearance" to appearance(snapshot.allTimelineAppearance),
            "exportedTimeline" to exportedTimeline(snapshot.exportedTimeline),
            "moments" to array(snapshot.moments.map(::moment)),
            "timelines" to array(snapshot.timelines.map(::timeline)),
            "memberships" to array(snapshot.memberships.flatMap { (momentId, timelineIds) ->
                timelineIds.sortedBy { it.value }.map { timelineId -> obj("momentId" to string(momentId.value), "timelineId" to string(timelineId.value)) }
            }),
            "tags" to array(tags.map { obj("canonical" to string(it.canonical), "label" to string(it.label)) }),
            "momentTags" to array(snapshot.moments.flatMap { moment ->
                moment.tags.map { tag -> obj("momentId" to string(moment.id.value), "tagCanonical" to string(tag.canonical)) }
            }),
        )
        return JsonValue.Object(root).encode()
    }

    fun decodeSnapshot(value: String): PortableArchiveSnapshot {
        val root = JsonParser(value).parse().objectValue()
        val tagEntries = root.array("tags")
        require(tagEntries.map { it.objectValue().string("canonical") }.distinct().size == tagEntries.size) { "Duplicate tag id." }
        val tagLabels = tagEntries.associate { item ->
            val tag = item.objectValue()
            tag.string("canonical") to tag.string("label")
        }
        require(tagLabels.all { (canonical, label) -> Tag.ofOrNull(label)?.canonical == canonical }) { "Archive tag identity is invalid." }
        val momentTagEntries = root.array("momentTags")
        val momentTags = momentTagEntries.groupBy(
            keySelector = { it.objectValue().string("momentId") },
            valueTransform = { it.objectValue().string("tagCanonical") },
        )
        val moments = root.array("moments").map { item ->
            val objectValue = item.objectValue()
            decodeMoment(
                objectValue,
                momentTags[objectValue.string("id")].orEmpty().mapNotNull { canonical ->
                    tagLabels[canonical]?.let(Tag::ofOrNull)
                },
            )
        }
        val momentIds = moments.mapTo(mutableSetOf()) { it.id.value }
        require(momentTagEntries.all { entry ->
            val link = entry.objectValue()
            link.string("momentId") in momentIds && link.string("tagCanonical") in tagLabels
        }) { "Archive tag relationship is invalid." }
        val timelines = root.array("timelines").map { decodeTimeline(it.objectValue()) }
        val memberships = root.array("memberships")
            .map { it.objectValue() }
            .groupBy(
                keySelector = { MomentId(it.string("momentId")) },
                valueTransform = { TimelineId(it.string("timelineId")) },
            ).mapValues { it.value.toSet() }
        return PortableArchiveSnapshot(
            exportedAtEpochMilliseconds = root.long("exportedAt"),
            moments = moments,
            timelines = timelines,
            memberships = memberships,
            allTimelineAppearance = root.objectOrNull("allAppearance")?.let(::decodeAppearance) ?: TimelineAppearance(),
            exportedTimeline = root.objectOrNull("exportedTimeline")?.let(::decodeExportedTimeline)
                ?: PortableTimelineIdentity(
                    appearance = root.objectOrNull("allAppearance")?.let(::decodeAppearance) ?: TimelineAppearance(),
                ),
        )
    }

    fun encodeManifest(manifest: PortableArchiveManifest): String = obj(
        "format" to string(manifest.format),
        "formatVersion" to number(manifest.formatVersion.toLong()),
        "appVersion" to string(manifest.appVersion),
        "exportedAt" to number(manifest.exportedAtEpochMilliseconds),
        "momentCount" to number(manifest.momentCount.toLong()),
        "timelineCount" to number(manifest.timelineCount.toLong()),
        "earliestMoment" to nullableNumber(manifest.earliestMomentEpochMilliseconds),
        "latestMoment" to nullableNumber(manifest.latestMomentEpochMilliseconds),
        "archiveSha256" to string(manifest.archiveSha256),
        "media" to array(manifest.media.map { entry ->
            obj(
                "storageRef" to string(entry.storageRef),
                "path" to string(entry.archivePath),
                "bytes" to number(entry.byteCount),
                "sha256" to string(entry.sha256),
                "mediaType" to string(entry.mediaType),
            )
        }),
    ).encode()

    fun decodeManifest(value: String): PortableArchiveManifest {
        val root = JsonParser(value).parse().objectValue()
        return PortableArchiveManifest(
            format = root.string("format"),
            formatVersion = root.long("formatVersion").toInt(),
            appVersion = root.nullableString("appVersion").orEmpty(),
            exportedAtEpochMilliseconds = root.long("exportedAt"),
            momentCount = root.long("momentCount").toInt(),
            timelineCount = root.long("timelineCount").toInt(),
            earliestMomentEpochMilliseconds = root.nullableLong("earliestMoment"),
            latestMomentEpochMilliseconds = root.nullableLong("latestMoment"),
            archiveSha256 = root.string("archiveSha256"),
            media = root.array("media").map { item ->
                val entry = item.objectValue()
                PortableArchiveEntry(
                    storageRef = entry.string("storageRef"),
                    archivePath = entry.string("path"),
                    byteCount = entry.long("bytes"),
                    sha256 = entry.string("sha256"),
                    mediaType = entry.string("mediaType"),
                )
            },
        )
    }

    private fun moment(value: Moment): JsonValue = obj(
        "id" to string(value.id.value),
        "createdAt" to number(value.createdAt.epochMilliseconds),
        "updatedAt" to nullableNumber(value.updatedAt?.epochMilliseconds),
        "title" to string(value.title),
        "content" to string(value.content),
        "favorite" to bool(value.isFavorite),
        "feeling" to nullableString(value.feeling?.name),
        "location" to (value.location?.let(::location) ?: JsonValue.Null),
        "attachments" to array(value.attachments.sortedBy { it.sortIndex }.map(::attachment)),
    )

    private fun decodeMoment(value: Map<String, JsonValue>, tags: List<Tag>): Moment = Moment(
        id = MomentId(value.string("id")),
        createdAt = Instant(value.long("createdAt")),
        updatedAt = value.nullableLong("updatedAt")?.let(::Instant),
        title = value.string("title"),
        content = value.string("content"),
        isFavorite = value.boolean("favorite"),
        feeling = value.nullableString("feeling")?.let { MomentFeeling.valueOf(it) },
        location = value.objectOrNull("location")?.let(::decodeLocation),
        tags = tags,
        attachments = value.array("attachments").map { decodeAttachment(it.objectValue()) },
    )

    private fun timeline(value: Timeline.Custom): JsonValue = obj(
        "id" to string(value.id.value),
        "name" to string(value.name),
        "appearance" to appearance(value.appearance),
        "coverPhotoRef" to nullableString(value.coverPhotoRef?.value),
    )

    private fun decodeTimeline(value: Map<String, JsonValue>): Timeline.Custom = Timeline.Custom(
        id = TimelineId(value.string("id")),
        name = value.string("name"),
        appearance = value.objectOrNull("appearance")?.let(::decodeAppearance) ?: TimelineAppearance(),
        coverPhotoRef = value.nullableString("coverPhotoRef")?.let(::MediaStorageRef),
    )

    private fun exportedTimeline(value: PortableTimelineIdentity): JsonValue = obj(
        "name" to string(value.name),
        "customTimelineId" to nullableString(value.customTimelineId?.value),
        "appearance" to appearance(value.appearance),
        "coverPhotoRef" to nullableString(value.coverPhotoRef?.value),
    )

    private fun decodeExportedTimeline(value: Map<String, JsonValue>) = PortableTimelineIdentity(
        name = value.string("name"),
        customTimelineId = value.nullableString("customTimelineId")?.let(::TimelineId),
        appearance = value.objectOrNull("appearance")?.let(::decodeAppearance) ?: TimelineAppearance(),
        coverPhotoRef = value.nullableString("coverPhotoRef")?.let(::MediaStorageRef),
    )

    private fun appearance(value: TimelineAppearance): JsonValue = obj(
        "wallpaper" to string(value.wallpaper.name),
        "momentTheme" to string(value.momentTheme.name),
    )

    private fun decodeAppearance(value: Map<String, JsonValue>) = TimelineAppearance(
        wallpaper = TimelineWallpaper.valueOf(value.string("wallpaper")),
        momentTheme = MomentTheme.valueOf(value.string("momentTheme")),
    )

    private fun location(value: ReliveLocation): JsonValue = obj(
        "latitude" to nullableNumber(value.latitude),
        "longitude" to nullableNumber(value.longitude),
        "placeName" to nullableString(value.placeName),
        "locality" to nullableString(value.locality),
        "region" to nullableString(value.region),
        "country" to nullableString(value.country),
    )

    private fun decodeLocation(value: Map<String, JsonValue>) = ReliveLocation(
        latitude = value.nullableDouble("latitude"),
        longitude = value.nullableDouble("longitude"),
        placeName = value.nullableString("placeName"),
        locality = value.nullableString("locality"),
        region = value.nullableString("region"),
        country = value.nullableString("country"),
    )

    private fun attachment(value: MediaAttachment): JsonValue = obj(
        "id" to string(value.id.value),
        "mediaType" to string(value.type.name),
        "storageRef" to string(value.storageRef.value),
        "sortIndex" to number(value.sortIndex.toLong()),
    )

    private fun decodeAttachment(value: Map<String, JsonValue>) = MediaAttachment(
        id = MediaAttachmentId(value.string("id")),
        type = MediaType.valueOf(value.string("mediaType")),
        storageRef = MediaStorageRef(value.string("storageRef")),
        sortIndex = value.long("sortIndex").toInt(),
    )
}

private sealed interface JsonValue {
    data class Object(val values: Map<String, JsonValue>) : JsonValue
    data class Array(val values: List<JsonValue>) : JsonValue
    data class StringValue(val value: String) : JsonValue
    data class NumberValue(val value: String) : JsonValue
    data class BooleanValue(val value: Boolean) : JsonValue
    data object Null : JsonValue

    fun encode(): String = when (this) {
        is Object -> values.entries.joinToString(prefix = "{", postfix = "}") { (key, value) -> "${StringValue(key).encode()}:${value.encode()}" }
        is Array -> values.joinToString(prefix = "[", postfix = "]") { it.encode() }
        is StringValue -> buildString {
            append('"')
            value.forEach { character ->
                append(
                    when (character) {
                        '"' -> "\\\""
                        '\\' -> "\\\\"
                        '\b' -> "\\b"
                        '\u000C' -> "\\f"
                        '\n' -> "\\n"
                        '\r' -> "\\r"
                        '\t' -> "\\t"
                        else -> if (character.code < 0x20) "\\u${character.code.toString(16).padStart(4, '0')}" else character
                    },
                )
            }
            append('"')
        }
        is NumberValue -> value
        is BooleanValue -> value.toString()
        Null -> "null"
    }
}

private class JsonParser(private val source: String) {
    private var index = 0
    fun parse(): JsonValue {
        val value = readValue()
        whitespace()
        require(index == source.length) { "Unexpected JSON content" }
        return value
    }
    private fun readValue(): JsonValue {
        whitespace()
        require(index < source.length) { "Unexpected end of JSON" }
        return when (source[index]) {
            '{' -> readObject()
            '[' -> readArray()
            '"' -> JsonValue.StringValue(readString())
            't' -> literal("true", JsonValue.BooleanValue(true))
            'f' -> literal("false", JsonValue.BooleanValue(false))
            'n' -> literal("null", JsonValue.Null)
            else -> readNumber()
        }
    }
    private fun readObject(): JsonValue.Object {
        index++
        whitespace()
        val values = linkedMapOf<String, JsonValue>()
        if (take('}')) return JsonValue.Object(values)
        while (true) {
            val key = readString()
            whitespace(); require(take(':')) { "Expected ':'" }
            require(values.put(key, readValue()) == null) { "Duplicate JSON key: $key" }
            whitespace()
            if (take('}')) break
            require(take(',')) { "Expected ','" }
            whitespace()
        }
        return JsonValue.Object(values)
    }
    private fun readArray(): JsonValue.Array {
        index++
        whitespace()
        val values = mutableListOf<JsonValue>()
        if (take(']')) return JsonValue.Array(values)
        while (true) {
            values += readValue()
            whitespace()
            if (take(']')) break
            require(take(',')) { "Expected ','" }
        }
        return JsonValue.Array(values)
    }
    private fun readString(): String {
        require(take('"')) { "Expected string" }
        return buildString {
            while (index < source.length) {
                val character = source[index++]
                when (character) {
                    '"' -> return@buildString
                    '\\' -> {
                        require(index < source.length) { "Invalid escape" }
                        append(
                            when (val escaped = source[index++]) {
                                '"', '\\', '/' -> escaped
                                'b' -> '\b'
                                'f' -> '\u000C'
                                'n' -> '\n'
                                'r' -> '\r'
                                't' -> '\t'
                                'u' -> source.substring(index, (index + 4).coerceAtMost(source.length)).also {
                                    require(it.length == 4) { "Invalid unicode escape" }; index += 4
                                }.toInt(16).toChar()
                                else -> error("Invalid escape")
                            },
                        )
                    }
                    else -> append(character)
                }
            }
        }
    }
    private fun readNumber(): JsonValue.NumberValue {
        val start = index
        while (index < source.length && source[index] in "-+0123456789.eE") index++
        require(index > start) { "Expected JSON value" }
        return JsonValue.NumberValue(source.substring(start, index).also { it.toDouble() })
    }
    private fun <T : JsonValue> literal(text: String, value: T): T {
        require(source.startsWith(text, index)) { "Invalid JSON literal" }
        index += text.length
        return value
    }
    private fun whitespace() { while (index < source.length && source[index].isWhitespace()) index++ }
    private fun take(character: Char): Boolean = if (index < source.length && source[index] == character) { index++; true } else false
}

private fun JsonValue.objectValue(): Map<String, JsonValue> = (this as? JsonValue.Object)?.values ?: error("Expected object")
private fun Map<String, JsonValue>.string(name: String) = (get(name) as? JsonValue.StringValue)?.value ?: error("Missing string $name")
private fun Map<String, JsonValue>.nullableString(name: String) = when (val value = get(name)) { null, JsonValue.Null -> null; is JsonValue.StringValue -> value.value; else -> error("Invalid string $name") }
private fun Map<String, JsonValue>.long(name: String) = (get(name) as? JsonValue.NumberValue)?.value?.toLong() ?: error("Missing number $name")
private fun Map<String, JsonValue>.nullableLong(name: String) = when (val value = get(name)) { null, JsonValue.Null -> null; is JsonValue.NumberValue -> value.value.toLong(); else -> error("Invalid number $name") }
private fun Map<String, JsonValue>.nullableDouble(name: String) = when (val value = get(name)) { null, JsonValue.Null -> null; is JsonValue.NumberValue -> value.value.toDouble(); else -> error("Invalid number $name") }
private fun Map<String, JsonValue>.boolean(name: String) = (get(name) as? JsonValue.BooleanValue)?.value ?: error("Missing boolean $name")
private fun Map<String, JsonValue>.array(name: String) = (get(name) as? JsonValue.Array)?.values ?: error("Missing array $name")
private fun Map<String, JsonValue>.objectOrNull(name: String) = when (val value = get(name)) { null, JsonValue.Null -> null; is JsonValue.Object -> value.values; else -> error("Invalid object $name") }
private fun obj(vararg values: Pair<String, JsonValue>) = JsonValue.Object(linkedMapOf(*values))
private fun array(values: List<JsonValue>) = JsonValue.Array(values)
private fun string(value: String) = JsonValue.StringValue(value)
private fun nullableString(value: String?) = value?.let(::string) ?: JsonValue.Null
private fun number(value: Long) = JsonValue.NumberValue(value.toString())
private fun nullableNumber(value: Long?) = value?.let(::number) ?: JsonValue.Null
private fun nullableNumber(value: Double?) = value?.let { JsonValue.NumberValue(it.toString()) } ?: JsonValue.Null
private fun bool(value: Boolean) = JsonValue.BooleanValue(value)
