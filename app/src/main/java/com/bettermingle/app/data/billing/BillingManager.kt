package com.bettermingle.app.data.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.bettermingle.app.data.preferences.SettingsManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class SubscriptionProduct(
    val productId: String,
    val name: String,
    val description: String,
    val monthlyPrice: String,
    val yearlyPrice: String,
    val monthlyOfferToken: String?,
    val yearlyOfferToken: String?,
    val productDetails: ProductDetails?
)

data class BillingUiState(
    val isConnected: Boolean = false,
    val isPremium: Boolean = false,
    val products: List<SubscriptionProduct> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class BillingManager(
    private val context: Context,
    private val settingsManager: SettingsManager
) {
    companion object {
        private const val TAG = "BillingManager"
        const val PRODUCT_PRO = "mingle_pro"
        const val PRODUCT_BUSINESS = "mingle_business"
        const val PRODUCT_LIFETIME = "mingle_lifetime"
        private const val BASE_PLAN_MONTHLY = "monthly"
        private const val BASE_PLAN_YEARLY = "yearly"
    }

    private val scope = CoroutineScope(Dispatchers.Main)
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(BillingUiState())
    val uiState: StateFlow<BillingUiState> = _uiState

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    scope.launch { handlePurchase(purchase) }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d(TAG, "User cancelled purchase")
            }
            else -> {
                Log.e(TAG, "Purchase error: ${billingResult.debugMessage}")
                _uiState.value = _uiState.value.copy(error = "Nákup se nezdařil")
            }
        }
    }

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases()
        .build()

    fun startConnection() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing connected")
                    _uiState.value = _uiState.value.copy(isConnected = true, isLoading = false)
                    scope.launch {
                        queryProducts()
                        queryExistingPurchases()
                    }
                } else {
                    Log.e(TAG, "Billing setup failed: ${billingResult.debugMessage}")
                    _uiState.value = _uiState.value.copy(
                        isConnected = false,
                        isLoading = false,
                        error = "Připojení k obchodu selhalo"
                    )
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.d(TAG, "Billing disconnected")
                _uiState.value = _uiState.value.copy(isConnected = false)
            }
        })
    }

    private suspend fun queryProducts() {
        val subProductList = listOf(PRODUCT_PRO, PRODUCT_BUSINESS).map { productId ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }

        val subParams = QueryProductDetailsParams.newBuilder()
            .setProductList(subProductList)
            .build()

        val subResult = suspendCancellableCoroutine { cont ->
            billingClient.queryProductDetailsAsync(subParams) { billingResult, productDetailsList ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    cont.resume(productDetailsList)
                } else {
                    Log.e(TAG, "Query products failed: ${billingResult.debugMessage}")
                    cont.resume(emptyList())
                }
            }
        }

        // Query lifetime (INAPP) product
        val inappProductList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_LIFETIME)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val inappParams = QueryProductDetailsParams.newBuilder()
            .setProductList(inappProductList)
            .build()

        val inappResult = suspendCancellableCoroutine { cont ->
            billingClient.queryProductDetailsAsync(inappParams) { billingResult, productDetailsList ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    cont.resume(productDetailsList)
                } else {
                    Log.e(TAG, "Query INAPP products failed: ${billingResult.debugMessage}")
                    cont.resume(emptyList())
                }
            }
        }

        val result = subResult + inappResult

        val products = result.map { details ->
            val monthlyOffer = details.subscriptionOfferDetails?.find { offer ->
                offer.basePlanId == BASE_PLAN_MONTHLY
            }
            val yearlyOffer = details.subscriptionOfferDetails?.find { offer ->
                offer.basePlanId == BASE_PLAN_YEARLY
            }

            SubscriptionProduct(
                productId = details.productId,
                name = details.name,
                description = details.description,
                monthlyPrice = monthlyOffer?.pricingPhases?.pricingPhaseList
                    ?.lastOrNull()?.formattedPrice ?: "",
                yearlyPrice = yearlyOffer?.pricingPhases?.pricingPhaseList
                    ?.lastOrNull()?.formattedPrice ?: "",
                monthlyOfferToken = monthlyOffer?.offerToken,
                yearlyOfferToken = yearlyOffer?.offerToken,
                productDetails = details
            )
        }

        _uiState.value = _uiState.value.copy(products = products)
        Log.d(TAG, "Loaded ${products.size} products")
    }

    private suspend fun queryExistingPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        val result = suspendCancellableCoroutine { cont ->
            billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    cont.resume(purchases)
                } else {
                    cont.resume(emptyList())
                }
            }
        }

        val hasActiveSub = result.any { purchase ->
            purchase.purchaseState == Purchase.PurchaseState.PURCHASED
        }

        _uiState.value = _uiState.value.copy(isPremium = hasActiveSub)
        settingsManager.updatePremiumStatus(hasActiveSub, null)

        // Acknowledge any unacknowledged purchases
        result.filter {
            it.purchaseState == Purchase.PurchaseState.PURCHASED && !it.isAcknowledged
        }.forEach { purchase ->
            acknowledgePurchase(purchase)
        }
    }

    fun launchPurchaseFlow(
        activity: Activity,
        productDetails: ProductDetails,
        offerToken: String
    ) {
        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
            .setOfferToken(offerToken)
            .build()

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    private suspend fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                acknowledgePurchase(purchase)
            }

            // Update local premium status
            _uiState.value = _uiState.value.copy(isPremium = true)
            settingsManager.updatePremiumStatus(true, null)

            // Sync to Firestore
            syncPurchaseToCloud(purchase)

            Log.d(TAG, "Purchase successful: ${purchase.products}")
        }
    }

    private suspend fun acknowledgePurchase(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        suspendCancellableCoroutine { cont ->
            billingClient.acknowledgePurchase(params) { billingResult ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Purchase acknowledged")
                } else {
                    Log.e(TAG, "Acknowledge failed: ${billingResult.debugMessage}")
                }
                cont.resume(Unit)
            }
        }
    }

    private fun syncPurchaseToCloud(purchase: Purchase) {
        val userId = auth.currentUser?.uid ?: return
        val data = mapOf(
            "isPremium" to true,
            "purchaseToken" to purchase.purchaseToken,
            "products" to purchase.products,
            "purchaseTime" to purchase.purchaseTime,
            "updatedAt" to System.currentTimeMillis()
        )

        firestore.collection("users").document(userId)
            .update(data)
    }

    fun endConnection() {
        billingClient.endConnection()
    }
}
