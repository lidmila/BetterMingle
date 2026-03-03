package com.bettermingle.app.ui.screen.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bettermingle.app.data.model.EventStatus
import com.bettermingle.app.ui.component.BetterMingleButton
import com.bettermingle.app.ui.component.BetterMingleTextField
import com.bettermingle.app.ui.component.EmptyState
import com.bettermingle.app.ui.component.EventCard
import com.bettermingle.app.ui.theme.AccentOrange
import com.bettermingle.app.ui.theme.BetterMingleMotion
import com.bettermingle.app.ui.theme.PrimaryBlue
import com.bettermingle.app.ui.theme.Spacing
import com.bettermingle.app.ui.theme.TextOnColor
import com.bettermingle.app.viewmodel.EventListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventListScreen(
    onEventClick: (String) -> Unit,
    onCreateEvent: () -> Unit,
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

    val statusLabels = mapOf(
        EventStatus.PLANNING to "Plánování",
        EventStatus.CONFIRMED to "Potvrzeno",
        EventStatus.ONGOING to "Probíhá",
        EventStatus.COMPLETED to "Dokončeno"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Tvoje akce", style = MaterialTheme.typography.headlineSmall)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateEvent,
                containerColor = AccentOrange,
                contentColor = TextOnColor,
                modifier = Modifier.scale(fabScale.value)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nová akce")
            }
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
            }
            uiState.events.isEmpty() -> {
                EmptyState(
                    icon = Icons.Default.Celebration,
                    title = "Zatím žádné akce",
                    description = "Vytvoř svou první akci a pozvi kamarády!",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    action = {
                        BetterMingleButton(
                            text = "Nová akce",
                            onClick = onCreateEvent,
                            isCta = true
                        )
                    }
                )
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // Search bar
                    BetterMingleTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.updateSearch(it) },
                        label = "Hledat akce...",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.screenPadding)
                            .padding(top = Spacing.sm),
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = PrimaryBlue
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(Spacing.sm))

                    // Filter chips
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = Spacing.screenPadding),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        items(statusLabels.entries.toList()) { (status, label) ->
                            FilterChip(
                                selected = uiState.statusFilter == status,
                                onClick = {
                                    viewModel.updateStatusFilter(
                                        if (uiState.statusFilter == status) null else status
                                    )
                                },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryBlue,
                                    selectedLabelColor = TextOnColor
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(Spacing.sm))

                    // Event list
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(Spacing.screenPadding),
                        verticalArrangement = Arrangement.spacedBy(Spacing.itemSpacing)
                    ) {
                        itemsIndexed(uiState.filteredEvents, key = { _, event -> event.id }) { index, event ->
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
                                    participantCount = 0,
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
