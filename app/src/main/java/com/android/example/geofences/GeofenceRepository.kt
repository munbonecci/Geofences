package com.android.example.geofences

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import java.util.concurrent.TimeUnit

@SuppressLint("UnspecifiedImmutableFlag")
class GeofenceRepository(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "GeofenceRepository"
        private const val REMINDERS = "REMINDERS"
        const val UNIQUE_WORK_NAME = "StartMyServiceViaWorker"
    }

    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val geofencingClient = LocationServices.getGeofencingClient(context)
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
        } else {
            PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }

    fun addGeofence(
        reminder: GeofenceData,
        success: () -> Unit,
        failure: (error: String) -> Unit
    ) {
        val geofence = buildGeofence(reminder)
        if (geofence != null
            && ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            geofencingClient
                .addGeofences(buildGeofencingRequest(geofence), geofencePendingIntent)
                .addOnSuccessListener {
                    saveAll(getAll() + reminder)
                    success()
                    startServiceViaWorker(context)
                }
                .addOnFailureListener {
                    failure(GeofenceErrorMessages.getErrorString(context, it))
                }
        }
    }

    private fun buildGeofence(reminder: GeofenceData): Geofence? {
        val latitude = reminder.latLng?.latitude
        val longitude = reminder.latLng?.longitude
        val radius = reminder.radius

        if (latitude != null && longitude != null && radius != null) {
            return Geofence.Builder()
                .setRequestId(reminder.id)
                .setCircularRegion(
                    latitude,
                    longitude,
                    radius.toFloat()
                )
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .build()
        }

        return null
    }

    private fun buildGeofencingRequest(geofence: Geofence): GeofencingRequest {
        return GeofencingRequest.Builder()
            .setInitialTrigger(0)
            .addGeofences(listOf(geofence))
            .build()
    }

    fun removeGeofence(
        reminder: GeofenceData,
        success: () -> Unit,
        failure: (error: String) -> Unit
    ) {
        geofencingClient
            .removeGeofences(listOf(reminder.id))
            .addOnSuccessListener {
                saveAll(getAll() - reminder)
                success()
            }
            .addOnFailureListener {
                failure(GeofenceErrorMessages.getErrorString(context, it))
            }
    }

    private fun saveAll(list: List<GeofenceData>) {
        preferences
            .edit()
            .putString(REMINDERS, gson.toJson(list))
            .apply()
    }

    fun getAll(): List<GeofenceData> {
        if (preferences.contains(REMINDERS)) {
            val remindersString = preferences.getString(REMINDERS, null)
            val arrayOfGeofenceData = gson.fromJson(
                remindersString,
                Array<GeofenceData>::class.java
            )
            if (arrayOfGeofenceData != null) {
                return arrayOfGeofenceData.toList()
            }
        }
        return listOf()
    }

    fun get(requestId: String?) = getAll().firstOrNull { it.id == requestId }

    fun getLast() = getAll().lastOrNull()

    private fun startServiceViaWorker(context: Context) {
        val workManager = WorkManager.getInstance(context)

        val request = PeriodicWorkRequest.Builder(
            GeofenceWorker::class.java,
            16,
            TimeUnit.MINUTES
        ).build()

        workManager.enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

}