package com.bettermingle.app.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.bettermingle.app.MainActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import com.bettermingle.app.utils.safeDocuments

class EventCountdownWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val widgetData = loadNextEvent()

        provideContent {
            GlanceTheme {
                WidgetContent(widgetData)
            }
        }
    }

    private suspend fun loadNextEvent(): WidgetData {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return WidgetData()

        return try {
            val firestore = FirebaseFirestore.getInstance()
            val now = System.currentTimeMillis()

            // Find events where user is participant with future start date
            val participantDocs = firestore.collectionGroup("participants")
                .whereEqualTo("userId", userId)
                .get().await()

            var closestEvent: WidgetData? = null
            var closestDiff = Long.MAX_VALUE

            for (participantDoc in participantDocs.safeDocuments) {
                val eventRef = participantDoc.reference.parent.parent ?: continue
                try {
                    val eventDoc = eventRef.get().await()
                    val startDate = (eventDoc.get("startDate") as? Number)?.toLong() ?: continue
                    if (startDate <= now) continue
                    val diff = startDate - now
                    if (diff < closestDiff) {
                        closestDiff = diff
                        closestEvent = WidgetData(
                            eventName = eventDoc.getString("name") ?: "",
                            eventId = eventDoc.id,
                            startDate = startDate,
                            daysLeft = TimeUnit.MILLISECONDS.toDays(diff).toInt(),
                            hoursLeft = (TimeUnit.MILLISECONDS.toHours(diff) % 24).toInt()
                        )
                    }
                } catch (_: Exception) { }
            }

            closestEvent ?: WidgetData()
        } catch (_: Exception) {
            WidgetData()
        }
    }
}

private data class WidgetData(
    val eventName: String = "",
    val eventId: String = "",
    val startDate: Long = 0L,
    val daysLeft: Int = 0,
    val hoursLeft: Int = 0
)

@Composable
private fun WidgetContent(data: WidgetData) {
    val primaryColor = Color(0xFF5B5FEF)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Color.White)
            .cornerRadius(16.dp)
            .padding(16.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (data.eventName.isNotEmpty()) {
            Text(
                text = data.eventName,
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(Color(0xFF1A1B3D))
                ),
                maxLines = 2
            )

            Spacer(modifier = GlanceModifier.height(8.dp))

            val countdownText = when {
                data.daysLeft > 0 -> "za ${data.daysLeft}d ${data.hoursLeft}h"
                data.hoursLeft > 0 -> "za ${data.hoursLeft}h"
                else -> "Brzy!"
            }

            Text(
                text = countdownText,
                style = TextStyle(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(primaryColor)
                )
            )

            Spacer(modifier = GlanceModifier.height(4.dp))

            Text(
                text = "BetterMingle",
                style = TextStyle(
                    fontSize = 10.sp,
                    color = ColorProvider(Color(0xFF6E7191))
                )
            )
        } else {
            Text(
                text = "Žádná nadcházející akce",
                style = TextStyle(
                    fontSize = 13.sp,
                    color = ColorProvider(Color(0xFF6E7191))
                )
            )

            Spacer(modifier = GlanceModifier.height(4.dp))

            Text(
                text = "BetterMingle",
                style = TextStyle(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(primaryColor)
                )
            )
        }
    }
}
