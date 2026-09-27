package com.example.ui.component

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordPressManagerDialog(
    initialSiteUrl: String,
    initialUsername: String,
    initialAppPassword: String,
    initialConsumerKey: String,
    initialConsumerSecret: String,
    connectionStatus: WpConnectionStatus?,
    orders: List<WcOrder>,
    posts: List<WpPost>,
    salesReport: WcSalesReport?,
    isLoading: Boolean,
    onSaveConfig: (url: String, user: String, pass: String, ck: String, cs: String) -> Unit,
    onRefresh: () -> Unit,
    onCreatePost: (title: String, content: String, status: String, scheduledDateIso: String?) -> Unit,
    onCreateCoupon: (code: String, amount: String, type: String) -> Unit,
    onUpdateOrderStatus: (orderId: Long, status: String) -> Unit,
    onAddOrderToSchedule: (order: WcOrder) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(if (initialSiteUrl.isNotBlank()) 0 else 3) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .testTag("wordpress_manager_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = PanelBackground),
            border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF21759B).copy(alpha = 0.2f))
                                .border(1.dp, Color(0xFF21759B), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("W", fontWeight = FontWeight.Black, fontSize = 22.sp, color = CyanAccent)
                        }
                        Column {
                            Text(
                                text = "مدیریت وردپرس و ووکامرس",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = InkLight
                            )
                            val statusText = if (connectionStatus?.isConnected == true) {
                                "🟢 متصل به ${connectionStatus.siteName ?: ""}"
                            } else {
                                "⚪ نیاز به تنظیم یا بررسی اتصال"
                            }
                            Text(text = statusText, fontSize = 11.sp, color = TextMuted)
                        }
                    }

                    Row {
                        IconButton(onClick = onRefresh) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "تازه‌سازی", tint = CyanAccent)
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "بستن", tint = TextMuted)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tab Selector
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = PanelRaised,
                    contentColor = CyanAccent,
                    indicator = {},
                    divider = {}
                ) {
                    val tabs = listOf("🛒 ووکامرس", "📝 مقالات", "🎟️ تخفیف", "⚙️ اتصال")
                    tabs.forEachIndexed { index, title ->
                        val isSelected = selectedTab == index
                        Tab(
                            selected = isSelected,
                            onClick = { selectedTab = index },
                            modifier = Modifier
                                .padding(vertical = 4.dp, horizontal = 2.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) AmberPrimary.copy(alpha = 0.2f) else Color.Transparent)
                        ) {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) AmberPrimary else TextMuted,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = AmberPrimary,
                        trackColor = BorderLine
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Tab Content
                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        0 -> WooCommerceOrdersTab(
                            orders = orders,
                            salesReport = salesReport,
                            onUpdateStatus = onUpdateOrderStatus,
                            onAddToSchedule = onAddOrderToSchedule
                        )
                        1 -> WordPressPostsTab(
                            posts = posts,
                            onCreatePost = onCreatePost
                        )
                        2 -> WooCommerceCouponsTab(
                            onCreateCoupon = onCreateCoupon
                        )
                        3 -> WordPressSettingsTab(
                            siteUrl = initialSiteUrl,
                            username = initialUsername,
                            appPassword = initialAppPassword,
                            consumerKey = initialConsumerKey,
                            consumerSecret = initialConsumerSecret,
                            connectionStatus = connectionStatus,
                            onSave = onSaveConfig
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WooCommerceOrdersTab(
    orders: List<WcOrder>,
    salesReport: WcSalesReport?,
    onUpdateStatus: (orderId: Long, status: String) -> Unit,
    onAddToSchedule: (order: WcOrder) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Sales Report Card
        if (salesReport != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PanelRaised),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AmberPrimary.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "📊 خلاصه فروش ماه جاری ووکامرس",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = AmberPrimary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            StatItem("مجموع فروش", "${salesReport.totalSales} ت")
                            StatItem("تعداد سفارشات", "${salesReport.totalOrders}")
                            StatItem("میانگین خرید", "${salesReport.averageSales} ت")
                        }
                    }
                }
            }
        }

        if (orders.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "هنوز سفارشی یافت نشد یا سایت متصل نیست.\nمی‌توانید با گفتن «سفارش‌های جدید ووکامرس رو بیار» استعلام بگیرید.",
                        color = TextMuted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }
            }
        } else {
            items(orders) { order ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = PanelRaised),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderLine)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "سفارش #${order.number} • ${order.customerName}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = InkLight
                            )
                            StatusBadge(order.status)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "اقلام: ${order.itemsSummary.ifEmpty { "بدون جزئیات کالا" }}",
                            fontSize = 12.sp,
                            color = TextMuted,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "مبلغ: ${order.total} ${order.currency}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AmberPrimary
                            )
                            Text(
                                text = order.dateCreated.take(10),
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Actions Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onAddToSchedule(order) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                            ) {
                                Icon(imageVector = Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("یادآور ارسال", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            if (order.status != "completed") {
                                OutlinedButton(
                                    onClick = { onUpdateStatus(order.id, "completed") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldAccent)
                                ) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("تکمیل شد", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = title, fontSize = 11.sp, color = TextMuted)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = InkLight)
    }
}

@Composable
private fun StatusBadge(status: String) {
    val (color, text) = when (status) {
        "processing" -> CyanAccent to "در حال پردازش"
        "completed" -> EmeraldAccent to "تکمیل‌شده"
        "pending" -> AmberPrimary to "در انتظار پرداخت"
        "on-hold" -> CoralAccent to "در انتظار بررسی"
        else -> TextMuted to status
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text = text, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun WordPressPostsTab(
    posts: List<WpPost>,
    onCreatePost: (title: String, content: String, status: String, scheduledDateIso: String?) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        Button(
            onClick = { showCreateDialog = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = PanelBackground)
            Spacer(modifier = Modifier.width(6.dp))
            Text("ثبت / زمان‌بندی مقاله جدید", fontWeight = FontWeight.Bold, color = PanelBackground, fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (posts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "هنوز مقاله‌ای بارگذاری نشده است.\nمی‌توانید با گفتن صوتی: «یک مقاله درباره ... در سایتم ثبت کن» یا دکمه بالا مقاله بسازید.",
                    color = TextMuted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(posts) { post ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = PanelRaised),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderLine)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = post.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = InkLight,
                                    modifier = Modifier.weight(1f)
                                )
                                val (badgeColor, badgeText) = when (post.status.lowercase()) {
                                    "publish" -> EmeraldAccent to "منتشرشده 🚀"
                                    "future" -> CyanAccent to "زمان‌بندی شده ⏰"
                                    else -> AmberPrimary to "پیش‌نویس 📝"
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(badgeColor.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(text = badgeText, fontSize = 10.sp, color = badgeColor)
                                }
                            }

                            if (!post.excerpt.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = post.excerpt,
                                    fontSize = 12.sp,
                                    color = TextMuted,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (!post.link.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier
                                        .clickable {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(post.link))
                                            context.startActivity(intent)
                                        },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.Default.OpenInBrowser, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("مشاهده در سایت وردپرس", fontSize = 11.sp, color = CyanAccent)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var newTitle by remember { mutableStateOf("") }
        var newContent by remember { mutableStateOf("") }
        var newStatus by remember { mutableStateOf("draft") }
        var scheduleIso by remember { mutableStateOf("2026-09-19T10:00:00") }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("ثبت و زمان‌بندی مقاله در وردپرس", fontWeight = FontWeight.Bold, color = InkLight) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("عنوان مقاله") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newContent,
                        onValueChange = { newContent = it },
                        label = { Text("متن یا نکات مهم") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("وضعیت انتشار در وردپرس:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AmberPrimary)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = newStatus == "draft", onClick = { newStatus = "draft" })
                            Text("پیش‌نویس", fontSize = 11.sp, color = InkLight)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = newStatus == "publish", onClick = { newStatus = "publish" })
                            Text("انتشار فوری", fontSize = 11.sp, color = InkLight)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = newStatus == "future", onClick = { newStatus = "future" })
                            Text("زمان‌بندی", fontSize = 11.sp, color = InkLight)
                        }
                    }

                    if (newStatus == "future") {
                        OutlinedTextField(
                            value = scheduleIso,
                            onValueChange = { scheduleIso = it },
                            label = { Text("تاریخ و ساعت انتشار (فرمت ISO)") },
                            placeholder = { Text("2026-09-19T18:00:00") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTitle.isNotBlank()) {
                            val isoParam = if (newStatus == "future") scheduleIso.trim() else null
                            onCreatePost(newTitle, newContent, newStatus, isoParam)
                            showCreateDialog = false
                        }
                    }
                ) {
                    Text("ارسال به وردپرس")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }
}

@Composable
private fun WooCommerceCouponsTab(
    onCreateCoupon: (code: String, amount: String, type: String) -> Unit
) {
    var couponCode by remember { mutableStateOf("") }
    var couponAmount by remember { mutableStateOf("20") }
    var discountType by remember { mutableStateOf("percent") }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = PanelRaised),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderLine)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🎟️ ساخت سریع کد تخفیف در ووکامرس",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AmberPrimary
                )
                Text(
                    text = "همچنین می‌توانید با گفتن ویس: «کد تخفیف ۱۵ درصدی با نام BAHAR بساز» به صورت صوتی کد تخفیف ایجاد کنید.",
                    fontSize = 11.5.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                OutlinedTextField(
                    value = couponCode,
                    onValueChange = { couponCode = it.uppercase() },
                    label = { Text("کد تخفیف (مثلاً: NOROOZ)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = couponAmount,
                    onValueChange = { couponAmount = it },
                    label = { Text("مقدار یا درصد تخفیف (مثلاً: 20)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = discountType == "percent", onClick = { discountType = "percent" })
                    Text("درصدی (%)", fontSize = 12.sp, color = InkLight)
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(selected = discountType == "fixed_cart", onClick = { discountType = "fixed_cart" })
                    Text("مبلغ ثابت سبد خرید", fontSize = 12.sp, color = InkLight)
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        if (couponCode.isNotBlank() && couponAmount.isNotBlank()) {
                            onCreateCoupon(couponCode, couponAmount, discountType)
                            couponCode = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = PanelBackground)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ایجاد و فعال‌سازی در ووکامرس", fontWeight = FontWeight.Bold, color = PanelBackground)
                }
            }
        }
    }
}

@Composable
private fun WordPressSettingsTab(
    siteUrl: String,
    username: String,
    appPassword: String,
    consumerKey: String,
    consumerSecret: String,
    connectionStatus: WpConnectionStatus?,
    onSave: (url: String, user: String, pass: String, ck: String, cs: String) -> Unit
) {
    var url by remember { mutableStateOf(siteUrl) }
    var user by remember { mutableStateOf(username) }
    var pass by remember { mutableStateOf(appPassword) }
    var ck by remember { mutableStateOf(consumerKey) }
    var cs by remember { mutableStateOf(consumerSecret) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = PanelRaised),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderLine)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "⚙️ مشخصات اتصال به سایت وردپرس",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = InkLight
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("آدرس سایت (مثلاً: https://myshop.com)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = user,
                        onValueChange = { user = it },
                        label = { Text("نام کاربری مدیر (Username)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = pass,
                        onValueChange = { pass = it },
                        label = { Text("گذرواژه کاربردی (Application Password)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "💡 در پیشخوان وردپرس: کاربران > شناسنامه شما > بخش گذرواژه‌های کاربردی",
                        fontSize = 11.sp,
                        color = CyanAccent,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "کلیدهای ووکامرس (اختیاری - در صورت عدم استفاده از Application Password):",
                        fontSize = 11.sp,
                        color = TextMuted
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = ck,
                        onValueChange = { ck = it },
                        label = { Text("Consumer Key (ck_...)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = cs,
                        onValueChange = { cs = it },
                        label = { Text("Consumer Secret (cs_...)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { onSave(url, user, pass, ck, cs) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
                    ) {
                        Text("💾 ذخیره و تست اتصال به سایت", fontWeight = FontWeight.Bold, color = PanelBackground)
                    }
                }
            }
        }
    }
}
