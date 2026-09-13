package com.vaibhav.relive.platform.exporting

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MagazineHtmlSplitterTest {
    @Test
    fun bodyAttributesDoNotDuplicateTheCompleteMagazineIntoEveryPage() {
        val html = """
            <!doctype html><html><head></head><body style="--paper:#E6F0F7">
            <section id="cover">Cover</section>
            <section id="selected-moment">Selected moment</section>
            <section class="diary-continuation">Continuation</section>
            </body></html>
        """.trimIndent()

        val pages = splitMagazineHtml(html)

        assertEquals(3, pages.size)
        assertTrue(pages.all { "<body style=\"--paper:#E6F0F7\">" in it })
        assertTrue("id=\"cover\"" in pages[0])
        assertFalse("id=\"selected-moment\"" in pages[0])
        assertTrue("id=\"selected-moment\"" in pages[1])
        assertFalse("id=\"cover\"" in pages[1])
        assertTrue("diary-continuation" in pages[2])
        assertTrue(pages.all { page -> Regex("<section\\b").findAll(page).count() == 1 })
    }

    @Test
    fun smallA4RoundingOverflowDoesNotCreateABlankTrailingPage() {
        assertEquals(
            1,
            magazinePhysicalPageCount(
                contentHeight = 1_159,
                pageHeight = 1_123,
                roundingTolerance = 36,
            ),
        )
        assertEquals(
            2,
            magazinePhysicalPageCount(
                contentHeight = 1_160,
                pageHeight = 1_123,
                roundingTolerance = 36,
            ),
        )
        assertEquals(
            2,
            magazinePhysicalPageCount(
                contentHeight = 2_258,
                pageHeight = 1_123,
                roundingTolerance = 36,
            ),
        )
    }

    @Test
    fun selectedPaperPaletteIsAvailableToEveryNativePdfPage() {
        val palette = magazinePdfPalette(
            "<body style=\"--paper:#E6F0F7;--accent:#41657C\"></body>",
        )

        assertEquals(0xffe6f0f7.toInt(), palette.paperColor)
        assertEquals(0xff41657c.toInt(), palette.accentColor)
    }
}
