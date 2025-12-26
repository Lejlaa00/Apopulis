package com.example.apopulis.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.apopulis.R
import com.example.apopulis.databinding.DialogNewsDetailBinding
import com.example.apopulis.model.NewsItem
import com.example.apopulis.util.DateFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

class NewsDetailDialogFragment : DialogFragment() {

    private var _binding: DialogNewsDetailBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val ARG_NEWS_ITEM = "news_item"

        fun newInstance(newsItem: NewsItem): NewsDetailDialogFragment {
            return NewsDetailDialogFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_NEWS_ITEM, newsItem)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.FullScreenDialogStyle)
        isCancelable = true // Allow dismissal with back button
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogNewsDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val newsItem = arguments?.getParcelable<NewsItem>(ARG_NEWS_ITEM)
        if (newsItem != null) {
            setupViews(newsItem)
            setupAnimation()
        } else {
            dismiss()
            return
        }

        binding.btnClose.setOnClickListener {
            dismiss()
        }
    }

    private fun setupAnimation() {
        // Start with dialog content invisible and translated down
        binding.root.alpha = 0f
        binding.root.translationY = 100f

        // Animate in with fade + slide up
        binding.root.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(300)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
            setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    private fun setupViews(newsItem: NewsItem) {
        // Title
        binding.tvTitle.text = newsItem.title

        // Author (if available in future API updates)
        // For now, this will be hidden since NewsItem doesn't have author field
        // When author field is added to NewsItem, uncomment and use:
        // newsItem.author?.let { author ->
        //     binding.tvAuthor.text = author
        //     binding.tvAuthor.visibility = View.VISIBLE
        // } ?: run {
        //     binding.tvAuthor.visibility = View.GONE
        // }
        binding.tvAuthor.visibility = View.GONE

        // Published time
        binding.tvPublishedTime.text = DateFormatter.formatRelativeTime(newsItem.publishedAt)

        // Location
        newsItem.locationId?.name?.let { locationName ->
            binding.tvLocation.text = locationName
            binding.layoutLocation.visibility = View.VISIBLE
        } ?: run {
            binding.layoutLocation.visibility = View.GONE
        }

        // Category
        newsItem.categoryId?.name?.let { categoryName ->
            binding.tvCategory.text = categoryName
            binding.tvCategory.visibility = View.VISIBLE
        } ?: run {
            binding.tvCategory.visibility = View.GONE
        }

        // Content
        val content = newsItem.content ?: newsItem.summary ?: ""
        binding.tvContent.text = content
        if (content.isEmpty()) {
            binding.tvContent.visibility = View.GONE
        }

        // Image
        newsItem.imageUrl?.let { imageUrl ->
            binding.imgNews.visibility = View.VISIBLE
            loadImage(imageUrl)
        } ?: run {
            binding.imgNews.visibility = View.GONE
        }

        // Stats
        val hasStats = newsItem.views > 0 || newsItem.likes > 0 || newsItem.commentsCount > 0
        if (hasStats) {
            binding.layoutStats.visibility = View.VISIBLE
            binding.tvViews.text = "👁 ${newsItem.views}"
            binding.tvLikes.text = "👍 ${newsItem.likes}"
            binding.tvComments.text = "💬 ${newsItem.commentsCount}"
        } else {
            binding.layoutStats.visibility = View.GONE
        }
    }

    private fun loadImage(imageUrl: String) {
        lifecycleScope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    try {
                        val url = URL(imageUrl)
                        val connection = url.openConnection()
                        connection.connectTimeout = 5000
                        connection.readTimeout = 5000
                        connection.getInputStream().use { input ->
                            BitmapFactory.decodeStream(input)
                        }
                    } catch (e: Exception) {
                        null
                    }
                }
                bitmap?.let {
                    binding.imgNews.setImageBitmap(it)
                } ?: run {
                    binding.imgNews.visibility = View.GONE
                }
            } catch (e: Exception) {
                binding.imgNews.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

