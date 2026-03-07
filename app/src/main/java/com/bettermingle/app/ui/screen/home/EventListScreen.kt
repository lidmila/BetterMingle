package com.bettermingle.app.ui.screen.home

import com.bettermingle.app.R
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
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
import com.bettermingle.app.ui.theme.BetterMingleThemeColors
import com.bettermingle.app.ui.theme.PrimaryBlue
import com.bettermingle.app.ui.theme.Spacing
import com.bettermingle.app.ui.theme.Success
import com.bettermingle.app.ui.theme.TextOnColor

import com.bettermingle.app.data.ads.AdManager
import com.bettermingle.app.data.preferences.PremiumTier
import com.bettermingle.app.data.preferences.SettingsManager
import com.bettermingle.app.ui.component.NativeAdCard
import com.bettermingle.app.viewmodel.EventListViewModel

private data class StatusChipStyle(
    val bgColor: Color,
    val textColor: Color,
    val labelResId: Int
)

@Composable
private fun statusChipStyles(): Map<EventStatus, StatusChipStyle> {
    val ext = BetterMingleThemeColors.extended
    return mapOf(
        EventStatus.PLANNING to StatusChipStyle(ext.pastelGold, AccentGold, R.string.event_status_planning),
        EventStatus.CONFIRMED to StatusChipStyle(ext.pastelBlue, PrimaryBlue, R.string.event_status_confirmed),
        EventStatus.ONGOING to StatusChipStyle(ext.pastelGreen, Success, R.string.event_status_ongoing),
        EventStatus.COMPLETED to StatusChipStyle(ext.pastelGray, MaterialTheme.colorScheme.onSurfaceVariant, R.string.event_status_completed),
        EventStatus.CANCELLED to StatusChipStyle(ext.pastelOrange, AccentOrange, R.string.event_status_cancelled)
    )
}

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
    viewModel: EventListViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val settings by settingsManager.settingsFlow.collectAsState(initial = null)
    val showAds = settings?.let { AdManager.hasAds(it.premiumTier) } ?: false
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
        containerColor = MaterialTheme.colorScheme.background,
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
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            val chipStyles = statusChipStyles()
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                            ) {
                                items(chipStyles.entries.toList()) { (status, style) ->
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
                                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        border = if (!selected) {
                                            FilterChipDefaults.filterChipBorder(
                                                enabled = true,
                                                selected = false,
                                                borderColor = MaterialTheme.colorScheme.outline
                                            )
                                        } else {
                                            null
                                        }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(Spacing.md))
                        }

                        // Sections
                        sections.forEachIndexed { sectionIndex, section ->
                            // Native ad after first section for FREE tier
                            if (sectionIndex == 1 && showAds) {
                                item {
                                    NativeAdCard(
                                        modifier = Modifier.padding(vertical = Spacing.sm)
                                    )
                                }
                            }
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
                                val eventClickHandler = remember(event.id) { { onEventClick(event.id) } }
                                EventCard(
                                    event = event,
                                    participantCount = uiState.participantCounts[event.id] ?: 0,
                                    isHero = isHero,
                                    onClick = eventClickHandler
                                )
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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .background(BetterMingleThemeColors.extended.pastelBlue, RoundedCornerShape(100.dp))
                .padding(horizontal = 10.dp, vertical = 2.dp)
        )
    }
}

