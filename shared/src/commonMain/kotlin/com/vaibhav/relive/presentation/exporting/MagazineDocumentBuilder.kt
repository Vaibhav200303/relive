package com.vaibhav.relive.presentation.exporting

import com.vaibhav.relive.domain.model.LocalCalendarDate
import com.vaibhav.relive.domain.model.MediaType
import com.vaibhav.relive.domain.model.Moment
import com.vaibhav.relive.platform.exporting.MagazineDocument
import com.vaibhav.relive.presentation.date.EditorialTimeFormatter
import com.vaibhav.relive.presentation.date.RediscoverCalendar

/** Offline print diary. Every user value and local path is escaped before interpolation. */
object MagazineDocumentBuilder {
    fun html(document: MagazineDocument): String {
        val options = document.options
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
        *{box-sizing:border-box}html,body{margin:0;padding:0;background:#F3EBDD;color:#2D2722;font-family:Kalam,Inter,Arial,sans-serif}body{font-size:12pt;line-height:1.6;background-image:repeating-linear-gradient(0deg,rgba(112,91,68,.045) 0,rgba(112,91,68,.045) .2mm,transparent .2mm,transparent 7mm)}
        .cover{height:297mm;margin:-14mm -16mm -17mm;page-break-after:always;display:flex;flex-direction:column;justify-content:flex-end;padding:25mm 21mm;background-size:cover;background-position:center;position:relative;overflow:hidden}.cover:after{content:"";position:absolute;inset:0;background:linear-gradient(180deg,rgba(35,27,22,.03),rgba(35,27,22,.76))}.cover-paper:after{background:linear-gradient(145deg,#F5EDDF,#DED0BB)}.cover-copy{position:relative;z-index:2;color:#fff;max-width:148mm}.cover-paper .cover-copy{color:#40372F}.wordmark{font-family:Fraunces,Georgia,serif;font-size:21pt;font-style:italic;color:#D5AA82;margin:0 0 29mm}.cover-paper .wordmark{color:#8B5E3C}.cover h1{font-family:Fraunces,Georgia,serif;font-size:42pt;line-height:1.02;margin:0 0 5mm;font-weight:600}.cover h1:after{content:"";display:block;width:26mm;border-bottom:.35mm solid currentColor;opacity:.5;margin-top:7mm}.cover .subtitle{font-family:Fraunces,Georgia,serif;font-size:16pt;font-style:italic;max-width:116mm;margin:0}.cover .edition{text-transform:uppercase;letter-spacing:.18em;font-size:9pt;margin-top:12mm}.cover-doodle{position:absolute;z-index:1;color:rgba(255,255,255,.42);fill:none;stroke:currentColor;stroke-width:1.7;stroke-linecap:round;stroke-linejoin:round}.cover-paper .cover-doodle{color:rgba(139,94,60,.35)}.cover-journal{width:86mm;right:-10mm;top:28mm;transform:rotate(7deg)}.cover-sprig{width:48mm;left:-7mm;top:17mm;transform:rotate(-13deg)}.cover-spark{width:25mm;right:24mm;bottom:40mm}
        .diary-day,.quiet-gap,.diary-photo-page{break-before:page;page-break-before:always;min-height:260mm;position:relative;isolation:isolate}.day-heading{height:33mm;border-bottom:.25mm solid rgba(92,67,48,.44);display:flex;align-items:flex-end;justify-content:space-between;padding:0 1mm 6mm;position:relative;z-index:2}.day-kicker{font-family:Kalam,Inter,sans-serif;font-size:10pt;color:#744B31;margin:0 0 1.5mm}.day-heading h1{font-family:Kalam,Inter,sans-serif;font-size:31pt;line-height:1;margin:0;font-weight:700;color:#29231F}.day-count{font-family:Kalam,Inter,sans-serif;font-size:10.5pt;color:#5C5047;margin:0 0 1mm}.day-body{padding:10mm 3mm 0;position:relative;z-index:2}.day-body.centered{min-height:217mm;display:flex;align-items:center}.day-body.centered .moment{width:100%}.moment{position:relative;padding:4mm 5mm 8mm;margin:0 0 8mm;background:transparent;break-inside:auto;color:#2D2722}.moment+.moment{border-top:.2mm solid rgba(104,78,56,.18);padding-top:10mm}.moment-copy{position:relative;z-index:2}.moment-time{font-family:Kalam,Inter,sans-serif;font-weight:700;font-size:10pt;color:#744B31;margin:0 0 2mm}.moment h2{font-family:Kalam,Inter,sans-serif;font-size:25pt;line-height:1.16;margin:0 0 2.5mm;font-weight:400;color:#29231F}.content{font-family:Kalam,Inter,sans-serif;font-size:15pt;line-height:1.55;margin:0 0 4mm;color:#2D2722}.metadata{display:flex;flex-wrap:wrap;gap:3mm;color:#554A42;font-family:Kalam,Inter,sans-serif;font-size:10.5pt}.tags{display:flex;flex-wrap:wrap;gap:2mm;margin-top:2mm}.tags span,.feeling{border-bottom:.2mm solid rgba(77,61,49,.48);padding:.5mm 1mm;font-family:Inter,Arial,sans-serif;text-transform:uppercase;letter-spacing:.09em;font-size:8.5pt;color:#493F37}
        .photos{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:5mm;margin-top:7mm;padding:3mm 2mm 2mm;break-inside:avoid;align-items:start}.photo{position:relative;margin:0;padding:3mm 3mm 7mm;background:#FFFCF6;box-shadow:1.5mm 2mm 4mm rgba(54,39,25,.16);break-inside:avoid;transform:rotate(-1.1deg)}.photo:nth-child(even){transform:rotate(1.35deg)}.photo:nth-child(3n){transform:rotate(-.55deg)}.photo:before{content:"";position:absolute;width:25mm;height:5mm;left:50%;top:-3mm;transform:translateX(-50%) rotate(.6deg);background:rgba(218,196,157,.67)}.photo img{display:block;width:100%;height:54mm;object-fit:contain;background:#F3EBDD}.photos-1{grid-template-columns:1fr}.photos-1 .photo{width:min(100%,140mm);justify-self:center}.photos-1 .photo img{height:auto;max-height:108mm;object-fit:contain}.photos-2{grid-template-columns:1.15fr .85fr;align-items:center}.photos-2 .photo:first-child img{height:72mm}.photos-2 .photo:nth-child(2) img{height:57mm}.photos-3{grid-template-columns:1.3fr .8fr;grid-template-rows:auto auto}.photos-3 .photo:first-child{grid-row:1/3}.photos-3 .photo:first-child img{height:92mm}.photos-3 .photo:not(:first-child) img{height:40mm}.photos-4 .photo img{height:51mm}.diary-photo-page{display:flex;align-items:center}.diary-photo-page>.photos{width:100%;margin:0;position:relative;z-index:2}
        .page-doodles{position:absolute;left:0;right:0;top:36mm;bottom:0;width:100%;height:calc(100% - 36mm);z-index:1;pointer-events:none;color:rgba(139,94,60,.17);fill:none;stroke:currentColor;stroke-width:1.4;stroke-linecap:round;stroke-linejoin:round}.page-doodles .soft{opacity:.65}.page-doodles .faint{opacity:.42}.quiet-gap{display:flex;align-items:center;justify-content:center;text-align:center}.gap-copy{max-width:118mm;position:relative;z-index:2}.gap-copy h2{font-family:Kalam,Inter,sans-serif;font-size:27pt;font-weight:700;color:#29231F;margin:2mm 0 3mm;border-bottom:.25mm solid #744B31;padding-bottom:3mm}.gap-copy p{font-family:Kalam,Inter,sans-serif;font-size:15pt;color:#2D2722;margin:0}.gap-copy p+p{font-size:12pt;color:#51463E}.page-flourish{position:absolute;right:1mm;bottom:1mm;width:22mm;color:rgba(139,94,60,.17);fill:none;stroke:currentColor;stroke-width:1.5;stroke-linecap:round}
        @media screen{body{width:210mm;margin:auto}.cover{height:297mm;margin:0}.diary-day,.quiet-gap,.diary-photo-page{padding:14mm 16mm 17mm;min-height:297mm}}
        </style></head><body>
        <section class="$coverClass"$coverStyle>${coverDoodles()}<div class="cover-copy"><p class="wordmark">Relive</p><h1>${escape(options.title)}</h1><p class="subtitle">${escape(options.subtitle)}</p><p class="edition">${escape(document.scopeTitle)} · ${document.moments.size} moments</p></div></section>
        $diaryPages
        </body></html>
        """.trimIndent()
    }

    private fun dayPage(date: LocalCalendarDate, moments: List<Moment>, document: MagazineDocument): String {
        val photoItems = moments.map { moment -> printablePhotos(moment, document) }
        val inlinePhotoCount = if (
            moments.size == 1 &&
            moments.single().title.length + moments.single().content.length <= INLINE_PHOTO_COPY_LIMIT
        ) {
            minOf(photoItems.single().size, MAX_PHOTOS_PER_PAGE)
        } else {
            0
        }
        val entries = moments.mapIndexed { index, moment ->
            momentEntry(
                moment = moment,
                index = index,
                photos = photoItems[index].take(if (index == 0) inlinePhotoCount else 0),
                totalPhotoCount = photoItems[index].size,
            )
        }.joinToString("\n")
        val count = if (moments.size == 1) "One moment" else "${moments.size} moments"
        val centered = moments.singleOrNull()?.let(::isShortMoment) == true
        val photoPages = photoItems.flatMapIndexed { momentIndex, items ->
            items.drop(if (momentIndex == 0) inlinePhotoCount else 0)
                .chunked(MAX_PHOTOS_PER_PAGE)
                .mapIndexed { pageIndex, pageItems -> photoPage(pageItems, date.day + momentIndex + pageIndex) }
        }.joinToString("\n")
        return """
        <section class="diary-day"><header class="day-heading"><div><p class="day-kicker">a page from the diary</p><h1>${escape(formatDiaryDate(date))}</h1></div><p class="day-count">${count.lowercase()}</p></header><div class="day-body ${if (centered) "single centered" else if (moments.size == 1) "single" else "many"}">$entries</div>${pageDoodles(date.day)}${pageFlourish()}</section>
        $photoPages
        """.trimIndent()
    }

    private fun momentEntry(moment: Moment, index: Int, photos: List<String>, totalPhotoCount: Int): String {
        val location = moment.location?.let { value ->
            listOf(value.placeName, value.locality, value.region, value.country)
                .mapNotNull { it?.trim()?.takeIf(String::isNotBlank) }
                .distinct()
                .joinToString(", ")
                .takeIf(String::isNotBlank)
        }
        val tags = moment.tags.joinToString("") { "<span>${escape(it.label)}</span>" }
        val feeling = moment.feeling?.name?.let { "<span class=\"feeling\">${escape(it)}</span>" }.orEmpty()
        return """
        <article class="moment moment-$index photo-count-$totalPhotoCount"><div class="moment-copy"><p class="moment-time">${escape(EditorialTimeFormatter.format(moment.createdAt))}</p>${moment.title.takeIf(String::isNotBlank)?.let { "<h2>${escape(it)}</h2>" }.orEmpty()}${moment.content.takeIf(String::isNotBlank)?.let { "<p class=\"content\">${escape(it).replace("\n", "<br>")}</p>" }.orEmpty()}<div class="metadata">${location?.let { "<span>${escape(it)}</span>" }.orEmpty()}$feeling</div>${if (tags.isBlank()) "" else "<div class=\"tags\">$tags</div>"}</div>${photoGrid(photos)}</article>
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
    """.trimIndent()

    private fun pageDoodles(seed: Int): String = when (seed.mod(3)) {
        0 -> """
            <svg class="page-doodles" viewBox="0 0 670 860" preserveAspectRatio="none" aria-hidden="true">
                <g class="soft"><path d="M71 104c-13 0-13 20 0 20s13-20 0-20Zm0 20v48m0-27c-15-9-23-3-25 7 11 4 19 1 25-7Zm0 13c15-9 23-3 25 7-11 4-19 1-25-7Z"/><path d="M565 128h47v34h-47zM577 120h22v8m-11 8a10 10 0 1 0 0 20 10 10 0 0 0 0-20Z"/></g>
                <g class="faint"><path d="M96 340c26 3 42 19 47 48m0 0-12-11m12 11 3-16M539 326c-9-13-28-3-20 11 7 13 20 21 20 21s13-8 20-21c8-14-11-24-20-11Z"/><path d="M100 646c26-43 28-88 45-132m-24 83c-23-4-33-19-34-34 19-1 32 9 36 28m5-27c23-8 32-25 29-41-18 4-28 17-29 35"/></g>
                <path d="M558 603c0-13 20-13 20 0s-20 13-20 0Zm20 0h45m-25 0c-8-13-2-23 8-25 4 11 1 19-8 25Zm14 0c-8 13-2 23 8 25 4-11 1-19-8-25ZM178 730h45m-32-12v24m17-18v12"/>
            </svg>
        """.trimIndent()
        1 -> """
            <svg class="page-doodles" viewBox="0 0 670 860" preserveAspectRatio="none" aria-hidden="true">
                <g class="soft"><circle cx="92" cy="140" r="24"/><path d="M92 102v20m0 36v20m-38-38h20m36 0h20m-65-27 14 14m26 26 14 14m0-54-14 14m-26 26-14 14M525 132c-9-13-28-3-20 11 7 13 20 21 20 21s13-8 20-21c8-14-11-24-20-11Z"/></g>
                <g class="faint"><path d="M486 292a42 42 0 0 1 84 0m-72 0a30 30 0 0 1 60 0m-48 0a18 18 0 0 1 36 0M76 384h16c8 0 8 13 0 13H76c-14 0-14-23 0-23h18c20 0 20 33 0 33H74"/><path d="M566 523c-13 0-13 20 0 20s13-20 0-20Zm0 20v48m0-27c-15-9-23-3-25 7 11 4 19 1 25-7Zm0 13c15-9 23-3 25 7-11 4-19 1-25-7Z"/></g>
                <path d="M110 685h52m-38-14v28m21-21v14M456 708c25-9 48-27 55-58m0 0-13 11m13-11 4 17"/>
            </svg>
        """.trimIndent()
        else -> """
            <svg class="page-doodles" viewBox="0 0 670 860" preserveAspectRatio="none" aria-hidden="true">
                <g class="soft"><path d="M73 112h45v31H73zM84 143h23m-6-31V99h-11v13m5 7a9 9 0 1 0 0 18 9 9 0 0 0 0-18ZM536 106h48v31h-48zm8 0 16-17 16 17m-22 31v-18h12v18"/></g>
                <g class="faint"><path d="M88 327h42v27H88zm42 7h8c11 0 11 17 0 17h-8m-29-24c-6-12 9-15 3-27m12 27c-6-12 9-15 3-27M520 358h16c8 0 8 13 0 13h-16c-14 0-14-23 0-23h18c20 0 20 33 0 33h-20"/><path d="M124 676c26-43 28-88 45-132m-24 83c-23-4-33-19-34-34 19-1 32 9 36 28m5-27c23-8 32-25 29-41-18 4-28 17-29 35"/></g>
                <path d="M521 674h47v34h-47zM533 666h22v8m-11 8a10 10 0 1 0 0 20 10 10 0 0 0 0-20ZM270 731h50m-35-13v26m18-19v12"/>
            </svg>
        """.trimIndent()
    }

    private fun pageFlourish(): String = """<svg class="page-flourish" viewBox="0 0 100 100" aria-hidden="true"><path d="M50 94V45M50 72C30 69 21 56 22 42c17 1 28 10 29 24M50 59c20-5 30-18 29-33-16 2-27 12-29 25"/></svg>"""
    private val MONTHS = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
    private const val INLINE_PHOTO_COPY_LIMIT = 180
    private const val MAX_PHOTOS_PER_PAGE = 4
}
