package com.adityamhatre.bookingscheduler.service

import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.FileProvider
import com.adityamhatre.bookingscheduler.Application
import com.adityamhatre.bookingscheduler.BuildConfig
import com.adityamhatre.bookingscheduler.dtos.BookingDetails
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.io.File


class RenderService(private val context: Context) {
    private val queue = Volley.newRequestQueue(context)
    private val baseUrl = "https://vs-booking-scheduler-push-notify-server.onrender.com"

    private fun getAuthHeaders(): MutableMap<String, String> {
        val headers = HashMap<String, String>()
        headers["Content-Type"] = "application/json"
        try {
            val app = context.applicationContext as com.adityamhatre.bookingscheduler.Application
            val scope = "oauth2:https://www.googleapis.com/auth/calendar"
            val token = com.google.android.gms.auth.GoogleAuthUtil.getToken(context, app.account, scope)
            headers["Authorization"] = "Bearer $token"
        } catch (e: Exception) {
            Log.e("RenderService", "Failed to get Google OAuth token for backend authentication", e)
        }
        return headers
    }

    fun notifyNewBooking(bookingDetails: BookingDetails) {
        val request = object : JsonObjectRequest(
            Request.Method.POST,
            "$baseUrl/notifications/newBookingCreated",
            bookingDetails.toNotificationServerJson(),
            {},
            {}
        ) {
            override fun getHeaders(): MutableMap<String, String> = getAuthHeaders()
        }

        queue.addWithRetry(request)
    }

    fun notifyUpdateBooking(bookingDetails: BookingDetails) {
        val request = object : JsonObjectRequest(
            Request.Method.POST,
            "$baseUrl/notifications/updatedBooking",
            bookingDetails.toNotificationServerJson(),
            {},
            {}
        ) {
            override fun getHeaders(): MutableMap<String, String> = getAuthHeaders()
        }

        queue.addWithRetry(request)
    }

    fun removeBooking(bookingDetails: BookingDetails) {
        val request = object : JsonObjectRequest(
            Request.Method.POST,
            "$baseUrl/deleteBooking",
            bookingDetails.toNotificationServerJson(),
            {},
            {}
        ) {
            override fun getHeaders(): MutableMap<String, String> = getAuthHeaders()
        }

        queue.addWithRetry(request)
    }

    fun checkForUpdates() {
        val buildNumber = BuildConfig.VERSION_CODE
        val request = StringRequest(Request.Method.GET,
            "$baseUrl/checkForUpdates",
            { response ->
                val validResponse = JSONObject(response)
                if (validResponse.getLong("buildNumber") > buildNumber) {
                    Toast.makeText(
                        Application.getInstance().applicationContext,
                        "Downloading update...",
                        Toast.LENGTH_SHORT
                    ).show()
                    downloadAndInstall(validResponse.getString("downloadLink"))
                }
            },
            {}
        )
        queue.addWithRetry(request)
    }

    private fun downloadAndInstall(downloadLink: String) {
        if (!isExternalStorageWritable()) {
            return
        }

        val externalCacheFile = File(Application.getInstance().externalCacheDir, "update.apk")
        val request = FileRequest(
            downloadLink,
            {
                externalCacheFile.writeBytes(it)

                val urlApk: Uri = FileProvider.getUriForFile(
                    Application.getApplicationContext(),
                    BuildConfig.APPLICATION_ID + ".provider",
                    externalCacheFile
                )
                val intent = Intent(Intent.ACTION_VIEW)
                intent.setDataAndType(urlApk, "application/vnd.android.package-archive")
                intent.flags = FLAG_GRANT_READ_URI_PERMISSION or FLAG_ACTIVITY_NEW_TASK

                startActivity(Application.getApplicationContext(), intent, null)
            },
            { Log.e("RenderService.downloadAndInstall", it?.message.toString()) }
        )

        queue.addWithRetry(request)
    }

    private fun isExternalStorageWritable(): Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }

    fun getBookingSummary(callback: (JSONObject) -> Unit) {
        val startTime = System.currentTimeMillis()
        val request = StringRequest(Request.Method.GET,
            "$baseUrl/bookingsSummary",
            { response ->
                println("getBookingSummary took ${System.currentTimeMillis() - startTime}ms")
                val validResponse = JSONObject(response)
                callback(validResponse)
            },
            { error ->
                println("getBookingSummary failed after ${System.currentTimeMillis() - startTime}ms: ${error.message}")
            }
        )

        request.setShouldCache(false)
        queue.addWithRetry(request)
    }

}

private fun <T> RequestQueue.addWithRetry(request: Request<T>) {
    request.retryPolicy = DefaultRetryPolicy(2 * 60 * 1000, 3, 2f)
    this.add(request)
    val log = if (request.method == 0) "GET" else if (request.method == 1) "POST" else "UNKNOWN"
    println("Adding request: $log ${request.url} to queue")
}
