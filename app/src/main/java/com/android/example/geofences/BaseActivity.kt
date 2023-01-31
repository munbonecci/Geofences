package com.android.example.geofences

import androidx.appcompat.app.AppCompatActivity

abstract class BaseActivity : AppCompatActivity() {
  fun getRepository() = (application as GeofenceApplication).getRepository()
}