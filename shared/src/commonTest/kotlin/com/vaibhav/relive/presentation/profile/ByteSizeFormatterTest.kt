package com.vaibhav.relive.presentation.profile

import kotlin.test.Test
import kotlin.test.assertEquals

class ByteSizeFormatterTest {
    @Test fun formats_zero_and_bytes() {
        assertEquals("0 B", formatByteSize(0))
        assertEquals("842 B", formatByteSize(842))
    }

    @Test fun formats_binary_kb_mb_gb_and_large_units() {
        assertEquals("1 KB", formatByteSize(1024))
        assertEquals("18.4 MB", formatByteSize((18.4 * 1024 * 1024).toLong()))
        assertEquals("6.8 GB", formatByteSize((6.8 * 1024 * 1024 * 1024).toLong()))
        assertEquals("1 TB", formatByteSize(1024L * 1024 * 1024 * 1024))
    }
}
