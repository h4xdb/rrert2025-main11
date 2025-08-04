package com.batteryrepair.erp.data.repository

import com.batteryrepair.erp.data.models.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class FirebaseRepository {
    
    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    // References
    private val usersRef = database.getReference("users")
    private val customersRef = database.getReference("customers")
    private val batteriesRef = database.getReference("batteries")
    private val statusHistoryRef = database.getReference("status_history")
    private val staffNotesRef = database.getReference("staff_notes")
    private val settingsRef = database.getReference("settings")
    
    // Authentication
    suspend fun signIn(username: String, password: String): Result<User> {
        return try {
            // For demo purposes, we'll use email format for Firebase Auth
            val email = "$username@batteryrepair.local"
            val result = auth.signInWithEmailAndPassword(email, password).await()
            
            result.user?.let { firebaseUser ->
                val userSnapshot = usersRef.child(firebaseUser.uid).get().await()
                val user = userSnapshot.getValue(User::class.java)
                if (user != null) {
                    Result.success(user)
                } else {
                    Result.failure(Exception("User data not found"))
                }
            } ?: Result.failure(Exception("Authentication failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun getCurrentUser(): User? {
        return auth.currentUser?.let { firebaseUser ->
            // This would typically be cached or retrieved from local storage
            null // Placeholder - implement proper user caching
        }
    }
    
    fun signOut() {
        auth.signOut()
    }
    
    // Battery Operations
    suspend fun createBattery(battery: Battery): Result<String> {
        return try {
            val batteryId = generateBatteryId()
            val batteryWithId = battery.copy(id = batteriesRef.push().key ?: "", batteryId = batteryId)
            
            batteriesRef.child(batteryWithId.id).setValue(batteryWithId).await()
            
            // Add initial status history
            addStatusHistory(BatteryStatusHistory(
                id = statusHistoryRef.push().key ?: "",
                batteryId = batteryWithId.id,
                status = BatteryStatus.RECEIVED,
                comments = "Battery received from customer${if (battery.isPickup) " - Pickup service" else ""}",
                updatedBy = auth.currentUser?.uid ?: ""
            ))
            
            Result.success(batteryWithId.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateBatteryStatus(batteryId: String, status: BatteryStatus, comments: String, servicePrice: Double? = null): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any>(
                "status" to status
            )
            servicePrice?.let { updates["servicePrice"] = it }
            
            batteriesRef.child(batteryId).updateChildren(updates).await()
            
            // Add status history
            addStatusHistory(BatteryStatusHistory(
                id = statusHistoryRef.push().key ?: "",
                batteryId = batteryId,
                status = status,
                comments = comments,
                updatedBy = auth.currentUser?.uid ?: ""
            ))
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun getBatteries(): Flow<List<Battery>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val batteries = mutableListOf<Battery>()
                for (batterySnapshot in snapshot.children) {
                    batterySnapshot.getValue(Battery::class.java)?.let { battery ->
                        batteries.add(battery)
                    }
                }
                trySend(batteries.sortedByDescending { it.inwardDate })
            }
            
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        
        batteriesRef.addValueEventListener(listener)
        awaitClose { batteriesRef.removeEventListener(listener) }
    }
    
    fun getBatteriesByStatus(status: BatteryStatus): Flow<List<Battery>> = callbackFlow {
        val query = batteriesRef.orderByChild("status").equalTo(status.name)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val batteries = mutableListOf<Battery>()
                for (batterySnapshot in snapshot.children) {
                    batterySnapshot.getValue(Battery::class.java)?.let { battery ->
                        batteries.add(battery)
                    }
                }
                trySend(batteries.sortedByDescending { it.inwardDate })
            }
            
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        
        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }
    
    suspend fun searchBatteries(query: String): Result<List<Battery>> {
        return try {
            val snapshot = batteriesRef.get().await()
            val batteries = mutableListOf<Battery>()
            
            for (batterySnapshot in snapshot.children) {
                batterySnapshot.getValue(Battery::class.java)?.let { battery ->
                    if (battery.batteryId.contains(query, ignoreCase = true) ||
                        battery.batteryType.contains(query, ignoreCase = true)) {
                        batteries.add(battery)
                    }
                }
            }
            
            Result.success(batteries)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Customer Operations
    suspend fun createOrGetCustomer(customer: Customer): Result<String> {
        return try {
            // Check if customer exists by mobile number
            val existingCustomer = customersRef.orderByChild("mobile").equalTo(customer.mobile).get().await()
            
            if (existingCustomer.exists()) {
                val customerId = existingCustomer.children.first().key ?: ""
                Result.success(customerId)
            } else {
                val customerId = customersRef.push().key ?: ""
                val customerWithId = customer.copy(id = customerId)
                customersRef.child(customerId).setValue(customerWithId).await()
                Result.success(customerId)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getCustomer(customerId: String): Result<Customer> {
        return try {
            val snapshot = customersRef.child(customerId).get().await()
            val customer = snapshot.getValue(Customer::class.java)
            if (customer != null) {
                Result.success(customer)
            } else {
                Result.failure(Exception("Customer not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Status History
    private suspend fun addStatusHistory(statusHistory: BatteryStatusHistory) {
        statusHistoryRef.child(statusHistory.id).setValue(statusHistory).await()
    }
    
    fun getStatusHistory(batteryId: String): Flow<List<BatteryStatusHistory>> = callbackFlow {
        val query = statusHistoryRef.orderByChild("batteryId").equalTo(batteryId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val history = mutableListOf<BatteryStatusHistory>()
                for (historySnapshot in snapshot.children) {
                    historySnapshot.getValue(BatteryStatusHistory::class.java)?.let { item ->
                        history.add(item)
                    }
                }
                trySend(history.sortedByDescending { it.updatedAt })
            }
            
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        
        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }
    
    // Staff Notes
    suspend fun addStaffNote(note: StaffNote): Result<Unit> {
        return try {
            val noteWithId = note.copy(id = staffNotesRef.push().key ?: "")
            staffNotesRef.child(noteWithId.id).setValue(noteWithId).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun getStaffNotes(batteryId: String): Flow<List<StaffNote>> = callbackFlow {
        val query = staffNotesRef.orderByChild("batteryId").equalTo(batteryId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val notes = mutableListOf<StaffNote>()
                for (noteSnapshot in snapshot.children) {
                    noteSnapshot.getValue(StaffNote::class.java)?.let { note ->
                        notes.add(note)
                    }
                }
                trySend(notes.sortedByDescending { it.createdAt })
            }
            
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        
        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }
    
    // Utility Functions
    private suspend fun generateBatteryId(): String {
        return try {
            val prefix = getSetting("battery_id_prefix", "BAT")
            val startNum = getSetting("battery_id_start", "1").toInt()
            val padding = getSetting("battery_id_padding", "4").toInt()
            
            // Get the last battery to determine next number
            val snapshot = batteriesRef.orderByKey().limitToLast(1).get().await()
            val lastNum = if (snapshot.exists()) {
                val lastBattery = snapshot.children.first().getValue(Battery::class.java)
                lastBattery?.batteryId?.removePrefix(prefix)?.toIntOrNull() ?: (startNum - 1)
            } else {
                startNum - 1
            }
            
            val nextNum = lastNum + 1
            "$prefix${nextNum.toString().padStart(padding, '0')}"
        } catch (e: Exception) {
            "BAT${System.currentTimeMillis().toString().takeLast(4)}"
        }
    }
    
    private suspend fun getSetting(key: String, defaultValue: String): String {
        return try {
            val snapshot = settingsRef.child(key).get().await()
            snapshot.getValue(String::class.java) ?: defaultValue
        } catch (e: Exception) {
            defaultValue
        }
    }
    
    // Statistics
    suspend fun getDashboardStats(): Result<DashboardStats> {
        return try {
            val batteriesSnapshot = batteriesRef.get().await()
            val batteries = mutableListOf<Battery>()
            
            for (batterySnapshot in batteriesSnapshot.children) {
                batterySnapshot.getValue(Battery::class.java)?.let { battery ->
                    batteries.add(battery)
                }
            }
            
            val stats = DashboardStats(
                totalBatteries = batteries.size,
                pendingBatteries = batteries.count { it.status in listOf(BatteryStatus.RECEIVED, BatteryStatus.PENDING) },
                readyBatteries = batteries.count { it.status == BatteryStatus.READY },
                completedBatteries = batteries.count { it.status in listOf(BatteryStatus.DELIVERED, BatteryStatus.RETURNED) },
                notRepairableBatteries = batteries.count { it.status == BatteryStatus.NOT_REPAIRABLE },
                totalRevenue = batteries.filter { it.status in listOf(BatteryStatus.DELIVERED, BatteryStatus.RETURNED) }
                    .sumOf { it.servicePrice + if (it.isPickup) it.pickupCharge else 0.0 }
            )
            
            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class DashboardStats(
    val totalBatteries: Int,
    val pendingBatteries: Int,
    val readyBatteries: Int,
    val completedBatteries: Int,
    val notRepairableBatteries: Int,
    val totalRevenue: Double
)