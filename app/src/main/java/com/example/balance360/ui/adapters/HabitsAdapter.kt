package com.example.balance360.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.balance360.R
import com.example.balance360.data.Habit
 

class HabitsAdapter(
    private val items: MutableList<Habit>,
    private val isCompleted: (Habit) -> Boolean,
    private val onToggle: (Habit, Boolean) -> Unit,
    private val onDelete: (Habit) -> Unit,
    private val onEdit: (Habit) -> Unit,
    private val getStreak: (Habit) -> Int
) : RecyclerView.Adapter<HabitsAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.tvName)
        val icon: TextView = v.findViewById(R.id.tvIcon)
        val cb: CheckBox = v.findViewById(R.id.cbDone)
        val delete: ImageView = v.findViewById(R.id.btnDelete)
        val edit: ImageView = v.findViewById(R.id.btnEdit)
        val desc: TextView = v.findViewById(R.id.tvDesc)
        val streak: TextView = v.findViewById(R.id.tvStreak)
        val completeLabel: TextView = v.findViewById(R.id.tvCompleteLabel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_habit, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.name.text = item.name
        holder.icon.text = item.icon ?: "🎯"
        // description under title
        val d = item.description?.trim().orEmpty()
        if (d.isNotEmpty()) {
            holder.desc.visibility = View.VISIBLE
            holder.desc.text = d
        } else {
            holder.desc.visibility = View.GONE
            holder.desc.text = ""
        }
        holder.cb.setOnCheckedChangeListener(null)
        holder.cb.isChecked = isCompleted(item)
        holder.cb.setOnCheckedChangeListener { _, checked -> onToggle(item, checked) }
        holder.delete.setOnClickListener { onDelete(item) }
        holder.edit.setOnClickListener { onEdit(item) }

        // streak binding
        val s = getStreak(item)
        if (s > 0) {
            holder.streak.visibility = View.VISIBLE
            holder.streak.text = holder.itemView.context.getString(R.string.streak_days, s)
        } else {
            holder.streak.visibility = View.GONE
            holder.streak.text = ""
        }

        // Show label only if not completed
        holder.completeLabel.visibility = if (!isCompleted(item)) View.VISIBLE else View.GONE
    }

    fun remove(item: Habit) {
        val idx = items.indexOfFirst { it.id == item.id }
        if (idx >= 0) {
            items.removeAt(idx)
            notifyItemRemoved(idx)
        }
    }

    fun add(item: Habit) {
        items.add(0, item)
        notifyItemInserted(0)
    }

    fun update(item: Habit) {
        val idx = items.indexOfFirst { it.id == item.id }
        if (idx >= 0) {
            items[idx] = item
            notifyItemChanged(idx)
        }
    }

    // Replace the entire dataset
    fun setItems(newItems: List<Habit>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    // Expose a read-only snapshot of current items
    fun currentItems(): List<Habit> = items.toList()
}


