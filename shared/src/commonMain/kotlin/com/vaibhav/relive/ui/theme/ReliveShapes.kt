package com.vaibhav.relive.ui.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.dp

/**
 * Semantic shape tokens for common Relive containers, derived from [ReliveRadii].
 *
 * Components should reach for these named shapes rather than building a
 * `RoundedCornerShape(<radius>)` inline, so a later radius change flows through the
 * theme instead of every call site. Asymmetric shapes (sheet) are provided ready-made.
 */
@Immutable
data class ReliveShapes(
    /** Standard content card / media container. Defaults to M3 medium (12dp). */
    val card: CornerBasedShape,
    /** Dialogs and modal overlays. Defaults to M3 xl (28dp). */
    val dialog: CornerBasedShape,
    /**
     * Bottom sheet — top corners only. Preserves the ground-attached feel of a sheet
     * anchored to the screen bottom rather than a floating dialog.
     */
    val sheet: CornerBasedShape,
    /** Chips / tag pills — fully rounded. */
    val chip: CornerBasedShape,
    /** Standard buttons — fully rounded (M3 button default). */
    val button: CornerBasedShape,
    /** Pill or circular container — fully rounded. */
    val pill: CornerBasedShape,
)

fun reliveShapes(radii: ReliveRadii): ReliveShapes = ReliveShapes(
    card = RoundedCornerShape(radii.medium),
    dialog = RoundedCornerShape(radii.xl),
    sheet = RoundedCornerShape(
        topStart = radii.xl,
        topEnd = radii.xl,
        bottomEnd = radii.none,
        bottomStart = radii.none,
    ),
    chip = RoundedCornerShape(radii.full),
    button = RoundedCornerShape(radii.full),
    pill = RoundedCornerShape(radii.full),
)

val DefaultReliveShapes: ReliveShapes = reliveShapes(ReliveRadii())
