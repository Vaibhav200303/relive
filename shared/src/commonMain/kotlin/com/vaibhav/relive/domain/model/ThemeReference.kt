package com.vaibhav.relive.domain.model

/**
 * Timeline theme selection. Only the identifier lives in the domain; concrete palette
 * / typography / motion values are resolved by the UI layer (see DESIGN_SYSTEM.md).
 */
enum class ThemeReference {
    WarmJournal,
    MonochromeArchive,
    FilmMemory,
}
