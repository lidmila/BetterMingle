package com.bettermingle.app.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.bettermingle.app.ui.theme.PrimaryBlue
import com.bettermingle.app.ui.theme.Spacing
import com.bettermingle.app.ui.theme.TextSecondary
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

data class PlaceResult(
    val name: String,
    val address: String,
    val lat: Double?,
    val lng: Double?
)

@Composable
fun PlacesAutocompleteField(
    value: String,
    onValueChange: (String) -> Unit,
    onPlaceSelected: (PlaceResult) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Místo"
) {
    val context = LocalContext.current
    var predictions by remember { mutableStateOf<List<AutocompletePrediction>>(emptyList()) }
    var showSuggestions by remember { mutableStateOf(false) }
    var placesAvailable by remember { mutableStateOf(true) }

    val placesClient = remember {
        try {
            if (Places.isInitialized()) Places.createClient(context) else null
        } catch (_: Exception) {
            null
        }
    }

    if (placesClient == null) placesAvailable = false

    // Debounced autocomplete search
    LaunchedEffect(value) {
        if (!placesAvailable || value.length < 3) {
            predictions = emptyList()
            showSuggestions = false
            return@LaunchedEffect
        }
        delay(300)
        try {
            val request = FindAutocompletePredictionsRequest.builder()
                .setQuery(value)
                .setCountries("CZ", "SK")
                .build()
            val response = placesClient!!.findAutocompletePredictions(request).await()
            predictions = response.autocompletePredictions.take(5)
            showSuggestions = predictions.isNotEmpty()
        } catch (_: Exception) {
            predictions = emptyList()
            showSuggestions = false
        }
    }

    Column(modifier = modifier) {
        BetterMingleTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                showSuggestions = true
            },
            label = label,
            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) }
        )

        AnimatedVisibility(visible = showSuggestions && predictions.isNotEmpty()) {
            BetterMingleCard {
                Column {
                    predictions.forEachIndexed { index, prediction ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showSuggestions = false
                                    val placeId = prediction.placeId
                                    val primaryText = prediction.getPrimaryText(null).toString()
                                    onValueChange(primaryText)

                                    // Fetch place details
                                    if (placesClient != null) {
                                        val fields = listOf(
                                            Place.Field.DISPLAY_NAME,
                                            Place.Field.FORMATTED_ADDRESS,
                                            Place.Field.LOCATION
                                        )
                                        val fetchRequest = FetchPlaceRequest.builder(placeId, fields).build()
                                        placesClient.fetchPlace(fetchRequest)
                                            .addOnSuccessListener { fetchResponse ->
                                                val place = fetchResponse.place
                                                onPlaceSelected(
                                                    PlaceResult(
                                                        name = place.displayName ?: primaryText,
                                                        address = place.formattedAddress ?: "",
                                                        lat = place.location?.latitude,
                                                        lng = place.location?.longitude
                                                    )
                                                )
                                            }
                                            .addOnFailureListener {
                                                onPlaceSelected(
                                                    PlaceResult(
                                                        name = primaryText,
                                                        address = prediction.getSecondaryText(null).toString(),
                                                        lat = null,
                                                        lng = null
                                                    )
                                                )
                                            }
                                    }
                                }
                                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = prediction.getPrimaryText(null).toString(),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = prediction.getSecondaryText(null).toString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                        if (index < predictions.lastIndex) {
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}
