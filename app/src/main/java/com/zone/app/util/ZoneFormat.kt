package com.zone.app.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import com.zone.app.data.SplitMember
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Currency
import java.util.Locale
import java.util.UUID
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

object ZoneFormat {

    private val ISO: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val LONG = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.getDefault())
    private val MEDIUM = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
    private val SHORT = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())

    fun todayISO(): String = LocalDate.now().format(ISO)

    fun toISO(date: LocalDate): String = date.format(ISO)

    fun parseISO(iso: String): LocalDate = try {
        LocalDate.parse(iso, ISO)
    } catch (_: Exception) {
        LocalDate.now()
    }

    fun dateLong(iso: String): String = parseISO(iso).format(LONG)

    fun dateMedium(iso: String): String = parseISO(iso).format(MEDIUM)

    fun dateShort(iso: String): String = parseISO(iso).format(SHORT)

    fun relDate(iso: String): String {
        val days = ChronoUnit.DAYS.between(parseISO(iso), LocalDate.now())
        return when {
            days == 0L -> "Today"
            days == 1L -> "Yesterday"
            days in 2L..6L -> "$days days ago"
            else -> dateMedium(iso)
        }
    }

    fun daysUntil(iso: String): Long =
        ChronoUnit.DAYS.between(LocalDate.now(), parseISO(iso))

    fun monthKey(iso: String): String = iso.take(7)

    fun monthLabel(monthKey: String): String {
        val (year, month) = monthKey.split("-").map { it.toIntOrNull() ?: 1 }
        val ym = YearMonth.of(year, month.coerceIn(1, 12))
        return ym.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + ym.year
    }

    fun addMonths(monthKey: String, delta: Int): String {
        val ym = YearMonth.parse(monthKey).plusMonths(delta.toLong())
        return ym.toString()
    }

    fun currentMonth(): String = YearMonth.now().toString()

    fun startOfWeek(base: LocalDate): LocalDate {
        // Monday-start, like the web app.
        val dow = (base.dayOfWeek.value + 6) % 7
        return base.minusDays(dow.toLong())
    }

    fun weekDates(offsetWeeks: Int): List<LocalDate> {
        val start = startOfWeek(LocalDate.now()).plusWeeks(offsetWeeks.toLong())
        return (0..6).map { start.plusDays(it.toLong()) }
    }

    fun uid(prefix: String): String =
        "$prefix-" + System.currentTimeMillis().toString(36) + "-" + UUID.randomUUID().toString().take(5)

    /** Currency formatting with the workspace's display currency. */
    fun money(value: Double, currency: String): String {
        val locale = when (currency) {
            "INR" -> Locale("en", "IN")
            "EUR" -> Locale.GERMANY
            "GBP" -> Locale.UK
            else -> Locale.US
        }
        return try {
            val fmt = NumberFormat.getCurrencyInstance(locale)
            fmt.currency = Currency.getInstance(currency)
            fmt.maximumFractionDigits = if (abs(value) >= 1000) 0 else 2
            fmt.minimumFractionDigits = 0
            fmt.format(value)
        } catch (_: Exception) {
            currency + " %.2f".format(value)
        }
    }

    fun currencySymbol(currency: String): String = when (currency) {
        "INR" -> "₹"; "USD" -> "$"; "EUR" -> "€"; "GBP" -> "£"
        "AED" -> "د.إ"; "AUD" -> "A$"; "CAD" -> "C$"; "SGD" -> "S$"
        else -> currency
    }

    fun num(value: Double, digits: Int = 2): String =
        String.format(Locale.US, "%.${digits}f", value)

    fun words(text: String): Int = text.trim().split(Regex("\\s+")).count { it.isNotEmpty() }

    fun readingMinutes(text: String): Int =
        maxOf(1, ceil(words(text) / 200.0).toInt())

    fun initials(text: String, count: Int = 2): String =
        text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.take(count)
            .joinToString("") { it.first().uppercase() }.ifBlank { "•" }

    /** Deterministic 0..5 tone index, like `toneFor` in the web app. */
    fun toneFor(text: String): Int =
        ((text.fold(0) { acc, c -> acc * 31 + c.code }).let { (it % 100000 + 100000) % 100000 }) % 6

    /**
     * Greedy minimum-cash-flow settlement for a Split group.
     * Returns ordered transfers from debtor member id to creditor member id.
     */
    fun settlePlan(members: List<SplitMember>, nets: Map<String, Double>): List<Triple<String, String, Double>> {
        val creditors = members.mapNotNull { m ->
            val n = nets[m.id] ?: 0.0
            if (n > 0.005) m.id to n else null
        }.sortedByDescending { it.second }.toMutableList()
        val debtors = members.mapNotNull { m ->
            val n = nets[m.id] ?: 0.0
            if (n < -0.005) m.id to -n else null
        }.sortedByDescending { it.second }.toMutableList()

        val transfers = mutableListOf<Triple<String, String, Double>>()
        var i = 0
        var j = 0
        var debtorsList = debtors.map { it.copy() }.toMutableList()
        var creditorsList = creditors.map { it.copy() }.toMutableList()
        while (i < debtorsList.size && j < creditorsList.size) {
            val amount = minOf(debtorsList[i].second, creditorsList[j].second)
            if (amount > 0.004) {
                val rounded = Math.round(amount * 100) / 100.0
                transfers += Triple(debtorsList[i].first, creditorsList[j].first, rounded)
            }
            debtorsList[i] = debtorsList[i].copy(second = debtorsList[i].second - amount)
            creditorsList[j] = creditorsList[j].copy(second = creditorsList[j].second - amount)
            if (debtorsList[i].second <= 0.005) i++
            if (creditorsList[j].second <= 0.005) j++
        }
        return transfers
    }

    fun csvEscape(value: String): String {
        val v = value.replace("\"", "\"\"")
        return if (v.contains(',') || v.contains('"') || v.contains('\n')) "\"$v\"" else v
    }

    fun shareText(context: Context, subject: String, text: String, mime: String = "text/plain") {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, subject))
    }

    fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    }
}
