package com.vaibhav.relive.presentation.exporting

import com.vaibhav.relive.domain.exporting.DiaryPaper
import com.vaibhav.relive.domain.model.LocalCalendarDate
import com.vaibhav.relive.domain.model.MediaType
import com.vaibhav.relive.domain.model.Moment
import com.vaibhav.relive.domain.model.MomentFeeling
import com.vaibhav.relive.platform.exporting.MagazineDocument
import com.vaibhav.relive.presentation.date.EditorialTimeFormatter
import com.vaibhav.relive.presentation.date.RediscoverCalendar

/** Offline print diary. Every user value and local path is escaped before interpolation. */
object MagazineDocumentBuilder {
    fun html(document: MagazineDocument): String {
        val options = document.options
        val palette = options.paper.cssPalette()
        val coverClass = if (options.coverPhotoPath == null) "cover cover-paper" else "cover cover-photo"
        val coverStyle = options.coverPhotoPath?.let { " style=\"background-image:url('${fileUrl(it)}')\"" }.orEmpty()
        val days = document.moments
            .sortedBy { it.createdAt.epochMilliseconds }
            .groupBy { RediscoverCalendar.localDate(it.createdAt) }
            .entries
            .toList()
        val diaryPages = buildString {
            days.forEachIndexed { index, (date, moments) ->
                if (index > 0) append(gapPage(days[index - 1].key, date))
                append(dayPage(date, moments, document))
            }
        }
        return """
        <!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width">
        <style>
        ${document.fontCss}
        @page { size:A4 portrait; margin:14mm 16mm 17mm; }
        *{box-sizing:border-box}html,body{margin:0;padding:0;background:var(--paper);color:var(--ink);font-family:Kalam,Inter,Arial,sans-serif}body{font-size:14pt;line-height:1.62;background-image:repeating-linear-gradient(0deg,var(--ruling) 0,var(--ruling) .22mm,transparent .22mm,transparent 8mm)}
        .cover{height:297mm;margin:-14mm -16mm -17mm;page-break-after:always;display:flex;flex-direction:column;justify-content:flex-end;padding:25mm 21mm;background-size:cover;background-position:center;position:relative;overflow:hidden}.cover:after{content:"";position:absolute;inset:0;background:linear-gradient(180deg,rgba(35,27,22,.03),rgba(35,27,22,.76))}.cover-paper:after{background:linear-gradient(145deg,var(--paper),var(--paper-deep))}.cover-copy{position:relative;z-index:2;color:#fff;max-width:154mm}.cover-paper .cover-copy{color:var(--ink)}.wordmark{font-family:Fraunces,Georgia,serif;font-size:23pt;color:#E8C7A7;margin:0 0 26mm}.cover-paper .wordmark{color:var(--accent)}.cover h1{font-family:Kalam,Inter,sans-serif;font-size:48pt;line-height:1.02;margin:0 0 5mm;font-weight:700}.cover h1:after{content:"";display:block;width:31mm;border-bottom:.5mm solid currentColor;opacity:.48;margin-top:7mm}.cover .subtitle{font-family:Kalam,Inter,sans-serif;font-size:20pt;line-height:1.3;max-width:124mm;margin:0}.cover .edition{text-transform:uppercase;letter-spacing:.16em;font-family:Inter,Arial,sans-serif;font-size:9.5pt;margin-top:12mm}.cover-doodle{position:absolute;z-index:1;color:rgba(255,255,255,.42);fill:none;stroke:currentColor;stroke-width:1.7;stroke-linecap:round;stroke-linejoin:round}.cover-paper .cover-doodle{color:var(--doodle-strong)}.cover-journal{width:96mm;right:-12mm;top:24mm;transform:rotate(7deg)}.cover-sprig{width:58mm;left:-8mm;top:13mm;transform:rotate(-13deg)}.cover-spark{width:29mm;right:22mm;bottom:40mm}.cover-organic{width:115mm;left:-26mm;bottom:32mm;opacity:.55}.cover-figure{width:72mm;right:-5mm;bottom:1mm;opacity:.62}
        .diary-day,.diary-continuation,.quiet-gap,.diary-photo-page{break-before:page;page-break-before:always;min-height:260mm;position:relative;isolation:isolate}.day-heading{height:38mm;border-bottom:.3mm solid var(--rule);display:flex;align-items:flex-end;justify-content:space-between;padding:0 1mm 7mm;position:relative;z-index:2}.day-kicker{font-family:Kalam,Inter,sans-serif;font-size:12pt;font-weight:700;color:var(--accent);margin:0 0 1.5mm}.day-heading h1{font-family:Kalam,Inter,sans-serif;font-size:36pt;line-height:1;margin:0;font-weight:700;color:var(--ink-strong)}.day-count{font-family:Kalam,Inter,sans-serif;font-size:12pt;color:var(--ink-soft);margin:0 0 1mm}.day-body{padding:11mm 3mm 0;position:relative;z-index:2}.diary-continuation .day-body{padding:5mm 1mm 0}.day-body.centered{min-height:211mm;display:flex;align-items:center}.day-body.centered .moment{width:100%}.moment{position:relative;padding:5mm 5mm 9mm;margin:0 0 8mm;background:transparent;break-inside:auto;color:var(--ink)}.diary-continuation .moment{padding:2mm 3mm 4mm;margin-bottom:0}.moment+.moment{border-top:.25mm solid var(--rule-soft);padding-top:11mm}.moment-copy{position:relative;z-index:2}.moment-time{font-family:Kalam,Inter,sans-serif;font-weight:700;font-size:12pt;color:var(--accent);margin:0 0 2mm}.moment h2{font-family:Kalam,Inter,sans-serif;font-size:30pt;line-height:1.12;margin:0 0 3mm;font-weight:700;color:var(--ink-strong)}.content{font-family:Kalam,Inter,sans-serif;font-size:18pt;line-height:1.5;margin:0 0 5mm;color:var(--ink)}.diary-continuation .content{line-height:1.42;margin-bottom:2mm}.metadata{display:flex;flex-wrap:wrap;gap:3mm;color:var(--ink-soft);font-family:Kalam,Inter,sans-serif;font-size:12pt}.tags{display:flex;flex-wrap:wrap;gap:2mm;margin-top:2.5mm}.tags span{border-bottom:.25mm solid var(--rule);padding:.5mm 1mm;font-family:Kalam,Inter,sans-serif;font-weight:700;letter-spacing:.035em;font-size:10pt;color:var(--ink-soft)}.feeling{display:block;width:max-content;margin-top:5mm;font-family:"Apple Color Emoji","Segoe UI Emoji","Noto Color Emoji",sans-serif;font-size:30pt;line-height:1;filter:drop-shadow(0 1mm 1.2mm rgba(0,0,0,.12))}
        .photos{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:5mm;margin-top:7mm;padding:3mm 2mm 2mm;break-inside:avoid;align-items:start}.photo{position:relative;margin:0;padding:3mm 3mm 7mm;background:#FFFCF6;box-shadow:1.5mm 2mm 4mm rgba(54,39,25,.16);break-inside:avoid;transform:rotate(-1.1deg)}.photo:nth-child(even){transform:rotate(1.35deg)}.photo:nth-child(3n){transform:rotate(-.55deg)}.photo:before{content:"";position:absolute;width:25mm;height:5mm;left:50%;top:-3mm;transform:translateX(-50%) rotate(.6deg);background:rgba(218,196,157,.67)}.photo img{display:block;width:100%;height:54mm;object-fit:contain;background:var(--paper)}.photos-1{grid-template-columns:1fr}.photos-1 .photo{width:min(100%,140mm);justify-self:center}.photos-1 .photo img{height:auto;max-height:108mm;object-fit:contain}.photos-2{grid-template-columns:1.15fr .85fr;align-items:center}.photos-2 .photo:first-child img{height:72mm}.photos-2 .photo:nth-child(2) img{height:57mm}.photos-3{grid-template-columns:1.3fr .8fr;grid-template-rows:auto auto}.photos-3 .photo:first-child{grid-row:1/3}.photos-3 .photo:first-child img{height:92mm}.photos-3 .photo:not(:first-child) img{height:40mm}.photos-4 .photo img{height:51mm}.diary-photo-page{display:flex;align-items:center}.diary-photo-page>.photos{width:100%;margin:0;position:relative;z-index:2}.diary-photo-page .photos-1 .photo img{max-height:205mm}.diary-photo-page .photos-2 .photo img{height:145mm}.diary-photo-page .photos-3 .photo:first-child img{height:185mm}.diary-photo-page .photos-3 .photo:not(:first-child) img{height:82mm}.diary-photo-page .photos-4 .photo img{height:94mm}
        .page-doodles{position:absolute;inset:0;width:100%;height:100%;z-index:1;pointer-events:none;color:var(--doodle);fill:none;stroke:currentColor;stroke-width:2;stroke-linecap:round;stroke-linejoin:round}.page-doodles .soft{opacity:.72}.page-doodles .faint{opacity:.52}.page-doodles .wash{fill:currentColor;stroke:none;opacity:.13}.quiet-gap{display:flex;align-items:center;justify-content:center;text-align:center}.gap-copy{max-width:128mm;position:relative;z-index:2}.gap-copy h2{font-family:Kalam,Inter,sans-serif;font-size:32pt;font-weight:700;color:var(--ink-strong);margin:2mm 0 4mm;border-bottom:.3mm solid var(--accent);padding-bottom:3mm}.gap-copy p{font-family:Kalam,Inter,sans-serif;font-size:19pt;color:var(--ink);margin:0}.gap-copy p+p{font-size:15pt;color:var(--ink-soft)}.page-flourish{position:absolute;right:1mm;bottom:1mm;width:28mm;color:var(--doodle);fill:none;stroke:currentColor;stroke-width:1.8;stroke-linecap:round}
        @media screen{body{width:210mm;margin:auto}.cover{height:297mm;margin:0}.diary-day,.diary-continuation,.quiet-gap,.diary-photo-page{padding:14mm 16mm 17mm;min-height:297mm}}
        </style></head><body style="${palette.inlineVariables()}">
        <section class="$coverClass"$coverStyle>${coverDoodles()}<div class="cover-copy"><p class="wordmark">Relive</p><h1>${escape(options.title)}</h1><p class="subtitle">${escape(options.subtitle)}</p><p class="edition">${escape(document.scopeTitle)} · ${document.moments.size} moments</p></div></section>
        $diaryPages
        </body></html>
        """.trimIndent()
    }

    private fun dayPage(date: LocalCalendarDate, moments: List<Moment>, document: MagazineDocument): String {
        val hasMediaThatMustStayBeforeTheNextMoment = moments.size > 1 && moments.any { moment ->
            printablePhotos(moment, document).isNotEmpty()
        }
        if (moments.any { it.content.length > FIRST_PAGE_CONTENT_LIMIT } || hasMediaThatMustStayBeforeTheNextMoment) {
            return paginatedDayPage(date, moments, document)
        }
        val photoItems = moments.map { moment -> printablePhotos(moment, document) }
        val inlinePhotoCounts = moments.mapIndexed { index, moment ->
            if (moment.title.length + moment.content.length <= INLINE_PHOTO_COPY_LIMIT) {
                minOf(photoItems[index].size, MAX_PHOTOS_PER_PAGE)
            } else {
                0
            }
        }
        val entries = moments.mapIndexed { index, moment ->
            momentEntry(
                moment = moment,
                index = index,
                photos = photoItems[index].take(inlinePhotoCounts[index]),
                totalPhotoCount = photoItems[index].size,
            )
        }.joinToString("\n")
        val count = if (moments.size == 1) "One moment" else "${moments.size} moments"
        val centered = moments.singleOrNull()?.let(::isShortMoment) == true
        val photoPages = photoItems.flatMapIndexed { momentIndex, items ->
            items.drop(inlinePhotoCounts[momentIndex])
                .chunked(MAX_PHOTOS_PER_PAGE)
                .mapIndexed { pageIndex, pageItems -> photoPage(pageItems, date.day + momentIndex + pageIndex) }
        }.joinToString("\n")
        return """
        <section class="diary-day"><header class="day-heading"><div><p class="day-kicker">a page from the diary</p><h1>${escape(formatDiaryDate(date))}</h1></div><p class="day-count">${count.lowercase()}</p></header><div class="day-body ${if (centered) "single centered" else if (moments.size == 1) "single" else "many"}">$entries</div>${pageDoodles(date.day)}${pageFlourish()}</section>
        $photoPages
        """.trimIndent()
    }

    private fun paginatedDayPage(date: LocalCalendarDate, moments: List<Moment>, document: MagazineDocument): String {
        val count = if (moments.size == 1) "One moment" else "${moments.size} moments"
        val pages = mutableListOf<String>()
        moments.forEachIndexed { momentIndex, moment ->
            val photos = printablePhotos(moment, document)
            val chunks = splitWriting(moment.content)
            chunks.forEachIndexed { chunkIndex, chunk ->
                val firstChunk = chunkIndex == 0
                val lastChunk = chunkIndex == chunks.lastIndex
                val inlinePhotos = if (firstChunk && moment.title.length + moment.content.length <= INLINE_PHOTO_COPY_LIMIT) {
                    photos.take(MAX_PHOTOS_PER_PAGE)
                } else {
                    emptyList()
                }
                val entry = momentEntry(
                    moment = moment.copy(content = chunk),
                    index = momentIndex,
                    photos = inlinePhotos,
                    totalPhotoCount = photos.size,
                    showHeading = firstChunk,
                    showEnding = lastChunk,
                )
                val isFirstSection = pages.isEmpty()
                pages += if (isFirstSection) {
                    """<section class="diary-day"><header class="day-heading"><div><p class="day-kicker">a page from the diary</p><h1>${escape(formatDiaryDate(date))}</h1></div><p class="day-count">${count.lowercase()}</p></header><div class="day-body single">$entry</div>${pageDoodles(date.day)}${pageFlourish()}</section>"""
                } else {
                    """<section class="diary-continuation"><div class="day-body single">$entry</div>${pageDoodles(date.day + momentIndex + chunkIndex)}${pageFlourish()}</section>"""
                }
            }
            photos.drop(if (moment.title.length + moment.content.length <= INLINE_PHOTO_COPY_LIMIT) MAX_PHOTOS_PER_PAGE else 0)
                .chunked(MAX_PHOTOS_PER_PAGE)
                .forEachIndexed { pageIndex, pageItems ->
                    pages += photoPage(pageItems, date.day + momentIndex + pageIndex)
                }
        }
        return pages.joinToString("\n")
    }

    private fun splitWriting(content: String): List<String> {
        if (content.length <= FIRST_PAGE_CONTENT_LIMIT) return listOf(content)
        val continuationCount =
            (content.length - FIRST_PAGE_CONTENT_LIMIT + CONTINUATION_CONTENT_LIMIT - 1) /
                CONTINUATION_CONTENT_LIMIT
        val capacities = listOf(FIRST_PAGE_CONTENT_LIMIT) + List(continuationCount) { CONTINUATION_CONTENT_LIMIT }
        val chunks = mutableListOf<String>()
        var remaining = content
        capacities.dropLast(1).forEachIndexed { index, capacity ->
            val remainingCapacity = capacities.drop(index).sum()
            val balancedTarget = (remaining.length.toLong() * capacity / remainingCapacity).toInt()
            val boundary = remaining.substring(0, balancedTarget.coerceAtMost(remaining.lastIndex) + 1)
                .indexOfLast { it.isWhitespace() }
                .takeIf { it >= balancedTarget / 2 }
                ?: balancedTarget
            chunks += remaining.substring(0, boundary)
            remaining = remaining.substring(boundary).trimStart()
        }
        chunks += remaining
        return chunks
    }

    private fun momentEntry(
        moment: Moment,
        index: Int,
        photos: List<String>,
        totalPhotoCount: Int,
        showHeading: Boolean = true,
        showEnding: Boolean = true,
    ): String {
        val location = moment.location?.let { value ->
            listOf(value.placeName, value.locality, value.region, value.country)
                .mapNotNull { it?.trim()?.takeIf(String::isNotBlank) }
                .distinct()
                .joinToString(", ")
                .takeIf(String::isNotBlank)
        }
        val tags = if (showEnding) moment.tags.joinToString("") { "<span>${escape(it.label)}</span>" } else ""
        val feeling = moment.feeling?.takeIf { showEnding }?.let { value ->
            "<span class=\"feeling\" role=\"img\" aria-label=\"Feeling: ${escape(value.name)}\" title=\"Feeling: ${escape(value.name)}\">${value.emoji()}</span>"
        }.orEmpty()
        return """
        <article class="moment moment-$index photo-count-$totalPhotoCount"><div class="moment-copy">${if (showHeading) "<p class=\"moment-time\">${escape(EditorialTimeFormatter.format(moment.createdAt))}</p>" else ""}${moment.title.takeIf { showHeading && it.isNotBlank() }?.let { "<h2>${escape(it)}</h2>" }.orEmpty()}${moment.content.takeIf(String::isNotBlank)?.let { "<p class=\"content\">${escape(it).replace("\n", "<br>")}</p>" }.orEmpty()}${location?.takeIf { showEnding }?.let { "<div class=\"metadata\"><span>${escape(it)}</span></div>" }.orEmpty()}${if (tags.isBlank()) "" else "<div class=\"tags\">$tags</div>"}$feeling</div>${photoGrid(photos)}</article>
        """.trimIndent()
    }

    private fun printablePhotos(moment: Moment, document: MagazineDocument): List<String> =
        moment.attachments
            .filter { it.type == MediaType.Image }
            .sortedBy { it.sortIndex }
            .mapNotNull { attachment ->
                document.mediaAssets[attachment.storageRef.value]?.printablePath?.let { path ->
                    "<figure class=\"photo\"><img src=\"${fileUrl(path)}\" alt=\"\"></figure>"
                }
            }

    private fun photoGrid(photos: List<String>): String =
        if (photos.isEmpty()) "" else "<div class=\"photos photos-${photos.size}\">${photos.joinToString("")}</div>"

    private fun photoPage(photos: List<String>, seed: Int): String = """
        <section class="diary-photo-page">${photoGrid(photos)}${pageDoodles(seed)}${pageFlourish()}</section>
    """.trimIndent()

    private fun isShortMoment(moment: Moment): Boolean =
        moment.title.length + moment.content.length <= 520 && moment.attachments.count { it.type == MediaType.Image } <= 3

    private fun gapPage(previous: LocalCalendarDate, next: LocalCalendarDate): String {
        if (calendarDayNumber(next) - calendarDayNumber(previous) <= 1) return ""
        val firstMissing = previous.nextDate()
        val lastMissing = next.previousDate()
        val span = if (firstMissing == lastMissing) formatDiaryDate(firstMissing) else "${formatDiaryDate(firstMissing)} - ${formatDiaryDate(lastMissing)}"
        return """
        <section class="quiet-gap"><div class="gap-copy"><h2>${escape(span)}</h2><p>No moments were kept here.</p><p>Life was still being lived.</p></div>${pageDoodles(firstMissing.day + lastMissing.day)}${pageFlourish()}</section>
        """.trimIndent()
    }

    fun escape(value: String): String = buildString(value.length) {
        value.forEach { character ->
            append(when (character) { '&' -> "&amp;"; '<' -> "&lt;"; '>' -> "&gt;"; '"' -> "&quot;"; '\'' -> "&#39;"; else -> character })
        }
    }

    private fun fileUrl(path: String): String {
        val encoded = path.replace('\\', '/').encodeToByteArray().joinToString("") { byte ->
            val value = byte.toInt() and 0xff
            val safe = value in 'a'.code..'z'.code || value in 'A'.code..'Z'.code || value in '0'.code..'9'.code || value.toChar() in "/:-._~"
            if (safe) value.toChar().toString() else "%${value.toString(16).uppercase().padStart(2, '0')}"
        }
        return "file://$encoded"
    }

    private fun formatDiaryDate(date: LocalCalendarDate): String = "${date.day}${ordinalSuffix(date.day)} ${MONTHS[date.month - 1]} ${date.year}"
    private fun ordinalSuffix(day: Int): String = when { day % 100 in 11..13 -> "th"; day % 10 == 1 -> "st"; day % 10 == 2 -> "nd"; day % 10 == 3 -> "rd"; else -> "th" }
    private fun LocalCalendarDate.nextDate(): LocalCalendarDate = when { day < daysInMonth(year, month) -> copy(day = day + 1); month < 12 -> LocalCalendarDate(year, month + 1, 1); else -> LocalCalendarDate(year + 1, 1, 1) }
    private fun LocalCalendarDate.previousDate(): LocalCalendarDate = when { day > 1 -> copy(day = day - 1); month > 1 -> LocalCalendarDate(year, month - 1, daysInMonth(year, month - 1)); else -> LocalCalendarDate(year - 1, 12, 31) }
    private fun calendarDayNumber(date: LocalCalendarDate): Long { val a = (14 - date.month) / 12; val y = date.year + 4800 - a; val m = date.month + 12 * a - 3; return date.day.toLong() + (153L * m + 2) / 5 + 365L * y + y / 4 - y / 100 + y / 400 - 32045 }
    private fun daysInMonth(year: Int, month: Int): Int = when (month) { 2 -> if (year % 400 == 0 || year % 4 == 0 && year % 100 != 0) 29 else 28; 4, 6, 9, 11 -> 30; else -> 31 }

    private fun coverDoodles(): String = """
        <svg class="cover-doodle cover-journal" viewBox="0 0 180 150" aria-hidden="true"><path d="M90 40v85L20 112V31c25-9 48-5 70 9Z"/><path d="M90 40v85l70-13V31c-25-9-48-5-70 9Z"/><path d="M38 55c14-4 27-3 39 2M38 75c14-4 27-3 39 2M103 57c12-5 25-6 39-2M103 77c12-5 25-6 39-2"/></svg>
        <svg class="cover-doodle cover-sprig" viewBox="0 0 100 150" aria-hidden="true"><path d="M45 140C57 100 46 58 63 12"/><path d="M50 112C24 105 16 89 17 74c20 2 34 13 36 29M53 83c25-7 35-24 34-40-20 3-32 15-34 31M58 49C39 42 33 27 36 16c15 3 23 12 24 25"/></svg>
        <svg class="cover-doodle cover-spark" viewBox="0 0 80 80" aria-hidden="true"><path d="M40 5v70M5 40h70M17 17l46 46M63 17 17 63"/></svg>
        <svg class="cover-doodle cover-organic" viewBox="0 0 240 260" aria-hidden="true"><path d="M-8 84C24 28 92 18 121 61c23 34-4 64 22 91 28 29 70 3 89 39 17 31-9 72-52 78H-8Z"/><path d="M24 100c31-25 62-24 82 3s7 55-19 72c-25 16-61 8-73-19-9-20-4-40 10-56Z"/></svg>
        <svg class="cover-doodle cover-figure" viewBox="0 0 150 210" aria-hidden="true"><circle cx="77" cy="37" r="17"/><path d="M69 55c-23 17-34 47-27 76l-22 55m65-125c27 22 35 55 22 88l25 43M45 82c-19 8-31 21-39 39m91-35c20 4 36 17 46 36M41 132c15 13 36 15 66 7"/></svg>
    """.trimIndent()

    private fun pageDoodles(seed: Int): String = when (seed.mod(3)) {
        0 -> """
            <svg class="page-doodles" viewBox="0 0 670 860" preserveAspectRatio="none" aria-hidden="true">
                <path class="wash" d="M-32 86C14 28 91 22 121 67c25 37-8 70 21 101 31 33 79 1 99 42 19 38-21 78-84 76H-32ZM585 690c42-31 96-6 112 39v151H526c-2-49 20-80 59-90 31-8 27-59 0-100Z"/>
                <g class="faint"><path d="M-18 216c50-28 104-8 111 37 6 39-35 65-79 45M591 438c-34-25-42-70-5-88 35-17 78 13 102 52"/><circle cx="612" cy="236" r="34"/><path d="M602 214c13 8 19 21 17 39m-30-12c17-4 31 1 42 14"/></g>
                <g class="soft"><path d="M71 104c-13 0-13 20 0 20s13-20 0-20Zm0 20v48m0-27c-15-9-23-3-25 7 11 4 19 1 25-7Zm0 13c15-9 23-3 25 7-11 4-19 1-25-7Z"/><path d="M565 128h47v34h-47zM577 120h22v8m-11 8a10 10 0 1 0 0 20 10 10 0 0 0 0-20Z"/></g>
                <g class="faint"><path d="M96 340c26 3 42 19 47 48m0 0-12-11m12 11 3-16M539 326c-9-13-28-3-20 11 7 13 20 21 20 21s13-8 20-21c8-14-11-24-20-11Z"/><path d="M100 646c26-43 28-88 45-132m-24 83c-23-4-33-19-34-34 19-1 32 9 36 28m5-27c23-8 32-25 29-41-18 4-28 17-29 35"/></g>
                <path d="M558 603c0-13 20-13 20 0s-20 13-20 0Zm20 0h45m-25 0c-8-13-2-23 8-25 4 11 1 19-8 25Zm14 0c-8 13-2 23 8 25 4-11 1-19-8-25ZM178 730h45m-32-12v24m17-18v12"/>
            </svg>
        """.trimIndent()
        1 -> """
            <svg class="page-doodles" viewBox="0 0 670 860" preserveAspectRatio="none" aria-hidden="true">
                <path class="wash" d="M550-20c36 24 45 60 20 92-30 39-8 79 38 80 41 1 72 25 80 64V-20ZM-25 647c30-39 75-51 111-20 31 27 11 64 39 86 29 24 67 4 91 32 22 26 7 70-18 115H-25Z"/>
                <g class="faint"><path d="M-12 330c38-35 88-25 104 13 14 33-12 66-56 72M580 578c45-28 88-10 104 26"/><path d="M596 747c-18-38-12-77 17-102m0 0-4 20m4-20-19 7"/></g>
                <g class="soft"><circle cx="92" cy="140" r="24"/><path d="M92 102v20m0 36v20m-38-38h20m36 0h20m-65-27 14 14m26 26 14 14m0-54-14 14m-26 26-14 14M525 132c-9-13-28-3-20 11 7 13 20 21 20 21s13-8 20-21c8-14-11-24-20-11Z"/></g>
                <g class="faint"><path d="M486 292a42 42 0 0 1 84 0m-72 0a30 30 0 0 1 60 0m-48 0a18 18 0 0 1 36 0M76 384h16c8 0 8 13 0 13H76c-14 0-14-23 0-23h18c20 0 20 33 0 33H74"/><path d="M566 523c-13 0-13 20 0 20s13-20 0-20Zm0 20v48m0-27c-15-9-23-3-25 7 11 4 19 1 25-7Zm0 13c15-9 23-3 25 7-11 4-19 1-25-7Z"/></g>
                <path d="M110 685h52m-38-14v28m21-21v14M456 708c25-9 48-27 55-58m0 0-13 11m13-11 4 17"/>
            </svg>
        """.trimIndent()
        else -> """
            <svg class="page-doodles" viewBox="0 0 670 860" preserveAspectRatio="none" aria-hidden="true">
                <path class="wash" d="M-24-15h177c-3 39-27 59-61 66-42 9-24 56 8 79 31 22 26 66-3 91-34 30-86 18-121-9ZM553 610c39-26 91-11 112 27 15 27 9 57-13 76-31 27-9 70 33 84v63H510c-4-45 12-76 43-92 35-18 37-62 0-82-32-17-32-55 0-76Z"/>
                <g class="faint"><path d="M-9 478c45-23 92-3 98 38 5 35-29 59-72 47M595 305c-31-31-29-70 3-91 27-17 61-5 87 24"/><circle cx="83" cy="742" r="29"/><path d="M70 733c9-12 23-17 39-14m-42 31c17 4 31 0 43-12"/></g>
                <g class="soft"><path d="M73 112h45v31H73zM84 143h23m-6-31V99h-11v13m5 7a9 9 0 1 0 0 18 9 9 0 0 0 0-18ZM536 106h48v31h-48zm8 0 16-17 16 17m-22 31v-18h12v18"/></g>
                <g class="faint"><path d="M88 327h42v27H88zm42 7h8c11 0 11 17 0 17h-8m-29-24c-6-12 9-15 3-27m12 27c-6-12 9-15 3-27M520 358h16c8 0 8 13 0 13h-16c-14 0-14-23 0-23h18c20 0 20 33 0 33h-20"/><path d="M124 676c26-43 28-88 45-132m-24 83c-23-4-33-19-34-34 19-1 32 9 36 28m5-27c23-8 32-25 29-41-18 4-28 17-29 35"/></g>
                <path d="M521 674h47v34h-47zM533 666h22v8m-11 8a10 10 0 1 0 0 20 10 10 0 0 0 0-20ZM270 731h50m-35-13v26m18-19v12"/>
            </svg>
        """.trimIndent()
    }

    private fun pageFlourish(): String = """<svg class="page-flourish" viewBox="0 0 100 100" aria-hidden="true"><path d="M50 94V45M50 72C30 69 21 56 22 42c17 1 28 10 29 24M50 59c20-5 30-18 29-33-16 2-27 12-29 25"/></svg>"""

    private fun MomentFeeling.emoji(): String = when (this) {
        MomentFeeling.Great -> "😊"
        MomentFeeling.Good -> "🙂"
        MomentFeeling.Low -> "😔"
    }

    private fun DiaryPaper.cssPalette(): DiaryCssPalette = when (this) {
        DiaryPaper.WarmCream -> DiaryCssPalette("#F3EBDD", "#DED0BB", "#2D2722", "#29231F", "#554A42", "#744B31", "rgba(116,75,49,.38)", "rgba(116,75,49,.16)", "rgba(139,94,60,.22)", "rgba(139,94,60,.42)", "rgba(112,91,68,.055)")
        DiaryPaper.BlushPink -> DiaryCssPalette("#F8E8E8", "#EBCFD1", "#40262B", "#351E23", "#67474D", "#874956", "rgba(135,73,86,.38)", "rgba(135,73,86,.16)", "rgba(184,107,120,.22)", "rgba(184,107,120,.42)", "rgba(135,73,86,.05)")
        DiaryPaper.SageGreen -> DiaryCssPalette("#E8EEE2", "#CDD8C4", "#263125", "#1F291E", "#4A5B47", "#4E674A", "rgba(78,103,74,.38)", "rgba(78,103,74,.16)", "rgba(120,145,111,.23)", "rgba(120,145,111,.44)", "rgba(78,103,74,.052)")
        DiaryPaper.Lavender -> DiaryCssPalette("#EEE8F7", "#D9CFE9", "#312943", "#282137", "#574E69", "#65547F", "rgba(101,84,127,.38)", "rgba(101,84,127,.16)", "rgba(142,123,173,.23)", "rgba(142,123,173,.44)", "rgba(101,84,127,.052)")
        DiaryPaper.PowderBlue -> DiaryCssPalette("#E6F0F7", "#CBDDEB", "#22323E", "#1B2932", "#455B69", "#41657C", "rgba(65,101,124,.38)", "rgba(65,101,124,.16)", "rgba(111,147,170,.23)", "rgba(111,147,170,.44)", "rgba(65,101,124,.052)")
        DiaryPaper.SoftPeach -> DiaryCssPalette("#FAE9DE", "#EBCFBE", "#3B2B24", "#30211C", "#614A3F", "#87533D", "rgba(135,83,61,.38)", "rgba(135,83,61,.16)", "rgba(185,124,94,.23)", "rgba(185,124,94,.44)", "rgba(135,83,61,.052)")
    }

    private data class DiaryCssPalette(
        val paper: String,
        val paperDeep: String,
        val ink: String,
        val inkStrong: String,
        val inkSoft: String,
        val accent: String,
        val rule: String,
        val ruleSoft: String,
        val doodle: String,
        val doodleStrong: String,
        val ruling: String,
    ) {
        fun inlineVariables(): String = "--paper:$paper;--paper-deep:$paperDeep;--ink:$ink;--ink-strong:$inkStrong;--ink-soft:$inkSoft;--accent:$accent;--rule:$rule;--rule-soft:$ruleSoft;--doodle:$doodle;--doodle-strong:$doodleStrong;--ruling:$ruling"
    }
    private val MONTHS = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
    private const val INLINE_PHOTO_COPY_LIMIT = 180
    private const val FIRST_PAGE_CONTENT_LIMIT = 1_200
    private const val CONTINUATION_CONTENT_LIMIT = 1_650
    private const val MAX_PHOTOS_PER_PAGE = 4
}
