package com.example.apopulis.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.apopulis.R
import com.example.apopulis.model.NewsItem
import com.example.apopulis.util.DateFormatter

class NewsAdapter(
    private val onItemClick: (NewsItem) -> Unit = {}
) : ListAdapter<NewsItem, NewsAdapter.NewsViewHolder>(NewsDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_news, parent, false)
        return NewsViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class NewsViewHolder(
        itemView: View,
        private val onItemClick: (NewsItem) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val titleView: TextView = itemView.findViewById(R.id.tvNewsTitle)
        private val summaryView: TextView = itemView.findViewById(R.id.tvNewsSummary)
        private val dateView: TextView = itemView.findViewById(R.id.tvNewsDate)
        private val viewsView: TextView = itemView.findViewById(R.id.tvViews)
        private val likesView: TextView = itemView.findViewById(R.id.tvLikes)

        fun bind(newsItem: NewsItem) {
            titleView.text = newsItem.title
            summaryView.text = newsItem.summary ?: newsItem.content?.take(100) ?: ""
            dateView.text = DateFormatter.formatRelativeTime(newsItem.publishedAt)
            viewsView.text = "👁 ${newsItem.views}"
            likesView.text = "👍 ${newsItem.likes}"

            // Set click listener on the entire item
            itemView.setOnClickListener {
                onItemClick(newsItem)
            }
        }
    }

    class NewsDiffCallback : DiffUtil.ItemCallback<NewsItem>() {
        override fun areItemsTheSame(oldItem: NewsItem, newItem: NewsItem): Boolean {
            return oldItem._id == newItem._id
        }

        override fun areContentsTheSame(oldItem: NewsItem, newItem: NewsItem): Boolean {
            return oldItem == newItem
        }
    }
}

