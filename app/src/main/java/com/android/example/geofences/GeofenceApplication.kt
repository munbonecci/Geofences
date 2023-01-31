package com.android.example.geofences

import android.app.Application

class GeofenceApplication : Application() {

  private lateinit var repository: GeofenceRepository

  override fun onCreate() {
    super.onCreate()
    repository = GeofenceRepository(this)
  }

  fun getRepository() = repository
}