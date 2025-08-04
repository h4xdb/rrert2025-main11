package com.batteryrepair.erp.data.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class StaffNote(
    val id: String = "",
    val batteryId: String = "",
    val note: String = "",
    val noteType: NoteType = NoteType.FOLLOWUP,
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isResolved: Boolean = false,
    val createdByUser: User? = null // For joined data
) : Parcelable

enum class NoteType(val displayName: String, val colorRes: Int) {
    FOLLOWUP("Follow-up", android.R.color.holo_blue_light),
    REMINDER("Reminder", android.R.color.holo_orange_light),
    ISSUE("Issue", android.R.color.holo_red_light),
    RESOLVED("Resolved", android.R.color.holo_green_light)
}