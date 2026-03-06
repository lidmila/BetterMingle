package com.bettermingle.app.ui.screen.event

import com.bettermingle.app.R
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.StarRate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bettermingle.app.data.model.EventRating
import com.bettermingle.app.ui.component.BetterMingleButton
import com.bettermingle.app.ui.component.BetterMingleCard
import com.bettermingle.app.ui.component.BetterMingleTextField
import com.bettermingle.app.ui.component.EmptyState
import com.bettermingle.app.ui.theme.AccentGold
import com.bettermingle.app.ui.theme.BackgroundPrimary
import com.bettermingle.app.ui.theme.PastelGold
import com.bettermingle.app.ui.theme.PrimaryBlue
import com.bettermingle.app.ui.theme.Spacing
import com.bettermingle.app.ui.theme.Success
import com.bettermingle.app.ui.theme.TextSecondary
import com.bettermingle.app.utils.ActivityLogger
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RatingScreen(
    eventId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val otherRatings = remember { mutableStateListOf<EventRating>() }
    var overallRating by remember { mutableIntStateOf(0) }
    var organizationRating by remember { mutableIntStateOf(0) }
    var atmosphereRating by remember { mutableIntStateOf(0) }
    var venueRating by remember { mutableIntStateOf(0) }
    var userComment by remember { mutableStateOf("") }
    var hasSubmitted by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    val currentUserId = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    val scope = rememberCoroutineScope()

    LaunchedEffect(eventId) {
        try {
            val firestore = FirebaseFirestore.getInstance()
            val snapshot = firestore.collection("events").document(eventId)
                .collection("ratings").get().await()

            val loaded = snapshot.documents.map { doc ->
                val data = doc.data ?: emptyMap()
                EventRating(
                    id = doc.id,
                    eventId = eventId,
                    userId = data["userId"] as? String ?: "",
                    overallRating = (data["overallRating"] as? Number)?.toInt() ?: 0,
                    organizationRating = (data["organizationRating"] as? Number)?.toInt() ?: 0,
                    atmosphereRating = (data["atmosphereRating"] as? Number)?.toInt() ?: 0,
                    venueRating = (data["venueRating"] as? Number)?.toInt() ?: 0,
                    comment = data["comment"] as? String ?: "",
                    createdAt = (data["createdAt"] as? Number)?.toLong() ?: 0
                )
            }

            val existingRating = loaded.find { it.userId == currentUserId }
            if (existingRating != null) {
                overallRating = existingRating.overallRating
                organizationRating = existingRating.organizationRating
                atmosphereRating = existingRating.atmosphereRating
                venueRating = existingRating.venueRating
                userComment = existingRating.comment
                hasSubmitted = true
            }

            otherRatings.clear()
            otherRatings.addAll(loaded.filter { it.userId != currentUserId })
        } catch (_: Exception) { }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.rating_title), style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundPrimary
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(Spacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // User rating form
            item {
                BetterMingleCard {
                    Column {
                        Text(
                            text = if (hasSubmitted && !isEditing) stringResource(R.string.rating_your_rating) else stringResource(R.string.rating_rate_event),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        // Average score display when submitted
                        if (hasSubmitted && !isEditing) {
                            val avg = listOf(overallRating, organizationRating, atmosphereRating, venueRating)
                                .filter { it > 0 }
                                .let { if (it.isEmpty()) 0f else it.average().toFloat() }

                            Spacer(modifier = Modifier.height(Spacing.md))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "%.1f".format(avg),
                                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 40.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = if (avg >= 4f) Success else if (avg >= 3f) AccentGold else PrimaryBlue
                                )
                                Spacer(modifier = Modifier.width(Spacing.sm))
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = AccentGold,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(Spacing.md))

                        val canEdit = !hasSubmitted || isEditing

                        RatingCategory(
                            label = stringResource(R.string.rating_overall),
                            rating = overallRating,
                            onRatingChange = { if (canEdit) overallRating = it }
                        )

                        Spacer(modifier = Modifier.height(Spacing.sm))

                        RatingCategory(
                            label = stringResource(R.string.rating_organization),
                            rating = organizationRating,
                            onRatingChange = { if (canEdit) organizationRating = it }
                        )

                        Spacer(modifier = Modifier.height(Spacing.sm))

                        RatingCategory(
                            label = stringResource(R.string.rating_atmosphere),
                            rating = atmosphereRating,
                            onRatingChange = { if (canEdit) atmosphereRating = it }
                        )

                        Spacer(modifier = Modifier.height(Spacing.sm))

                        RatingCategory(
                            label = stringResource(R.string.rating_venue),
                            rating = venueRating,
                            onRatingChange = { if (canEdit) venueRating = it }
                        )

                        if (canEdit) {
                            Spacer(modifier = Modifier.height(Spacing.md))

                            BetterMingleTextField(
                                value = userComment,
                                onValueChange = { userComment = it },
                                label = stringResource(R.string.rating_comment_label),
                                singleLine = false,
                                maxLines = 4
                            )

                            Spacer(modifier = Modifier.height(Spacing.md))

                            BetterMingleButton(
                                text = if (isEditing) stringResource(R.string.rating_save_changes) else stringResource(R.string.rating_submit),
                                onClick = {
                                    val hasAnyRating = overallRating > 0 || organizationRating > 0 ||
                                            atmosphereRating > 0 || venueRating > 0
                                    if (hasAnyRating) {
                                        hasSubmitted = true
                                        isEditing = false
                                        scope.launch {
                                            try {
                                                val ratingData = mapOf(
                                                    "userId" to currentUserId,
                                                    "overallRating" to overallRating,
                                                    "organizationRating" to organizationRating,
                                                    "atmosphereRating" to atmosphereRating,
                                                    "venueRating" to venueRating,
                                                    "comment" to userComment,
                                                    "createdAt" to System.currentTimeMillis()
                                                )
                                                FirebaseFirestore.getInstance()
                                                    .collection("events").document(eventId)
                                                    .collection("ratings").document(currentUserId)
                                                    .set(ratingData).await()
                                                ActivityLogger.log(eventId, "rating", context.getString(R.string.activity_rated_event, overallRating.toString()))
                                            } catch (_: Exception) { }
                                        }
                                    }
                                },
                                enabled = overallRating > 0 || organizationRating > 0 ||
                                        atmosphereRating > 0 || venueRating > 0,
                                isCta = true
                            )
                        } else {
                            // Submitted view — show comment and edit button
                            if (userComment.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(Spacing.sm))
                                Text(
                                    text = userComment,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }

                            Spacer(modifier = Modifier.height(Spacing.md))

                            BetterMingleButton(
                                text = stringResource(R.string.rating_edit),
                                onClick = { isEditing = true },
                                isCta = false
                            )
                        }
                    }
                }
            }

            // Other ratings summary
            if (otherRatings.isNotEmpty()) {
                item {
                    RatingSummary(ratings = otherRatings)
                }
            } else if (hasSubmitted) {
                item {
                    EmptyState(
                        icon = Icons.Default.StarRate,
                        illustration = R.drawable.il_empty_rating,
                        title = stringResource(R.string.rating_empty_title),
                        description = stringResource(R.string.rating_empty_description)
                    )
                }
            }
        }
    }
}

@Composable
private fun RatingCategory(
    label: String,
    rating: Int,
    onRatingChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            for (i in 1..5) {
                val isSelected = i <= rating
                val animatedScale by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0.85f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "starScale"
                )

                IconButton(
                    onClick = { onRatingChange(i) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = stringResource(R.string.rating_star_description, label, i),
                        tint = AccentGold,
                        modifier = Modifier
                            .size(28.dp)
                            .scale(animatedScale)
                    )
                }
            }
        }
    }
}

@Composable
private fun RatingSummary(ratings: List<EventRating>) {
    val avgOverall = ratings.map { it.overallRating }.filter { it > 0 }.let {
        if (it.isEmpty()) 0f else it.average().toFloat()
    }
    val avgOrganization = ratings.map { it.organizationRating }.filter { it > 0 }.let {
        if (it.isEmpty()) 0f else it.average().toFloat()
    }
    val avgAtmosphere = ratings.map { it.atmosphereRating }.filter { it > 0 }.let {
        if (it.isEmpty()) 0f else it.average().toFloat()
    }
    val avgVenue = ratings.map { it.venueRating }.filter { it > 0 }.let {
        if (it.isEmpty()) 0f else it.average().toFloat()
    }

    val allAvgs = listOf(avgOverall, avgOrganization, avgAtmosphere, avgVenue).filter { it > 0 }
    val totalAvg = if (allAvgs.isEmpty()) 0f else allAvgs.average().toFloat()

    BetterMingleCard {
        Column {
            Text(
                text = stringResource(R.string.rating_others_title, ratings.size),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            // Big average score
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "%.1f".format(totalAvg),
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 36.sp),
                    fontWeight = FontWeight.Bold,
                    color = if (totalAvg >= 4f) Success else if (totalAvg >= 3f) AccentGold else PrimaryBlue
                )
                Spacer(modifier = Modifier.width(Spacing.xs))
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = AccentGold,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // Category progress bars
            CategoryProgressBar(stringResource(R.string.rating_overall), avgOverall)
            Spacer(modifier = Modifier.height(Spacing.sm))
            CategoryProgressBar(stringResource(R.string.rating_organization), avgOrganization)
            Spacer(modifier = Modifier.height(Spacing.sm))
            CategoryProgressBar(stringResource(R.string.rating_atmosphere), avgAtmosphere)
            Spacer(modifier = Modifier.height(Spacing.sm))
            CategoryProgressBar(stringResource(R.string.rating_venue), avgVenue)
        }
    }
}

@Composable
private fun CategoryProgressBar(label: String, average: Float) {
    val animatedProgress by animateFloatAsState(
        targetValue = (average / 5f).coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "progressAnim"
    )

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Text(
                text = "%.1f".format(average),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = AccentGold,
            trackColor = PastelGold,
            strokeCap = StrokeCap.Round
        )
    }
}
