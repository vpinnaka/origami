package com.origami.assistant.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.origami.assistant.ui.chat.model.ChatMessage
import com.origami.assistant.ui.chat.model.MessageRole
import com.origami.assistant.ui.theme.*

@Composable
fun ChatBubble(message: ChatMessage, modifier: Modifier = Modifier) {
    when (message.role) {
        MessageRole.USER -> UserBubble(message, modifier)
        MessageRole.ASSISTANT -> AssistantBubble(message, modifier)
        MessageRole.TOOL -> ToolBubble(message, modifier)
    }
}

@Composable
private fun UserBubble(message: ChatMessage, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(18.dp, 4.dp, 18.dp, 18.dp))
                .background(BubbleUser)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(
                text = message.content,
                color = BubbleUserText,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun AssistantBubble(message: ChatMessage, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        Column {
            Box(
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .clip(RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp))
                    .background(BubbleAssistant)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                if (message.isStreaming && message.content.isEmpty()) {
                    TypingIndicator()
                } else {
                    Text(
                        text = message.content,
                        color = BubbleAssistantText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolBubble(message: ChatMessage, modifier: Modifier = Modifier) {
    val isResult = message.isToolResult
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BubbleToolBorder, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .background(BubbleTool)
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isResult) Icons.Default.CheckCircle else Icons.Default.Build,
                    contentDescription = null,
                    tint = BubbleToolText,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = if (isResult) "Tool result: ${message.toolName}" else message.content,
                        color = BubbleToolText,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace
                    )
                    if (isResult && message.content.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = message.content,
                            color = BubbleToolText.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 8
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TypingIndicator(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    Row(
        modifier = modifier.padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { i ->
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, delayMillis = i * 150),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot_$i"
            )
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .alpha(alpha)
                    .background(BubbleAssistantText.copy(alpha = 0.5f), shape = androidx.compose.foundation.shape.CircleShape)
            )
        }
    }
}
