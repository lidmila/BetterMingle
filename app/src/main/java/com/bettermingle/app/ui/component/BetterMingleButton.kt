package com.bettermingle.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import android.view.HapticFeedbackConstants
import com.bettermingle.app.ui.theme.AccentOrange
import com.bettermingle.app.ui.theme.CornerRadius
import com.bettermingle.app.ui.theme.TextOnColor
import com.bettermingle.app.ui.theme.Spacing

private val PillShape = RoundedCornerShape(CornerRadius.pill)

@Composable
fun BetterMingleButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isCta: Boolean = false
) {
    val view = LocalView.current

    if (isCta) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(52.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = PillShape,
                    ambientColor = AccentOrange.copy(alpha = 0.3f),
                    spotColor = AccentOrange.copy(alpha = 0.3f)
                )
                .background(
                    color = AccentOrange,
                    shape = PillShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Button(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    onClick()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = TextOnColor
                ),
                shape = PillShape,
                contentPadding = PaddingValues(horizontal = Spacing.buttonPadding, vertical = 12.dp)
            ) {
                Text(text = text, style = MaterialTheme.typography.labelLarge)
            }
        }
    } else {
        Button(
            onClick = onClick,
            modifier = modifier
                .fillMaxWidth()
                .height(52.dp),
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(),
            shape = PillShape,
            contentPadding = PaddingValues(horizontal = Spacing.buttonPadding, vertical = 12.dp)
        ) {
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun BetterMingleOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        enabled = enabled,
        shape = PillShape
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun BetterMingleTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}
