package com.bettermingle.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bettermingle.app.ui.theme.CardShadow
import com.bettermingle.app.ui.theme.CornerRadius
import com.bettermingle.app.ui.theme.Spacing

private val CardShape = RoundedCornerShape(CornerRadius.card)

@Composable
fun BetterMingleCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val baseModifier = modifier
        .fillMaxWidth()
        .shadow(
            elevation = 6.dp,
            shape = CardShape,
            ambientColor = CardShadow,
            spotColor = CardShadow
        )
        .clip(CardShape)
        .background(Color.White)

    val clickModifier = if (onClick != null) {
        baseModifier.clickable(onClick = onClick)
    } else {
        baseModifier
    }

    Column(
        modifier = clickModifier.padding(Spacing.cardPadding),
        content = content
    )
}
