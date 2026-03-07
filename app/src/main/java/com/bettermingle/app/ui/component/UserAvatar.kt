package com.bettermingle.app.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.bettermingle.app.R
import com.bettermingle.app.ui.theme.PrimaryBlue
import com.bettermingle.app.ui.theme.TextOnColor

private val avatarResources = mapOf(
    1 to R.drawable.avatar_1,
    2 to R.drawable.avatar_2,
    3 to R.drawable.avatar_3,
    4 to R.drawable.avatar_4,
    5 to R.drawable.avatar_5,
    6 to R.drawable.avatar_6,
    7 to R.drawable.avatar_7,
    8 to R.drawable.avatar_8,
    9 to R.drawable.avatar_9,
    10 to R.drawable.avatar_10,
    11 to R.drawable.avatar_11,
    12 to R.drawable.avatar_12,
    13 to R.drawable.avatar_13,
    14 to R.drawable.avatar_14,
    15 to R.drawable.avatar_15,
    16 to R.drawable.avatar_16,
    17 to R.drawable.avatar_17,
    18 to R.drawable.avatar_18,
    19 to R.drawable.avatar_19,
    20 to R.drawable.avatar_20,
    21 to R.drawable.avatar_21,
    22 to R.drawable.avatar_22,
    23 to R.drawable.avatar_23,
    24 to R.drawable.avatar_24,
    25 to R.drawable.avatar_25,
    26 to R.drawable.avatar_26,
    27 to R.drawable.avatar_27,
    28 to R.drawable.avatar_28,
    29 to R.drawable.avatar_29,
    30 to R.drawable.avatar_30,
    31 to R.drawable.avatar_31,
    32 to R.drawable.avatar_32,
    33 to R.drawable.avatar_33,
    34 to R.drawable.avatar_34,
    35 to R.drawable.avatar_35,
    36 to R.drawable.avatar_36,
    37 to R.drawable.avatar_37,
    38 to R.drawable.avatar_38,
    39 to R.drawable.avatar_39,
    40 to R.drawable.avatar_40
)

const val AVATAR_PREFIX = "avatar://"
val AVATAR_COUNT = avatarResources.size

fun isBuiltInAvatar(url: String): Boolean = url.startsWith(AVATAR_PREFIX)

fun builtInAvatarUrl(index: Int): String = "$AVATAR_PREFIX$index"

fun getAvatarResourceId(index: Int): Int? = avatarResources[index]

fun parseAvatarIndex(url: String): Int? {
    if (!url.startsWith(AVATAR_PREFIX)) return null
    return url.removePrefix(AVATAR_PREFIX).toIntOrNull()
}

private val AvatarBackground = PrimaryBlue

@Composable
fun UserAvatar(
    avatarUrl: String,
    displayName: String,
    size: Dp = 40.dp,
    modifier: Modifier = Modifier
) {
    val avatarIndex = remember(avatarUrl) { parseAvatarIndex(avatarUrl) }
    val resId = remember(avatarIndex) { avatarIndex?.let { getAvatarResourceId(it) } }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(AvatarBackground)
            .border(2.dp, MaterialTheme.colorScheme.surface.copy(alpha = 0.3f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        when {
            resId != null -> {
                Image(
                    painter = painterResource(id = resId),
                    contentDescription = stringResource(R.string.user_avatar_description),
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
            avatarUrl.isNotEmpty() -> {
                val context = LocalContext.current
                val sizePx = with(androidx.compose.ui.platform.LocalDensity.current) {
                    (size * 2f).roundToPx()
                }
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(avatarUrl)
                        .size(sizePx)
                        .crossfade(true)
                        .build(),
                    contentDescription = stringResource(R.string.user_avatar_description),
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
            else -> {
                Text(
                    text = displayName.firstOrNull()?.uppercase() ?: "?",
                    style = if (size >= 64.dp) MaterialTheme.typography.headlineMedium
                           else MaterialTheme.typography.titleMedium,
                    color = TextOnColor
                )
            }
        }
    }
}
