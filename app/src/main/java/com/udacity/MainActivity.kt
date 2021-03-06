package com.udacity

import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import java.io.File


const val FILE_NAME = "File Name"
const val STATUS = "Status"

class MainActivity : AppCompatActivity() {

    private var downloadID: Long = 0
    private var selectedURL: String? = null
    private var fileName: String? = null
    private var status = " "

    private lateinit var notificationManager: NotificationManager
    private lateinit var pendingIntent: PendingIntent
    private lateinit var action: NotificationCompat.Action

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        status = getString(R.string.fail_text)

        registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))

        custom_button.setOnClickListener {
            download()
        }

        createChannel(getString(R.string.notification_id), getString(R.string.notification_title))
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?)
        {
            Log.i("MainActivity", "onReceive called")

            status = getString(R.string.success_text)

            val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)

            if(downloadID == id)
            {
                // Reset download state
                custom_button.buttonState = ButtonState.Completed

                val query = DownloadManager.Query().setFilterById(id)
                val downloadManager = context!!.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val cursor = downloadManager.query(query)
                if(cursor.moveToFirst()) {
                    val statusValue = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                    status = if (statusValue == DownloadManager.STATUS_SUCCESSFUL) {
                        getString(R.string.success_text)
                    } else {
                        getString(R.string.fail_text)
                    }
                }
            }

            notificationManager = ContextCompat.getSystemService(applicationContext, NotificationManager::class.java) as NotificationManager
            notificationManager.sendNotification(
                    applicationContext.getString(R.string.notification_description),
                    applicationContext)
        //if(this::notificationManager.isInitialized) { }
        }
    }

    private fun download() {
        // If a radio button has been selected
        if(selectedURL != null)
        {
            custom_button.buttonState = ButtonState.Loading

            // Make a directory
            val direct = File(getExternalFilesDir(null), "/repos")
            if (!direct.exists()) {
                direct.mkdirs()
            }

            Log.i("MainViewModel", "downloading $selectedURL")
            val request =
                    DownloadManager.Request(Uri.parse(selectedURL)) // previously was URL
                            .setTitle(getString(R.string.app_name))
                            .setDescription(getString(R.string.app_description))
                            .setRequiresCharging(false)
                            .setAllowedOverMetered(true)
                            .setAllowedOverRoaming(true)
                            // Save in directory
                            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,"/repos/repository.zip" )

            val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager

            downloadID =
                    downloadManager.enqueue(request)// enqueue puts the download request in the queue.

            // No longer creating an instance of the notification manager here

        } else
        {
            // If a radio button has not been selected
            Log.i("MainActivity", "File was not selected")

            // Set loading state to completed
            custom_button.buttonState = ButtonState.Completed

            // Show a toast to remind user to select a file
            Toast.makeText(this, "Please select a file to download", Toast.LENGTH_SHORT).show()

        }
    }

    private fun createChannel(channelId: String, channelName: String) {
        // 1: Safety check the OS version for API 26 and greater
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            // 2: Create a unique name for the notification channel
            val notificationChannel = NotificationChannel(
                    channelId,
                    channelName,
                    // Change importance
                    NotificationManager.IMPORTANCE_HIGH
            )

            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.RED
            notificationChannel.enableVibration(true)
            notificationChannel.description = "Download complete"
            val notificationManager = getSystemService(
                    NotificationManager::class.java
            )

            // 3: Create the channel using notificationManager
            notificationManager.createNotificationChannel(notificationChannel)

        }
    }


    fun onGlideSelected(view: View)
    {
        Log.i("MainActivity", "onGlideSelected")
        selectedURL = getString(R.string.glide_file_url)
        //selectedURL = "https://github.com/bumptech/glide/archive/refs/heads/master.zip"
        fileName = getString(R.string.glide_file_name)
    }

    fun onLoadAppSelected(view: View)
    {
        Log.i("MainActivity", "onLoadAppSelected")
        selectedURL = getString(R.string.udacity_file_url)
        fileName = getString(R.string.udacity_file_name)
    }

    fun onRetrofitSelected(view: View)
    {
        Log.i("MainActivity", "onRetrofitSelected")
        selectedURL = getString(R.string.retrofit_file_url)
        fileName = getString(R.string.retrofit_file_name)
    }

    // Pulled from NotificationUtils

    private val CHANNEL_ID = 0
    private val CHANNEL = "Downloads"

    // This is the code needed to create and trigger a notification
    fun NotificationManager.sendNotification(messageBody: String, applicationContext: Context) {

        val channelId = "${applicationContext.packageName}-${applicationContext.getString(R.string.app_name)}"

        // Create intent with applicationContext and activity to be launched
        val contentIntent = Intent(applicationContext, DetailActivity::class.java)
        contentIntent.putExtra(FILE_NAME, fileName)
        contentIntent.putExtra(STATUS, status)

        // Create pending intent - System will use the pending intent to open your app
        // The PendingIntent flag specifies the option to create a new pending intent or use an existing one
        pendingIntent = PendingIntent.getActivity(
                applicationContext,
                CHANNEL_ID,
                contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT)


        val happyImage = BitmapFactory.decodeResource(applicationContext.resources, R.drawable.done)
        val bigPicStyle = NotificationCompat.BigPictureStyle()
                .bigPicture(happyImage)
                .bigLargeIcon(null)

        // Get an instance of NotificationCompat.Builder
        val builder = NotificationCompat.Builder(
                applicationContext,
                applicationContext.getString(R.string.notification_id))
                // Set title, text and icon to builder
                .setSmallIcon(R.drawable.circle_icons_download)
                .setStyle(bigPicStyle)
                .setLargeIcon(happyImage)
                .setContentTitle(applicationContext.getString(R.string.notification_title))
                .setContentText(messageBody)

                // Pass pending intent to notification
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .addAction(0, resources.getString(R.string.click_message),pendingIntent)

        // Call notify to send the notification
        notify(CHANNEL_ID, builder.build())

        //LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(builder.build())
    }

}


