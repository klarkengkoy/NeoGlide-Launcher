package com.samidevstudio.neoglide.data.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.samidevstudio.neoglide.data.repository.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesRepository: UserPreferencesRepository,
) : PurchasesUpdatedListener {

    sealed class RestoreStatus {
        data object Idle : RestoreStatus()
        data object Processing : RestoreStatus()
        data object Success : RestoreStatus()
        data object NoPurchase : RestoreStatus()
        data object NoNetwork : RestoreStatus()
        data class Error(val message: String) : RestoreStatus()
    }

    private val scope = CoroutineScope(Dispatchers.IO)

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .enableAutoServiceReconnection()
        .build()

    private val _productDetails = MutableStateFlow<Map<String, ProductDetails>>(emptyMap())
    val productDetails = _productDetails.asStateFlow()

    private val _isBillingReady = MutableStateFlow(value = false)

    private val _restoreStatus = MutableStateFlow<RestoreStatus>(RestoreStatus.Idle)
    val restoreStatus = _restoreStatus.asStateFlow()

    companion object {
        const val PRODUCT_LIFETIME = "neoglide_premium_lifetime"
        const val PRODUCT_MONTHLY = "neoglide_premium_monthly"
        const val PRODUCT_YEARLY = "neoglide_premium_yearly"

        private val INAPP_PRODUCTS = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_LIFETIME)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        private val SUBS_PRODUCTS = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_MONTHLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_YEARLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )
    }

    fun startConnection() {
        billingClient.startConnection(
            object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d("BillingManager", "Billing setup finished")
                    _isBillingReady.value = true
                    queryProductDetails()
                    checkPurchases()
                } else {
                    Log.e("BillingManager", "Billing setup failed: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.d("BillingManager", "Billing service disconnected")
                _isBillingReady.value = false
            }
        })
    }

    private fun queryProductDetails() {
        val inAppParams = QueryProductDetailsParams.newBuilder()
            .setProductList(INAPP_PRODUCTS)
            .build()

        billingClient.queryProductDetailsAsync(inAppParams) { billingResult, queryProductDetailsResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val productDetailsList = queryProductDetailsResult.productDetailsList
                val detailsMap = productDetailsList.associateBy { it.productId }
                _productDetails.update { it + detailsMap }
                Log.d("BillingManager", "In-app product details queried: ${detailsMap.keys}")
            } else {
                Log.e("BillingManager", "Failed to query in-app products: ${billingResult.responseCode} - ${billingResult.debugMessage}")
            }
        }

        val subsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(SUBS_PRODUCTS)
            .build()

        billingClient.queryProductDetailsAsync(subsParams) { billingResult, queryProductDetailsResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val productDetailsList = queryProductDetailsResult.productDetailsList
                val detailsMap = productDetailsList.associateBy { it.productId }
                _productDetails.update { it + detailsMap }
                Log.d("BillingManager", "Subscription product details queried: ${detailsMap.keys}")
            } else {
                Log.e("BillingManager", "Failed to query subscription products: ${billingResult.responseCode} - ${billingResult.debugMessage}")
            }
        }
    }

    fun checkPurchases() {
        // Check for Lifetime (INAPP)
        val inAppParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(inAppParams) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val hasLifetime = purchases.any { purchase ->
                    (purchase.products.contains(PRODUCT_LIFETIME)) &&
                            (purchase.purchaseState == Purchase.PurchaseState.PURCHASED)
                }
                if (hasLifetime) {
                    updatePremiumStatus(true)
                } else {
                    // Only check subscriptions if lifetime is not owned
                    checkSubscriptions()
                }
            }
        }
    }

    fun checkSubscriptions() {
        val subsParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(subsParams) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val hasActiveSub = purchases.any { purchase ->
                    (purchase.products.contains(PRODUCT_MONTHLY) || purchase.products.contains(PRODUCT_YEARLY)) &&
                            purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                updatePremiumStatus(hasActiveSub)
            }
        }
    }

    fun restorePurchases() {
        if (!isNetworkAvailable()) {
            _restoreStatus.value = RestoreStatus.NoNetwork
            return
        }

        if (!_isBillingReady.value) {
            _restoreStatus.value = RestoreStatus.Error("Google Play Store is currently unavailable. Please ensure you are signed in to a Google Account and that the Play Store app is enabled.")
            return
        }

        _restoreStatus.value = RestoreStatus.Processing

        val inAppParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(inAppParams) { inAppResult, inAppPurchases ->
            if (inAppResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val hasLifetime = inAppPurchases.any { purchase ->
                    purchase.products.contains(PRODUCT_LIFETIME) &&
                            purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                }

                if (hasLifetime) {
                    updatePremiumStatus(true)
                    _restoreStatus.value = RestoreStatus.Success
                } else {
                    // Check subscriptions
                    val subsParams = QueryPurchasesParams.newBuilder()
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()

                    billingClient.queryPurchasesAsync(subsParams) { subsResult, subsPurchases ->
                        if (subsResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            val hasActiveSub = subsPurchases.any { purchase ->
                                (purchase.products.contains(PRODUCT_MONTHLY) || purchase.products.contains(PRODUCT_YEARLY)) &&
                                        purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                            }

                            if (hasActiveSub) {
                                updatePremiumStatus(true)
                                _restoreStatus.value = RestoreStatus.Success
                            } else {
                                updatePremiumStatus(false)
                                _restoreStatus.value = RestoreStatus.NoPurchase
                            }
                        } else {
                            val message = when (subsResult.responseCode) {
                                BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE -> "Network error: Play Store service is currently unavailable. Please try again later."
                                BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> "Google Play Billing is unavailable. Please check if your device is supported and you are signed in."
                                else -> subsResult.debugMessage.ifEmpty { "Error checking subscriptions" }
                            }
                            _restoreStatus.value = RestoreStatus.Error(message)
                        }
                    }
                }
            } else {
                val message = when (inAppResult.responseCode) {
                    BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE -> "Network error: Play Store service is currently unavailable. Please try again later."
                    BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> "Google Play Billing is unavailable. Please check if your device is supported and you are signed in."
                    else -> inAppResult.debugMessage.ifEmpty { "Error checking lifetime purchase" }
                }
                _restoreStatus.value = RestoreStatus.Error(message)
            }
        }
    }

    fun resetRestoreStatus() {
        _restoreStatus.value = RestoreStatus.Idle
    }

    fun launchBillingFlow(activity: Activity, productId: String) {
        val productDetails = _productDetails.value[productId] ?: return

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .apply {
                    productDetails.subscriptionOfferDetails?.firstOrNull()?.let {
                        setOfferToken(it.offerToken)
                    }
                }
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK if purchases != null -> {
                for (purchase in purchases) {
                    handlePurchase(purchase)
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d("BillingManager", "User canceled purchase")
            }
            else -> {
                Log.e("BillingManager", "Purchase failed: ${billingResult.debugMessage}")
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgeParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient.acknowledgePurchase(acknowledgeParams) { billingResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        Log.d("BillingManager", "Purchase acknowledged")
                        updatePremiumStatus(true)
                    }
                }
            } else {
                updatePremiumStatus(true)
            }
        }
    }

    private fun updatePremiumStatus(isPremium: Boolean) {
        scope.launch {
            preferencesRepository.updateIsPremium(isPremium)
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
