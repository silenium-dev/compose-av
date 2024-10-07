package dev.silenium.multimedia.compose.format

import kotlin.time.Duration

fun Duration.format(): String {
    val seconds = inWholeSeconds
    val minutes = seconds / 60
    val hours = minutes / 60
    return "%02d:%02d:%02d".format(hours, minutes % 60, seconds % 60)
}
