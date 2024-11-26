package com.tfg.viajeslog.view.stop

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tfg.viajeslog.R
import com.tfg.viajeslog.model.data.Stop
import com.tfg.viajeslog.view.adapters.ImageAdapter
import com.tfg.viajeslog.viewmodel.StopViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.tfg.viajeslog.model.data.Trip
import java.util.Calendar
import java.util.Date

class DetailedStopActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var stopID: String
    private lateinit var tripID: String
    private var stop: Stop? = null
    private lateinit var stopViewModel: StopViewModel

    private lateinit var tv_date: TextView
    private lateinit var tv_time: TextView
    private lateinit var tv_place: TextView
    private lateinit var tv_address: TextView
    private lateinit var tv_notes: TextView
    private lateinit var rv_images: RecyclerView
    private lateinit var timestampFb: Timestamp
    private lateinit var tv_name: TextView

    private lateinit var toolbar: Toolbar
    private lateinit var mapManager: SupportMapFragment
    private lateinit var mMap: GoogleMap
    private lateinit var latLng: LatLng

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detailed_stop)
        initLateinit()

        //Get Stop
        stopViewModel = ViewModelProvider(this).get(StopViewModel::class.java)
        stopViewModel.loadStop(tripID, stopID)
        stopViewModel.stop.observe(this, Observer {
            if (it != null) {
                tv_name.text = it.name

                var calendar = Calendar.getInstance()
                calendar.time = Date(it.timestamp!!.seconds * 1000)
                timestampFb = it.timestamp!!


                tv_date.text = String.format(
                    "%02d",
                    calendar.get(Calendar.DAY_OF_MONTH)
                ) + "/" + String.format("%02d", calendar.get(Calendar.MONTH)) + "/" + calendar.get(
                    Calendar.YEAR
                ).toString()

                tv_time.text =
                    String.format("%02d", calendar.get(Calendar.HOUR_OF_DAY)) + ":" + String.format(
                        "%02d",
                        calendar.get(Calendar.MINUTE)
                    )

                if (it.namePlace.toString().isNotBlank()) {
                    tv_place.text = it.namePlace.toString()
                } else {
                    tv_place.visibility = View.GONE
                }

                if (it.addressPlace.toString().isNotBlank()) {
                    tv_address.text = it.addressPlace.toString()
                } else {
                    tv_address.visibility = View.GONE
                }

                if (it.text.toString().isNotBlank()) {
                    tv_notes.text = it.text
                } else {
                    tv_notes.visibility = View.GONE
                }

                stop = it
                setupMap()
                loadMultimedia()
            }
        })

        //Mapa
        mapManager = supportFragmentManager.findFragmentById(R.id.mapStop) as SupportMapFragment
        mapManager.getMapAsync(this)

        //Toolbar
        setSupportActionBar(toolbar)
        getSupportActionBar()?.setDisplayHomeAsUpEnabled(true);
        getSupportActionBar()?.setDisplayShowHomeEnabled(true);

    }

    private fun loadMultimedia() {
        if (!stop?.photos.isNullOrEmpty()) {
            val layoutManager =
                LinearLayoutManager(rv_images.context, LinearLayoutManager.HORIZONTAL, false)
            var adapter = ImageAdapter(stop?.photos!!)
            rv_images.layoutManager = layoutManager
            rv_images.adapter = adapter
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isMyLocationButtonEnabled = false
        mMap.uiSettings.isZoomControlsEnabled = false
        mMap.uiSettings.isCompassEnabled = false

    }

    private fun setupMap() {
        if (stop != null) {
            latLng = LatLng(stop!!.geoPoint!!.latitude, stop!!.geoPoint!!.longitude)
            mMap.addMarker(
                MarkerOptions().position(latLng)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker))
            )

            val bld = LatLngBounds.Builder()
            bld.include(latLng)
            val bounds = bld.build()
            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 1))
            mMap.animateCamera(CameraUpdateFactory.zoomTo(12.5f))
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }

            R.id.delete -> {
                deleteStop()
            }

            R.id.edit -> {
                editStop()
            }
        }
        return true
    }

    private fun editStop() {
        val intent = Intent(this@DetailedStopActivity, PostStopActivity::class.java)
        intent.putExtra("tripID", tripID) // Pass the trip ID
        intent.putExtra("stopID", stopID) // Pass the stop ID to identify the stop
        intent.putExtra("isEditMode", true) // Indicate that this is the edit mode
        startActivity(intent)
        finish() // Finish the current activity if you don't want the user to navigate back to it
    }

    private fun deleteStop() {
        FirebaseFirestore.getInstance().collection("trips").document(tripID).collection("stops")
            .document(stopID).delete().addOnSuccessListener {
                finish()
            }

        // Actualizar las fechas del viaje (initDate, endDate, duración)
        FirebaseFirestore.getInstance().collection("trips").document(tripID).get()
            .addOnSuccessListener { document ->
                val trip = document.toObject(Trip::class.java)
                val currentInitDate = trip?.initDate ?: Timestamp(Date(9999, 12, 31))
                val currentEndDate = trip?.endDate ?: Timestamp(Date(0, 1, 1))

                var newInitDate = currentInitDate
                var newEndDate = currentEndDate

                // Comparar y actualizar initDate y endDate
                if (timestampFb.toDate().before(currentInitDate.toDate())) {
                    newInitDate = timestampFb
                }
                if (timestampFb.toDate().after(currentEndDate.toDate())) {
                    newEndDate = timestampFb
                }

                // Calcular duración en días
                val durationInDays =
                    ((newEndDate.seconds - newInitDate.seconds) / (60 * 60 * 24)).toInt()

                // Actualizar en la base de datos
                FirebaseFirestore.getInstance().collection("trips").document(tripID).update(
                        mapOf(
                            "initDate" to newInitDate,
                            "endDate" to newEndDate,
                            "duration" to durationInDays
                        )
                    ).addOnSuccessListener {
                        Toast.makeText(
                            applicationContext, "Fechas y duración actualizadas", Toast.LENGTH_SHORT
                        ).show()
                    }.addOnFailureListener { ex ->
                        Toast.makeText(
                            applicationContext,
                            "Error al actualizar fechas: ${ex.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.stop_toolbar, menu)
        return true
    }

    private fun initLateinit() {

        //Get Trip Intent
        stopID = intent.getStringExtra("stopID").toString()
        tripID = intent.getStringExtra("tripID").toString()
        stop = Stop()

        tv_date = findViewById(R.id.tv_date)
        tv_time = findViewById(R.id.tv_time)
        tv_place = findViewById(R.id.tvPlace)
        tv_address = findViewById(R.id.tvAddress)
        tv_notes = findViewById(R.id.tv_notes)
        rv_images = findViewById(R.id.rv_images)
        tv_name = findViewById(R.id.tvName)

        toolbar = findViewById(R.id.tb_stop)

    }

}