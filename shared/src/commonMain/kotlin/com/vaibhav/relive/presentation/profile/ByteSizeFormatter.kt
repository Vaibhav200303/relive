package com.vaibhav.relive.presentation.profile

/** Shared, locale-neutral archive-size formatter using binary file-size units. */
fun formatByteSize(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB", "PB")
    var value = bytes.toDouble()
    var unit = 0
    while (value >= 1024.0 && unit < units.lastIndex) {
        value /= 1024.0
        unit++
    }
    if (unit == 0) return "$bytes B"
    val precision = if (value < 100.0) 1 else 0
    val text = value.toFixed(precision).trimEnd('0').trimEnd('.')
    return "$text ${units[unit]}"
}

private fun Double.toFixed(decimals: Int): String {
    val scale = if (decimals == 0) 1.0 else 10.0
    val rounded = kotlin.math.round(this * scale) / scale
    return rounded.toString()
}
