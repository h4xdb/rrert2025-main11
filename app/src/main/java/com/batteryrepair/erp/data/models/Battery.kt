package com.batteryrepair.erp.data.models

import android.os.Parcelable
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
    RECEIVED("Received", android.R.color.darker_gray),
    PENDING("Pending", android.R.color.holo_orange_light),
    READY("Ready", android.R.color.holo_green_light),
    DELIVERED("Delivered", android.R.color.holo_blue_light),
    RETURNED("Returned", android.R.color.holo_blue_dark),
    NOT_REPAIRABLE("Not Repairable", android.R.color.holo_red_light)
}