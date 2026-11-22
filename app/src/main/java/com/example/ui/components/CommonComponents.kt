package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.ErrorRedContainer
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenContainer

@Composable
fun AppLogoBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(38.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.School,
            contentDescription = "MCQ Learning App Logo",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
fun OnlineStatusChip(
    isOnline: Boolean,
    onlineText: String,
    offlineText: String,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isOnline) SuccessGreenContainer else ErrorRedContainer
    val fgColor = if (isOnline) SuccessGreen else ErrorRed
    Surface(
        modifier = modifier.testTag("online_status_chip"),
        shape = CircleShape,
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (isOnline) Icons.Default.CloudDone else Icons.Default.CloudOff,
                contentDescription = if (isOnline) onlineText else offlineText,
                tint = fgColor,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = if (isOnline) onlineText else offlineText,
                style = MaterialTheme.typography.labelSmall,
                color = fgColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun BengaliEmptyState(
    icon: ImageVector,
    message: String,
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = message,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            if (!actionLabel.isNullOrBlank() && onAction != null) {
                Spacer(Modifier.height(4.dp))
                Button(onClick = onAction) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
fun LoadingCard(message: String = "লোড হচ্ছে...", modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ErrorBannerCard(
    message: String,
    onRetry: (() -> Unit)? = null,
    retryLabel: String = "আবার চেষ্টা করুন",
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            if (onRetry != null) {
                Button(onClick = onRetry) {
                    Text(retryLabel)
                }
            }
        }
    }
}

@Composable
fun MetricStatCard(
    label: String,
    value: String,
    icon: ImageVector,
    tint: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = tint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Renders basic LaTeX / mathematical notation (fractions, square roots, superscripts,
 * subscripts, and Greek/math Unicode symbols) natively inside Compose Text.
 */
@Composable
fun MathText(
    text: String,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    color: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    val formatted = remember(text) { formatLatexToAnnotated(text) }
    Text(
        text = formatted,
        style = style,
        color = color,
        modifier = modifier
    )
}

private fun formatLatexToAnnotated(raw: String): androidx.compose.ui.text.AnnotatedString {
    var cleaned = raw
        .replace("$$", "")
        .replace("$", "")
        .replace("\\times", "×")
        .replace("\\div", "÷")
        .replace("\\pm", "±")
        .replace("\\leq", "≤")
        .replace("\\le", "≤")
        .replace("\\geq", "≥")
        .replace("\\ge", "≥")
        .replace("\\neq", "≠")
        .replace("\\approx", "≈")
        .replace("\\pi", "π")
        .replace("\\theta", "θ")
        .replace("\\alpha", "α")
        .replace("\\beta", "β")
        .replace("\\gamma", "γ")
        .replace("\\Delta", "Δ")
        .replace("\\sum", "∑")
        .replace("\\int", "∫")
        .replace("\\infty", "∞")
        .replace("\\rightarrow", "→")
        .replace("\\degree", "°")

    cleaned = Regex("""\\frac\{([^}]+)\}\{([^}]+)\}""").replace(cleaned) { m ->
        "(${m.groupValues[1]}/${m.groupValues[2]})"
    }
    cleaned = Regex("""\\sqrt\{([^}]+)\}""").replace(cleaned) { m ->
        "√(${m.groupValues[1]})"
    }

    return buildAnnotatedString {
        var i = 0
        while (i < cleaned.length) {
            if ((cleaned[i] == '^' || cleaned[i] == '_') && i + 1 < cleaned.length) {
                val isSup = cleaned[i] == '^'
                val shift = if (isSup) BaselineShift.Superscript else BaselineShift.Subscript
                if (cleaned[i + 1] == '{') {
                    val endBrace = cleaned.indexOf('}', i + 2)
                    if (endBrace > i + 2) {
                        withStyle(SpanStyle(baselineShift = shift, fontSize = 11.sp)) {
                            append(cleaned.substring(i + 2, endBrace))
                        }
                        i = endBrace + 1
                        continue
                    }
                } else {
                    withStyle(SpanStyle(baselineShift = shift, fontSize = 11.sp)) {
                        append(cleaned[i + 1])
                    }
                    i += 2
                    continue
                }
            }
            append(cleaned[i])
            i++
        }
    }
}

data class ChartBarData(
    val label: String,
    val percentage: Float
)

@Composable
fun SubjectPerformanceBarChart(
    items: List<ChartBarData>,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return
    val primary = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceVariant
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items.forEach { item ->
            val pct = item.percentage.coerceIn(0f, 100f)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${pct.toInt()}%",
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = primary
                    )
                }
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                ) {
                    drawRoundRect(
                        color = track,
                        cornerRadius = CornerRadius(8f, 8f),
                        size = Size(size.width, size.height)
                    )
                    drawRoundRect(
                        color = primary,
                        cornerRadius = CornerRadius(8f, 8f),
                        size = Size(size.width * (pct / 100f), size.height)
                    )
                }
            }
        }
    }
}

@Composable
fun WeeklyProgressLineChart(
    points: List<Pair<String, Float>>,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return
    val lineColor = MaterialTheme.colorScheme.secondary
    val dotColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .padding(horizontal = 8.dp, vertical = 12.dp)
        ) {
            val h = size.height
            val w = size.width
            for (step in 0..2) {
                val y = h * (step / 2f)
                drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1.5f)
            }
            if (points.size == 1) {
                val y = h - (points[0].second.coerceIn(0f, 100f) / 100f) * h
                drawCircle(dotColor, radius = 8f, center = Offset(w / 2f, y))
                return@Canvas
            }
            val stepX = w / (points.size - 1).coerceAtLeast(1)
            val path = Path()
            val coords = points.mapIndexed { idx, (_, pct) ->
                val x = idx * stepX
                val y = h - (pct.coerceIn(0f, 100f) / 100f) * h
                Offset(x, y)
            }
            coords.forEachIndexed { idx, pt ->
                if (idx == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
            }
            drawPath(path, color = lineColor, style = Stroke(width = 5f, cap = StrokeCap.Round))
            coords.forEach { pt ->
                drawCircle(Color.White, radius = 8f, center = pt)
                drawCircle(dotColor, radius = 5.5f, center = pt)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            points.forEach { (label, _) ->
                Text(
                    text = label.takeLast(5),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

fun subjectIconForName(iconName: String): ImageVector = when (iconName) {
    "menu_book" -> Icons.Default.MenuBook
    "translate" -> Icons.Default.Translate
    "calculate" -> Icons.Default.Calculate
    "public" -> Icons.Default.Public
    "flag" -> Icons.Default.Flag
    "language" -> Icons.Default.Language
    "science" -> Icons.Default.Science
    "bolt" -> Icons.Default.Bolt
    "biotech" -> Icons.Default.Biotech
    "eco" -> Icons.Default.Eco
    "computer" -> Icons.Default.Computer
    "terrain" -> Icons.Default.Terrain
    "history_edu" -> Icons.Default.HistoryEdu
    "psychology" -> Icons.Default.Psychology
    "work" -> Icons.Default.Work
    else -> Icons.Default.CheckCircle
}
