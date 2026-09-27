package com.example.ui.component

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.example.ui.theme.*
import java.io.File

@Composable
fun IosPwaDialog(
    initialPwaUrl: String,
    onSavePwaUrl: (String) -> Unit,
    onResetPwaUrl: () -> Unit,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    var urlText by remember(initialPwaUrl) { mutableStateOf(initialPwaUrl) }
    var isSaved by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("ios_pwa_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = PanelBackground),
            border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(CyanAccent.copy(alpha = 0.15f))
                        .border(1.dp, CyanAccent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneIphone,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "نسخه آیفون و وب‌اپلیکیشن (PWA)",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = InkLight,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "تنظیم آدرس دلخواه و دریافت بسته فایل‌ها جهت بارگذاری روی هاست شخصی",
                    fontSize = 11.5.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Editable URL Input Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(PanelRaised)
                        .border(1.dp, BorderLine, RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "آدرس اینترنتی وب‌اپلیکیشن (PWA URL):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent
                        )
                        TextButton(
                            onClick = {
                                onResetPwaUrl()
                                isSaved = true
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = TextMuted2, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("بازنشانی", fontSize = 11.sp, color = TextMuted2)
                        }
                    }

                    OutlinedTextField(
                        value = urlText,
                        onValueChange = {
                            urlText = it
                            isSaved = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pwa_url_input"),
                        leadingIcon = {
                            Icon(Icons.Default.Link, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("لینک وب ویرا", urlText)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "آدرس کپی شد", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "کپی", tint = TextMuted, modifier = Modifier.size(16.dp))
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = BorderLine,
                            focusedTextColor = InkLight,
                            unfocusedTextColor = InkLight,
                            focusedContainerColor = Color(0xFF0F172A),
                            unfocusedContainerColor = Color(0xFF0F172A)
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                    )

                    Button(
                        onClick = {
                            onSavePwaUrl(urlText)
                            isSaved = true
                            Toast.makeText(context, "آدرس با موفقیت ذخیره شد", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSaved) EmeraldAccent else CyanAccent,
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(
                            imageVector = if (isSaved) Icons.Default.Check else Icons.Default.Link,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isSaved) "آدرس ذخیره شد" else "ذخیره و اعمال این آدرس",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action buttons: Open in browser & Share link
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "دستیار صوتی ویرا (نسخه آیفون و وب)")
                                putExtra(Intent.EXTRA_TEXT, "دستیار صوتی و زمان‌بندی کاری ویرا:\n${urlText.trim()}")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "اشتراک‌گذاری لینک PWA"))
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = InkLight),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderLine)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("اشتراک لینک", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            try {
                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(urlText.trim()))
                                context.startActivity(browserIntent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "آدرس نامعتبر است", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                    ) {
                        Icon(imageVector = Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("باز کردن وب", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // PWA Bundle Export Box (User requested feature to provide files for custom web hosting)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = PanelRaised,
                    border = androidx.compose.foundation.BorderStroke(1.dp, AmberPrimary.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderZip,
                                contentDescription = null,
                                tint = AmberPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "دریافت بسته فایل‌های وب‌اپلیکیشن (ZIP)",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmberPrimary
                            )
                        }

                        Text(
                            text = "فایل‌های کامل PWA آماده شده‌اند. می‌توانید فایل ZIP را در تلگرام، ایمیل یا حافظه گوشی ذخیره کرده و در هاست، ساب‌دامین یا سرور دلخواه خود اکسترکت کنید.",
                            fontSize = 11.sp,
                            color = TextMuted,
                            lineHeight = 16.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { sharePwaZipBundle(context) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AmberPrimary,
                                    contentColor = Color.Black
                                )
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("دریافت فایل ZIP", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    val downloadUrl = "${urlText.trimEnd('/')}/vira-pwa-bundle.zip"
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = InkLight),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderLine)
                            ) {
                                Text("لینک مستقیم", fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Steps for iPhone Safari
                Text(
                    text = "راهنمای نصب در آیفون (Safari):",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = InkLight,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right
                )

                Spacer(modifier = Modifier.height(6.dp))

                IosStepItem(
                    number = "۱",
                    title = "باز کردن آدرس در مرورگر Safari",
                    desc = "آدرس ثبت شده را در مرورگر سافاری گوشی آیفون خود باز کنید."
                )

                IosStepItem(
                    number = "۲",
                    title = "لمس دکمه اشتراک‌گذاری (Share ⎋)",
                    desc = "در نوار پایین سافاری، دکمه مربع با فلش رو به بالا را بزنید."
                )

                IosStepItem(
                    number = "۳",
                    title = "انتخاب «Add to Home Screen» ➕",
                    desc = "گزینه «افزودن به صفحه اصلی» را انتخاب کنید تا آیکون ویرا مانند یک اپلیکیشن بومی در آیفون ظاهر شود."
                )

                IosStepItem(
                    number = "🍏",
                    title = "ثبت مستقیم در تقویم رسمی اپل",
                    desc = "در نسخه وب، با لمس دکمه «افزودن به تقویم آیفون»، جلسات فوراً در تقویم رسمی iOS همراه با آلارم سینک می‌شوند."
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onDismissRequest,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PanelRaised)
                ) {
                    Text("بستن پنجره", color = TextMuted, fontSize = 13.sp)
                }
            }
        }
    }
}

private fun sharePwaZipBundle(context: Context) {
    try {
        val cacheFile = File(context.cacheDir, "vira-pwa-bundle.zip")
        context.assets.open("vira-pwa-bundle.zip").use { input ->
            cacheFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            cacheFile
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "بسته کامل وب‌اپلیکیشن ویرا (PWA Bundle)")
            putExtra(Intent.EXTRA_TEXT, "فایل‌های وب‌اپلیکیشن دستیار هوشمند صوتی ویرا (PWA) برای آپلود در هاست یا دامنه دلخواه.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "ارسال و ذخیره بسته ZIP وب‌اپلیکیشن"))
    } catch (e: Exception) {
        Toast.makeText(context, "خطا در آماده‌سازی فایل: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

@Composable
private fun IosStepItem(
    number: String,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1E293B).copy(alpha = 0.5f))
            .padding(10.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(Color(0xFF6366F1)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = InkLight
            )
            Text(
                text = desc,
                fontSize = 10.5.sp,
                color = TextMuted,
                lineHeight = 15.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
