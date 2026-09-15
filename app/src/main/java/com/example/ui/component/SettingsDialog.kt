package com.example.ui.component

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.BorderLine
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.InkLight
import com.example.ui.theme.OnAmber
import com.example.ui.theme.PanelBackground
import com.example.ui.theme.PanelRaised
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextMuted2

@Composable
fun SettingsDialog(
    currentApiKey: String,
    currentDefaultAlarmMinutes: Int,
    onSave: (apiKey: String, defaultAlarmMinutes: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var apiKey by remember { mutableStateOf(currentApiKey) }
    var showApiKey by remember { mutableStateOf(false) }
    var selectedAlarmMinutes by remember { mutableStateOf(currentDefaultAlarmMinutes) }

    val alarmOptions = listOf(
        0 to "در موعد (به موقع)",
        15 to "۱۵ دقیقه قبل",
        30 to "۳۰ دقیقه قبل",
        60 to "۱ ساعت قبل"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = PanelBackground,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderLine),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = AmberPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "تنظیمات دستیار صوتی",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = InkLight
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "بستن",
                            tint = TextMuted
                        )
                    }
                }

                HorizontalDivider(color = BorderLine)

                // API Key Field
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "کلید API پلتفرم GapGPT",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = InkLight
                        )

                        // External link to GapGPT
                        Row(
                            modifier = Modifier.clickable {
                                try {
                                    val browserIntent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://gapgpt.app")
                                    )
                                    context.startActivity(browserIntent)
                                } catch (_: Exception) {}
                            },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "دریافت کلید (gapgpt.app)",
                                fontSize = 11.5.sp,
                                color = CyanAccent
                            )
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = null,
                                tint = CyanAccent,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        placeholder = { Text("sk-...", color = TextMuted2, fontSize = 13.sp) },
                        visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showApiKey = !showApiKey }) {
                                Icon(
                                    imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "نمایش/مخفی‌سازی کلید",
                                    tint = TextMuted
                                )
                            }
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = AmberPrimary
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("api_key_input"),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = PanelRaised,
                            unfocusedContainerColor = PanelRaised,
                            focusedBorderColor = AmberPrimary,
                            unfocusedBorderColor = BorderLine,
                            focusedTextColor = InkLight,
                            unfocusedTextColor = InkLight
                        )
                    )

                    Text(
                        text = "کلید API شما مستقیماً در دستگاه ذخیره می‌شود و برای تبدیل صوت به برنامه کاری استفاده خواهد شد.",
                        fontSize = 11.5.sp,
                        color = TextMuted2,
                        lineHeight = 17.sp
                    )
                }

                HorizontalDivider(color = BorderLine)

                // Default Alarm advance time
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "زمان پیش‌فرض آلارم قبل از موعد",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = InkLight
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        alarmOptions.forEach { (minutes, label) ->
                            val isSelected = selectedAlarmMinutes == minutes
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) AmberPrimary.copy(alpha = 0.15f) else PanelRaised,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) AmberPrimary else BorderLine
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedAlarmMinutes = minutes }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 13.sp,
                                        color = if (isSelected) AmberPrimary else InkLight,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = AmberPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Save Button
                Button(
                    onClick = {
                        onSave(apiKey.trim(), selectedAlarmMinutes)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_settings_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AmberPrimary,
                        contentColor = OnAmber
                    )
                ) {
                    Text(
                        text = "ذخیره تنظیمات",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
