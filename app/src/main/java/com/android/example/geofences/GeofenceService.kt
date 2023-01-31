package com.android.example.geofences

import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent


class GeofenceService : Service() {

    init {
        isServiceRunning = false
    }

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val geofencingEvent = intent?.let { GeofencingEvent.fromIntent(it) }
        geofencingEvent?.let { event ->
            if (event.hasError()) {
                val errorMessage = GeofenceErrorMessages.getErrorString(
                    this,
                    event.errorCode
                )
                Log.e(TAG, errorMessage)
            }

            if (event.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                val reminder = event.triggeringGeofences?.let { getFirstReminder(it) }
                val message = reminder?.message
                val latLng = reminder?.latLng
                if (message != null && latLng != null) {
                    sendNotification(this, message, latLng)
                }
            }
        }
        Log.d(TAG, "onStartCommand called")
        return START_STICKY
    }

    private fun getFirstReminder(triggeringGeoFences: List<Geofence>): GeofenceData? {
        val firstGeofence = triggeringGeoFences[0]
        return (application as GeofenceApplication).getRepository().get(firstGeofence.requestId)
    }

    @Suppress("DEPRECATION")
    override fun onDestroy() {
        Log.d(TAG, "onDestroy called")
        isServiceRunning = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) stopForeground(
            STOP_FOREGROUND_REMOVE
        ) else stopForeground(true)

        val broadcastIntent = Intent(this, GeofenceBroadcastReceiver::class.java)
        sendBroadcast(broadcastIntent)
        super.onDestroy()
    }

    companion object {
        var isServiceRunning: Boolean = false
        private const val TAG = "GeoFence service"
    }
}