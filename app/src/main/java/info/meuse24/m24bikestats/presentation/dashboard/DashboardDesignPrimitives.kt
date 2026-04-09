package info.meuse24.m24bikestats.presentation.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import info.meuse24.m24bikestats.presentation.theme.DesignTokens

internal enum class DashboardStatusBadgeTone {
    Neutral,
    Positive,
    Informative,
    Warning,
    Danger,
}

internal enum class DashboardMetricTone {
    Neutral,
    Positive,
    Informative,
    Warning,
}

internal data class DashboardMetricTileModel(
    val label: String,
    val value: String,
    val supportingText: String? = null,
    val tone: DashboardMetricTone = DashboardMetricTone.Neutral,
)

@Composable
internal fun DashboardPageContainer(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = DesignTokens.ContentMaxWidth),
            content = content,
        )
    }
}

@Composable
internal fun DashboardSectionCard(
    modifier: Modifier = Modifier,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surfaceContainerLow,
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(12.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(DesignTokens.CardCornerMedium),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = DesignTokens.CardBorderAlpha),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            verticalArrangement = verticalArrangement,
            content = content,
        )
    }
}

@Composable
internal fun DashboardHeroSurface(
    modifier: Modifier = Modifier,
    accentTone: DashboardStatusBadgeTone = DashboardStatusBadgeTone.Informative,
    content: @Composable ColumnScope.() -> Unit,
) {
    DashboardSectionCard(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentPadding = PaddingValues(0.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = DesignTokens.CardCornerMedium,
                        topEnd = DesignTokens.CardCornerMedium,
                    ),
                )
                .background(statusToneContainerColor(accentTone)),
        )
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content,
        )
    }
}

@Composable
internal fun DashboardMetricGrid(
    items: List<DashboardMetricTileModel>,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowItems.forEach { item ->
                    DashboardMetricTile(
                        label = item.label,
                        value = item.value,
                        supportingText = item.supportingText,
                        tone = item.tone,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
internal fun DashboardMetricTile(
    label: String,
    value: String,
    supportingText: String? = null,
    tone: DashboardMetricTone = DashboardMetricTone.Neutral,
    modifier: Modifier = Modifier,
) {
    val colors = metricToneColors(tone)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = colors.container,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = colors.label,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = colors.value,
                fontWeight = FontWeight.SemiBold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            supportingText?.takeIf { it.isNotBlank() }?.let { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.label,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
internal fun DashboardStatusBadge(
    label: String,
    tone: DashboardStatusBadgeTone = DashboardStatusBadgeTone.Neutral,
    modifier: Modifier = Modifier,
) {
    val (containerColor, contentColor) = statusToneColors(tone)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = containerColor,
        contentColor = contentColor,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
internal fun DashboardMetaRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun metricToneColors(
    tone: DashboardMetricTone,
): DashboardMetricTileColors {
    val colorScheme = MaterialTheme.colorScheme
    return when (tone) {
        DashboardMetricTone.Neutral -> DashboardMetricTileColors(
            container = colorScheme.surfaceContainerLowest,
            label = colorScheme.onSurfaceVariant,
            value = colorScheme.onSurface,
        )

        DashboardMetricTone.Positive -> DashboardMetricTileColors(
            container = colorScheme.secondaryContainer,
            label = colorScheme.onSecondaryContainer.copy(alpha = 0.82f),
            value = colorScheme.onSecondaryContainer,
        )

        DashboardMetricTone.Informative -> DashboardMetricTileColors(
            container = colorScheme.primaryContainer,
            label = colorScheme.onPrimaryContainer.copy(alpha = 0.82f),
            value = colorScheme.onPrimaryContainer,
        )

        DashboardMetricTone.Warning -> DashboardMetricTileColors(
            container = colorScheme.tertiaryContainer,
            label = colorScheme.onTertiaryContainer.copy(alpha = 0.82f),
            value = colorScheme.onTertiaryContainer,
        )
    }
}

@Composable
private fun statusToneContainerColor(
    tone: DashboardStatusBadgeTone,
): androidx.compose.ui.graphics.Color = when (tone) {
    DashboardStatusBadgeTone.Neutral -> MaterialTheme.colorScheme.outlineVariant
    DashboardStatusBadgeTone.Positive -> MaterialTheme.colorScheme.secondary
    DashboardStatusBadgeTone.Informative -> MaterialTheme.colorScheme.primary
    DashboardStatusBadgeTone.Warning -> MaterialTheme.colorScheme.tertiary
    DashboardStatusBadgeTone.Danger -> MaterialTheme.colorScheme.error
}

@Composable
private fun statusToneColors(
    tone: DashboardStatusBadgeTone,
): Pair<androidx.compose.ui.graphics.Color, androidx.compose.ui.graphics.Color> = when (tone) {
    DashboardStatusBadgeTone.Neutral -> MaterialTheme.colorScheme.surfaceContainerHighest to MaterialTheme.colorScheme.onSurface
    DashboardStatusBadgeTone.Positive -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    DashboardStatusBadgeTone.Informative -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    DashboardStatusBadgeTone.Warning -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
    DashboardStatusBadgeTone.Danger -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
}

private data class DashboardMetricTileColors(
    val container: androidx.compose.ui.graphics.Color,
    val label: androidx.compose.ui.graphics.Color,
    val value: androidx.compose.ui.graphics.Color,
)
