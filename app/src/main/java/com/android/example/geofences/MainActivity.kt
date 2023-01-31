package com.android.example.geofences

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.LocationManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.example.geofences.databinding.ActivityMainBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.material.snackbar.Snackbar

class MainActivity : BaseActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    companion object {
        private const val MY_LOCATION_REQUEST_CODE = 329
        private const val NEW_REMINDER_REQUEST_CODE = 330
        private const val EXTRA_LAT_LNG = "EXTRA_LAT_LNG"

        fun newIntent(context: Context, latLng: LatLng): Intent {
            return Intent(context, MainActivity::class.java).apply {
                putExtra(EXTRA_LAT_LNG, latLng)
            }
        }
    }

    private var googleMap: GoogleMap? = null
    private lateinit var locationManager: LocationManager
    private lateinit var binding: ActivityMainBinding

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(initViewBinding(layoutInflater))

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.apply {
            newGeofence.visibility = View.GONE

            newGeofence.setOnClickListener {
                googleMap?.run {
                    val intent = AddGeoFenceActivity.newIntent(
                        this@MainActivity,
                        cameraPosition.target,
                        cameraPosition.zoom
                    )
                    startActivityForResult(intent, NEW_REMINDER_REQUEST_CODE)
                }
            }
        }

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        askForLocationPermission()
    }

    private fun initViewBinding(inflater: LayoutInflater): View {
        binding = ActivityMainBinding.inflate(inflater)
        return binding.root
    }

    private fun askForLocationPermission() {
        if (!checkSinglePermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
            !checkSinglePermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                MY_LOCATION_REQUEST_CODE
            )
        }
    }

    private fun Context.checkSinglePermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            this, permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == NEW_REMINDER_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            showGeoFences()

            val reminder = getRepository().getLast()
            reminder?.latLng?.let { CameraUpdateFactory.newLatLngZoom(it, 15f) }
                ?.let { googleMap?.moveCamera(it) }

            Snackbar.make(binding.main, R.string.geofence_added_success, Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == MY_LOCATION_REQUEST_CODE) onMapAndPermissionReady()
    }

    private fun onMapAndPermissionReady() {
        googleMap?.let { map ->
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                map.isMyLocationEnabled = true
                binding.newGeofence.visibility = View.VISIBLE
                showGeoFences()
                centerCamera()
                goToMyPosition()
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun centerCamera() {
        intent.extras?.let { extras ->
            if (extras.containsKey(EXTRA_LAT_LNG)) {
                val latLng = extras.get(EXTRA_LAT_LNG) as LatLng
                googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
            }
        }
    }

    private fun goToMyPosition() {
        val bestProvider = locationManager.getBestProvider(Criteria(), false)
        val location = bestProvider?.let { it ->
            if (!checkSinglePermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
                !checkSinglePermission(Manifest.permission.ACCESS_COARSE_LOCATION)
            ) {
                return
            }
            locationManager.getLastKnownLocation(it)
        }
        location?.let {
            val latLng = LatLng(it.latitude, it.longitude)
            googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
        }
    }

    private fun showGeoFences() {
        googleMap?.run {
            clear()
            for (geofence in getRepository().getAll()) {
                showGeofenceInMap(this@MainActivity, this, geofence)
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap
        this.googleMap?.run {
            uiSettings.isMyLocationButtonEnabled = true
            uiSettings.isMapToolbarEnabled = false
            uiSettings.isZoomGesturesEnabled = true
            uiSettings.isZoomControlsEnabled = true
            uiSettings.setAllGesturesEnabled(true)
            setOnMarkerClickListener(this@MainActivity)
        }
        onMapAndPermissionReady()
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        val geofence = getRepository().get(marker.tag as String)
        geofence?.let {
            showRemoveGeofenceAlert(geofence)
        }
        return true
    }

    private fun showRemoveGeofenceAlert(geofenceData: GeofenceData) {
        val alertDialog = AlertDialog.Builder(this).create()
        alertDialog.run {
            setMessage(getString(R.string.geofence_removal_alert))
            setButton(
                AlertDialog.BUTTON_POSITIVE,
                getString(R.string.geofence_removal_alert_positive)
            ) { dialog, _ ->
                removeGeofence(geofenceData)
                dialog.dismiss()
            }
            setButton(
                AlertDialog.BUTTON_NEGATIVE,
                getString(R.string.geofence_removal_alert_negative)
            ) { dialog, _ ->
                dialog.dismiss()
            }
            show()
        }
    }

    private fun removeGeofence(geofenceData: GeofenceData) {
        getRepository().removeGeofence(
            geofenceData,
            success = {
                showGeoFences()
                Snackbar.make(binding.main, R.string.geofence_removed_success, Snackbar.LENGTH_LONG).show()
            },
            failure = {
                Snackbar.make(binding.main, it, Snackbar.LENGTH_LONG).show()
            })
    }
}