package fr.cinepass.ticket.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

private val dateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(Locale.FRANCE)
private val shortDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.FRANCE)
private val timeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm", Locale.FRANCE)

fun Long.toLocalDateTime(): LocalDateTime =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDateTime()

fun LocalDateTime.toEpochMillis(): Long =
    atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

/**
 * `DatePicker` de Material 3 travaille en millisecondes UTC : ces deux
 * conversions évitent le décalage d'un jour selon le fuseau de l'appareil.
 */
fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

fun Long.toUtcLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

fun formatFullDate(epochMillis: Long): String =
    epochMillis.toLocalDateTime().format(dateFormatter).replaceFirstChar { it.uppercase() }

fun formatShortDate(epochMillis: Long): String = formatShortDate(epochMillis.toLocalDateTime().toLocalDate())

fun formatShortDate(date: LocalDate): String =
    date.format(shortDateFormatter).replaceFirstChar { it.uppercase() }

fun formatTime(epochMillis: Long): String = epochMillis.toLocalDateTime().format(timeFormatter)

fun formatDateTime(epochMillis: Long): String =
    "${formatShortDate(epochMillis)} · ${formatTime(epochMillis)}"
