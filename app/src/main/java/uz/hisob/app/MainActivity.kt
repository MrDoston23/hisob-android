package uz.hisob.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.YearMonth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = Store(applicationContext)
        Reminders.ensureChannel(this)
        Reminders.schedule(this)
        setContent { HisobTheme { App(store) } }
    }
}

sealed class SheetReq {
    data object Chooser : SheetReq()
    data class Edit(val kind: Kind, val item: Item?) : SheetReq()
    data class Pay(val id: String) : SheetReq()
    data object Settings : SheetReq()
}

private data class Tab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val TABS = listOf(
    Tab("Month", Icons.Rounded.CalendarMonth),
    Tab("Calendar", Icons.Rounded.Event),
    Tab("Spending", Icons.Rounded.ShoppingBag),
    Tab("Credits", Icons.Rounded.CreditCard),
    Tab("Insights", Icons.Rounded.BarChart),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(store: Store) {
    val p = LocalPalette.current
    var tab by remember { mutableIntStateOf(0) }
    var ym by remember { mutableStateOf(YearMonth.now()) }
    var sheet by remember { mutableStateOf<SheetReq?>(null) }
    val snack = remember { SnackbarHostState() }
    var toast by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(toast) { toast?.let { snack.showSnackbar(it); toast = null } }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    val data = store.data

    Scaffold(
        containerColor = p.bg,
        snackbarHost = { SnackbarHost(snack) },
        topBar = {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 12.dp, top = 10.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (tab == 0) "Hisob" else TABS[tab].label,
                    style = MaterialTheme.typography.headlineMedium, color = p.ink, modifier = Modifier.weight(1f),
                )
                MonthSwitch(ym, onPrev = { ym = ym.minusMonths(1) }, onNext = { ym = ym.plusMonths(1) }, onReset = { ym = YearMonth.now() })
                IconButton(onClick = { sheet = SheetReq.Settings }) { Icon(Icons.Rounded.Settings, "Settings", tint = p.muted) }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = p.surface, tonalElevation = 0.dp) {
                TABS.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = tab == i, onClick = { tab = i },
                        icon = { Icon(t.icon, null) },
                        label = { Text(t.label, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = p.lapis, selectedTextColor = p.lapis, indicatorColor = p.infoBg,
                            unselectedIconColor = p.muted, unselectedTextColor = p.muted,
                        ),
                    )
                }
            }
        },
        floatingActionButton = {
            if (tab != 2) ExtendedFloatingActionButton(
                onClick = { sheet = SheetReq.Chooser },
                containerColor = p.lapis, contentColor = p.onLapis,
                icon = { Icon(Icons.Rounded.Add, null) }, text = { Text("Add", fontWeight = FontWeight.Bold) },
            )
        },
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                0 -> MonthScreen(data, ym, onPay = { sheet = SheetReq.Pay(it) })
                1 -> CalendarScreen(data, ym, onPay = { sheet = SheetReq.Pay(it) },
                    onDeleteExpense = { id -> store.update { d -> d.copy(expenses = d.expenses.filterNot { it.id == id }) }; toast = "Deleted" })
                2 -> SpendScreen(data, ym,
                    onAdd = { e -> store.update { d -> d.copy(expenses = d.expenses + e) }; toast = "Added ${fmt(e.amount)} ${data.currency}" },
                    onDelete = { id -> store.update { d -> d.copy(expenses = d.expenses.filterNot { it.id == id }) }; toast = "Deleted" })
                3 -> PlansScreen(data, onEdit = { sheet = SheetReq.Edit(it.kind, it) }, onAdd = { sheet = SheetReq.Edit(it, null) })
                4 -> InsightsScreen(data, ym)
            }
        }
    }

    sheet?.let { req ->
        ModalBottomSheet(
            onDismissRequest = { sheet = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = p.surface,
        ) {
            when (req) {
                SheetReq.Chooser -> ChooserSheet { k ->
                    sheet = null
                    when (k) {
                        null -> tab = 2
                        else -> sheet = SheetReq.Edit(k, null)
                    }
                }
                is SheetReq.Edit -> EditSheet(
                    kind = req.kind, item = req.item,
                    onSave = { it2 ->
                        store.update { d -> d.copy(items = d.items.filterNot { it.id == it2.id } + it2) }
                        sheet = null; toast = if (req.item == null) "Added" else "Saved"
                    },
                    onDelete = { id ->
                        store.update { d -> d.copy(items = d.items.filterNot { it.id == id }, paid = d.paid.mapValues { (_, m) -> m - id }) }
                        sheet = null; toast = "Deleted"
                    },
                    onCancel = { sheet = null },
                )
                is SheetReq.Pay -> {
                    val item = data.items.firstOrNull { it.id == req.id }
                    if (item != null) PaySheet(data, item, ym,
                        onPaid = { paid ->
                            store.update { d -> d.copy(paid = d.paid + (ym to ((d.paid[ym] ?: emptyMap()) + (item.id to paid)))) }
                            sheet = null; toast = "Marked paid"
                        },
                        onUnpaid = {
                            store.update { d -> d.copy(paid = d.paid + (ym to ((d.paid[ym] ?: emptyMap()) - item.id))) }
                            sheet = null; toast = "Marked unpaid"
                        },
                        onEdit = { sheet = SheetReq.Edit(item.kind, item) },
                    )
                }
                SheetReq.Settings -> SettingsSheet(data) { cur, hour ->
                    store.update { d -> d.copy(currency = cur, remindHour = hour) }
                    sheet = null; toast = "Saved"
                }
            }
        }
    }
}

@Composable
private fun MonthSwitch(ym: YearMonth, onPrev: () -> Unit, onNext: () -> Unit, onReset: () -> Unit) {
    val p = LocalPalette.current
    Surface(shape = MaterialTheme.shapes.extraLarge, color = p.surface, border = androidx.compose.foundation.BorderStroke(1.dp, p.line)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrev, modifier = Modifier.size(36.dp)) { Icon(Icons.Rounded.ChevronLeft, "Previous month", tint = p.muted) }
            TextButton(onClick = onReset, contentPadding = PaddingValues(horizontal = 2.dp)) {
                Text(ym.label(), fontWeight = FontWeight.Bold, color = p.ink, style = NumStyle.copy(fontSize = 14.sp))
            }
            IconButton(onClick = onNext, modifier = Modifier.size(36.dp)) { Icon(Icons.Rounded.ChevronRight, "Next month", tint = p.muted) }
        }
    }
}
