package com.example.myapplication

import android.graphics.Color
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.util.Calendar

class LogAdapter : ListAdapter<LogEvento, LogAdapter.LogViewHolder>(LogDiffCallback()) {

    class LogViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val vStatusDot: View = view.findViewById(R.id.vStatusDot)
        val tvLogStatus: TextView = view.findViewById(R.id.tvLogStatus)
        val tvLogData: TextView = view.findViewById(R.id.tvLogData)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_log, parent, false)
        return LogViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        val log = getItem(position)
        
        val statusText = if (log.isOnline) "Ficou Online" else "Ficou Offline"
        val color = if (log.isOnline) Color.GREEN else Color.RED
        
        holder.tvLogStatus.text = statusText
        holder.vStatusDot.background.setTint(color)

        val cal = Calendar.getInstance().apply { timeInMillis = log.timestamp }
        holder.tvLogData.text = DateFormat.format("dd/MM HH:mm:ss", cal).toString()
    }

    class LogDiffCallback : DiffUtil.ItemCallback<LogEvento>() {
        override fun areItemsTheSame(oldItem: LogEvento, newItem: LogEvento): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: LogEvento, newItem: LogEvento): Boolean = oldItem == newItem
    }
}
