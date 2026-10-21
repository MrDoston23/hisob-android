package uz.hisob.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth

@Composable
private fun SheetBody(title: String, sub: String?, content: @Composable ColumnScope.() -> Unit) {
    val p = LocalPalette.current
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding()
            .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column {
            Text(title, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold), color = p.ink)
            if (sub != null) Text(sub, color = p.muted, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp))
        }
        content()
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LocalPalette.current.muted, modifier = Modifier.padding(bottom = 4.dp))
}

@Composable
private fun SwitchRow(title: String, sub: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val p = LocalPalette.current
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(p.bg).border(1.dp, p.line, RoundedCornerShape(14.dp))
            .clickable { onChange(!checked) }.padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, color = p.ink)
            Text(sub, fontSize = 12.sp, color = p.muted)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun Stepper(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Column(modifier) {
        FieldLabel(label)
        Row(
            Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, p.line, RoundedCornerShape(12.dp)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onMinus) { Icon(Icons.Rounded.Remove, "Less", tint = p.muted) }
            Text(value, Modifier.weight(1f), style = NumStyle.copy(fontSize = 15.sp), color = p.ink, textAlign = androidx.compose.ui.text.style.TextAlign.Center, maxLines = 1)
            IconButton(onClick = onPlus) { Icon(Icons.Rounded.Add, "More", tint = p.muted) }
        }
    }
}

/* ---------- chooser ---------- */

@Composable
fun ChooserSheet(onPick: (Kind?) -> Unit) {
    SheetBody("Add", "What do you want to track?") {
        ChoiceRow(Icons.Rounded.ShoppingBag, 0xFFC08A1E, "Expense", "Something you just paid for") { onPick(null) }
        ChoiceRow(Icons.Rounded.CreditCard, 0xFF2443C4, "Credit", "A bank loan or installment, paid monthly") { onPick(Kind.CREDIT) }
        ChoiceRow(Icons.Rounded.Bolt, 0xFF1BA39C, "Bill", "Electricity, gas, internet, phone and more") { onPick(Kind.BILL) }
    }
}

@Composable
private fun ChoiceRow(icon: ImageVector, color: Long, title: String, sub: String, onClick: () -> Unit) {
    val p = LocalPalette.current
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(p.bg).border(1.dp, p.line, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(Color(color)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Color.White)
        }
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, color = p.ink)
            Text(sub, fontSize = 13.sp, color = p.muted)
        }
    }
}

/* ---------- add / edit credit or bill ---------- */

@Composable
fun EditSheet(kind: Kind, item: Item?, onSave: (Item) -> Unit, onDelete: (String) -> Unit, onCancel: () -> Unit) {
    val p = LocalPalette.current
    val isNew = item == null
    var bank by remember { mutableStateOf(item?.bank ?: "") }
    var name by remember { mutableStateOf(item?.name ?: "") }
    var cat by remember { mutableStateOf(item?.cat ?: "electricity") }
    var amount by remember { mutableStateOf(item?.amount?.toString() ?: "") }
    var day by remember { mutableIntStateOf(item?.day ?: if (kind == Kind.CREDIT) 5 else 10) }
    var start by remember { mutableStateOf(item?.start ?: YearMonth.now()) }
    var months by remember { mutableIntStateOf(item?.months ?: 12) }
    var auto by remember { mutableStateOf(item?.auto ?: false) }
    var err by remember { mutableStateOf<String?>(null) }
    var armDelete by remember { mutableStateOf(false) }

    val title = (if (isNew) "New " else "Edit ") + if (kind == Kind.CREDIT) "credit" else "bill"
    val sub = if (kind == Kind.CREDIT) "Bank, monthly payment and the day it is due." else "Usual amount and the day it is due each month."

    SheetBody(title, sub) {
        if (kind == Kind.CREDIT) {
            Column {
                FieldLabel("Bank")
                OutlinedTextField(bank, { bank = it; err = null }, Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp), placeholder = { Text("Kapitalbank") })
                Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Kapitalbank", "Hamkorbank", "Ipoteka", "TBC", "Anorbank", "Uzum Nasiya", "Asakabank").forEach { b ->
                        AssistChip(onClick = { bank = b; err = null }, label = { Text(b) })
                    }
                }
            }
            Column {
                FieldLabel("What is it for")
                OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp), placeholder = { Text("Car loan, phone, microloan") })
            }
        } else {
            Column {
                FieldLabel("Name")
                OutlinedTextField(name, { name = it; err = null }, Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp), placeholder = { Text("Electricity") })
                Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    BILL_CATS.forEach { c ->
                        FilterChip(
                            selected = cat == c.key,
                            onClick = { cat = c.key; if (name.isBlank() || BILL_CATS.any { it.label == name }) name = c.label },
                            label = { Text(c.label) },
                            leadingIcon = { Box(Modifier.size(8.dp).clip(CircleShape).background(Color(c.color))) },
                        )
                    }
                }
            }
        }
        Column {
            FieldLabel(if (kind == Kind.CREDIT) "Monthly payment" else "Usual amount")
            AmountField(amount, { amount = it; err = null }, "0", Modifier.fillMaxWidth())
        }
        Stepper("Due day of the month", "Day $day", { if (day > 1) day-- }, { if (day < 31) day++ })
        if (kind == Kind.CREDIT) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Stepper("First payment", start.label(), { start = start.minusMonths(1) }, { start = start.plusMonths(1) }, Modifier.weight(1f))
                Stepper("Total months", "$months", { if (months > 1) months-- }, { if (months < 600) months++ }, Modifier.weight(1f))
            }
            val total = (parseAmount(amount) ?: 0L) * months
            if (total > 0) Text(
                "$months payments, debt free in ${start.plusMonths((months - 1).toLong()).label()}. Total ${fmt(total)}.",
                color = p.lapisDeep.takeIf { p == LightPalette } ?: p.ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(p.infoBg).padding(12.dp),
            )
        }
        SwitchRow("Bank takes it automatically", "Counts as paid once the date passes", auto) { auto = it }
        if (err != null) Text(err!!, color = p.bad, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!isNew) OutlinedButton(
                onClick = { if (armDelete) onDelete(item!!.id) else armDelete = true },
                Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = p.bad),
            ) { Text(if (armDelete) "Tap to confirm" else "Delete", fontWeight = FontWeight.Bold, maxLines = 1) }
            else OutlinedButton(onClick = onCancel, Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(14.dp)) { Text("Cancel", fontWeight = FontWeight.Bold) }
            Button(
                onClick = {
                    val a = parseAmount(amount)
                    when {
                        kind == Kind.CREDIT && bank.isBlank() -> err = "Enter the bank name."
                        kind == Kind.BILL && name.isBlank() -> err = "Give this bill a name."
                        a == null || a <= 0 -> err = "Enter the amount."
                        else -> {
                            val now = YearMonth.now()
                            val since = item?.since ?: if (kind == Kind.CREDIT && start.isAfter(now)) start else now
                            onSave(
                                Item(
                                    id = item?.id ?: java.util.UUID.randomUUID().toString(), kind = kind,
                                    name = name.trim(), bank = bank.trim(), cat = if (kind == Kind.BILL) cat else "other",
                                    amount = a, day = day, auto = auto, start = start, months = months, since = since,
                                )
                            )
                        }
                    }
                },
                Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(14.dp),
            ) { Text(if (isNew) "Save" else "Save changes", fontWeight = FontWeight.Bold) }
        }
    }
}

/* ---------- mark paid ---------- */

@Composable
fun PaySheet(d: AppData, item: Item, ym: YearMonth, onPaid: (Paid) -> Unit, onUnpaid: () -> Unit, onEdit: () -> Unit) {
    val p = LocalPalette.current
    val s = d.status(item, ym)
    val rec = d.paid[ym]?.get(item.id)
    var amount by remember { mutableStateOf((rec?.amount ?: item.amount).toString()) }
    var err by remember { mutableStateOf<String?>(null) }
    val sub = "${item.subtitle()} · due ${s.due.shortLabel()}" + if (item.auto) " · bank debits automatically" else ""

    SheetBody(item.title(), sub) {
        Column {
            FieldLabel("Amount paid")
            AmountField(amount, { amount = it; err = null }, "0", Modifier.fillMaxWidth())
        }
        if (rec != null) Text("Paid on ${rec.date.shortLabel()}", color = p.good, fontWeight = FontWeight.SemiBold)
        if (err != null) Text(err!!, color = p.bad, fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (rec != null) OutlinedButton(onClick = onUnpaid, Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(14.dp)) { Text("Mark unpaid", fontWeight = FontWeight.Bold) }
            else OutlinedButton(onClick = onEdit, Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(14.dp)) { Text("Edit details", fontWeight = FontWeight.Bold) }
            Button(
                onClick = {
                    val a = parseAmount(amount)
                    if (a == null) err = "Enter the amount you paid."
                    else onPaid(Paid(a, if (ym == YearMonth.now()) LocalDate.now() else s.due))
                },
                Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(14.dp),
            ) { Text(if (rec != null) "Update" else "Mark paid", fontWeight = FontWeight.Bold) }
        }
    }
}

/* ---------- settings ---------- */

@Composable
fun SettingsSheet(d: AppData, onSave: (String, Int) -> Unit) {
    val p = LocalPalette.current
    var cur by remember { mutableStateOf(d.currency) }
    var hour by remember { mutableIntStateOf(d.remindHour) }
    SheetBody("Settings", "Your data stays on this phone.") {
        Column {
            FieldLabel("Currency label")
            OutlinedTextField(cur, { cur = it.take(6) }, Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp))
        }
        Stepper("Daily reminder time", "%02d:00".format(hour), { hour = (hour + 23) % 24 }, { hour = (hour + 1) % 24 })
        Text("You get a reminder the day before and on the day each payment is due, including auto debits, so your card has enough money.",
            fontSize = 12.sp, color = p.muted)
        Button(onClick = { onSave(cur.trim().ifBlank { "so'm" }, hour) }, Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)) {
            Text("Save", fontWeight = FontWeight.Bold)
        }
    }
}
