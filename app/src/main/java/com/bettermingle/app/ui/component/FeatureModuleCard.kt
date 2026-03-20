package com.bettermingle.app.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.bettermingle.app.R
import com.bettermingle.app.ui.theme.AccentOrange
import com.bettermingle.app.ui.theme.BetterMingleThemeColors
import com.bettermingle.app.ui.theme.CornerRadius
import com.bettermingle.app.ui.theme.Spacing
import com.bettermingle.app.ui.theme.TextOnColor
import com.bettermingle.app.ui.theme.pastelForColor

import com.bettermingle.app.utils.debouncedClick
import kotlinx.coroutines.launch

private val CardShape = RoundedCornerShape(CornerRadius.card)
private val BadgeColor = AccentOrange
private val BadgeShape = RoundedCornerShape(CornerRadius.pill)

@Composable
fun FeatureModuleCard(
    title: String,
    icon: ImageVector,
    iconTint: Color,
    subtitle: String = "",
    badgeCount: Int = 0,
    onClick: () -> Unit,
    showMenu: Boolean = false,
    onDeleteClick: (() -> Unit)? = null,
    onColorClick: (() -> Unit)? = null,
    enablePressAnimation: Boolean = true,
    modifier: Modifier = Modifier
) {
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    var menuExpanded by remember { mutableStateOf(false) }

    // Map iconTint to pastel background color
    val ext = BetterMingleThemeColors.extended
    val pastelBg = pastelForColor(iconTint, ext)

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 120.dp)
                .scale(scale.value)
                .then(
                    if (enablePressAnimation) {
                        Modifier.pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    scope.launch {
                                        scale.animateTo(0.92f, spring(stiffness = Spring.StiffnessMediumLow))
                                    }
                                    tryAwaitRelease()
                                    scope.launch {
                                        scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                                    }
                                },
                                onTap = { debouncedClick(action = onClick) }
                            )
                        }
                    } else {
                        Modifier.clickable { debouncedClick(action = onClick) }
                    }
                )
                .shadow(
                    elevation = 8.dp,
                    shape = CardShape,
                    ambientColor = iconTint.copy(alpha = 0.20f),
                    spotColor = iconTint.copy(alpha = 0.15f)
                )
                .clip(CardShape)
                .background(pastelBg)
                .padding(Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon in white circle
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(Spacing.iconMD)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (subtitle.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Three dots menu for module actions
        if (showMenu && (onDeleteClick != null || onColorClick != null)) {
            Box(modifier = Modifier.align(Alignment.TopEnd)) {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    if (onColorClick != null) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_change_color)) },
                            onClick = {
                                menuExpanded = false
                                onColorClick()
                            }
                        )
                    }
                    if (onDeleteClick != null) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.dashboard_remove_module)) },
                            onClick = {
                                menuExpanded = false
                                onDeleteClick()
                            }
                        )
                    }
                }
            }
        }

        // Badge with gradient
        if (badgeCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
                    .background(BadgeColor, BadgeShape)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (badgeCount > 99) "99+" else "$badgeCount",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextOnColor
                )
            }
        }

    }
}
