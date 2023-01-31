package com.android.example.geofences

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequest

import androidx.work.WorkManager

class GeofenceBroadcastReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val workManager = WorkManager.getInstance(context)
    val startServiceRequest = OneTimeWorkRequest.Builder(GeofenceWorker::class.java)
      .build()
    workManager.enqueue(startServiceRequest)
  }
}