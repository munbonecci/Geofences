package com.android.example.geofences

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.SeekBar
import com.android.example.geofences.databinding.ActivityNewGeofenceBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.snackbar.Snackbar
import kotlin.math.roundToInt


class AddGeoFenceActivity : BaseActivity(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private lateinit var binding: ActivityNewGeofenceBinding

    private var reminder = GeofenceData(latLng = null, radius = null, message = null)

    private val radiusBarChangeListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onStartTrackingTouch(seekBar: SeekBar?) {}
        override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            updateRadiusWithProgress(progress)
            showReminderUpdate()
        }
    }

    private fun updateRadiusWithProgress(progress: Int) {
        val radius = getRadius(progress)
        reminder.radius = radius
        binding.radiusDescription.text =
            getString(R.string.radius_description, radius.roundToInt().toString())
    }

    companion object {
        private const val EXTRA_LAT_LNG = "EXTRA_LAT_LNG"
        private const val EXTRA_ZOOM = "EXTRA_ZOOM"

        fun newIntent(context: Context, latLng: LatLng, zoom: Float): Intent {
            return Intent(context, AddGeoFenceActivity::class.java).apply {
                putExtra(EXTRA_LAT_LNG, latLng)
                putExtra(EXTRA_ZOOM, zoom)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(initViewBinding(layoutInflater))

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.apply {
            instructionTitle.visibility = View.GONE
            instructionSubtitle.visibility = View.GONE
            radiusBar.visibility = View.GONE
            radiusDescription.visibility = View.GONE
            message.visibility = View.GONE
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun initViewBinding(inflater: LayoutInflater): View {
        binding = ActivityNewGeofenceBinding.inflate(inflater)
        return binding.root
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap
        this.googleMap.apply {
            uiSettings.isMapToolbarEnabled = false
            uiSettings.isZoomGesturesEnabled = true
            uiSettings.isZoomControlsEnabled = true
        }
        centerCamera()
        showConfigureLocationStep()
    }

    @Suppress("DEPRECATION")
    private fun centerCamera() {
        intent.extras?.let { extras ->
            val latLng = extras.get(EXTRA_LAT_LNG) as LatLng
            val zoom = extras.getFloat(EXTRA_ZOOM)
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom))
        }
    }

    private fun showConfigureLocationStep() {
        binding.apply {
            marker.visibility = View.VISIBLE
            instructionTitle.visibility = View.VISIBLE
            instructionSubtitle.visibility = View.VISIBLE
            radiusBar.visibility = View.GONE
            radiusDescription.visibility = View.GONE
            message.visibility = View.GONE
            instructionTitle.text = getString(R.string.instruction_where_description)
            next.setOnClickListener {
                reminder.latLng = googleMap.cameraPosition.target
                showConfigureRadiusStep()
            }
        }
        showReminderUpdate()
    }

    private fun showConfigureRadiusStep() {
        binding.apply {
            marker.visibility = View.GONE
            instructionTitle.visibility = View.VISIBLE
            instructionSubtitle.visibility = View.GONE
            radiusBar.visibility = View.VISIBLE
            radiusDescription.visibility = View.VISIBLE
            message.visibility = View.GONE
            instructionTitle.text = getString(R.string.instruction_radius_description)
            next.setOnClickListener {
                showConfigureMessageStep()
            }
            radiusBar.setOnSeekBarChangeListener(radiusBarChangeListener)
            updateRadiusWithProgress(radiusBar.progress)
        }
        googleMap.animateCamera(CameraUpdateFactory.zoomTo(15f))
        showReminderUpdate()
    }

    private fun getRadius(progress: Int) = 100 + (2 * progress.toDouble() + 1) * 100

    private fun showConfigureMessageStep() {
        binding.apply {
            marker.visibility = View.GONE
            instructionTitle.visibility = View.VISIBLE
            instructionSubtitle.visibility = View.GONE
            radiusBar.visibility = View.GONE
            radiusDescription.visibility = View.GONE
            message.visibility = View.VISIBLE
            instructionTitle.text = getString(R.string.instruction_message_description)
            next.setOnClickListener {
                hideKeyboard(this@AddGeoFenceActivity, message)

                reminder.message = message.text.toString()

                if (reminder.message.isNullOrEmpty()) message.error =
                    getString(R.string.error_required)
                else addReminder(reminder)
            }
            message.requestFocusWithKeyboard()
        }
        showReminderUpdate()
    }

    private fun addReminder(reminder: GeofenceData) {
        getRepository().addGeofence(reminder,
            success = {
                setResult(Activity.RESULT_OK)
                finish()
            },
            failure = {
                Snackbar.make(binding.main, it, Snackbar.LENGTH_LONG).show()
            })
    }

    private fun showReminderUpdate() {
        googleMap.clear()
        showGeofenceInMap(this, googleMap, reminder)
    }
}