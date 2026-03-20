package com.bettermingle.app.data.model

data class DetailedEventReport(
    val eventName: String,
    val eventDescription: String,
    val eventTheme: String,
    val locationName: String,
    val startDate: Long?,
    val endDate: Long?,
    val status: String,
    val inviteCode: String,
    val participants: List<ParticipantDetail>,
    val polls: List<PollDetail>,
    val budgetCategories: List<BudgetCategoryDetail>,
    val expenses: List<ExpenseDetail>,
    val wishlistItems: List<WishlistItemDetail>,
    val tasks: List<TaskDetail>,
    val packingItems: List<PackingItemDetail>,
    val carpoolRides: List<CarpoolRideDetail>
)

data class ParticipantDetail(
    val displayName: String,
    val rsvp: String
)

data class PollDetail(
    val title: String,
    val isClosed: Boolean,
    val options: List<PollOptionDetail>
)

data class PollOptionDetail(
    val label: String,
    val voteCount: Int
)

data class BudgetCategoryDetail(
    val name: String,
    val planned: Double,
    val actualTotal: Double
)

data class ExpenseDetail(
    val description: String,
    val amount: Double,
    val currency: String,
    val paidByName: String,
    val category: String,
    val splits: List<ExpenseSplitDetail>
)

data class ExpenseSplitDetail(
    val userName: String,
    val amount: Double,
    val isSettled: Boolean
)

data class WishlistItemDetail(
    val name: String,
    val price: Double?,
    val status: String,
    val claimedByName: String?
)

data class TaskDetail(
    val name: String,
    val isCompleted: Boolean,
    val assignedToNames: List<String>,
    val deadline: Long?
)

data class PackingItemDetail(
    val name: String,
    val isChecked: Boolean,
    val responsibleName: String?
)

data class CarpoolRideDetail(
    val driverName: String,
    val departureLocation: String,
    val departureTime: Long?,
    val availableSeats: Int,
    val type: String,
    val passengers: List<CarpoolPassengerDetail>
)

data class CarpoolPassengerDetail(
    val displayName: String,
    val status: String
)
