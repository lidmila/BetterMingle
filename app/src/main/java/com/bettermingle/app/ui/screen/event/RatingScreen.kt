package com.bettermingle.app.ui.screen.event

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.StarRate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bettermingle.app.data.model.EventRating
import com.bettermingle.app.ui.component.BetterMingleButton
import com.bettermingle.app.ui.component.BetterMingleCard
import com.bettermingle.app.ui.component.BetterMingleTextField
import com.bettermingle.app.ui.component.EmptyState
import com.bettermingle.app.ui.theme.AccentGold
import com.bettermingle.app.ui.theme.Spacing
import com.bettermingle.app.ui.theme.TextSecondary
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
    val ratings = remember { mutableStateListOf<EventRating>() }
    val userNames = remember { mutableMapOf<String, String>() }
    var userRating by remember { mutableIntStateOf(0) }
    var userComment by remember { mutableStateOf("") }
    var hasSubmitted by remember { mutableStateOf(false) }
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
                    comment = data["comment"] as? String ?: "",
                    createdAt = (data["createdAt"] as? Number)?.toLong() ?: 0
                )
            }

            // Load user names
            val allUserIds = loaded.map { it.userId }.distinct()
            for (uid in allUserIds) {
                try {
                    val userDoc = firestore.collection("users").document(uid).get().await()
                    userNames[uid] = userDoc.getString("displayName") ?: uid.take(8)
                } catch (_: Exception) { userNames[uid] = uid.take(8) }
            }

            // Check if current user already rated
            val existingRating = loaded.find { it.userId == currentUserId }
            if (existingRating != null) {
                userRating = existingRating.overallRating
                userComment = existingRating.comment
                hasSubmitted = true
            }

            // Show other people's ratings (not current user's)
            ratings.clear()
            ratings.addAll(loaded.filter { it.userId != currentUserId })
        } catch (_: Exception) { }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hodnocení", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
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
            // User rating card
            item {
                BetterMingleCard {
                    Column {
                        Text(
                            text = if (hasSubmitted) "Tvoje hodnocení" else "Ohodnoť akci",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(Spacing.md))

                        RatingStars(
                            rating = userRating,
                            onRatingChange = { if (!hasSubmitted) userRating = it },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )

                        if (!hasSubmitted) {
                            Spacer(modifier = Modifier.height(Spacing.md))

                            BetterMingleTextField(
                                value = userComment,
                                onValueChange = { userComment = it },
                                label = "Komentář (volitelné)",
                                singleLine = false,
                                maxLines = 4
                            )

                            Spacer(modifier = Modifier.height(Spacing.md))

                            BetterMingleButton(
                                text = "Odeslat hodnocení",
                                onClick = {
                                    if (userRating > 0) {
                                        hasSubmitted = true
                                        scope.launch {
                                            try {
                                                val ratingData = mapOf(
                                                    "userId" to currentUserId,
                                                    "overallRating" to userRating,
                                                    "comment" to userComment,
                                                    "createdAt" to System.currentTimeMillis()
                                                )
                                                FirebaseFirestore.getInstance()
                                                    .collection("events").document(eventId)
                                                    .collection("ratings").document(currentUserId)
                                                    .set(ratingData).await()
                                            } catch (_: Exception) { }
                                        }
                                    }
                                },
                                enabled = userRating > 0,
                                isCta = true
                            )
                        } else if (userComment.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(Spacing.sm))
                            Text(
                                text = userComment,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // Other ratings
            if (ratings.isNotEmpty()) {
                item {
                    Text(
                        text = "Hodnocení ostatních",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                items(ratings, key = { it.id }) { rating ->
                    RatingItem(rating = rating, userName = userNames[rating.userId] ?: rating.userId.take(8))
                }
            } else if (hasSubmitted) {
                item {
                    EmptyState(
                        icon = Icons.Default.StarRate,
                        title = "Zatím jen ty",
                        description = "Počkej na hodnocení ostatních účastníků."
                    )
                }
            }
        }
    }
}

@Composable
fun RatingStars(
    rating: Int,
    onRatingChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (i in 1..5) {
            IconButton(
                onClick = { onRatingChange(i) },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = if (i <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "Hvězda $i",
                    tint = AccentGold,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}

@Composable
private fun RatingItem(rating: EventRating, userName: String) {
    BetterMingleCard {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = userName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                Row {
                    for (i in 1..5) {
                        Icon(
                            imageVector = if (i <= rating.overallRating) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null,
                            tint = AccentGold,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (rating.comment.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = rating.comment,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}
