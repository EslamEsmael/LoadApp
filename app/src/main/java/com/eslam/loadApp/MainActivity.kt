package com.eslam.loadApp

import android.Manifest
import android.annotation.SuppressLint
import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.content.ContextCompat
import com.eslam.loadApp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private var downloadID: Long = 0

    private var downloadStatus = "Fail"

    private lateinit var selectedDownloadableUri: URL

    private val NOTIFICATION_ID = 0

    private lateinit var notificationManager: NotificationManager
    private lateinit var pendingIntent: PendingIntent
    private lateinit var action: NotificationCompat.Action

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        createChannel()
        registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))

        binding.downloadRadioGroup.setOnCheckedChangeListener { radioGroup, i ->
            selectedDownloadableUri = when (i) {
                R.id.retrofit_radio_button -> URL.RETROFIT_URI
                R.id.udacity_radio_button -> URL.UDACITY_URI
                R.id.glide_radio_button -> URL.GLIDE_URI
                else -> URL.RETROFIT_URI
            }
        }


        binding.customButton.setOnClickListener {
            if (this::selectedDownloadableUri.isInitialized) {
                val cm = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val activeNetwork: NetworkInfo? = cm.activeNetworkInfo
                val isConnected: Boolean = activeNetwork?.isConnectedOrConnecting == true
                if (isConnected) {
                    if (ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        binding.customButton.buttonState = ButtonState.Loading
                        download()
                    } else {
                        requestPermissions(
                            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                            PermissionInfo.PROTECTION_DANGEROUS
                        )
                    }
                } else {
                    Toast.makeText(
                        this,
                        getString(R.string.no_network_available),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                Toast.makeText(this, getString(R.string.select_something), Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (downloadID == id) {
                downloadStatus = "Success"
                binding.customButton.buttonState = ButtonState.Completed
                createNotification()
            }
        }
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun createNotification() {
        notificationManager = ContextCompat.getSystemService(
            this,
            NotificationManager::class.java
        ) as NotificationManager

        val detailIntent = Intent(this, DetailActivity::class.java)
        detailIntent.putExtra("fileName", selectedDownloadableUri.title)
        detailIntent.putExtra("status", downloadStatus)
        pendingIntent = TaskStackBuilder.create(this).run {
            addNextIntentWithParentStack(detailIntent)
            getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
        } as PendingIntent
        action = NotificationCompat.Action(
            R.drawable.ic_assistant_black_24dp,
            getString(R.string.notification_button),
            pendingIntent
        )

        val contentIntent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val contentPendingIntent = PendingIntent.getActivity(
            applicationContext,
            NOTIFICATION_ID,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_assistant_black_24dp)
            .setContentTitle(selectedDownloadableUri.title)
            .setContentText(selectedDownloadableUri.text)
            .setContentIntent(contentPendingIntent)
            .addAction(action)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    private fun createChannel() {
        val notificationChannel = NotificationChannel(
            CHANNEL_ID,
            "DownloadingChannel",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            setShowBadge(false)
        }
        notificationChannel.enableLights(true)
        notificationChannel.lightColor = Color.BLUE
        notificationChannel.enableVibration(true)
        notificationChannel.description = "Download completed!"

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(notificationChannel)
    }

    private fun download() {
        val request =
            DownloadManager.Request(Uri.parse(selectedDownloadableUri.uri))
                .setTitle(getString(R.string.app_name))
                .setDescription(getString(R.string.app_description))
                .setRequiresCharging(false)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

        val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        downloadID =
            downloadManager.enqueue(request)// enqueue puts the download request in the queue.
    }

    companion object {
        private enum class URL(val uri: String, val title: String, val text: String) {
            GLIDE_URI(
                "https://github.com/bumptech/glide/archive/master.zip",
                "Glide: Image Loading Library By BumpTech",
                "Glide repository is downloaded"
            ),
            UDACITY_URI(
                "https://github.com/udacity/nd940-c3-advanced-android-programming-project-starter/archive/master.zip",
                "Udacity: Android Kotlin Nanodegree",
                "The Project 3 repository is downloaded"
            ),
            RETROFIT_URI(
                "https://github.com/square/retrofit/archive/master.zip",
                "Retrofit: Type-safe HTTP client by Square, Inc",
                "Retrofit repository is downloaded"
            ),
        }

        private const val CHANNEL_ID = "channelId"
    }

}
