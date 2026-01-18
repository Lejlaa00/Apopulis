package com.example.apopulis.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.apopulis.MainActivity
import com.example.apopulis.R
import com.example.apopulis.network.RetrofitInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ViralNewsWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "ViralNewsWorker"
        private const val CHANNEL_ID = "viral_news_channel"
        private const val CHANNEL_NAME = "Viral News Notifications"
        private const val NOTIFICATION_ID = 1001
        private const val PREFS_NAME = "ApopulisSettings"
        private const val KEY_VIRAL_THRESHOLD = "viral_threshold"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_LAST_CHECK_TIME = "last_check_time"
        private const val DEFAULT_VIRAL_THRESHOLD = 5
    }

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting viral news check...")

                // Check if notifications are enabled
                val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val notificationsEnabled = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
                
                if (!notificationsEnabled) {
                    Log.d(TAG, "Notifications disabled, skipping check")
                    return@withContext Result.success()
                }

                // Get viral threshold from settings
                val viralThreshold = prefs.getInt(KEY_VIRAL_THRESHOLD, DEFAULT_VIRAL_THRESHOLD)
                
                // Get last check time
                val lastCheckTime = prefs.getLong(KEY_LAST_CHECK_TIME, System.currentTimeMillis() - 900000) // 15 min ago
                
                Log.d(TAG, "Checking for viral news with threshold: $viralThreshold")

                // Call backend to check for viral news
                val response = RetrofitInstance.newsApi.checkViralNews(
                    threshold = viralThreshold,
                    since = lastCheckTime
                )

                if (response.isSuccessful) {
                    val viralNews = response.body()
                    
                    Log.d(TAG, "Response: hasViralNews=${viralNews?.hasViralNews}, count=${viralNews?.viralNewsItems?.size}")
                    
                    if (viralNews != null && viralNews.hasViralNews && viralNews.viralNewsItems.isNotEmpty()) {
                        Log.d(TAG, "Found ${viralNews.viralNewsItems.size} viral news items - SHOWING NOTIFICATION")
                        
                        showNotification(viralNews.viralNewsItems.size, viralNews.viralNewsItems.firstOrNull()?.title)
                    } else {
                        Log.d(TAG, "No viral news found")
                    }

                    prefs.edit().putLong(KEY_LAST_CHECK_TIME, System.currentTimeMillis()).apply()
                    
                    Result.success()
                } else {
                    Log.e(TAG, "Error checking viral news: ${response.code()}")
                    Result.retry()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception checking viral news", e)
                Result.failure()
            }
        }
    }

    private fun showNotification(count: Int, firstTitle: String?) {
        try {
            Log.d(TAG, "showNotification() called - Creating notification channel")
            createNotificationChannel()

            val intent = Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            val pendingIntent = PendingIntent.getActivity(
                applicationContext,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val title = if (count == 1) "News is Going Viral!" else "$count News Items Going Viral!"
            val text = firstTitle ?: "Tap to see what's trending"
            
            Log.d(TAG, "Building notification: title=$title, text=$text")

            val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)

            Log.d(TAG, "✅ Notification posted successfully: $title")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error showing notification", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for viral news"
                enableLights(true)
                enableVibration(true)
            }

            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created: $CHANNEL_ID")
        }
    }
}
