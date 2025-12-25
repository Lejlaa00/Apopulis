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
import java.text.SimpleDateFormat
import java.util.*

class NewsAdapter : ListAdapter<NewsItem, NewsAdapter.NewsViewHolder>(NewsDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_news, parent, false)
        return NewsViewHolder(view)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class NewsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleView: TextView = itemView.findViewById(R.id.tvNewsTitle)
        private val summaryView: TextView = itemView.findViewById(R.id.tvNewsSummary)
        private val dateView: TextView = itemView.findViewById(R.id.tvNewsDate)
        private val viewsView: TextView = itemView.findViewById(R.id.tvViews)
        private val likesView: TextView = itemView.findViewById(R.id.tvLikes)

        fun bind(newsItem: NewsItem) {
            titleView.text = newsItem.title
            summaryView.text = newsItem.summary ?: newsItem.content?.take(100) ?: ""
            dateView.text = formatDate(newsItem.publishedAt)
            viewsView.text = "👁 ${newsItem.views}"
            likesView.text = "👍 ${newsItem.likes}"
        }

        private fun formatDate(dateString: String?): String {
            if (dateString == null) return "Recently"

            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                inputFormat.timeZone = TimeZone.getTimeZone("UTC")
                val date = inputFormat.parse(dateString)

                if (date != null) {
                    val now = Date()
                    val diff = now.time - date.time
                    val minutes = diff / (1000 * 60)
                    val hours = diff / (1000 * 60 * 60)
                    val days = diff / (1000 * 60 * 60 * 24)

                    when {
                        minutes < 1 -> "Just now"
                        minutes < 60 -> "$minutes m ago"
                        hours < 24 -> "$hours h ago"
                        days < 7 -> "$days d ago"
                        else -> {
                            val outputFormat = SimpleDateFormat("MMM d", Locale.getDefault())
                            outputFormat.format(date)
                        }
                    }
                } else {
                    "Recently"
                }
            } catch (e: Exception) {
                "Recently"
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

