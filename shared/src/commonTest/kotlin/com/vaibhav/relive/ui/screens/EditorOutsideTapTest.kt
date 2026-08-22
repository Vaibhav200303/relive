package com.vaibhav.relive.ui.screens

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EditorOutsideTapTest {
    private val editor = Rect(left = 10f, top = 20f, right = 110f, bottom = 220f)

    @Test fun tapInsideEditorIsNotOutside() = assertFalse(isTapOutsideEditor(editor, Offset(60f, 120f)))
    @Test fun tapOnEditorEdgeIsNotOutside() = assertFalse(isTapOutsideEditor(editor, Offset(10f, 20f)))
    @Test fun tapJustOutsideEditorIsOutside() = assertTrue(isTapOutsideEditor(editor, Offset(110.1f, 220f)))
    @Test fun tapWithNoEditorIsNoOp() = assertFalse(isTapOutsideEditor(null, Offset.Zero))
}
