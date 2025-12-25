package com.example.apopulis.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.apopulis.R
import com.google.android.material.card.MaterialCardView

data class CategoryItem(
    val id: String,
    val name: String
)

class CategoryAdapter(
    private val onCategoryClick: (CategoryItem) -> Unit
) : ListAdapter<CategoryItem, CategoryAdapter.CategoryViewHolder>(CategoryDiffCallback()) {

    private var selectedPosition = RecyclerView.NO_POSITION

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_chip, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position), position == selectedPosition, onCategoryClick)
    }

    fun selectCategory(position: Int) {
        val previousPosition = selectedPosition
        selectedPosition = position

        if (previousPosition != RecyclerView.NO_POSITION) {
            notifyItemChanged(previousPosition)
        }
        if (position != RecyclerView.NO_POSITION) {
            notifyItemChanged(position)
        }
    }

    fun clearSelection() {
        val previousPosition = selectedPosition
        selectedPosition = RecyclerView.NO_POSITION
        if (previousPosition != RecyclerView.NO_POSITION) {
            notifyItemChanged(previousPosition)
        }
    }

    class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.cardView)
        private val categoryName: TextView = itemView.findViewById(R.id.tvCategoryName)

        fun bind(
            category: CategoryItem,
            isSelected: Boolean,
            onCategoryClick: (CategoryItem) -> Unit
        ) {
            categoryName.text = category.name

            // Update visual state
            val context = itemView.context
            if (isSelected) {
                // Selected state - purple background
                cardView.setCardBackgroundColor(
                    ContextCompat.getColor(context, R.color.apopulis_purple)
                )
                categoryName.setTextColor(
                    ContextCompat.getColor(context, android.R.color.white)
                )
                cardView.elevation = 4f
            } else {
                // Unselected state - default background
                val typedValue = android.util.TypedValue()
                context.theme.resolveAttribute(android.R.attr.colorBackground, typedValue, true)
                cardView.setCardBackgroundColor(typedValue.data)

                context.theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
                categoryName.setTextColor(typedValue.data)
                cardView.elevation = 2f
            }

            itemView.setOnClickListener {
                onCategoryClick(category)
            }
        }
    }

    class CategoryDiffCallback : DiffUtil.ItemCallback<CategoryItem>() {
        override fun areItemsTheSame(oldItem: CategoryItem, newItem: CategoryItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CategoryItem, newItem: CategoryItem): Boolean {
            return oldItem == newItem
        }
    }
}

