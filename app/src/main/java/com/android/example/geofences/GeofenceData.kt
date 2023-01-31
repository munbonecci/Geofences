package com.android.example.geofences

import com.google.android.gms.maps.model.LatLng
import java.util.*

data class GeofenceData(val id: String = UUID.randomUUID().toString(),
                        var latLng: LatLng?,
                        var radius: Double?,
                        var message: String?)