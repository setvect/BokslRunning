package com.boksl.running.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.boksl.running.R

object AppUiTokens {
    val Background = Color(0xFF000000)
    val Card = Color(0xFF1B1B1E)
    val Dialog = Color(0xFF2A2A30)
    val Pill = Color(0xFF38383C)
    val SurfaceMuted = Color(0xFF242429)
    val Divider = Color(0xFF2E2E33)
    val TextPrimary = Color(0xFFF5F5F5)
    val TextSecondary = Color(0xFFA2A2A7)
    val Accent = Color(0xFF8ED548)
    val AccentSecondary = Color(0xFF66A1FF)
    val AccentMuted = Color(0xFFFF8A5B)
    val Error = Color(0xFFFF7C73)

    val ScreenHorizontalPadding = 16.dp
    val ScreenTopPadding = 18.dp
    val ScreenSpacing = 22.dp
    val CardPadding = 24.dp
    val CardSpacing = 18.dp
    val CardRadius = 30.dp
    val PrimaryButtonRadius = 20.dp
    val SecondaryButtonRadius = 18.dp
}

@Composable
fun AppScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    onNavigateUp: (() -> Unit)? = null,
    endContent: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (onNavigateUp != null) {
                IconButton(
                    onClick = onNavigateUp,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_back_arrow),
                        contentDescription = "뒤로",
                        tint = AppUiTokens.TextPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = AppUiTokens.TextPrimary,
                fontWeight = FontWeight.Bold,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            content = endContent,
        )
    }
}

@Composable
fun AppIconActionButton(
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(32.dp),
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = contentDescription,
            tint = AppUiTokens.TextPrimary,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
fun AppSectionCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(AppUiTokens.CardPadding),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(AppUiTokens.CardSpacing),
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppUiTokens.CardRadius),
        colors = CardDefaults.cardColors(containerColor = AppUiTokens.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            verticalArrangement = verticalArrangement,
            content = content,
        )
    }
}

@Composable
fun AppPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = AppUiTokens.Accent,
    contentColor: Color = AppUiTokens.Background,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier =
            modifier
                .fillMaxWidth()
                .height(58.dp),
        shape = RoundedCornerShape(AppUiTokens.PrimaryButtonRadius),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = containerColor,
                contentColor = contentColor,
                disabledContainerColor = AppUiTokens.Pill,
                disabledContentColor = AppUiTokens.TextSecondary,
            ),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun AppSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = AppUiTokens.Pill,
    contentColor: Color = AppUiTokens.TextPrimary,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier =
            modifier
                .fillMaxWidth()
                .height(54.dp),
        shape = RoundedCornerShape(AppUiTokens.SecondaryButtonRadius),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = containerColor,
                contentColor = contentColor,
                disabledContainerColor = AppUiTokens.SurfaceMuted,
                disabledContentColor = AppUiTokens.TextSecondary,
            ),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun AppTextAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = AppUiTokens.TextSecondary,
    enabled: Boolean = true,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        colors =
            ButtonDefaults.textButtonColors(
                contentColor = color,
                disabledContentColor = AppUiTokens.TextSecondary.copy(alpha = 0.5f),
            ),
    ) {
        Text(text = text)
    }
}

@Composable
fun AppStatusCard(
    title: String,
    message: String,
    accentColor: Color = AppUiTokens.TextPrimary,
    modifier: Modifier = Modifier,
) {
    AppSectionCard(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = AppUiTokens.CardPadding, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = accentColor,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = AppUiTokens.TextSecondary,
        )
    }
}

@Composable
fun AppStateCard(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    AppSectionCard(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = AppUiTokens.TextPrimary,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = AppUiTokens.TextSecondary,
        )
        if (actionText != null && onAction != null) {
            AppSecondaryButton(text = actionText, onClick = onAction)
        }
    }
}

@Composable
fun AppMetricLine(
    value: String,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = AppUiTokens.TextPrimary,
            fontWeight = FontWeight.Bold,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(accent),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = AppUiTokens.TextSecondary,
            )
        }
    }
}

@Composable
fun AppDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    confirmText: String,
    onConfirm: () -> Unit,
    dismissText: String = "취소",
    confirmColor: Color = AppUiTokens.TextPrimary,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppUiTokens.Dialog,
        shape = RoundedCornerShape(26.dp),
        titleContentColor = AppUiTokens.TextPrimary,
        textContentColor = AppUiTokens.TextSecondary,
        title = { Text(text = title) },
        text = { Text(text = message) },
        confirmButton = {
            AppTextAction(
                text = confirmText,
                onClick = onConfirm,
                color = confirmColor,
            )
        },
        dismissButton = {
            AppTextAction(
                text = dismissText,
                onClick = onDismiss,
            )
        },
    )
}

@Composable
fun AppHintRow(
    title: String,
    message: String,
    accent: Color = AppUiTokens.AccentSecondary,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(accent),
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = AppUiTokens.TextPrimary,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = AppUiTokens.TextSecondary,
            )
        }
    }
}

fun appScreenModifier(
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = AppUiTokens.ScreenHorizontalPadding,
    topPadding: Dp = AppUiTokens.ScreenTopPadding,
): Modifier = modifier.padding(horizontal = horizontalPadding, vertical = topPadding)
