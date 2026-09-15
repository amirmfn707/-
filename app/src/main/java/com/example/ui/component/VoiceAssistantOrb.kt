package com.example.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberDim
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.BorderLine
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.InkLight
import com.example.ui.theme.OnAmber
import com.example.ui.theme.PanelBackground
import com.example.ui.theme.PanelRaised
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextMuted2
import com.example.voice.VoiceState

@Composable
fun VoiceAssistantOrb(
    voiceState: VoiceState,
    isAiProcessing: Boolean,
    aiStatusMessage: String?,
    onStartVoice: () -> Unit,
    onStopVoice: () -> Unit,
    onSubmitText: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var textInput by remember { mutableStateOf("") }

    val isListening = voiceState is VoiceState.Listening || voiceState is VoiceState.Initializing

    // Pulse animation when listening
    val infiniteTransition = rememberInfiniteTransition(label = "orbPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.25f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val waveAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "waveAlpha"
    )

    val sampleSuggestions = listOf(
        "فردا ساعت ۱۰ جلسه تیمی با مدیر",
        "امروز ساعت ۱۷ تماس کاری با مشتری",
        "پنج‌شنبه خرید برای دفتر کار",
        "فردا ساعت ۹ صبح ارسال گزارش هفتگی"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(PanelBackground)
            .border(1.dp, BorderLine, RoundedCornerShape(24.dp))
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Glowing Orb / Microphone Trigger
        Box(
            modifier = Modifier
                .size(110.dp)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            // Ambient outer glow ring
            if (isListening || isAiProcessing) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(
                            if (isListening) CoralAccent.copy(alpha = waveAlpha)
                            else CyanAccent.copy(alpha = waveAlpha)
                        )
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(94.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(AmberPrimary.copy(alpha = 0.12f))
                )
            }

            // Central button
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = if (isListening) {
                                listOf(CoralAccent, Color(0xFFB03A2E))
                            } else if (isAiProcessing) {
                                listOf(CyanAccent, Color(0xFF1B6A8C))
                            } else {
                                listOf(Color(0xFFF7D08A), AmberPrimary, AmberDim)
                            }
                        )
                    )
                    .clickable {
                        if (isListening) onStopVoice() else onStartVoice()
                    }
                    .testTag("voice_assistant_mic_button"),
                contentAlignment = Alignment.Center
            ) {
                if (isAiProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(34.dp),
                        color = InkLight,
                        strokeWidth = 3.dp
                    )
                } else if (isListening) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "توقف ضبط صدا",
                        tint = InkLight,
                        modifier = Modifier.size(36.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "شروع گفتار صوتی",
                        tint = OnAmber,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // State message
        val statusText = when {
            isAiProcessing -> aiStatusMessage ?: "در حال پردازش با هوش مصنوعی GapGPT…"
            voiceState is VoiceState.Listening -> "در حال گوش دادن به صدای شما... صحبت کنید"
            voiceState is VoiceState.Initializing -> "در حال آماده‌سازی میکروفون..."
            voiceState is VoiceState.Error -> voiceState.message
            else -> "دکمه را بزنید و برنامه کاری خود را بگویید"
        }

        Text(
            text = statusText,
            color = if (voiceState is VoiceState.Error) CoralAccent else if (isListening) AmberPrimary else TextMuted,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Text input field with integrated Send button for silent/keyboard use
        OutlinedTextField(
            value = textInput,
            onValueChange = { textInput = it },
            placeholder = {
                Text(
                    text = "یا برنامه را اینجا بنویسید (مثلاً: فردا ساعت ۵ جلسه…)",
                    fontSize = 13.sp,
                    color = TextMuted2
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("transcript_text_input"),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = PanelRaised,
                unfocusedContainerColor = PanelRaised,
                focusedBorderColor = AmberPrimary,
                unfocusedBorderColor = BorderLine,
                focusedTextColor = InkLight,
                unfocusedTextColor = InkLight
            ),
            trailingIcon = {
                IconButton(
                    onClick = {
                        if (textInput.isNotBlank()) {
                            val msg = textInput
                            textInput = ""
                            onSubmitText(msg)
                        }
                    },
                    enabled = textInput.isNotBlank() && !isAiProcessing,
                    modifier = Modifier.testTag("send_transcript_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "ارسال متن به هوش مصنوعی",
                        tint = if (textInput.isNotBlank()) AmberPrimary else TextMuted2
                    )
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(
                onSend = {
                    if (textInput.isNotBlank()) {
                        val msg = textInput
                        textInput = ""
                        onSubmitText(msg)
                    }
                }
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Quick suggestion chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = AmberPrimary,
                modifier = Modifier.size(16.dp)
            )
            sampleSuggestions.forEach { suggestion ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PanelRaised,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderLine),
                    modifier = Modifier.clickable {
                        textInput = suggestion
                    }
                ) {
                    Text(
                        text = suggestion,
                        fontSize = 11.5.sp,
                        color = TextMuted,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}
