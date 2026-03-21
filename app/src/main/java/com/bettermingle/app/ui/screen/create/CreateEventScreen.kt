package com.bettermingle.app.ui.screen.create

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ScreenshotMonitor
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Surface
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import com.bettermingle.app.data.model.EventModule
import com.bettermingle.app.data.model.PREDEFINED_THEMES
import androidx.compose.ui.res.stringResource
import com.bettermingle.app.R
import com.bettermingle.app.ui.component.BetterMingleCard
import com.bettermingle.app.ui.component.CoachMarkBanner
import com.bettermingle.app.ui.component.CoachMarkIds
import com.bettermingle.app.ui.component.BetterMingleButton
import com.bettermingle.app.ui.component.BetterMingleOutlinedButton
import com.bettermingle.app.ui.component.BetterMingleTextField
import com.bettermingle.app.ui.component.PlacesAutocompleteField
import com.bettermingle.app.ui.component.PlaceResult
import com.bettermingle.app.ui.theme.AccentGold
import com.bettermingle.app.ui.theme.AccentOrange
import com.bettermingle.app.ui.theme.AccentPink
import com.bettermingle.app.ui.theme.PrimaryBlue
import com.bettermingle.app.ui.theme.Spacing
import com.bettermingle.app.ui.theme.Success

import com.bettermingle.app.viewmodel.CreateEventViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import com.bettermingle.app.data.preferences.PremiumTier
import com.bettermingle.app.data.preferences.TierLimits
import com.bettermingle.app.viewmodel.EventListViewModel
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.filled.Restaurant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventScreen(
    onEventCreated: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToUpgrade: () -> Unit = {},
    prefillName: String? = null,
    prefillDescription: String? = null,
    prefillLocation: String? = null,
    prefillIntroText: String? = null,
    prefillTheme: String? = null,
    prefillModules: List<String>? = null,
    viewModel: CreateEventViewModel = viewModel(),
    eventListViewModel: EventListViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val eventListState by eventListViewModel.uiState.collectAsState()

    // Tier limit check
    val premiumTier = viewModel.premiumTier.collectAsState(initial = PremiumTier.FREE).value
    val maxEvents = TierLimits.maxEvents(premiumTier)
    val currentEventCount = eventListState.events.size
    var showLimitDialog by remember { mutableStateOf(false) }

    LaunchedEffect(currentEventCount, maxEvents) {
        if (currentEventCount >= maxEvents) {
            showLimitDialog = true
        }
    }

    if (showLimitDialog && premiumTier != PremiumTier.BUSINESS) {
        val (dialogText, buttonText) = when (premiumTier) {
            PremiumTier.FREE -> stringResource(R.string.create_event_limit_free, maxEvents) to stringResource(R.string.create_event_limit_free_button)
            PremiumTier.PRO -> stringResource(R.string.create_event_limit_pro, maxEvents) to stringResource(R.string.create_event_limit_pro_button)
            PremiumTier.BUSINESS -> "" to "" // unreachable
        }
        AlertDialog(
            onDismissRequest = { showLimitDialog = false; onNavigateBack() },
            title = { Text(stringResource(R.string.create_event_limit_title)) },
            text = { Text(dialogText) },
            confirmButton = {
                TextButton(onClick = {
                    showLimitDialog = false
                    onNavigateToUpgrade()
                }) { Text(buttonText) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showLimitDialog = false
                    onNavigateBack()
                }) { Text(stringResource(R.string.common_back)) }
            }
        )
    }

    LaunchedEffect(uiState.createdEventId) {
        uiState.createdEventId?.let { onEventCreated(it) }
    }
    var currentStep by remember { mutableIntStateOf(0) }
    val totalSteps = 3

    // Template state — skip template selection when prefilling
    val hasPrefill = prefillName != null
    var showTemplateSelection by remember { mutableStateOf(!hasPrefill) }

    // Step 1 state
    var eventName by remember { mutableStateOf(prefillName ?: "") }
    var eventDescription by remember { mutableStateOf(prefillDescription ?: "") }
    var eventIntroText by remember { mutableStateOf(prefillIntroText ?: "") }
    var eventTheme by remember { mutableStateOf(prefillTheme ?: "") }
    var eventLocation by remember { mutableStateOf(prefillLocation ?: "") }
    var startDateMillis by remember { mutableStateOf<Long?>(null) }
    var endDateMillis by remember { mutableStateOf<Long?>(null) }
    var locationLat by remember { mutableStateOf<Double?>(null) }
    var locationLng by remember { mutableStateOf<Double?>(null) }
    var locationAddress by remember { mutableStateOf("") }
    var coverImageUri by remember { mutableStateOf<Uri?>(null) }
    var coverImageUrl by remember { mutableStateOf("") }

    // Step 2 state
    var inviteEmail by remember { mutableStateOf("") }
    val invitedEmails = remember { mutableStateListOf<String>() }

    // Step 3 state
    val selectedModules = remember {
        val prefilled = prefillModules?.mapNotNull { name ->
            try { EventModule.valueOf(name) } catch (_: Exception) { null }
        }
        if (prefilled.isNullOrEmpty()) {
            mutableStateListOf(EventModule.VOTING, EventModule.EXPENSES, EventModule.CHAT)
        } else {
            mutableStateListOf(*prefilled.toTypedArray())
        }
    }

    // Security state
    var securityEnabled by remember { mutableStateOf(false) }
    var eventPin by remember { mutableStateOf("") }
    var hideFinancials by remember { mutableStateOf(false) }
    var screenshotProtection by remember { mutableStateOf(false) }
    var autoDeleteDays by remember { mutableIntStateOf(0) }
    var requireApproval by remember { mutableStateOf(false) }
    val createScope = rememberCoroutineScope()
    val createContext = androidx.compose.ui.platform.LocalContext.current

    // Store selected template for writing items after creation
    var selectedTemplate by remember { mutableStateOf<EventTemplate?>(null) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    val hasUnsavedData = eventName.isNotBlank() || eventDescription.isNotBlank() ||
        eventLocation.isNotBlank() || invitedEmails.isNotEmpty()

    // Template selection before wizard
    if (showTemplateSelection) {
        TemplateSelectionScreen(
            premiumTier = premiumTier,
            onTemplateSelected = { template ->
                if (template != null) {
                    eventTheme = template.theme
                    eventDescription = createContext.getString(template.descriptionHintResId)
                    selectedModules.clear()
                    selectedModules.addAll(template.modules.filter { TierLimits.canUseModule(premiumTier, it) })
                    selectedTemplate = template
                }
                showTemplateSelection = false
            },
            onNavigateBack = onNavigateBack,
            onNavigateToUpgrade = onNavigateToUpgrade
        )
        return
    }

    BackHandler {
        if (currentStep > 0) currentStep--
        else if (hasUnsavedData) showDiscardDialog = true
        else onNavigateBack()
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(stringResource(R.string.create_event_discard_title)) },
            text = { Text(stringResource(R.string.create_event_discard_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    onNavigateBack()
                }) {
                    Text(stringResource(R.string.common_discard), color = AccentOrange)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (currentStep) {
                            0 -> stringResource(R.string.create_event_step_basics)
                            1 -> stringResource(R.string.create_event_step_invite)
                            2 -> stringResource(R.string.create_event_step_modules)
                            else -> ""
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentStep > 0) currentStep--
                        else if (hasUnsavedData) showDiscardDialog = true
                        else onNavigateBack()
                    }) {
                        Icon(
                            imageVector = if (currentStep > 0) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Close,
                            contentDescription = stringResource(R.string.common_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Progress bar
            LinearProgressIndicator(
                progress = { (currentStep + 1).toFloat() / totalSteps },
                modifier = Modifier.fillMaxWidth(),
                color = PrimaryBlue
            )

            // Step indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(totalSteps) { step ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = if (step <= currentStep) PrimaryBlue else PrimaryBlue.copy(alpha = 0.2f),
                                shape = MaterialTheme.shapes.extraSmall
                            )
                    )
                    if (step < totalSteps - 1) Spacer(modifier = Modifier.width(Spacing.sm))
                }
            }

            AnimatedContent(
                targetState = currentStep,
                label = "wizard_step"
            ) { step ->
                when (step) {
                    0 -> StepBasicInfo(
                        name = eventName,
                        onNameChange = { if (it.length <= 100) eventName = it },
                        description = eventDescription,
                        onDescriptionChange = { if (it.length <= 500) eventDescription = it },
                        introText = eventIntroText,
                        onIntroTextChange = { eventIntroText = it },
                        theme = eventTheme,
                        onThemeChange = { eventTheme = it },
                        location = eventLocation,
                        onLocationChange = { eventLocation = it },
                        onPlaceSelected = { place ->
                            eventLocation = place.name
                            locationAddress = place.address
                            locationLat = place.lat
                            locationLng = place.lng
                        },
                        startDateMillis = startDateMillis,
                        onStartDateChanged = { startDateMillis = it },
                        endDateMillis = endDateMillis,
                        onEndDateChanged = { endDateMillis = it },
                        coverImageUri = coverImageUri,
                        onCoverImageSelected = { coverImageUri = it },
                        onNext = { currentStep = 1 }
                    )
                    1 -> StepInvite(
                        email = inviteEmail,
                        onEmailChange = { inviteEmail = it },
                        invitedEmails = invitedEmails,
                        onAddEmail = {
                            if (inviteEmail.isNotBlank() && inviteEmail.contains("@")) {
                                invitedEmails.add(inviteEmail)
                                inviteEmail = ""
                            }
                        },
                        onRemoveEmail = { invitedEmails.remove(it) },
                        onNext = { currentStep = 2 },
                        onSkip = { currentStep = 2 }
                    )
                    2 -> StepModules(
                        selectedModules = selectedModules,
                        premiumTier = premiumTier,
                        onToggleModule = { module ->
                            if (selectedModules.contains(module)) {
                                selectedModules.remove(module)
                            } else {
                                selectedModules.add(module)
                            }
                        },
                        onNavigateToUpgrade = onNavigateToUpgrade,
                        securityEnabled = securityEnabled,
                        onSecurityEnabledChange = { securityEnabled = it },
                        eventPin = eventPin,
                        onEventPinChange = { eventPin = it },
                        hideFinancials = hideFinancials,
                        onHideFinancialsChange = { hideFinancials = it },
                        screenshotProtection = screenshotProtection,
                        onScreenshotProtectionChange = { screenshotProtection = it },
                        autoDeleteDays = autoDeleteDays,
                        onAutoDeleteDaysChange = { autoDeleteDays = it },
                        requireApproval = requireApproval,
                        onRequireApprovalChange = { requireApproval = it },
                        onCreate = {
                            createScope.launch {
                                // Upload cover image if selected
                                var uploadedUrl = ""
                                if (coverImageUri != null) {
                                    try {
                                        val ref = FirebaseStorage.getInstance().reference
                                            .child("covers/${UUID.randomUUID()}.jpg")
                                        ref.putFile(coverImageUri!!).await()
                                        uploadedUrl = ref.downloadUrl.await().toString()
                                    } catch (_: Exception) { }
                                }
                                coverImageUrl = uploadedUrl
                                viewModel.createEvent(
                                    name = eventName.trim(),
                                    description = eventDescription,
                                    theme = eventTheme,
                                    location = eventLocation,
                                    locationLat = locationLat,
                                    locationLng = locationLng,
                                    locationAddress = locationAddress,
                                    startDate = startDateMillis,
                                    endDate = endDateMillis,
                                    enabledModules = selectedModules.toList(),
                                    introText = eventIntroText,
                                    securityEnabled = securityEnabled,
                                    eventPin = eventPin,
                                    hideFinancials = hideFinancials,
                                    screenshotProtection = screenshotProtection,
                                    autoDeleteDays = autoDeleteDays,
                                    requireApproval = requireApproval,
                                    invitedEmails = invitedEmails.toList(),
                                    templateBudgetItems = selectedTemplate?.suggestedBudgetItems?.map {
                                        CreateEventViewModel.TemplateBudgetData(createContext.getString(it.nameResId), it.estimatedAmount)
                                    } ?: emptyList(),
                                    templateTasks = selectedTemplate?.suggestedTasks?.map {
                                        CreateEventViewModel.TemplateTaskData(createContext.getString(it.titleResId))
                                    } ?: emptyList(),
                                    templateScheduleBlocks = selectedTemplate?.suggestedScheduleBlocks?.map {
                                        CreateEventViewModel.TemplateScheduleData(createContext.getString(it.titleResId), it.timeLabel)
                                    } ?: emptyList()
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun StepBasicInfo(
    name: String,
    onNameChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    introText: String,
    onIntroTextChange: (String) -> Unit,
    theme: String,
    onThemeChange: (String) -> Unit,
    location: String,
    onLocationChange: (String) -> Unit,
    onPlaceSelected: (PlaceResult) -> Unit,
    startDateMillis: Long?,
    onStartDateChanged: (Long?) -> Unit,
    endDateMillis: Long?,
    onEndDateChanged: (Long?) -> Unit,
    coverImageUri: Uri?,
    onCoverImageSelected: (Uri?) -> Unit,
    onNext: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("d. M. yyyy HH:mm", Locale.forLanguageTag("cs-CZ")) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) onCoverImageSelected(uri) }

    // Date picker state
    var showDatePickerFor by remember { mutableStateOf<String?>(null) } // "start" or "end"
    var showTimePickerFor by remember { mutableStateOf<String?>(null) }
    var pendingDateMillis by remember { mutableStateOf<Long?>(null) }

    // Date picker dialog
    if (showDatePickerFor != null) {
        val initialMillis = if (showDatePickerFor == "start") startDateMillis else endDateMillis
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePickerFor = null },
            confirmButton = {
                TextButton(onClick = {
                    pendingDateMillis = datePickerState.selectedDateMillis
                    val target = showDatePickerFor
                    showDatePickerFor = null
                    showTimePickerFor = target
                }) { Text(stringResource(R.string.create_event_datepicker_continue)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerFor = null }) { Text(stringResource(R.string.create_event_datepicker_cancel)) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Time picker dialog
    if (showTimePickerFor != null) {
        val existing = if (showTimePickerFor == "start") startDateMillis else endDateMillis
        val cal = Calendar.getInstance().apply {
            if (existing != null) timeInMillis = existing
        }
        val timePickerState = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
            is24Hour = true
        )
        Dialog(
            onDismissRequest = {
                showTimePickerFor = null
                pendingDateMillis = null
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 6.dp,
                modifier = Modifier.padding(horizontal = Spacing.lg)
            ) {
                Column(
                    modifier = Modifier.padding(Spacing.lg),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.create_event_timepicker_title),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = Spacing.lg)
                    )
                    TimePicker(state = timePickerState)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = {
                            showTimePickerFor = null
                            pendingDateMillis = null
                        }) { Text(stringResource(R.string.create_event_timepicker_cancel)) }
                        TextButton(onClick = {
                            val datePart = pendingDateMillis ?: System.currentTimeMillis()
                            val calendar = Calendar.getInstance().apply {
                                timeInMillis = datePart
                                set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                                set(Calendar.MINUTE, timePickerState.minute)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            val result = calendar.timeInMillis
                            if (showTimePickerFor == "start") {
                                onStartDateChanged(result)
                            } else {
                                onEndDateChanged(result)
                            }
                            showTimePickerFor = null
                            pendingDateMillis = null
                        }) { Text(stringResource(R.string.create_event_timepicker_confirm)) }
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.lg)
    ) {
        Text(stringResource(R.string.create_event_basics_title), style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(Spacing.lg))

        // Cover image picker
        if (coverImageUri != null) {
            Box(modifier = Modifier.fillMaxWidth()) {
                AsyncImage(
                    model = coverImageUri,
                    contentDescription = stringResource(R.string.create_event_cover_description),
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                IconButton(
                    onClick = { onCoverImageSelected(null) },
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.create_event_cover_remove), tint = androidx.compose.ui.graphics.Color.White)
                }
            }
        } else {
            BetterMingleCard(onClick = { imagePickerLauncher.launch("image/*") }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = PrimaryBlue)
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text(stringResource(R.string.create_event_cover_add), color = PrimaryBlue)
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.formFieldSpacing))

        BetterMingleTextField(
            value = name,
            onValueChange = onNameChange,
            label = stringResource(R.string.create_event_name_label)
        )

        if (name.length > 90) {
            Text(
                text = "${name.length}/100",
                style = MaterialTheme.typography.labelSmall,
                color = if (name.length >= 100) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = Spacing.xs, top = Spacing.xs)
            )
        }

        Spacer(modifier = Modifier.height(Spacing.formFieldSpacing))

        BetterMingleTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = stringResource(R.string.create_event_description_label),
            singleLine = false,
            maxLines = 5
        )

        if (description.length > 450) {
            Text(
                text = "${description.length}/500",
                style = MaterialTheme.typography.labelSmall,
                color = if (description.length >= 500) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = Spacing.xs, top = Spacing.xs)
            )
        }

        Text(
            text = stringResource(R.string.create_event_formatting_hint),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = Spacing.xs, top = Spacing.xs)
        )

        Spacer(modifier = Modifier.height(Spacing.formFieldSpacing))

        BetterMingleTextField(
            value = introText,
            onValueChange = onIntroTextChange,
            label = stringResource(R.string.create_event_intro_label),
            singleLine = false,
            maxLines = 6
        )
        Text(
            text = stringResource(R.string.create_event_intro_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = Spacing.xs, top = Spacing.xs)
        )

        Spacer(modifier = Modifier.height(Spacing.formFieldSpacing))

        PlacesAutocompleteField(
            value = location,
            onValueChange = onLocationChange,
            onPlaceSelected = onPlaceSelected,
            label = stringResource(R.string.create_event_location_label)
        )

        Spacer(modifier = Modifier.height(Spacing.formFieldSpacing))

        BetterMingleTextField(
            value = theme,
            onValueChange = onThemeChange,
            label = stringResource(R.string.create_event_theme_label)
        )

        Spacer(modifier = Modifier.height(Spacing.sm))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            PREDEFINED_THEMES.forEach { t ->
                FilterChip(
                    selected = theme == t,
                    onClick = { onThemeChange(if (theme == t) "" else t) },
                    label = { Text(t) },
                    shape = RoundedCornerShape(100.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentPink.copy(alpha = 0.12f),
                        selectedLabelColor = AccentPink
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.lg))

        // Date & Time section
        Text(
            stringResource(R.string.create_event_section_datetime),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(Spacing.sm))

        // Start date
        Box(modifier = Modifier.clickable { showDatePickerFor = "start" }) {
            BetterMingleTextField(
                value = startDateMillis?.let { dateFormat.format(Date(it)) } ?: "",
                onValueChange = {},
                label = stringResource(R.string.create_event_start_label),
                enabled = false,
                leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                trailingIcon = if (startDateMillis != null) {
                    {
                        IconButton(onClick = { onStartDateChanged(null) }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.create_event_date_clear), modifier = Modifier.size(18.dp))
                        }
                    }
                } else null
            )
        }

        Spacer(modifier = Modifier.height(Spacing.formFieldSpacing))

        // End date
        Box(modifier = Modifier.clickable { showDatePickerFor = "end" }) {
            BetterMingleTextField(
                value = endDateMillis?.let { dateFormat.format(Date(it)) } ?: "",
                onValueChange = {},
                label = stringResource(R.string.create_event_end_label),
                enabled = false,
                leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                trailingIcon = if (endDateMillis != null) {
                    {
                        IconButton(onClick = { onEndDateChanged(null) }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.create_event_date_clear), modifier = Modifier.size(18.dp))
                        }
                    }
                } else null
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Spacer(modifier = Modifier.height(Spacing.xl))

        BetterMingleButton(
            text = stringResource(R.string.create_event_invite_continue),
            onClick = onNext,
            enabled = name.isNotBlank(),
            isCta = true
        )
    }
}

@Composable
private fun StepInvite(
    email: String,
    onEmailChange: (String) -> Unit,
    invitedEmails: List<String>,
    onAddEmail: () -> Unit,
    onRemoveEmail: (String) -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.lg)
    ) {
        Text(stringResource(R.string.create_event_invite_title), style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(Spacing.sm))
        Text(
            stringResource(R.string.create_event_invite_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(Spacing.lg))

        Row(verticalAlignment = Alignment.Bottom) {
            BetterMingleTextField(
                value = email,
                onValueChange = onEmailChange,
                label = stringResource(R.string.create_event_invite_email),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(Spacing.sm))
            BetterMingleButton(
                text = stringResource(R.string.create_event_invite_add),
                onClick = onAddEmail,
                modifier = Modifier.width(100.dp)
            )
        }

        Spacer(modifier = Modifier.height(Spacing.md))

        invitedEmails.forEach { invitedEmail ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = invitedEmail,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { onRemoveEmail(invitedEmail) }) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.create_event_invite_remove), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        BetterMingleButton(
            text = stringResource(R.string.create_event_invite_continue),
            onClick = onNext,
            isCta = true
        )

        Spacer(modifier = Modifier.height(Spacing.sm))

        BetterMingleOutlinedButton(
            text = stringResource(R.string.create_event_invite_skip),
            onClick = onSkip
        )
    }
}

data class ModuleOption(
    val module: EventModule,
    val label: String,
    val icon: ImageVector
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepModules(
    selectedModules: List<EventModule>,
    premiumTier: PremiumTier = PremiumTier.FREE,
    onToggleModule: (EventModule) -> Unit,
    onNavigateToUpgrade: () -> Unit = {},
    securityEnabled: Boolean,
    onSecurityEnabledChange: (Boolean) -> Unit,
    eventPin: String,
    onEventPinChange: (String) -> Unit,
    hideFinancials: Boolean,
    onHideFinancialsChange: (Boolean) -> Unit,
    screenshotProtection: Boolean,
    onScreenshotProtectionChange: (Boolean) -> Unit,
    autoDeleteDays: Int,
    onAutoDeleteDaysChange: (Int) -> Unit,
    requireApproval: Boolean,
    onRequireApprovalChange: (Boolean) -> Unit,
    onCreate: () -> Unit
) {
    val moduleOptions = listOf(
        ModuleOption(EventModule.VOTING, stringResource(R.string.create_event_module_voting), Icons.Default.HowToVote),
        ModuleOption(EventModule.EXPENSES, stringResource(R.string.create_event_module_expenses), Icons.Default.Payments),
        ModuleOption(EventModule.CARPOOL, stringResource(R.string.create_event_module_carpool), Icons.Default.DirectionsCar),
        ModuleOption(EventModule.ROOMS, stringResource(R.string.create_event_module_rooms), Icons.Default.Hotel),
        ModuleOption(EventModule.CHAT, stringResource(R.string.create_event_module_chat), Icons.AutoMirrored.Filled.Chat),
        ModuleOption(EventModule.SCHEDULE, stringResource(R.string.create_event_module_schedule), Icons.Default.CalendarMonth),
        ModuleOption(EventModule.TASKS, stringResource(R.string.create_event_module_tasks), Icons.AutoMirrored.Filled.Assignment),
        ModuleOption(EventModule.PACKING_LIST, stringResource(R.string.create_event_module_packing), Icons.Default.Backpack),
        ModuleOption(EventModule.WISHLIST, stringResource(R.string.create_event_module_wishlist), Icons.Default.CardGiftcard),
        ModuleOption(EventModule.CATERING, stringResource(R.string.create_event_module_catering), Icons.Default.Restaurant),
        ModuleOption(EventModule.BUDGET, stringResource(R.string.create_event_module_budget), Icons.Default.Payments)
    )
    val premiumLockedStr = stringResource(R.string.module_premium_locked)
    val businessLockedStr = stringResource(R.string.module_business_locked)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.lg)
    ) {
        Text(stringResource(R.string.create_event_modules_title), style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(Spacing.sm))
        Text(
            stringResource(R.string.create_event_modules_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(Spacing.lg))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            moduleOptions.forEach { option ->
                val isSelected = selectedModules.contains(option.module)
                val isLocked = !TierLimits.canUseModule(premiumTier, option.module)
                FilterChip(
                    selected = isSelected && !isLocked,
                    onClick = {
                        if (isLocked) onNavigateToUpgrade() else onToggleModule(option.module)
                    },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(option.label)
                            if (isLocked) {
                                Spacer(modifier = Modifier.width(4.dp))
                                val isBusiness = option.module in TierLimits.BUSINESS_MODULES
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = if (isBusiness) businessLockedStr else premiumLockedStr,
                                    modifier = Modifier.size(14.dp),
                                    tint = AccentGold
                                )
                            }
                        }
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = option.icon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    shape = RoundedCornerShape(100.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryBlue.copy(alpha = 0.12f),
                        selectedLabelColor = PrimaryBlue,
                        selectedLeadingIconColor = PrimaryBlue
                    ),
                    enabled = !isLocked
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.xl))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(Spacing.lg))

        // Security section
        SecuritySection(
            securityEnabled = securityEnabled,
            onSecurityEnabledChange = onSecurityEnabledChange,
            eventPin = eventPin,
            onEventPinChange = onEventPinChange,
            hideFinancials = hideFinancials,
            onHideFinancialsChange = onHideFinancialsChange,
            screenshotProtection = screenshotProtection,
            onScreenshotProtectionChange = onScreenshotProtectionChange,
            autoDeleteDays = autoDeleteDays,
            onAutoDeleteDaysChange = onAutoDeleteDaysChange,
            requireApproval = requireApproval,
            onRequireApprovalChange = onRequireApprovalChange
        )

        Spacer(modifier = Modifier.height(Spacing.xl))

        BetterMingleButton(
            text = stringResource(R.string.create_event_create_button),
            onClick = onCreate,
            isCta = true
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SecuritySection(
    securityEnabled: Boolean,
    onSecurityEnabledChange: (Boolean) -> Unit,
    eventPin: String,
    onEventPinChange: (String) -> Unit,
    hideFinancials: Boolean,
    onHideFinancialsChange: (Boolean) -> Unit,
    screenshotProtection: Boolean,
    onScreenshotProtectionChange: (Boolean) -> Unit,
    autoDeleteDays: Int,
    onAutoDeleteDaysChange: (Int) -> Unit,
    requireApproval: Boolean,
    onRequireApprovalChange: (Boolean) -> Unit
) {
    var pinVisible by remember { mutableStateOf(false) }

    BetterMingleCard {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Column {
                        Text(
                            text = stringResource(R.string.create_event_security_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(R.string.create_event_security_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = securityEnabled,
                    onCheckedChange = onSecurityEnabledChange,
                    colors = SwitchDefaults.colors(checkedTrackColor = PrimaryBlue)
                )
            }

            if (securityEnabled) {
                Spacer(modifier = Modifier.height(Spacing.lg))

                // PIN
                BetterMingleTextField(
                    value = eventPin,
                    onValueChange = { if (it.length <= 6) onEventPinChange(it) },
                    label = stringResource(R.string.create_event_security_pin_label),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = if (pinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { pinVisible = !pinVisible }) {
                            Icon(
                                if (pinVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (pinVisible) stringResource(R.string.create_event_security_pin_hide) else stringResource(R.string.create_event_security_pin_show)
                            )
                        }
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null)
                    }
                )

                Spacer(modifier = Modifier.height(Spacing.md))

                // Security toggles
                SecurityToggle(
                    icon = Icons.Default.VisibilityOff,
                    title = stringResource(R.string.create_event_security_hide_financials),
                    description = stringResource(R.string.create_event_security_hide_financials_desc),
                    checked = hideFinancials,
                    onCheckedChange = onHideFinancialsChange
                )

                Spacer(modifier = Modifier.height(Spacing.sm))

                SecurityToggle(
                    icon = Icons.Default.ScreenshotMonitor,
                    title = stringResource(R.string.create_event_security_screenshot),
                    description = stringResource(R.string.create_event_security_screenshot_desc),
                    checked = screenshotProtection,
                    onCheckedChange = onScreenshotProtectionChange
                )

                Spacer(modifier = Modifier.height(Spacing.sm))

                SecurityToggle(
                    icon = Icons.Default.VerifiedUser,
                    title = stringResource(R.string.create_event_security_approval),
                    description = stringResource(R.string.create_event_security_approval_desc),
                    checked = requireApproval,
                    onCheckedChange = onRequireApprovalChange
                )

                Spacer(modifier = Modifier.height(Spacing.md))

                // Auto-delete
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Timer,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.create_event_security_auto_delete),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = when (autoDeleteDays) {
                                0 -> stringResource(R.string.create_event_security_auto_delete_off)
                                else -> stringResource(R.string.create_event_security_auto_delete_days, autoDeleteDays)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.sm))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    listOf(0, 7, 30, 90).forEach { days ->
                        FilterChip(
                            selected = autoDeleteDays == days,
                            onClick = { onAutoDeleteDaysChange(days) },
                            label = {
                                Text(
                                    when (days) {
                                        0 -> stringResource(R.string.create_event_security_auto_delete_never)
                                        7 -> stringResource(R.string.create_event_security_auto_delete_7)
                                        30 -> stringResource(R.string.create_event_security_auto_delete_30)
                                        90 -> stringResource(R.string.create_event_security_auto_delete_90)
                                        else -> stringResource(R.string.create_event_security_auto_delete_days, days)
                                    }
                                )
                            },
                            shape = RoundedCornerShape(100.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryBlue.copy(alpha = 0.12f),
                                selectedLabelColor = PrimaryBlue
                            )
                        )
                    }
                }
            }
        }
    }
}

private data class TemplateBudgetItem(val nameResId: Int, val estimatedAmount: Int = 0)
private data class TemplateTask(val titleResId: Int)
private data class TemplateScheduleBlock(val titleResId: Int, val timeLabel: String)

private data class EventTemplate(
    val nameResId: Int,
    val emoji: String,
    val theme: String,
    val descriptionHintResId: Int,
    val modules: List<EventModule>,
    val isPremium: Boolean = false,
    val suggestedBudgetItems: List<TemplateBudgetItem> = emptyList(),
    val suggestedTasks: List<TemplateTask> = emptyList(),
    val suggestedScheduleBlocks: List<TemplateScheduleBlock> = emptyList()
)

private val eventTemplates = listOf(
    EventTemplate(R.string.create_event_template_wedding, "\uD83D\uDC8D", "Svatba", R.string.create_event_template_wedding_desc,
        listOf(EventModule.VOTING, EventModule.EXPENSES, EventModule.CHAT, EventModule.SCHEDULE, EventModule.TASKS, EventModule.CARPOOL, EventModule.ROOMS, EventModule.PACKING_LIST),
        suggestedBudgetItems = listOf(
            TemplateBudgetItem(R.string.template_budget_venue), TemplateBudgetItem(R.string.template_budget_catering),
            TemplateBudgetItem(R.string.template_budget_photographer), TemplateBudgetItem(R.string.template_budget_music),
            TemplateBudgetItem(R.string.template_budget_flowers), TemplateBudgetItem(R.string.template_budget_dress)
        ),
        suggestedTasks = listOf(
            TemplateTask(R.string.template_task_book_venue), TemplateTask(R.string.template_task_send_invitations),
            TemplateTask(R.string.template_task_order_flowers), TemplateTask(R.string.template_task_hire_photographer)
        ),
        suggestedScheduleBlocks = listOf(
            TemplateScheduleBlock(R.string.template_schedule_ceremony, "14:00"),
            TemplateScheduleBlock(R.string.template_schedule_reception, "16:00"),
            TemplateScheduleBlock(R.string.template_schedule_party, "20:00")
        )
    ),
    EventTemplate(R.string.create_event_template_birthday, "\uD83C\uDF82", "Narozeniny", R.string.create_event_template_birthday_desc,
        listOf(EventModule.VOTING, EventModule.EXPENSES, EventModule.CHAT, EventModule.PACKING_LIST),
        suggestedBudgetItems = listOf(
            TemplateBudgetItem(R.string.template_budget_food), TemplateBudgetItem(R.string.template_budget_drinks),
            TemplateBudgetItem(R.string.template_budget_cake), TemplateBudgetItem(R.string.template_budget_decorations)
        ),
        suggestedTasks = listOf(
            TemplateTask(R.string.template_task_buy_cake), TemplateTask(R.string.template_task_prepare_playlist),
            TemplateTask(R.string.template_task_send_invites)
        )
    ),
    EventTemplate(R.string.create_event_template_teambuilding, "\uD83E\uDD1D", "Teambuilding", R.string.create_event_template_teambuilding_desc,
        listOf(EventModule.SCHEDULE, EventModule.TASKS, EventModule.VOTING, EventModule.EXPENSES, EventModule.ROOMS),
        suggestedBudgetItems = listOf(
            TemplateBudgetItem(R.string.template_budget_venue), TemplateBudgetItem(R.string.template_budget_catering),
            TemplateBudgetItem(R.string.template_budget_activities)
        ),
        suggestedTasks = listOf(
            TemplateTask(R.string.template_task_book_venue), TemplateTask(R.string.template_task_prepare_agenda),
            TemplateTask(R.string.template_task_order_catering)
        ),
        suggestedScheduleBlocks = listOf(
            TemplateScheduleBlock(R.string.template_schedule_intro, "9:00"),
            TemplateScheduleBlock(R.string.template_schedule_activity_1, "10:00"),
            TemplateScheduleBlock(R.string.template_schedule_lunch, "12:00"),
            TemplateScheduleBlock(R.string.template_schedule_activity_2, "14:00")
        )
    ),
    EventTemplate(R.string.create_event_template_trip, "\uD83C\uDFD5\uFE0F", "Výlet", R.string.create_event_template_trip_desc,
        listOf(EventModule.CARPOOL, EventModule.ROOMS, EventModule.SCHEDULE, EventModule.PACKING_LIST, EventModule.EXPENSES),
        suggestedBudgetItems = listOf(
            TemplateBudgetItem(R.string.template_budget_transport), TemplateBudgetItem(R.string.template_budget_accommodation),
            TemplateBudgetItem(R.string.template_budget_food), TemplateBudgetItem(R.string.template_budget_activities)
        ),
        suggestedTasks = listOf(
            TemplateTask(R.string.template_task_pack_bags), TemplateTask(R.string.template_task_book_accommodation),
            TemplateTask(R.string.template_task_plan_route)
        ),
        suggestedScheduleBlocks = listOf(
            TemplateScheduleBlock(R.string.template_schedule_departure, "8:00"),
            TemplateScheduleBlock(R.string.template_schedule_arrival, "12:00"),
            TemplateScheduleBlock(R.string.template_schedule_first_activity, "14:00")
        )
    ),
    EventTemplate(R.string.create_event_template_bachelor, "\uD83D\uDC83", "Rozlučka", R.string.create_event_template_bachelor_desc,
        listOf(EventModule.CHAT, EventModule.SCHEDULE, EventModule.EXPENSES, EventModule.CARPOOL, EventModule.TASKS),
        suggestedBudgetItems = listOf(
            TemplateBudgetItem(R.string.template_budget_venue), TemplateBudgetItem(R.string.template_budget_food),
            TemplateBudgetItem(R.string.template_budget_drinks), TemplateBudgetItem(R.string.template_budget_activities),
            TemplateBudgetItem(R.string.template_budget_costumes)
        ),
        suggestedTasks = listOf(
            TemplateTask(R.string.template_task_book_venue), TemplateTask(R.string.template_task_plan_activities),
            TemplateTask(R.string.template_task_arrange_transport), TemplateTask(R.string.template_task_buy_decorations)
        ),
        suggestedScheduleBlocks = listOf(
            TemplateScheduleBlock(R.string.template_schedule_departure, "15:00"),
            TemplateScheduleBlock(R.string.template_schedule_dinner, "18:00"),
            TemplateScheduleBlock(R.string.template_schedule_party, "21:00")
        )
    ),
    EventTemplate(R.string.create_event_template_workshop, "\uD83D\uDCCB", "Workshop", R.string.create_event_template_workshop_desc,
        listOf(EventModule.SCHEDULE, EventModule.TASKS, EventModule.VOTING, EventModule.ROOMS),
        suggestedBudgetItems = listOf(
            TemplateBudgetItem(R.string.template_budget_venue), TemplateBudgetItem(R.string.template_budget_equipment)
        ),
        suggestedTasks = listOf(
            TemplateTask(R.string.template_task_book_venue), TemplateTask(R.string.template_task_prepare_agenda)
        ),
        suggestedScheduleBlocks = listOf(
            TemplateScheduleBlock(R.string.template_schedule_opening, "9:00"),
            TemplateScheduleBlock(R.string.template_schedule_workshop, "10:00"),
            TemplateScheduleBlock(R.string.template_schedule_lunch, "12:00"),
            TemplateScheduleBlock(R.string.template_schedule_presentation, "13:00")
        )
    ),
    EventTemplate(R.string.create_event_template_family, "\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67\u200D\uD83D\uDC66", "Rodinné setkání", R.string.create_event_template_family_desc,
        listOf(EventModule.CHAT, EventModule.SCHEDULE, EventModule.EXPENSES, EventModule.PACKING_LIST, EventModule.WISHLIST),
        suggestedBudgetItems = listOf(
            TemplateBudgetItem(R.string.template_budget_food), TemplateBudgetItem(R.string.template_budget_drinks),
            TemplateBudgetItem(R.string.template_budget_gifts), TemplateBudgetItem(R.string.template_budget_decorations)
        ),
        suggestedTasks = listOf(
            TemplateTask(R.string.template_task_send_invites), TemplateTask(R.string.template_task_book_restaurant),
            TemplateTask(R.string.template_task_buy_decorations)
        ),
        suggestedScheduleBlocks = listOf(
            TemplateScheduleBlock(R.string.template_schedule_arrival, "11:00"),
            TemplateScheduleBlock(R.string.template_schedule_lunch, "12:00"),
            TemplateScheduleBlock(R.string.template_schedule_games, "15:00")
        )
    ),
    EventTemplate(R.string.create_event_template_sports, "\u26BD", "Sport", R.string.create_event_template_sports_desc,
        listOf(EventModule.SCHEDULE, EventModule.VOTING, EventModule.TASKS, EventModule.ROOMS),
        suggestedBudgetItems = listOf(
            TemplateBudgetItem(R.string.template_budget_venue), TemplateBudgetItem(R.string.template_budget_equipment)
        ),
        suggestedTasks = listOf(
            TemplateTask(R.string.template_task_book_venue), TemplateTask(R.string.template_task_prepare_equipment)
        ),
        suggestedScheduleBlocks = listOf(
            TemplateScheduleBlock(R.string.template_schedule_warmup, "9:00"),
            TemplateScheduleBlock(R.string.template_schedule_match, "10:00"),
            TemplateScheduleBlock(R.string.template_schedule_awards, "16:00")
        )
    ),
    EventTemplate(R.string.create_event_template_festival, "\uD83C\uDFB5", "Festival", R.string.create_event_template_festival_desc,
        listOf(EventModule.SCHEDULE, EventModule.CARPOOL, EventModule.EXPENSES, EventModule.CHAT), isPremium = true,
        suggestedBudgetItems = listOf(
            TemplateBudgetItem(R.string.template_budget_tickets), TemplateBudgetItem(R.string.template_budget_transport),
            TemplateBudgetItem(R.string.template_budget_food), TemplateBudgetItem(R.string.template_budget_accommodation)
        ),
        suggestedTasks = listOf(
            TemplateTask(R.string.template_task_buy_tickets), TemplateTask(R.string.template_task_arrange_transport),
            TemplateTask(R.string.template_task_book_accommodation)
        )
    ),
    EventTemplate(R.string.create_event_template_corporate, "\uD83C\uDFE2", "Firemní akce", R.string.create_event_template_corporate_desc,
        listOf(EventModule.SCHEDULE, EventModule.TASKS, EventModule.VOTING, EventModule.EXPENSES, EventModule.ROOMS), isPremium = true,
        suggestedBudgetItems = listOf(
            TemplateBudgetItem(R.string.template_budget_venue), TemplateBudgetItem(R.string.template_budget_catering),
            TemplateBudgetItem(R.string.template_budget_activities)
        ),
        suggestedTasks = listOf(
            TemplateTask(R.string.template_task_book_venue), TemplateTask(R.string.template_task_prepare_agenda),
            TemplateTask(R.string.template_task_order_catering)
        ),
        suggestedScheduleBlocks = listOf(
            TemplateScheduleBlock(R.string.template_schedule_opening, "9:00"),
            TemplateScheduleBlock(R.string.template_schedule_presentation, "10:00"),
            TemplateScheduleBlock(R.string.template_schedule_lunch, "12:00"),
            TemplateScheduleBlock(R.string.template_schedule_activity_1, "14:00")
        )
    ),
    EventTemplate(R.string.create_event_template_party, "\uD83C\uDF89", "Oslava", R.string.create_event_template_party_desc,
        listOf(EventModule.VOTING, EventModule.EXPENSES, EventModule.CHAT, EventModule.PACKING_LIST), isPremium = true,
        suggestedBudgetItems = listOf(
            TemplateBudgetItem(R.string.template_budget_food), TemplateBudgetItem(R.string.template_budget_drinks),
            TemplateBudgetItem(R.string.template_budget_decorations), TemplateBudgetItem(R.string.template_budget_music)
        ),
        suggestedTasks = listOf(
            TemplateTask(R.string.template_task_send_invites), TemplateTask(R.string.template_task_prepare_playlist),
            TemplateTask(R.string.template_task_buy_decorations)
        )
    )
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun TemplateSelectionScreen(
    premiumTier: PremiumTier,
    onTemplateSelected: (EventTemplate?) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToUpgrade: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.create_event_template_picker_title), style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.common_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Spacing.lg)
        ) {
            Text(
                text = stringResource(R.string.create_event_template_picker_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Spacing.sm))

            CoachMarkBanner(
                id = CoachMarkIds.CREATE_EVENT_TEMPLATE,
                message = stringResource(R.string.coach_mark_create_event)
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                modifier = Modifier.weight(1f)
            ) {
                items(eventTemplates) { template ->
                    val isLocked = template.isPremium && premiumTier != PremiumTier.BUSINESS
                    BetterMingleCard(
                        onClick = {
                            if (isLocked) {
                                onNavigateToUpgrade()
                            } else {
                                onTemplateSelected(template)
                            }
                        }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = template.emoji,
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Spacer(modifier = Modifier.height(Spacing.xs))
                            Text(
                                text = stringResource(template.nameResId),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(template.descriptionHintResId),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                maxLines = 2
                            )
                            // Preview chips showing what the template includes
                            val hasExtras = template.suggestedBudgetItems.isNotEmpty() ||
                                template.suggestedTasks.isNotEmpty() ||
                                template.suggestedScheduleBlocks.isNotEmpty()
                            if (hasExtras) {
                                Spacer(modifier = Modifier.height(Spacing.xs))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = stringResource(R.string.template_preview_modules, template.modules.size),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PrimaryBlue,
                                        modifier = Modifier
                                            .background(PrimaryBlue.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                    if (template.suggestedBudgetItems.isNotEmpty()) {
                                        Text(
                                            text = stringResource(R.string.template_preview_budget, template.suggestedBudgetItems.size),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = AccentGold,
                                            modifier = Modifier
                                                .background(AccentGold.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                    if (template.suggestedTasks.isNotEmpty()) {
                                        Text(
                                            text = stringResource(R.string.template_preview_tasks, template.suggestedTasks.size),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = AccentPink,
                                            modifier = Modifier
                                                .background(AccentPink.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                    if (template.suggestedScheduleBlocks.isNotEmpty()) {
                                        Text(
                                            text = stringResource(R.string.template_preview_schedule, template.suggestedScheduleBlocks.size),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Success,
                                            modifier = Modifier
                                                .background(Success.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                            if (isLocked) {
                                Spacer(modifier = Modifier.height(Spacing.xs))
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = stringResource(R.string.create_event_template_premium_badge),
                                    tint = AccentGold,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            BetterMingleOutlinedButton(
                text = stringResource(R.string.create_event_template_custom),
                onClick = { onTemplateSelected(null) }
            )

            Spacer(modifier = Modifier.height(Spacing.lg))
        }
    }
}

@Composable
private fun SecurityToggle(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = PrimaryBlue,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(Spacing.sm))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = PrimaryBlue)
        )
    }
}
