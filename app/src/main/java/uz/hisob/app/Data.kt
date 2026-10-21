package uz.hisob.app

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

/* ---------- model ---------- */

enum class Kind { CREDIT, BILL }

data class Item(
    val id: String = UUID.randomUUID().toString(),
    val kind: Kind,
    val name: String,
    val bank: String = "",
    val cat: String = "other",
    val amount: Long,
    val day: Int,
    val auto: Boolean,
    val start: YearMonth = YearMonth.now(),
    val months: Int = 12,
    val since: YearMonth = YearMonth.now(),
)

data class Expense(
    val id: String = UUID.randomUUID().toString(),
    val amount: Long,
    val cat: String,
    val note: String,
    val date: LocalDate,
)

data class Paid(val amount: Long, val date: LocalDate)

data class AppData(
    val items: List<Item> = emptyList(),
    val expenses: List<Expense> = emptyList(),
    val paid: Map<YearMonth, Map<String, Paid>> = emptyMap(),
    val currency: String = "so'm",
    val remindHour: Int = 9,
)

/* ---------- categories ---------- */

data class Cat(val key: String, val label: String, val color: Long)

val SPEND_CATS = listOf(
    Cat("food", "Food", 0xFFE07A2E), Cat("groceries", "Groceries", 0xFF3F9C5A),
    Cat("transport", "Transport", 0xFF2C8FC7), Cat("fuel", "Fuel", 0xFF7A5BD6),
    Cat("family", "Family", 0xFFD14E8A), Cat("health", "Health", 0xFF1BA39C),
    Cat("shopping", "Shopping", 0xFFC99A1E), Cat("fun", "Fun", 0xFF8C6BE0),
    Cat("other", "Other", 0xFF7B849E),
)
val BILL_CATS = listOf(
    Cat("electricity", "Electricity", 0xFFE0A21E), Cat("gas", "Gas", 0xFFE0632E),
    Cat("water", "Water", 0xFF2C9BD6), Cat("internet", "Internet", 0xFF5B6BE0),
    Cat("phone", "Mobile", 0xFF1BA39C), Cat("rent", "Rent", 0xFFB0577A),
    Cat("subscription", "Subscription", 0xFF8C6BE0), Cat("other", "Other", 0xFF7B849E),
)
private val BANK_COLORS = listOf(
    0xFF2443C4, 0xFF0E8C6B, 0xFFC0392B, 0xFF8E44AD, 0xFFD35400, 0xFF16728F, 0xFFB7791F, 0xFF4C5BD4,
)

fun spendCat(k: String) = SPEND_CATS.firstOrNull { it.key == k } ?: SPEND_CATS.last()
fun billCat(k: String) = BILL_CATS.firstOrNull { it.key == k } ?: BILL_CATS.last()
fun bankColor(name: String): Long {
    var h = 0L
    name.forEach { h = (h * 31 + it.code) and 0xFFFFFFFFL }
    return BANK_COLORS[(h % BANK_COLORS.size).toInt()]
}

fun Item.title() = if (kind == Kind.CREDIT) bank else name
fun Item.subtitle() = if (kind == Kind.CREDIT) name.ifBlank { "Credit" } else billCat(cat).label
fun Item.color() = if (kind == Kind.CREDIT) bankColor(bank) else billCat(cat).color

fun initials(s: String): String {
    val p = s.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (p.isEmpty()) return "?"
    return (p[0].take(1) + (p.getOrNull(1)?.take(1) ?: "")).uppercase()
}

/* ---------- status logic ---------- */

enum class St { PAID, AUTO, OVERDUE, TODAY, UPCOMING }

data class Status(val st: St, val amount: Long, val due: LocalDate, val assumed: Boolean = false) {
    val settled get() = st == St.PAID || st == St.AUTO
}

fun dueDate(ym: YearMonth, day: Int): LocalDate = ym.atDay(day.coerceIn(1, ym.lengthOfMonth()))

fun Item.activeIn(ym: YearMonth): Boolean =
    if (kind == Kind.CREDIT) !ym.isBefore(start) && ym.isBefore(start.plusMonths(months.toLong())) else true

fun AppData.status(it: Item, ym: YearMonth, today: LocalDate = LocalDate.now()): Status {
    val due = dueDate(ym, it.day)
    val rec = paid[ym]?.get(it.id)
    return when {
        rec != null -> Status(St.PAID, rec.amount, due)
        ym.isBefore(it.since) -> Status(St.PAID, it.amount, due, assumed = true)
        it.auto && !due.isAfter(today) -> Status(St.AUTO, it.amount, due)
        due.isBefore(today) -> Status(St.OVERDUE, it.amount, due)
        due == today -> Status(St.TODAY, it.amount, due)
        else -> Status(St.UPCOMING, it.amount, due)
    }
}

fun AppData.monthItems(ym: YearMonth) = items.filter { it.activeIn(ym) }.sortedBy { it.day }
fun AppData.monthExpenses(ym: YearMonth) = expenses.filter { YearMonth.from(it.date) == ym }

data class Summary(
    val credits: Long, val bills: Long, val spend: Long, val settled: Long, val due: Long,
) {
    val total get() = credits + bills + spend
}

fun AppData.summary(ym: YearMonth): Summary {
    var c = 0L; var b = 0L; var s = 0L; var d = 0L
    monthItems(ym).forEach {
        val st = status(it, ym)
        if (it.kind == Kind.CREDIT) c += st.amount else b += st.amount
        if (st.settled) s += st.amount else d += st.amount
    }
    return Summary(c, b, monthExpenses(ym).sumOf { it.amount }, s, d)
}

data class Progress(val done: Int, val left: Int, val remaining: Long, val end: YearMonth) {
    val pct get() = if (done + left == 0) 0f else done.toFloat() / (done + left)
}

fun AppData.progress(it: Item): Progress {
    val now = YearMonth.now()
    val before = (now.year * 12 + now.monthValue) - (it.start.year * 12 + it.start.monthValue)
    val b = before.coerceIn(0, it.months)
    val cur = if (it.activeIn(now) && status(it, now).settled) 1 else 0
    val done = (b + cur).coerceAtMost(it.months)
    val left = it.months - done
    return Progress(done, left, left * it.amount, it.start.plusMonths((it.months - 1).toLong()))
}

/* ---------- formatting ---------- */

fun fmt(n: Long): String {
    val s = kotlin.math.abs(n).toString().reversed().chunked(3).joinToString(" ").reversed()
    return if (n < 0) "-$s" else s
}

fun short(n: Long): String = when {
    n >= 1_000_000 -> String.format(java.util.Locale.US, if (n >= 10_000_000) "%.1f" else "%.2f", n / 1e6)
        .trimEnd('0').trimEnd('.') + "M"
    n >= 1000 -> "${n / 1000}k"
    else -> n.toString()
}

private val MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
private val MONTHS_LONG = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
fun YearMonth.label() = "${MONTHS[monthValue - 1]} $year"
fun YearMonth.longName() = MONTHS_LONG[monthValue - 1]
fun YearMonth.shortName() = MONTHS[monthValue - 1]
fun LocalDate.shortLabel() = "$dayOfMonth ${MONTHS[monthValue - 1]}"

fun parseAmount(s: String): Long? = s.filter { it.isDigit() }.toLongOrNull()

/* ---------- storage ---------- */

class Store(private val ctx: Context) {
    var data by mutableStateOf(AppData())
        private set

    private val file get() = File(ctx.filesDir, "hisob.json")

    init { data = load(ctx) }

    fun update(f: (AppData) -> AppData) {
        data = f(data)
        save()
        Reminders.schedule(ctx)
    }

    private fun save() {
        try {
            val tmp = File(ctx.filesDir, "hisob.json.tmp")
            tmp.writeText(toJson(data).toString())
            tmp.renameTo(file)
        } catch (_: Exception) { }
    }

    companion object {
        fun load(ctx: Context): AppData = try {
            val f = File(ctx.filesDir, "hisob.json")
            if (f.exists()) fromJson(JSONObject(f.readText())) else AppData()
        } catch (_: Exception) { AppData() }

        fun toJson(d: AppData): JSONObject = JSONObject().apply {
            put("currency", d.currency)
            put("remindHour", d.remindHour)
            put("items", JSONArray().apply {
                d.items.forEach { i ->
                    put(JSONObject().apply {
                        put("id", i.id); put("kind", i.kind.name); put("name", i.name); put("bank", i.bank)
                        put("cat", i.cat); put("amount", i.amount); put("day", i.day); put("auto", i.auto)
                        put("start", i.start.toString()); put("months", i.months); put("since", i.since.toString())
                    })
                }
            })
            put("expenses", JSONArray().apply {
                d.expenses.forEach { e ->
                    put(JSONObject().apply {
                        put("id", e.id); put("amount", e.amount); put("cat", e.cat); put("note", e.note); put("date", e.date.toString())
                    })
                }
            })
            put("paid", JSONObject().apply {
                d.paid.forEach { (ym, m) ->
                    put(ym.toString(), JSONObject().apply {
                        m.forEach { (id, p) -> put(id, JSONObject().apply { put("amount", p.amount); put("date", p.date.toString()) }) }
                    })
                }
            })
        }

        fun fromJson(o: JSONObject): AppData {
            val items = mutableListOf<Item>()
            o.optJSONArray("items")?.let { a ->
                for (k in 0 until a.length()) {
                    val j = a.getJSONObject(k)
                    items += Item(
                        id = j.getString("id"), kind = Kind.valueOf(j.getString("kind")), name = j.optString("name"),
                        bank = j.optString("bank"), cat = j.optString("cat", "other"), amount = j.getLong("amount"),
                        day = j.getInt("day"), auto = j.optBoolean("auto"),
                        start = YearMonth.parse(j.optString("start", YearMonth.now().toString())),
                        months = j.optInt("months", 12),
                        since = YearMonth.parse(j.optString("since", YearMonth.now().toString())),
                    )
                }
            }
            val exps = mutableListOf<Expense>()
            o.optJSONArray("expenses")?.let { a ->
                for (k in 0 until a.length()) {
                    val j = a.getJSONObject(k)
                    exps += Expense(j.getString("id"), j.getLong("amount"), j.optString("cat", "other"), j.optString("note"), LocalDate.parse(j.getString("date")))
                }
            }
            val paid = mutableMapOf<YearMonth, Map<String, Paid>>()
            o.optJSONObject("paid")?.let { p ->
                p.keys().forEach { mk ->
                    val m = mutableMapOf<String, Paid>()
                    val pj = p.getJSONObject(mk)
                    pj.keys().forEach { id ->
                        val r = pj.getJSONObject(id)
                        m[id] = Paid(r.getLong("amount"), LocalDate.parse(r.getString("date")))
                    }
                    paid[YearMonth.parse(mk)] = m
                }
            }
            return AppData(items, exps, paid, o.optString("currency", "so'm"), o.optInt("remindHour", 9))
        }
    }
}
