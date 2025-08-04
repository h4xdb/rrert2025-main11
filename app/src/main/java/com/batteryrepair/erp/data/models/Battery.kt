package com.batteryrepair.erp.data.models

import android.os.Parcelable
import com.batteryrepair.erp.R
import kotlinx.parcelize.Parcelize

@Parcelize
data class Battery(
    val id: String = "",
    val batteryId: String = "",
    val customerId: String = "",
    val batteryType: String = "",
    val voltage: String = "",
    val capacity: String = "",
    val status: BatteryStatus = BatteryStatus.RECEIVED,
    val inwardDate: Long = System.currentTimeMillis(),
    val servicePrice: Double = 0.0,
    val pickupCharge: Double = 0.0,
    val isPickup: Boolean = false,
    val customer: Customer? = null // For joined data
) : Parcelable

enum class BatteryStatus(val displayName: String, val colorRes: Int) {
    RECEIVED("Received", R.color.status_received),
    PENDING("Pending", R.color.status_pending),
    READY("Ready", R.color.status_ready),
    DELIVERED("Delivered", R.color.status_delivered),
    RETURNED("Returned", R.color.status_returned),
    NOT_REPAIRABLE("Not Repairable", R.color.status_not_repairable)
}