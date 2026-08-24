package com.vaibhav.relive.presentation.cardcover

import com.vaibhav.relive.domain.model.MediaAttachment
import com.vaibhav.relive.domain.model.MediaType
import com.vaibhav.relive.domain.time.Instant

const val ALL_TIMELINE_COLLAGE_MAX_ITEMS: Int = 9
const val ALL_TIMELINE_COLLAGE_BUCKET_MILLIS: Long = 3L * 60L * 60L * 1_000L

data class AllTimelineCollageSelection(
    val attachments: List<MediaAttachment>,
    val layout: AllTimelineCollageLayout?,
)

data class AllTimelineCollageCell(
    val column: Int,
    val row: Int,
    val columnSpan: Int = 1,
    val rowSpan: Int = 1,
)

data class AllTimelineCollageLayout(
    val columns: Int,
    val rows: Int,
    val cells: List<AllTimelineCollageCell>,
) {
    init {
        require(columns > 0 && rows > 0)
        require(cells.isNotEmpty())
        cells.forEach { cell ->
            require(cell.column >= 0 && cell.row >= 0)
            require(cell.columnSpan > 0 && cell.rowSpan > 0)
            require(cell.column + cell.columnSpan <= columns)
            require(cell.row + cell.rowSpan <= rows)
        }
    }
}

fun allTimelineCollageBucket(now: Instant): Long =
    now.epochMilliseconds / ALL_TIMELINE_COLLAGE_BUCKET_MILLIS

fun resolveAllTimelineCollage(
    available: List<MediaAttachment>,
    bucket: Long,
    stableKey: String = "timeline-all",
): AllTimelineCollageSelection {
    val visual = available
        .asSequence()
        .filter { it.type == MediaType.Image || it.type == MediaType.Video }
        .distinctBy { it.id }
        .toList()
    if (visual.isEmpty()) return AllTimelineCollageSelection(emptyList(), null)

    val maximum = minOf(ALL_TIMELINE_COLLAGE_MAX_ITEMS, visual.size)
    val count = 1 + stableCollageIndex("$stableKey|$bucket|count", maximum)
    val selected = visual
        .sortedWith(
            compareBy<MediaAttachment> {
                stableCollageHash("$stableKey|$bucket|media|${it.id.value}")
            }.thenBy { it.id.value },
        )
        .take(count)
    val layoutSeed = stableCollageHash("$stableKey|$bucket|layout").toLong()
    return AllTimelineCollageSelection(
        attachments = selected,
        layout = resolveAllTimelineCollageLayout(selected.size, layoutSeed),
    )
}

fun resolveAllTimelineCollageLayout(itemCount: Int, seed: Long): AllTimelineCollageLayout {
    require(itemCount in 1..ALL_TIMELINE_COLLAGE_MAX_ITEMS)
    val alternate = seed.toUInt() % 2u == 1u
    return when (itemCount) {
        1 -> layout(1, 1, cell(0, 0))
        2 -> if (alternate) {
            layout(1, 2, cell(0, 0), cell(0, 1))
        } else {
            layout(2, 1, cell(0, 0), cell(1, 0))
        }
        3 -> if (alternate) {
            layout(2, 2, cell(1, 0, rowSpan = 2), cell(0, 0), cell(0, 1))
        } else {
            layout(2, 2, cell(0, 0, rowSpan = 2), cell(1, 0), cell(1, 1))
        }
        4 -> layout(2, 2, cell(0, 0), cell(1, 0), cell(0, 1), cell(1, 1))
        5 -> if (alternate) {
            layout(
                3,
                2,
                cell(2, 0, rowSpan = 2),
                cell(0, 0),
                cell(1, 0),
                cell(0, 1),
                cell(1, 1),
            )
        } else {
            layout(
                3,
                2,
                cell(0, 0, rowSpan = 2),
                cell(1, 0),
                cell(2, 0),
                cell(1, 1),
                cell(2, 1),
            )
        }
        6 -> if (alternate) mixedSix().mirrored() else mixedSix()
        7 -> if (alternate) rowBands(2, 2, 3) else rowBands(3, 2, 2)
        8 -> if (alternate) rowBands(3, 3, 2) else rowBands(2, 3, 3)
        else -> layout(
            3,
            3,
            *List(9) { index -> cell(index % 3, index / 3) }.toTypedArray(),
        )
    }
}

private fun mixedSix(): AllTimelineCollageLayout = layout(
    4,
    3,
    cell(0, 0, columnSpan = 2, rowSpan = 2),
    cell(2, 0, columnSpan = 2),
    cell(2, 1),
    cell(3, 1),
    cell(0, 2, columnSpan = 2),
    cell(2, 2, columnSpan = 2),
)

private fun rowBands(vararg counts: Int): AllTimelineCollageLayout {
    val columns = 6
    val cells = counts.flatMapIndexed { row, count ->
        val span = columns / count
        List(count) { column -> cell(column * span, row, columnSpan = span) }
    }
    return AllTimelineCollageLayout(columns = columns, rows = counts.size, cells = cells)
}

private fun AllTimelineCollageLayout.mirrored(): AllTimelineCollageLayout = copy(
    cells = cells.map { it.copy(column = columns - it.column - it.columnSpan) },
)

private fun layout(
    columns: Int,
    rows: Int,
    vararg cells: AllTimelineCollageCell,
): AllTimelineCollageLayout = AllTimelineCollageLayout(columns, rows, cells.toList())

private fun cell(
    column: Int,
    row: Int,
    columnSpan: Int = 1,
    rowSpan: Int = 1,
): AllTimelineCollageCell = AllTimelineCollageCell(column, row, columnSpan, rowSpan)

private fun stableCollageIndex(value: String, size: Int): Int =
    (stableCollageHash(value) % size.toUInt()).toInt()

private fun stableCollageHash(value: String): UInt {
    var hash = 2_166_136_261u
    value.forEach { character ->
        hash = (hash xor character.code.toUInt()) * 16_777_619u
    }
    return hash
}
