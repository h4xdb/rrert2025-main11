package com.batteryrepair.erp.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.batteryrepair.erp.data.repository.DashboardStats
import com.batteryrepair.erp.databinding.ItemDashboardStatBinding
import com.batteryrepair.erp.databinding.ItemDashboardActionBinding

enum class DashboardAction {
    NEW_BATTERY, TECHNICIAN_PANEL, SEARCH, REPORTS
}

class DashboardAdapter(
    private val onActionClick: (DashboardAction) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    
    private var stats: DashboardStats? = null
    private val actions = listOf(
        DashboardAction.NEW_BATTERY,
        DashboardAction.TECHNICIAN_PANEL,
        DashboardAction.SEARCH,
        DashboardAction.REPORTS
    )
    
    companion object {
        private const val TYPE_STATS = 0
        private const val TYPE_ACTION = 1
    }
    
    fun updateStats(newStats: DashboardStats) {
        stats = newStats
        notifyDataSetChanged()
    }
    
    override fun getItemViewType(position: Int): Int {
        return if (position < getStatsCount()) TYPE_STATS else TYPE_ACTION
    }
    
    override fun getItemCount(): Int = getStatsCount() + actions.size
    
    private fun getStatsCount(): Int = if (stats != null) 5 else 0
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_STATS -> {
                val binding = ItemDashboardStatBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                StatsViewHolder(binding)
            }
            else -> {
                val binding = ItemDashboardActionBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                ActionViewHolder(binding)
            }
        }
    }
    
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is StatsViewHolder -> {
                stats?.let { stats ->
                    when (position) {
                        0 -> holder.bind("Total Batteries", stats.totalBatteries.toString(), android.R.color.holo_blue_light)
                        1 -> holder.bind("Pending", stats.pendingBatteries.toString(), android.R.color.holo_orange_light)
                        2 -> holder.bind("Ready", stats.readyBatteries.toString(), android.R.color.holo_green_light)
                        3 -> holder.bind("Completed", stats.completedBatteries.toString(), android.R.color.holo_blue_dark)
                        4 -> holder.bind("Revenue", "₹${String.format("%.2f", stats.totalRevenue)}", android.R.color.holo_green_dark)
                    }
                }
            }
            is ActionViewHolder -> {
                val actionIndex = position - getStatsCount()
                val action = actions[actionIndex]
                holder.bind(action, onActionClick)
            }
        }
    }
    
    class StatsViewHolder(private val binding: ItemDashboardStatBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(title: String, value: String, colorRes: Int) {
            binding.tvTitle.text = title
            binding.tvValue.text = value
            binding.cardView.setCardBackgroundColor(binding.root.context.getColor(colorRes))
        }
    }
    
    class ActionViewHolder(private val binding: ItemDashboardActionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(action: DashboardAction, onActionClick: (DashboardAction) -> Unit) {
            val (title, icon) = when (action) {
                DashboardAction.NEW_BATTERY -> "Register New Battery" to android.R.drawable.ic_input_add
                DashboardAction.TECHNICIAN_PANEL -> "Technician Panel" to android.R.drawable.ic_menu_manage
                DashboardAction.SEARCH -> "Search Batteries" to android.R.drawable.ic_menu_search
                DashboardAction.REPORTS -> "Reports" to android.R.drawable.ic_menu_report_image
            }
            
            binding.tvTitle.text = title
            binding.ivIcon.setImageResource(icon)
            binding.root.setOnClickListener { onActionClick(action) }
        }
    }
}