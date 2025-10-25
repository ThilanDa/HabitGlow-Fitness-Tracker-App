package com.example.balance360.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.balance360.R
import com.example.balance360.data.MoodEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MoodAdapter(private val items: MutableList<MoodEntry>) : RecyclerView.Adapter<MoodAdapter.VH>() {
    private val fmt = SimpleDateFormat("EEE, HH:mm", Locale.getDefault())

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvEmoji: TextView = v.findViewById(R.id.tvEmoji)
        val tvNote: TextView = v.findViewById(R.id.tvNote)
        val tvTime: TextView = v.findViewById(R.id.tvTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_mood, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.tvEmoji.text = item.emoji
        holder.tvNote.text = item.note ?: ""
        holder.tvTime.text = fmt.format(Date(item.timestamp))
    }

    fun add(entry: MoodEntry) {
        items.add(0, entry)
        notifyItemInserted(0)
    }

    fun replace(newItems: List<MoodEntry>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
