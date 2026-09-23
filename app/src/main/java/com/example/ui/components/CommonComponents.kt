package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun MethodBadge(
    method: String,
    modifier: Modifier = Modifier,
    isSmall: Boolean = false
) {
    val methodColor = when (method.uppercase()) {
        "GET" -> HttpGet
        "POST" -> HttpPost
        "PUT" -> HttpPut
        "PATCH" -> HttpPatch
        "DELETE" -> HttpDelete
        "HEAD" -> HttpHead
        else -> HttpOptions
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(methodColor.copy(alpha = 0.18f))
            .border(1.dp, methodColor.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
            .padding(
                horizontal = if (isSmall) 6.dp else 10.dp,
                vertical = if (isSmall) 2.dp else 4.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = method.uppercase(),
            color = methodColor,
            fontWeight = FontWeight.Bold,
            fontSize = if (isSmall) 11.sp else 13.sp,
            fontFamily = JetBrainsMonoFontFamily,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun StatusBadge(
    statusCode: Int,
    statusText: String,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (statusCode) {
        in 200..299 -> Pair(EmeraldSuccess.copy(alpha = 0.2f), EmeraldSuccess)
        in 300..399 -> Pair(CyanAccent.copy(alpha = 0.2f), CyanAccent)
        in 400..499 -> Pair(AmberWarning.copy(alpha = 0.2f), AmberWarning)
        in 500..599 -> Pair(RoseDanger.copy(alpha = 0.2f), RoseDanger)
        else -> Pair(Slate700.copy(alpha = 0.3f), Slate400)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, textColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
            .testTag("status_badge"),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(textColor)
            )
            Text(
                text = if (statusCode > 0) "$statusCode $statusText" else statusText,
                color = textColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                fontFamily = JetBrainsMonoFontFamily
            )
        }
    }
}

@Composable
fun MetricChip(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontFamily = JetBrainsMonoFontFamily,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun FormattedJsonViewer(
    rawText: String,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE
) {
    val clipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            kotlinx.coroutines.delay(2000)
            copied = false
        }
    }

    val annotated = remember(rawText) {
        highlightJson(rawText)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.background)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
    ) {
        SelectionContainer {
            Text(
                text = annotated,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .testTag("json_response_text")
            )
        }

        // Copy button in top right
        IconButton(
            onClick = {
                clipboardManager.setText(AnnotatedString(rawText))
                copied = true
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .testTag("copy_response_button")
        ) {
            Icon(
                imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                contentDescription = if (copied) "Copied" else "Copy JSON",
                tint = if (copied) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun highlightJson(json: String): AnnotatedString {
    return buildAnnotatedString {
        val lines = json.lines()
        lines.forEachIndexed { index, line ->
            var i = 0
            while (i < line.length) {
                val ch = line[i]
                when {
                    ch == '"' -> {
                        val endQuote = line.indexOf('"', i + 1)
                        if (endQuote != -1) {
                            val content = line.substring(i, endQuote + 1)
                            val isKey = line.substring(endQuote + 1).trimStart().startsWith(":")
                            val color = if (isKey) CyanGlow else EmeraldSuccess
                            pushStyle(SpanStyle(color = color))
                            append(content)
                            pop()
                            i = endQuote + 1
                            continue
                        } else {
                            append(ch)
                        }
                    }
                    ch.isDigit() || ch == '-' -> {
                        val start = i
                        while (i < line.length && (line[i].isDigit() || line[i] == '.' || line[i] == 'e' || line[i] == 'E' || line[i] == '-' || line[i] == '+')) {
                            i++
                        }
                        pushStyle(SpanStyle(color = AmberWarning))
                        append(line.substring(start, i))
                        pop()
                        continue
                    }
                    line.startsWith("true", i) -> {
                        pushStyle(SpanStyle(color = VioletAccent, fontWeight = FontWeight.Bold))
                        append("true")
                        pop()
                        i += 4
                        continue
                    }
                    line.startsWith("false", i) -> {
                        pushStyle(SpanStyle(color = RoseDanger, fontWeight = FontWeight.Bold))
                        append("false")
                        pop()
                        i += 5
                        continue
                    }
                    line.startsWith("null", i) -> {
                        pushStyle(SpanStyle(color = Slate400, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic))
                        append("null")
                        pop()
                        i += 4
                        continue
                    }
                    ch == '{' || ch == '}' || ch == '[' || ch == ']' -> {
                        pushStyle(SpanStyle(color = BlueInfo))
                        append(ch)
                        pop()
                    }
                    ch == ':' || ch == ',' -> {
                        pushStyle(SpanStyle(color = Slate400))
                        append(ch)
                        pop()
                    }
                    else -> {
                        append(ch)
                    }
                }
                i++
            }
            if (index < lines.size - 1) append("\n")
        }
    }
}
