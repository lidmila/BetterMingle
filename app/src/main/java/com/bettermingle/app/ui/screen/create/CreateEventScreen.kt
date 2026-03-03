package com.bettermingle.app.ui.screen.create

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.CalendarMonth
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.bettermingle.app.data.model.EventModule
import com.bettermingle.app.ui.component.BetterMingleCard
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
import com.bettermingle.app.ui.theme.TextSecondary
import com.bettermingle.app.viewmodel.CreateEventViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventScreen(
    onEventCreated: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: CreateEventViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.createdEventId) {
        uiState.createdEventId?.let { onEventCreated(it) }
    }
    var currentStep by remember { mutableIntStateOf(0) }
    val totalSteps = 3

    // Step 1 state
    var eventName by remember { mutableStateOf("") }
    var eventDescription by remember { mutableStateOf("") }
    var eventTheme by remember { mutableStateOf("") }
    var eventLocation by remember { mutableStateOf("") }
    var startDateMillis by remember { mutableStateOf<Long?>(null) }
    var endDateMillis by remember { mutableStateOf<Long?>(null) }
    var locationLat by remember { mutableStateOf<Double?>(null) }
    var locationLng by remember { mutableStateOf<Double?>(null) }
    var locationAddress by remember { mutableStateOf("") }

    // Step 2 state
    var inviteEmail by remember { mutableStateOf("") }
    val invitedEmails = remember { mutableStateListOf<String>() }

    // Step 3 state
    val selectedModules = remember { mutableStateListOf(
        EventModule.VOTING, EventModule.EXPENSES, EventModule.CHAT
    ) }

    // Security state
    var securityEnabled by remember { mutableStateOf(false) }
    var eventPin by remember { mutableStateOf("") }
    var hideFinancials by remember { mutableStateOf(false) }
    var screenshotProtection by remember { mutableStateOf(false) }
    var autoDeleteDays by remember { mutableIntStateOf(0) }
    var requireApproval by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (currentStep) {
                            0 -> "Nová akce"
                            1 -> "Pozvat účastníky"
                            2 -> "Vybrat moduly"
                            else -> ""
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentStep > 0) currentStep-- else onNavigateBack()
                    }) {
                        Icon(
                            imageVector = if (currentStep > 0) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Close,
                            contentDescription = "Zpět"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
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
                        onNameChange = { eventName = it },
                        description = eventDescription,
                        onDescriptionChange = { eventDescription = it },
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
                        onToggleModule = { module ->
                            if (selectedModules.contains(module)) {
                                selectedModules.remove(module)
                            } else {
                                selectedModules.add(module)
                            }
                        },
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
                            viewModel.createEvent(
                                name = eventName,
                                description = eventDescription,
                                theme = eventTheme,
                                location = eventLocation,
                                locationLat = locationLat,
                                locationLng = locationLng,
                                locationAddress = locationAddress,
                                startDate = startDateMillis,
                                endDate = endDateMillis,
                                enabledModules = selectedModules.toList()
                            )
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StepBasicInfo(
    name: String,
    onNameChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    theme: String,
    onThemeChange: (String) -> Unit,
    location: String,
    onLocationChange: (String) -> Unit,
    onPlaceSelected: (PlaceResult) -> Unit,
    startDateMillis: Long?,
    onStartDateChanged: (Long?) -> Unit,
    endDateMillis: Long?,
    onEndDateChanged: (Long?) -> Unit,
    onNext: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("d. M. yyyy HH:mm", Locale("cs", "CZ")) }

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
                }) { Text("Pokračovat") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerFor = null }) { Text("Zrušit") }
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
        AlertDialog(
            onDismissRequest = {
                showTimePickerFor = null
                pendingDateMillis = null
            },
            confirmButton = {
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
                }) { Text("Potvrdit") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showTimePickerFor = null
                    pendingDateMillis = null
                }) { Text("Zrušit") }
            },
            title = { Text("Vyberte čas") },
            icon = { Icon(Icons.Default.AccessTime, contentDescription = null) },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(state = timePickerState)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.lg)
    ) {
        Text("Základní informace", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(Spacing.lg))

        BetterMingleTextField(
            value = name,
            onValueChange = onNameChange,
            label = "Název akce"
        )

        Spacer(modifier = Modifier.height(Spacing.formFieldSpacing))

        BetterMingleTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = "Popis",
            singleLine = false,
            maxLines = 3
        )

        Spacer(modifier = Modifier.height(Spacing.formFieldSpacing))

        PlacesAutocompleteField(
            value = location,
            onValueChange = onLocationChange,
            onPlaceSelected = onPlaceSelected,
            label = "Místo"
        )

        Spacer(modifier = Modifier.height(Spacing.formFieldSpacing))

        BetterMingleTextField(
            value = theme,
            onValueChange = onThemeChange,
            label = "Téma akce (nepovinné)"
        )

        Spacer(modifier = Modifier.height(Spacing.lg))

        // Date & Time section
        Text(
            "Datum a čas",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(Spacing.sm))

        // Start date
        Box(modifier = Modifier.clickable { showDatePickerFor = "start" }) {
            BetterMingleTextField(
                value = startDateMillis?.let { dateFormat.format(Date(it)) } ?: "",
                onValueChange = {},
                label = "Začátek",
                enabled = false,
                leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                trailingIcon = if (startDateMillis != null) {
                    {
                        IconButton(onClick = { onStartDateChanged(null) }) {
                            Icon(Icons.Default.Close, contentDescription = "Smazat", modifier = Modifier.size(18.dp))
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
                label = "Konec",
                enabled = false,
                leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                trailingIcon = if (endDateMillis != null) {
                    {
                        IconButton(onClick = { onEndDateChanged(null) }) {
                            Icon(Icons.Default.Close, contentDescription = "Smazat", modifier = Modifier.size(18.dp))
                        }
                    }
                } else null
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        BetterMingleButton(
            text = "Pokračovat",
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
        Text("Pozvi účastníky", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(Spacing.sm))
        Text(
            "Pozvánky můžeš poslat i později.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(Spacing.lg))

        Row(verticalAlignment = Alignment.Bottom) {
            BetterMingleTextField(
                value = email,
                onValueChange = onEmailChange,
                label = "E-mail účastníka",
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(Spacing.sm))
            BetterMingleButton(
                text = "Přidat",
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
                    Icon(Icons.Default.Close, contentDescription = "Odebrat", tint = TextSecondary)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        BetterMingleButton(
            text = "Pokračovat",
            onClick = onNext,
            isCta = true
        )

        Spacer(modifier = Modifier.height(Spacing.sm))

        BetterMingleOutlinedButton(
            text = "Přeskočit",
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
    onToggleModule: (EventModule) -> Unit,
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
        ModuleOption(EventModule.VOTING, "Hlasování", Icons.Default.HowToVote),
        ModuleOption(EventModule.EXPENSES, "Výdaje", Icons.Default.Payments),
        ModuleOption(EventModule.CARPOOL, "Spolujízda", Icons.Default.DirectionsCar),
        ModuleOption(EventModule.ROOMS, "Ubytování", Icons.Default.Hotel),
        ModuleOption(EventModule.CHAT, "Chat", Icons.AutoMirrored.Filled.Chat),
        ModuleOption(EventModule.SCHEDULE, "Harmonogram", Icons.Default.CalendarMonth),
        ModuleOption(EventModule.TASKS, "Úkoly", Icons.AutoMirrored.Filled.Assignment)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.lg)
    ) {
        Text("Vyber moduly", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(Spacing.sm))
        Text(
            "Vyber, co tvoje akce potřebuje. Můžeš změnit kdykoliv.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(Spacing.lg))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            moduleOptions.forEach { option ->
                val isSelected = selectedModules.contains(option.module)
                FilterChip(
                    selected = isSelected,
                    onClick = { onToggleModule(option.module) },
                    label = { Text(option.label) },
                    leadingIcon = {
                        Icon(
                            imageVector = option.icon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryBlue.copy(alpha = 0.12f),
                        selectedLabelColor = PrimaryBlue,
                        selectedLeadingIconColor = PrimaryBlue
                    )
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
            text = "Vytvořit akci",
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
                            text = "Zvýšená ochrana",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Pro firemní a citlivé akce",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
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
                    label = "PIN pro vstup (4–6 číslic)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = if (pinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { pinVisible = !pinVisible }) {
                            Icon(
                                if (pinVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (pinVisible) "Skrýt" else "Zobrazit"
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
                    title = "Skrýt finance",
                    description = "Finanční detaily vidí jen organizátor",
                    checked = hideFinancials,
                    onCheckedChange = onHideFinancialsChange
                )

                Spacer(modifier = Modifier.height(Spacing.sm))

                SecurityToggle(
                    icon = Icons.Default.ScreenshotMonitor,
                    title = "Ochrana obrazovky",
                    description = "Blokovat snímky obrazovky",
                    checked = screenshotProtection,
                    onCheckedChange = onScreenshotProtectionChange
                )

                Spacer(modifier = Modifier.height(Spacing.sm))

                SecurityToggle(
                    icon = Icons.Default.VerifiedUser,
                    title = "Schvalování účastníků",
                    description = "Noví účastníci musí být schváleni",
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
                            text = "Automatické smazání dat",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = when (autoDeleteDays) {
                                0 -> "Vypnuto"
                                else -> "$autoDeleteDays dní po konci akce"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
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
                                        0 -> "Nikdy"
                                        7 -> "7 dní"
                                        30 -> "30 dní"
                                        90 -> "90 dní"
                                        else -> "$days dní"
                                    }
                                )
                            },
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
                    color = TextSecondary
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
