package dev.falah.app

import kotlinx.datetime.LocalDate

private val weekdays = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
private val months = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")

fun LocalDate.weekdayName(): String = weekdays[dayOfWeek.ordinal]
fun LocalDate.weekdayLetter(): String = weekdays[dayOfWeek.ordinal].take(1)
fun LocalDate.longText(): String = "${weekdayName()}, $dayOfMonth ${months[monthNumber - 1]} $year"
fun greeting(hour: Int): String = when { hour < 12 -> "Good morning"; hour < 17 -> "Good afternoon"; else -> "Good evening" }
