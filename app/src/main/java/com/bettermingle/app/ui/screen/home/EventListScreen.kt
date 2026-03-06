package com.bettermingle.app.ui.screen.home

import com.bettermingle.app.R
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bettermingle.app.data.model.Event
import com.bettermingle.app.data.model.EventStatus
import com.bettermingle.app.ui.component.BetterMingleButton
import com.bettermingle.app.ui.component.BetterMingleTextField
import com.bettermingle.app.ui.component.EmptyState
import com.bettermingle.app.ui.component.ErrorState
import com.bettermingle.app.ui.component.EventCard
import com.bettermingle.app.ui.component.ShimmerEventCard
import com.bettermingle.app.ui.theme.AccentGold
import com.bettermingle.app.ui.theme.AccentOrange
import com.bettermingle.app.ui.theme.BackgroundPrimary
import com.bettermingle.app.ui.theme.BetterMingleMotion
import com.bettermingle.app.ui.theme.PastelBlue
import com.bettermingle.app.ui.theme.PastelGold
import com.bettermingle.app.ui.theme.PastelGray
import com.bettermingle.app.ui.theme.PastelGreen
import com.bettermingle.app.ui.theme.PastelOrange
import com.bettermingle.app.ui.theme.PrimaryBlue
import com.bettermingle.app.ui.theme.Spacing
import com.bettermingle.app.ui.theme.Success
import com.bettermingle.app.ui.theme.TextOnColor
import com.bettermingle.app.ui.theme.TextSecondary
import com.bettermingle.app.ui.theme.BorderColor
import com.bettermingle.app.viewmodel.EventListViewModel

private data class StatusChipStyle(
    val bgColor: Color,
    val textColor: Color,
    val labelResId: Int
)

private val statusChipStyles = mapOf(
    EventStatus.PLANNING to StatusChipStyle(PastelGold, AccentGold, R.string.event_status_planning),
    EventStatus.CONFIRMED to StatusChipStyle(PastelBlue, PrimaryBlue, R.string.event_status_confirmed),
    EventStatus.ONGOING to StatusChipStyle(PastelGreen, Success, R.string.event_status_ongoing),
    EventStatus.COMPLETED to StatusChipStyle(PastelGray, TextSecondary, R.string.event_status_completed),
    EventStatus.CANCELLED to StatusChipStyle(PastelOrange, AccentOrange, R.string.event_status_cancelled)
)

private data class EventSection(
    val titleResId: Int? = null,
    val events: List<Event>,
    val heroFirst: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventListScreen(
    onEventClick: (String) -> Unit,
    onCreateEvent: () -> Unit,
    onYearInReview: () -> Unit = {},
    viewModel: EventListViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var fabVisible by remember { mutableStateOf(false) }
    val fabScale = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        fabVisible = true
        fabScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
    }

    Scaffold(
        containerColor = BackgroundPrimary,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateEvent,
                containerColor = PrimaryBlue,
                contentColor = TextOnColor,
                shape = CircleShape,
                modifier = Modifier.scale(fabScale.value)
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.events_create))
            }
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.error != null && uiState.events.isEmpty() -> {
                    ErrorState(
                        modifier = Modifier.fillMaxSize(),
                        onRetry = { viewModel.refresh() }
                    )
                }
                uiState.isLoading && uiState.events.isEmpty() -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(Spacing.screenPadding),
                        verticalArrangement = Arrangement.spacedBy(Spacing.itemSpacing)
                    ) {
                        // Header shimmer
                        item {
                            Spacer(modifier = Modifier.height(Spacing.md))
                        }
                        items(4) {
                            ShimmerEventCard()
                        }
                    }
                }
                uiState.events.isEmpty() -> {
                    EmptyState(
                        icon = Icons.Default.Celebration,
                        illustration = R.drawable.il_empty_events,
                        iconDescription = stringResource(R.string.a11y_no_events),
                        title = stringResource(R.string.events_empty_title),
                        description = stringResource(R.string.events_empty_description),
                        modifier = Modifier.fillMaxSize(),
                        action = {
                            BetterMingleButton(
                                text = stringResource(R.string.events_create),
                                onClick = onCreateEvent,
                                isCta = true
                            )
                        }
                    )
                }
                else -> {
                    val filteredEvents = uiState.filteredEvents
                    val hasFilter = uiState.statusFilter != null
                    val showYearInReview = !uiState.yearInReviewDismissed && uiState.events.size >= 3

                    // Build sections
                    val sections = if (hasFilter) {
                        listOf(EventSection(events = filteredEvents))
                    } else {
                        buildList {
                            val ongoing = filteredEvents.filter { it.status == EventStatus.ONGOING }
                            val upcoming = filteredEvents.filter {
                                it.status == EventStatus.PLANNING || it.status == EventStatus.CONFIRMED
                            }
                            val completed = filteredEvents.filter { it.status == EventStatus.COMPLETED }
                            val cancelled = filteredEvents.filter { it.status == EventStatus.CANCELLED }

                            if (ongoing.isNotEmpty()) add(EventSection(R.string.events_section_ongoing, ongoing, heroFirst = true))
                            if (upcoming.isNotEmpty()) add(EventSection(R.string.events_section_upcoming, upcoming, heroFirst = true))
                            if (completed.isNotEmpty()) add(EventSection(R.string.events_section_completed, completed))
                            if (cancelled.isNotEmpty()) add(EventSection(R.string.events_section_cancelled, cancelled))
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = Spacing.screenPadding,
                            end = Spacing.screenPadding,
                            top = Spacing.md,
                            bottom = Spacing.floatingButtonOffset + Spacing.md
                        ),
                        verticalArrangement = Arrangement.spacedBy(Spacing.itemSpacing)
                    ) {
                        // Header
                        item {
                            Text(
                                text = stringResource(R.string.events_title),
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.events_count, uiState.events.size),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(Spacing.md))
                        }

                        // Search
                        item {
                            BetterMingleTextField(
                                value = uiState.searchQuery,
                                onValueChange = { viewModel.updateSearch(it) },
                                label = stringResource(R.string.events_search_label),
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = stringResource(R.string.events_search_icon),
                                        tint = PrimaryBlue
                                    )
                                }
                            )
                            Spacer(modifier = Modifier.height(Spacing.sm))
                        }

                        // Filter chips with pastel colors
                        item {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                            ) {
                                items(statusChipStyles.entries.toList()) { (status, style) ->
                                    val selected = uiState.statusFilter == status
                                    FilterChip(
                                        selected = selected,
                                        onClick = {
                                            viewModel.updateStatusFilter(
                                                if (selected) null else status
                                            )
                                        },
                                        label = { Text(stringResource(style.labelResId)) },
                                        shape = RoundedCornerShape(100.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = style.bgColor,
                                            selectedLabelColor = style.textColor,
                                            containerColor = Color.Transparent,
                                            labelColor = TextSecondary
                                        ),
                                        border = if (!selected) {
                                            FilterChipDefaults.filterChipBorder(
                                                enabled = true,
                                                selected = false,
                                                borderColor = BorderColor
                                            )
                                        } else {
                                            null
                                        }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(Spacing.md))
                        }

                        // Year in Review banner
                        if (showYearInReview) {
                            item {
                                YearInReviewBanner(
                                    onClick = onYearInReview,
                                    onDismiss = { viewModel.dismissYearInReview() }
                                )
                                Spacer(modifier = Modifier.height(Spacing.md))
                            }
                        }

                        // Sections
                        sections.forEach { section ->
                            if (section.titleResId != null) {
                                item {
                                    SectionHeader(
                                        title = stringResource(section.titleResId),
                                        count = section.events.size
                                    )
                                }
                            }

                            itemsIndexed(
                                section.events,
                                key = { _, event -> event.id }
                            ) { index, event ->
                                val isHero = section.heroFirst && index == 0
                                AnimatedVisibility(
                                    visible = true,
                                    enter = fadeIn(tween(BetterMingleMotion.STANDARD, delayMillis = index * 50)) +
                                            scaleIn(
                                                tween(BetterMingleMotion.STANDARD, delayMillis = index * 50),
                                                initialScale = 0.92f
                                            )
                                ) {
                                    EventCard(
                                        event = event,
                                        participantCount = uiState.participantCounts[event.id] ?: 0,
                                        isHero = isHero,
                                        onClick = { onEventClick(event.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    count: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
        )
        Text(
            text = "$count",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            modifier = Modifier
                .background(PastelBlue, RoundedCornerShape(100.dp))
                .padding(horizontal = 10.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun YearInReviewBanner(
    onClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(PrimaryBlue)
            .clickable(onClick = onClick)
            .padding(Spacing.cardPadding)
    ) {
        // Dismiss button
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(24.dp)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = stringResource(R.string.events_banner_close),
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(end = 28.dp)
        ) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(Spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.events_year_review_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = stringResource(R.string.events_year_review_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
            Icon(
                Icons.Default.ArrowForward,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
