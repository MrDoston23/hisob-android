package uz.hisob.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/* ================= shared pieces ================= */

val CardShape = RoundedCornerShape(20.dp)
val BottomPad = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp)

@Composable
fun CardBox(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val p = LocalPalette.current
    Column(
        modifier.fillMaxWidth().clip(CardShape).background(p.surface).border(1.dp, p.line, CardShape),
        content = content,
    )
}

@Composable
fun Ava(text: String, color: Long, size: Dp = 40.dp) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(12.dp)).background(Color(color)),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp) }
}

@Composable
fun Pill(text: String, bg: Color, fg: Color) {
    Text(
        text, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1,
        modifier = Modifier.padding(top = 3.dp).clip(CircleShape).background(bg).padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

@Composable
fun StatusPill(s: Status) {
    val p = LocalPalette.current
    when (s.st) {
        St.PAID -> Pill(if (s.assumed) "Before tracking" else "Paid", p.goodBg, p.good)
        St.AUTO -> Pill("Bank took it", p.infoBg, p.lapis)
        St.OVERDUE -> Pill("Overdue", p.badBg, p.bad)
        St.TODAY -> Pill("Due today", p.warnBg, p.warn)
        St.UPCOMING -> {
            val days = ChronoUnit.DAYS.between(LocalDate.now(), s.due)
            Pill(if (days in 1L..31L) "In $days day${if (days > 1) "s" else ""}" else "Upcoming", p.surface2, p.muted)
        }
    }
}

@Composable
fun SectionHeader(title: String, right: String? = null) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 10.dp, start = 4.dp, end = 4.dp), verticalAlignment = Alignment.Bottom) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = p.ink, modifier = Modifier.weight(1f))
        if (right != null) Text(right, style = NumStyle.copy(fontSize = 13.sp), color = p.muted)
    }
}

@Composable
fun LineDivider() { HorizontalDivider(color = LocalPalette.current.line, thickness = 1.dp) }

@Composable
fun ItemRow(d: AppData, item: Item, ym: YearMonth, onClick: () -> Unit) {
    val p = LocalPalette.current
    val s = d.status(item, ym)
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Ava(initials(item.title()), item.color())
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.title(), fontWeight = FontWeight.Bold, color = p.ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (item.auto) Text("  AUTO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = p.lapis, letterSpacing = 0.6.sp)
            }
            Text("${item.subtitle()} · day ${item.day}", fontSize = 13.sp, color = p.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(fmt(s.amount), style = NumStyle.copy(fontSize = 14.sp), color = p.ink)
            StatusPill(s)
        }
    }
}

@Composable
fun ExpenseRow(e: Expense, onDelete: () -> Unit) {
    val p = LocalPalette.current
    val c = spendCat(e.cat)
    var armed by remember(e.id) { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Ava(c.label.take(1), c.color)
        Column(Modifier.weight(1f)) {
            Text(e.note.ifBlank { c.label }, fontWeight = FontWeight.Bold, color = p.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(c.label, fontSize = 13.sp, color = p.muted)
        }
        Text(fmt(e.amount), style = NumStyle.copy(fontSize = 14.sp), color = p.ink)
        IconButton(onClick = { if (armed) onDelete() else armed = true }) {
            Icon(if (armed) Icons.Rounded.Check else Icons.Rounded.Close, if (armed) "Confirm delete" else "Delete", tint = if (armed) p.bad else p.muted)
        }
    }
}

@Composable
fun EmptyNote(text: String) {
    Text(text, color = LocalPalette.current.muted, fontSize = 14.sp, textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 22.dp))
}

/* ================= Month ================= */

@Composable
fun MonthScreen(d: AppData, ym: YearMonth, onPay: (String) -> Unit) {
    val p = LocalPalette.current
    val sm = d.summary(ym)
    val items = d.monthItems(ym)
    val credits = items.filter { it.kind == Kind.CREDIT }
    val bills = items.filter { it.kind == Kind.BILL }
    val next = items.firstOrNull { !d.status(it, ym).settled }
    val overdue = items.count { d.status(it, ym).st == St.OVERDUE }

    LazyColumn(contentPadding = BottomPad, verticalArrangement = Arrangement.spacedBy(0.dp)) {
        if (overdue > 1) item {
            Text("$overdue payments are overdue this month", color = p.bad, fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).clip(RoundedCornerShape(14.dp)).background(p.badBg).padding(12.dp))
        }
        item { Hero(d, ym, sm) }
        if (next != null) item {
            val s = d.status(next, ym)
            Spacer(Modifier.height(12.dp))
            CardBox(Modifier.clickable { onPay(next.id) }) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(
                        Modifier.size(52.dp, 56.dp).clip(RoundedCornerShape(14.dp))
                            .background(if (s.st == St.OVERDUE) p.badBg else p.warnBg),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                    ) {
                        Text("${s.due.dayOfMonth}", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = if (s.st == St.OVERDUE) p.bad else p.warn, lineHeight = 22.sp)
                        Text(YearMonth.from(s.due).shortName().uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (s.st == St.OVERDUE) p.bad else p.warn, letterSpacing = 1.sp)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(if (s.st == St.OVERDUE) "Overdue payment" else "Next payment", fontSize = 12.sp, color = p.muted)
                        Text("${next.title()} · ${next.subtitle()}", fontWeight = FontWeight.Bold, color = p.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(if (next.auto) "Bank debits your card automatically" else "Pay it yourself", fontSize = 12.sp, color = p.muted, maxLines = 1)
                    }
                    Text(fmt(s.amount), style = NumStyle.copy(fontSize = 14.sp), color = p.ink)
                }
            }
        } else if (items.isNotEmpty()) item {
            Spacer(Modifier.height(12.dp))
            CardBox { Text("Everything for ${ym.label()} is settled.", color = p.good, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp)) }
        }
        item { SectionHeader("Credits", fmt(credits.sumOf { d.status(it, ym).amount })) }
        item {
            CardBox {
                if (credits.isEmpty()) EmptyNote("No credit payments this month. Tap Add to create one.")
                credits.forEachIndexed { i, it -> if (i > 0) LineDivider(); ItemRow(d, it, ym) { onPay(it.id) } }
            }
        }
        item { SectionHeader("Bills", fmt(bills.sumOf { d.status(it, ym).amount })) }
        item {
            CardBox {
                if (bills.isEmpty()) EmptyNote("No bills yet. Tap Add for electricity, gas, internet and more.")
                bills.forEachIndexed { i, it -> if (i > 0) LineDivider(); ItemRow(d, it, ym) { onPay(it.id) } }
            }
        }
        item {
            Text("Tap a payment to mark it paid or change the amount. Auto payments count as paid once their date passes.",
                fontSize = 12.sp, color = p.muted, modifier = Modifier.padding(top = 12.dp, start = 4.dp, end = 4.dp))
        }
    }
}

@Composable
fun Hero(d: AppData, ym: YearMonth, sm: Summary) {
    val p = LocalPalette.current
    val tot = maxOf(1L, sm.total).toFloat()
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF3556DA), p.lapisDeep)))
            .padding(20.dp),
    ) {
        Text("${ym.longName().uppercase()} OUTGOING", color = p.onLapis.copy(alpha = .75f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 6.dp)) {
            Text(fmt(sm.total), color = p.onLapis, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp, style = NumStyle)
            Text(" ${d.currency}", color = p.onLapis.copy(alpha = .75f), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 5.dp))
        }
        Row(Modifier.padding(top = 16.dp).fillMaxWidth().height(8.dp).clip(CircleShape).background(Color.White.copy(alpha = .18f))) {
            if (sm.settled > 0) Box(Modifier.fillMaxHeight().weight(sm.settled / tot).background(Color.White))
            if (sm.spend > 0) Box(Modifier.fillMaxHeight().weight(sm.spend / tot).background(p.gold))
            val rest = 1f - (sm.settled + sm.spend) / tot
            if (rest > 0.001f) Spacer(Modifier.weight(rest))
        }
        Row(Modifier.padding(top = 14.dp).fillMaxWidth()) {
            HeroStat("Paid", sm.settled, Color.White, Modifier.weight(1f))
            HeroStat("Still due", sm.due, Color.White.copy(alpha = .3f), Modifier.weight(1f))
            HeroStat("Spent", sm.spend, p.gold, Modifier.weight(1f))
        }
    }
}

@Composable
private fun HeroStat(label: String, v: Long, dot: Color, modifier: Modifier) {
    val p = LocalPalette.current
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(dot))
            Text("  $label", color = p.onLapis.copy(alpha = .75f), fontSize = 11.sp)
        }
        Text(fmt(v), color = p.onLapis, style = NumStyle.copy(fontSize = 14.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/* ================= Calendar ================= */

@Composable
fun CalendarScreen(d: AppData, ym: YearMonth, onPay: (String) -> Unit, onDeleteExpense: (String) -> Unit) {
    val p = LocalPalette.current
    val today = LocalDate.now()
    var sel by remember(ym) { mutableIntStateOf(if (ym == YearMonth.now()) today.dayOfMonth else 1) }
    val items = d.monthItems(ym)
    val byDay = items.groupBy { dueDate(ym, it.day).dayOfMonth }
    val exps = d.monthExpenses(ym)
    val expDays = exps.map { it.date.dayOfMonth }.toSet()
    val offset = ym.atDay(1).dayOfWeek.value - 1
    val n = ym.lengthOfMonth()
    val cells = List(offset) { 0 } + (1..n).toList()

    LazyColumn(contentPadding = BottomPad) {
        item {
            CardBox {
                Column(Modifier.padding(12.dp)) {
                    Row(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                        listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su").forEach {
                            Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = p.muted)
                        }
                    }
                    cells.chunked(7).forEach { week ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            week.forEach { day ->
                                Box(Modifier.weight(1f).aspectRatio(1f)) {
                                    if (day > 0) {
                                        val isSel = day == sel
                                        val isToday = ym == YearMonth.now() && day == today.dayOfMonth
                                        Column(
                                            Modifier.fillMaxSize().padding(2.dp).clip(RoundedCornerShape(12.dp))
                                                .background(if (isSel) p.lapis else Color.Transparent)
                                                .then(if (isToday && !isSel) Modifier.border(1.5.dp, p.lapis, RoundedCornerShape(12.dp)) else Modifier)
                                                .clickable { sel = day },
                                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                                        ) {
                                            Text("$day", fontWeight = FontWeight.SemiBold, color = if (isSel) p.onLapis else p.ink, fontSize = 14.sp)
                                            Row(Modifier.height(6.dp).padding(top = 1.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                byDay[day].orEmpty().take(3).forEach { it ->
                                                    val st = d.status(it, ym).st
                                                    val c = when {
                                                        isSel -> Color.White
                                                        st == St.OVERDUE -> p.bad
                                                        st == St.PAID || st == St.AUTO -> p.good
                                                        st == St.TODAY -> p.warn
                                                        else -> p.lapis
                                                    }
                                                    Box(Modifier.size(5.dp).clip(CircleShape).background(c))
                                                }
                                                if (day in expDays) Box(Modifier.size(5.dp).clip(CircleShape).background(if (isSel) Color.White else p.gold))
                                            }
                                        }
                                    }
                                }
                            }
                            repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                    Row(Modifier.padding(top = 8.dp, start = 4.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Legend("Upcoming", p.lapis); Legend("Paid", p.good); Legend("Overdue", p.bad); Legend("Spending", p.gold)
                    }
                }
            }
        }
        val its = byDay[sel].orEmpty()
        val ex = exps.filter { it.date.dayOfMonth == sel }
        item { SectionHeader("$sel ${ym.longName()}", fmt(its.sumOf { d.status(it, ym).amount } + ex.sumOf { it.amount })) }
        item {
            CardBox {
                if (its.isEmpty() && ex.isEmpty()) EmptyNote("Nothing due or spent on this day.")
                its.forEachIndexed { i, it -> if (i > 0) LineDivider(); ItemRow(d, it, ym) { onPay(it.id) } }
                ex.forEachIndexed { i, e -> if (i > 0 || its.isNotEmpty()) LineDivider(); ExpenseRow(e) { onDeleteExpense(e.id) } }
            }
        }
    }
}

@Composable
private fun Legend(label: String, c: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(c))
        Text(" $label", fontSize = 11.sp, color = LocalPalette.current.muted)
    }
}

/* ================= Spending ================= */

@Composable
fun SpendScreen(d: AppData, ym: YearMonth, onAdd: (Expense) -> Unit, onDelete: (String) -> Unit) {
    val p = LocalPalette.current
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf("food") }
    var err by remember { mutableStateOf<String?>(null) }
    val exps = d.monthExpenses(ym).sortedByDescending { it.date }
    val total = exps.sumOf { it.amount }
    val byCat = exps.groupBy { it.cat }.mapValues { (_, v) -> v.sumOf { it.amount } }.toList().sortedByDescending { it.second }

    fun submit() {
        val a = parseAmount(amount)
        if (a == null || a <= 0) { err = "Enter an amount above zero."; return }
        val date = if (ym == YearMonth.now()) LocalDate.now() else ym.atDay(1)
        onAdd(Expense(amount = a, cat = cat, note = note.trim(), date = date))
        amount = ""; note = ""; err = null
    }

    LazyColumn(contentPadding = BottomPad) {
        item {
            CardBox {
                Column(Modifier.padding(14.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        AmountField(amount, { amount = it; err = null }, "Amount", Modifier.weight(1f))
                        OutlinedTextField(
                            note, { note = it }, Modifier.weight(1.2f), placeholder = { Text("What for?") }, singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { submit() }),
                        )
                    }
                    Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SPEND_CATS.forEach { c ->
                            FilterChip(
                                selected = cat == c.key, onClick = { cat = c.key },
                                label = { Text(c.label, fontWeight = FontWeight.SemiBold) },
                                leadingIcon = { Box(Modifier.size(8.dp).clip(CircleShape).background(Color(c.color))) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = p.ink, selectedLabelColor = p.bg),
                            )
                        }
                    }
                    if (err != null) Text(err!!, color = p.bad, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Button(onClick = { submit() }, Modifier.fillMaxWidth().padding(top = 8.dp).height(48.dp), shape = RoundedCornerShape(14.dp)) {
                        Text("Add expense", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        item { SectionHeader("Spent in ${ym.longName()}", "${fmt(total)} ${d.currency}") }
        if (byCat.isNotEmpty()) item {
            CardBox {
                Column(Modifier.padding(vertical = 6.dp)) {
                    val top = byCat.first().second.toFloat()
                    byCat.forEach { (k, v) -> HBar(spendCat(k).label, v / top, Color(spendCat(k).color), short(v)) }
                }
            }
        }
        if (exps.isEmpty()) item {
            Spacer(Modifier.height(12.dp)); CardBox { EmptyNote("No spending logged for ${ym.label()}. Type an amount above and tap Add expense.") }
        }
        exps.groupBy { it.date }.toSortedMap(Comparator.reverseOrder()).forEach { (date, list) ->
            item {
                val lbl = when (date) {
                    LocalDate.now() -> "Today"
                    LocalDate.now().minusDays(1) -> "Yesterday"
                    else -> date.shortLabel()
                }
                Text("${lbl.uppercase()} · ${fmt(list.sumOf { it.amount })}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = p.muted,
                    letterSpacing = 0.8.sp, modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 6.dp))
            }
            item {
                CardBox { list.forEachIndexed { i, e -> if (i > 0) LineDivider(); ExpenseRow(e) { onDelete(e.id) } } }
            }
        }
    }
}

@Composable
fun HBar(label: String, frac: Float, color: Color, value: String) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(label, Modifier.width(92.dp), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = p.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Box(Modifier.weight(1f).height(10.dp).clip(CircleShape).background(p.surface2)) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(frac.coerceIn(0.02f, 1f)).clip(CircleShape).background(color))
        }
        Text(value, style = NumStyle.copy(fontSize = 12.sp), color = p.ink)
    }
}

/* ================= Credits & bills ================= */

@Composable
fun PlansScreen(d: AppData, onEdit: (Item) -> Unit, onAdd: (Kind) -> Unit) {
    val p = LocalPalette.current
    var seg by remember { mutableIntStateOf(0) }
    val now = YearMonth.now()
    LazyColumn(contentPadding = BottomPad, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(p.surface2).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("Credits", "Bills").forEachIndexed { i, s ->
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(if (seg == i) p.surface else Color.Transparent)
                            .clickable { seg = i }.padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text(s, fontWeight = FontWeight.Bold, color = if (seg == i) p.ink else p.muted) }
                }
            }
        }
        if (seg == 0) {
            val cr = d.items.filter { it.kind == Kind.CREDIT }.sortedBy { it.day }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("Debt left", fmt(cr.sumOf { d.progress(it).remaining }), Modifier.weight(1f))
                    StatTile("Per month now", fmt(cr.filter { it.activeIn(now) }.sumOf { it.amount }), Modifier.weight(1f))
                }
            }
            if (cr.isEmpty()) item { CardBox { EmptyNote("No credits yet.") }; Spacer(Modifier.height(8.dp)); AddLink("Add a credit") { onAdd(Kind.CREDIT) } }
            items(cr, key = { c -> c.id }) { c -> CreditCard(d, c) { onEdit(c) } }
            item { Text("Months before today count as paid. This month counts once you mark it paid or the bank debits it.", fontSize = 12.sp, color = p.muted, modifier = Modifier.padding(horizontal = 4.dp)) }
        } else {
            val bl = d.items.filter { it.kind == Kind.BILL }.sortedBy { it.day }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("Bills per month", fmt(bl.sumOf { it.amount }), Modifier.weight(1f))
                    StatTile("Auto paid", "${bl.count { it.auto }} of ${bl.size}", Modifier.weight(1f))
                }
            }
            item {
                CardBox {
                    if (bl.isEmpty()) EmptyNote("No bills yet.")
                    bl.forEachIndexed { i, it ->
                        if (i > 0) LineDivider()
                        Row(
                            Modifier.fillMaxWidth().clickable { onEdit(it) }.padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Ava(initials(it.name), it.color())
                            Column(Modifier.weight(1f)) {
                                Text(it.name + if (it.auto) "  ·  auto" else "", fontWeight = FontWeight.Bold, color = p.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${billCat(it.cat).label} · day ${it.day}", fontSize = 13.sp, color = p.muted)
                            }
                            Text(fmt(it.amount), style = NumStyle.copy(fontSize = 14.sp), color = p.ink)
                        }
                    }
                }
            }
            if (bl.isEmpty()) item { AddLink("Add a bill") { onAdd(Kind.BILL) } }
        }
    }
}

@Composable
private fun AddLink(text: String, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(14.dp)) { Text(text, fontWeight = FontWeight.Bold) }
}

@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Column(modifier.clip(CardShape).background(p.surface).border(1.dp, p.line, CardShape).padding(14.dp)) {
        Text(label, fontSize = 12.sp, color = p.muted, fontWeight = FontWeight.SemiBold)
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = p.ink, style = NumStyle, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
fun CreditCard(d: AppData, it: Item, onClick: () -> Unit) {
    val p = LocalPalette.current
    val pr = d.progress(it)
    CardBox(Modifier.clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Ava(initials(it.bank), it.color(), 44.dp)
                Column(Modifier.weight(1f)) {
                    Text(it.bank, fontWeight = FontWeight.Bold, color = p.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${it.name.ifBlank { "Credit" }} · day ${it.day}${if (it.auto) " · auto debit" else ""}", fontSize = 12.sp, color = p.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(fmt(it.amount), style = NumStyle.copy(fontSize = 14.sp), color = p.ink)
                    Text("per month", fontSize = 11.sp, color = p.muted)
                }
            }
            Box(Modifier.padding(top = 14.dp).fillMaxWidth().height(6.dp).clip(CircleShape).background(p.surface2)) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(pr.pct.coerceIn(0f, 1f)).clip(CircleShape).background(p.lapis))
            }
            Row(Modifier.padding(top = 12.dp)) {
                MiniStat("Paid", "${pr.done} of ${it.months}", Modifier.weight(1f))
                MiniStat("Left to pay", fmt(pr.remaining), Modifier.weight(1f))
                MiniStat("Debt free", pr.end.label(), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier) {
    val p = LocalPalette.current
    Column(modifier) {
        Text(label, fontSize = 11.sp, color = p.muted, fontWeight = FontWeight.SemiBold)
        Text(value, style = NumStyle.copy(fontSize = 13.sp), color = p.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/* ================= Insights ================= */

@Composable
fun InsightsScreen(d: AppData, ym: YearMonth) {
    val p = LocalPalette.current
    val months = (5 downTo 0).map { ym.minusMonths(it.toLong()) }
    val sums = months.map { d.summary(it) }
    val max = maxOf(1L, sums.maxOf { it.total }).toFloat()
    val sm = sums.last()
    val diff = sm.total - sums[4].total
    val billBlue = Color(0xFF5DA9E9)

    LazyColumn(contentPadding = BottomPad) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("Total for ${ym.shortName()}", fmt(sm.total), Modifier.weight(1f))
                Column(Modifier.weight(1f).clip(CardShape).background(p.surface).border(1.dp, p.line, CardShape).padding(14.dp)) {
                    Text("vs ${months[4].shortName()}", fontSize = 12.sp, color = p.muted, fontWeight = FontWeight.SemiBold)
                    Text((if (diff > 0) "+" else "") + fmt(diff), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, style = NumStyle,
                        color = if (diff > 0) p.bad else p.good, maxLines = 1, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
        item { SectionHeader("Last 6 months", "in ${d.currency}") }
        item {
            CardBox {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth().height(150.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
                        sums.forEachIndexed { i, s ->
                            val h = (s.total / max).coerceAtLeast(0.03f)
                            Column(
                                Modifier.weight(1f).fillMaxHeight(h).clip(RoundedCornerShape(8.dp)).background(p.surface2)
                                    .then(if (months[i] == ym) Modifier.border(2.dp, p.lapis, RoundedCornerShape(8.dp)) else Modifier),
                            ) {
                                if (s.spend > 0) Box(Modifier.fillMaxWidth().weight(s.spend.toFloat()).background(p.gold))
                                if (s.bills > 0) Box(Modifier.fillMaxWidth().weight(s.bills.toFloat()).background(billBlue))
                                if (s.credits > 0) Box(Modifier.fillMaxWidth().weight(s.credits.toFloat()).background(p.lapis))
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        months.forEachIndexed { i, m ->
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(m.shortName(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = p.muted)
                                Text(short(sums[i].total), style = NumStyle.copy(fontSize = 10.sp), color = p.muted)
                            }
                        }
                    }
                    Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Legend("Credits", p.lapis); Legend("Bills", billBlue); Legend("Spending", p.gold)
                    }
                }
            }
        }
        val rows = buildList {
            add(Triple("Credits", sm.credits, p.lapis)); add(Triple("Bills", sm.bills, billBlue))
            d.monthExpenses(ym).groupBy { it.cat }.forEach { (k, v) -> add(Triple(spendCat(k).label, v.sumOf { it.amount }, Color(spendCat(k).color))) }
        }.filter { it.second > 0 }.sortedByDescending { it.second }
        item { SectionHeader("Where it went", ym.label()) }
        item {
            CardBox {
                Column(Modifier.padding(vertical = 6.dp)) {
                    if (rows.isEmpty()) EmptyNote("Nothing recorded for this month.")
                    val top = rows.firstOrNull()?.second?.toFloat() ?: 1f
                    rows.forEach { (l, v, c) -> HBar(l, v / top, c, "${if (sm.total > 0) v * 100 / sm.total else 0}%") }
                }
            }
        }
        val cr = d.items.filter { it.kind == Kind.CREDIT && d.progress(it).left > 0 }.sortedBy { d.progress(it).end }
        if (cr.isNotEmpty()) {
            item { SectionHeader("Debt free timeline") }
            item {
                CardBox {
                    cr.forEachIndexed { i, it ->
                        if (i > 0) LineDivider()
                        val pr = d.progress(it)
                        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Ava(initials(it.bank), it.color())
                            Column(Modifier.weight(1f)) {
                                Text(it.bank, fontWeight = FontWeight.Bold, color = p.ink)
                                Text("${pr.left} payment${if (pr.left > 1) "s" else ""} left", fontSize = 13.sp, color = p.muted)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(pr.end.label(), style = NumStyle.copy(fontSize = 14.sp), color = p.ink)
                                Text("frees ${short(it.amount)}/mo", fontSize = 12.sp, color = p.muted)
                            }
                        }
                    }
                }
            }
        }
    }
}

/* ================= amount field ================= */

@Composable
fun AmountField(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    val shown = parseAmount(value)?.let { fmt(it) } ?: ""
    OutlinedTextField(
        value = androidx.compose.ui.text.input.TextFieldValue(shown, androidx.compose.ui.text.TextRange(shown.length)),
        onValueChange = { tf -> onChange(tf.text.filter { it.isDigit() }.take(13)) },
        modifier = modifier,
        placeholder = { Text(label) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        textStyle = NumStyle.copy(fontSize = 16.sp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
    )
}
