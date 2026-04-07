package com.bettermingle.app.utils

import com.bettermingle.app.data.model.*
import com.bettermingle.app.utils.ParticipantUtils
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import java.util.concurrent.ConcurrentHashMap
import com.bettermingle.app.utils.safeDocuments

suspend fun loadDetailedEventReport(eventId: String): DetailedEventReport = coroutineScope {
    val firestore = FirebaseFirestore.getInstance()
    val eventRef = firestore.collection("events").document(eventId)

    val eventDoc = eventRef.get().await()

    // Build userId → displayName map from participants (thread-safe)
    val participantDocs = eventRef.collection("participants").get().await()
    val userIdToName = ConcurrentHashMap<String, String>()
    val participants = mutableListOf<ParticipantDetail>()
    for (doc in participantDocs.safeDocuments) {
        val uid = doc.id
        val name = doc.getString("displayName") ?: doc.getString("name") ?: uid
        val rsvp = (doc.getString("rsvp") ?: "PENDING").uppercase()
        userIdToName[uid] = name
        participants.add(ParticipantDetail(displayName = name, rsvp = rsvp))
    }

    // Resolve a userId to a name, fetching from users collection if needed
    suspend fun resolveName(userId: String?): String? {
        if (userId == null) return null
        userIdToName[userId]?.let { return it }
        if (ParticipantUtils.isManualId(userId)) return userId
        return try {
            val userDoc = firestore.collection("users").document(userId).get().await()
            val name = userDoc.getString("displayName") ?: userId
            userIdToName[userId] = name
            name
        } catch (_: Exception) {
            userId
        }
    }

    // Fetch subcollections in parallel (polls don't use resolveName so they're safe)
    val pollsDeferred = async {
        val pollDocs = eventRef.collection("polls").get().await()
        val now = System.currentTimeMillis()
        pollDocs.safeDocuments.map { pollDoc ->
            val title = pollDoc.getString("title") ?: pollDoc.getString("question") ?: ""
            val isClosed = pollDoc.getBoolean("isClosed") ?: false
            val deadline = (pollDoc.get("deadline") as? Number)?.toLong()
            val isExpired = deadline != null && now > deadline

            val optionDocs = eventRef.collection("polls").document(pollDoc.id)
                .collection("options").get().await()
            val options = optionDocs.safeDocuments.map { optDoc ->
                val label = optDoc.getString("text") ?: optDoc.getString("label") ?: ""
                val voteDocs = eventRef.collection("polls").document(pollDoc.id)
                    .collection("options").document(optDoc.id)
                    .collection("votes").get().await()
                PollOptionDetail(label = label, voteCount = voteDocs.size())
            }
            PollDetail(title = title, isClosed = isClosed || isExpired, options = options)
        }
    }

    val budgetDeferred = async {
        val categoryDocs = eventRef.collection("budgetCategories").get().await()
        categoryDocs.safeDocuments.map { catDoc ->
            val name = catDoc.getString("name") ?: ""
            val planned = (catDoc.get("planned") as? Number)?.toDouble() ?: 0.0
            val expenseDocs = eventRef.collection("budgetCategories").document(catDoc.id)
                .collection("expenses").get().await()
            val actualTotal = expenseDocs.safeDocuments.sumOf {
                (it.get("amount") as? Number)?.toDouble() ?: 0.0
            }
            BudgetCategoryDetail(name = name, planned = planned, actualTotal = actualTotal)
        }
    }

    // Sequential fetches for sections that need resolveName (avoid concurrent map issues)
    val expenses = run {
        val expenseDocs = eventRef.collection("expenses").get().await()
        expenseDocs.safeDocuments.map { expDoc ->
            val description = expDoc.getString("description") ?: ""
            val amount = (expDoc.get("amount") as? Number)?.toDouble() ?: 0.0
            val currency = expDoc.getString("currency") ?: "CZK"
            val paidById = expDoc.getString("paidBy") ?: ""
            val paidByName = resolveName(paidById) ?: paidById
            val category = expDoc.getString("category") ?: ""

            val splitDocs = eventRef.collection("expenses").document(expDoc.id)
                .collection("splits").get().await()
            val splits = splitDocs.safeDocuments.map { splitDoc ->
                val userId = splitDoc.getString("userId") ?: splitDoc.id
                val splitAmount = (splitDoc.get("amount") as? Number)?.toDouble() ?: 0.0
                val isSettled = splitDoc.getBoolean("isSettled") ?: false
                ExpenseSplitDetail(
                    userName = resolveName(userId) ?: userId,
                    amount = splitAmount,
                    isSettled = isSettled
                )
            }
            ExpenseDetail(
                description = description,
                amount = amount,
                currency = currency,
                paidByName = paidByName,
                category = category,
                splits = splits
            )
        }
    }

    val wishlistItems = run {
        val wishDocs = eventRef.collection("wishlistItems").get().await()
        wishDocs.safeDocuments.map { doc ->
            val name = doc.getString("name") ?: ""
            val price = (doc.get("price") as? Number)?.toDouble()
            val status = doc.getString("status") ?: "AVAILABLE"
            val claimedBy = doc.getString("claimedBy")
            WishlistItemDetail(
                name = name,
                price = price,
                status = status,
                claimedByName = resolveName(claimedBy)
            )
        }
    }

    val tasks = run {
        val taskDocs = eventRef.collection("tasks").get().await()
        taskDocs.safeDocuments.map { doc ->
            val name = doc.getString("name") ?: ""
            val isCompleted = doc.getBoolean("isCompleted") ?: false
            @Suppress("UNCHECKED_CAST")
            val assignedTo = (doc.get("assignedTo") as? List<String>) ?: emptyList()
            val deadline = (doc.get("deadline") as? Number)?.toLong()
            TaskDetail(
                name = name,
                isCompleted = isCompleted,
                assignedToNames = assignedTo.mapNotNull { resolveName(it) },
                deadline = deadline
            )
        }
    }

    val packingItems = run {
        val packDocs = eventRef.collection("packingItems").get().await()
        packDocs.safeDocuments.map { doc ->
            val name = doc.getString("name") ?: ""
            val isChecked = doc.getBoolean("isChecked") ?: false
            val userId = doc.getString("userId")
            PackingItemDetail(
                name = name,
                isChecked = isChecked,
                responsibleName = resolveName(userId)
            )
        }
    }

    val carpoolRides = run {
        val rideDocs = eventRef.collection("carpoolRides").get().await()
        rideDocs.safeDocuments.map { rideDoc ->
            val driverId = rideDoc.getString("driverId") ?: ""
            val driverName = resolveName(driverId) ?: driverId
            val departureLocation = rideDoc.getString("departureLocation") ?: ""
            val departureTime = (rideDoc.get("departureTime") as? Number)?.toLong()
            val availableSeats = (rideDoc.get("availableSeats") as? Number)?.toInt() ?: 0
            val type = rideDoc.getString("type") ?: ""

            val passengerDocs = eventRef.collection("carpoolRides").document(rideDoc.id)
                .collection("passengers").get().await()
            val passengers = passengerDocs.safeDocuments.map { pDoc ->
                val pName = resolveName(pDoc.id) ?: pDoc.getString("displayName") ?: pDoc.id
                val pStatus = pDoc.getString("status") ?: "PENDING"
                CarpoolPassengerDetail(displayName = pName, status = pStatus)
            }
            CarpoolRideDetail(
                driverName = driverName,
                departureLocation = departureLocation,
                departureTime = departureTime,
                availableSeats = availableSeats,
                type = type,
                passengers = passengers
            )
        }
    }

    DetailedEventReport(
        eventName = eventDoc.getString("name") ?: "",
        eventDescription = eventDoc.getString("description") ?: "",
        eventTheme = eventDoc.getString("theme") ?: "",
        locationName = eventDoc.getString("locationName") ?: "",
        startDate = (eventDoc.get("startDate") as? Number)?.toLong(),
        endDate = (eventDoc.get("endDate") as? Number)?.toLong(),
        status = eventDoc.getString("status") ?: "",
        inviteCode = eventDoc.getString("inviteCode") ?: "",
        participants = participants,
        polls = pollsDeferred.await(),
        budgetCategories = budgetDeferred.await(),
        expenses = expenses,
        wishlistItems = wishlistItems,
        tasks = tasks,
        packingItems = packingItems,
        carpoolRides = carpoolRides
    )
}
