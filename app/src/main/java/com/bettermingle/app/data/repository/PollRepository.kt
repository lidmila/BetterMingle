package com.bettermingle.app.data.repository

import android.content.Context
import com.bettermingle.app.data.database.AppDatabase
import com.bettermingle.app.data.model.Poll
import com.bettermingle.app.data.model.PollOption
import com.bettermingle.app.data.model.PollType
import com.bettermingle.app.data.model.PollVote
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class PollRepository(context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val pollDao = db.pollDao()
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun getPollsByEvent(eventId: String): Flow<List<Poll>> =
        pollDao.getPollsByEvent(eventId)

    fun getOptionsByPoll(pollId: String): Flow<List<PollOption>> =
        pollDao.getOptionsByPoll(pollId)

    suspend fun createPoll(
        eventId: String,
        title: String,
        pollType: PollType,
        allowMultiple: Boolean = false,
        isAnonymous: Boolean = false,
        deadline: Long? = null,
        options: List<String>
    ): String {
        val userId = auth.currentUser?.uid ?: throw IllegalStateException("Not logged in")
        val pollId = UUID.randomUUID().toString()

        val poll = Poll(
            id = pollId,
            eventId = eventId,
            createdBy = userId,
            title = title,
            pollType = pollType,
            allowMultiple = allowMultiple,
            isAnonymous = isAnonymous,
            deadline = deadline
        )

        pollDao.insertPoll(poll)

        val pollOptions = options.mapIndexed { index, label ->
            PollOption(
                id = UUID.randomUUID().toString(),
                pollId = pollId,
                label = label,
                sortOrder = index
            )
        }
        pollOptions.forEach { pollDao.insertOption(it) }

        syncPollToCloud(eventId, poll, pollOptions)
        return pollId
    }

    suspend fun vote(eventId: String, pollId: String, optionId: String, value: Int = 1) {
        val userId = auth.currentUser?.uid ?: return
        val voteId = "${userId}_${optionId}"
        val vote = PollVote(id = voteId, optionId = optionId, userId = userId, value = value)

        try {
            firestore.collection("events").document(eventId)
                .collection("polls").document(pollId)
                .collection("options").document(optionId)
                .collection("votes").document(voteId)
                .set(mapOf("userId" to userId, "value" to value), SetOptions.merge())
                .await()
        } catch (_: Exception) { }
    }

    suspend fun closePoll(eventId: String, poll: Poll) {
        val closed = poll.copy(isClosed = true)
        pollDao.updatePoll(closed)
        try {
            firestore.collection("events").document(eventId)
                .collection("polls").document(poll.id)
                .update("isClosed", true)
                .await()
        } catch (_: Exception) { }
    }

    private suspend fun syncPollToCloud(eventId: String, poll: Poll, options: List<PollOption>) {
        try {
            val pollData = mapOf(
                "createdBy" to poll.createdBy,
                "title" to poll.title,
                "pollType" to poll.pollType.name,
                "allowMultiple" to poll.allowMultiple,
                "isAnonymous" to poll.isAnonymous,
                "deadline" to poll.deadline,
                "isClosed" to poll.isClosed,
                "createdAt" to poll.createdAt
            )
            val pollRef = firestore.collection("events").document(eventId)
                .collection("polls").document(poll.id)
            pollRef.set(pollData, SetOptions.merge()).await()

            options.forEach { option ->
                val optData = mapOf(
                    "label" to option.label,
                    "description" to option.description,
                    "sortOrder" to option.sortOrder
                )
                pollRef.collection("options").document(option.id)
                    .set(optData, SetOptions.merge()).await()
            }
        } catch (_: Exception) { }
    }

    suspend fun syncFromCloud(eventId: String) {
        try {
            val pollDocs = firestore.collection("events").document(eventId)
                .collection("polls").get().await()

            for (doc in pollDocs.documents) {
                val data = doc.data ?: continue
                val pollType = try { PollType.valueOf(data["pollType"] as? String ?: "CUSTOM") }
                catch (_: Exception) { PollType.CUSTOM }

                val poll = Poll(
                    id = doc.id,
                    eventId = eventId,
                    createdBy = data["createdBy"] as? String ?: "",
                    title = data["title"] as? String ?: "",
                    pollType = pollType,
                    allowMultiple = data["allowMultiple"] as? Boolean ?: false,
                    isAnonymous = data["isAnonymous"] as? Boolean ?: false,
                    deadline = (data["deadline"] as? Number)?.toLong(),
                    isClosed = data["isClosed"] as? Boolean ?: false,
                    createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                )
                pollDao.insertPoll(poll)

                val optDocs = firestore.collection("events").document(eventId)
                    .collection("polls").document(doc.id)
                    .collection("options").get().await()

                for (optDoc in optDocs.documents) {
                    val optData = optDoc.data ?: continue
                    val option = PollOption(
                        id = optDoc.id,
                        pollId = doc.id,
                        label = optData["label"] as? String ?: "",
                        description = optData["description"] as? String ?: "",
                        sortOrder = (optData["sortOrder"] as? Number)?.toInt() ?: 0
                    )
                    pollDao.insertOption(option)
                }
            }
        } catch (_: Exception) { }
    }
}
