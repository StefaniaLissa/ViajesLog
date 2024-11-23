package com.tfg.viajeslog.view.trip

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tfg.viajeslog.R
import com.tfg.viajeslog.view.adapters.StopAdapter
import com.tfg.viajeslog.view.stop.CreateStopActivity
import com.tfg.viajeslog.viewmodel.StopViewModel
import com.tfg.viajeslog.viewmodel.TripViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.GeoPoint
import java.util.Date

class DetailedTripActivity : AppCompatActivity(), OnMapReadyCallback {

    //private lateinit var tv_title: TextView
    private lateinit var fab_newStop: FloatingActionButton
    private lateinit var toolbar: Toolbar
    private lateinit var viewModel: TripViewModel
    private lateinit var stopViewModel: StopViewModel
    private lateinit var stopRecyclerView: RecyclerView
    lateinit var stopAdapter: StopAdapter
    private lateinit var mMap: GoogleMap
    private lateinit var trip: String
    private lateinit var initDate: Date

    private lateinit var coordinates: ArrayList<GeoPoint>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detailed_trip)

        //tv_title = findViewById(R.id.tv_title)
        fab_newStop = findViewById(R.id.fab_newStop)
        toolbar = findViewById(R.id.toolbar)

        //Get Trip Intent
        trip = intent.getStringExtra("id").toString()
//        initDate = intent.getStringExtra("initDate").toString()

        //Get Trip
        viewModel = ViewModelProvider(this).get(TripViewModel::class.java)
        viewModel.loadTrip(trip)
        viewModel.trip.observe(this, Observer {
            if (it != null) {
                toolbar.title = it.name.toString()
                initDate = it.initDate?.toDate() ?: Date(9999 - 12 - 31)
            }
            Log.w("BD", "loadTripFragment")
        })

        setSupportActionBar(toolbar)

        // calling the action bar
        var actionBar = getSupportActionBar()
        actionBar!!.setDisplayHomeAsUpEnabled(true);
        //actionBar!!.setHomeAsUpIndicator(R.drawable.ic_arrow_back);


        //Get Trip Stops
        stopRecyclerView = findViewById(R.id.rv_stops)
        stopRecyclerView.layoutManager = LinearLayoutManager(this)
        stopRecyclerView.setHasFixedSize(true)
        stopAdapter = StopAdapter()
        stopRecyclerView.adapter = stopAdapter

        stopViewModel = ViewModelProvider(this).get(StopViewModel::class.java)
        stopViewModel.stopsForTrip.observe(this, Observer {
            stopAdapter.updateStopList(it)
            coordinates.clear()
            it.forEach {
//                val latLng = it.geoPoint?.let {
//                    LatLng(it.latitude, it.longitude)}
                it.geoPoint?.let { it2 ->
                    coordinates.add(it2)
                    LatLng(it2.latitude, it2.longitude).let {
                        mMap.addMarker(
                            MarkerOptions()
                                .position(it)
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker))
                        )
                    }
                }
            }
            stopAdapter.tripID = trip
            setupMap()
        })
        stopViewModel.loadStopsForTrip(trip)
        coordinates = stopViewModel.getCoordinates(trip)!!

//        stops = stopViewModel.getStops()!!

        //New Stop
        fab_newStop.setOnClickListener {
            val intent = Intent(this, CreateStopActivity::class.java)
            intent.putExtra("trip", trip)
            intent.putExtra("initDate", initDate)
            startActivity(intent)
        }

        //Map
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun setupMap() {
        coordinates.forEach { stop ->
            val latLng = stop.let {
                LatLng(it.latitude, it.longitude)
            }

            latLng?.let {
                mMap.addMarker(
                    MarkerOptions()
                        .position(it)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker))
                )
            }
        }

        // Cannot zoom to bounds until the map has a size.
        if (coordinates.size > 0) {
            val bld = LatLngBounds.Builder()
            for (i in 0 until coordinates.size) {
                val ll = LatLng(
                    coordinates.get(i).getLatitude(),
                    coordinates.get(i).getLongitude()
                )
                bld.include(ll)
            }

            val bounds = bld.build()
            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 70))
            if (coordinates.size == 1) {
                mMap.animateCamera(CameraUpdateFactory.zoomTo(12.5f))
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isMyLocationButtonEnabled = true
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.uiSettings.isCompassEnabled = true
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.trip_toolbar, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == R.id.album) {
            val fragment = AlbumFragment()
            val bundle = Bundle()
            bundle.putString("trip", trip)
            fragment.arguments = bundle
            supportFragmentManager.beginTransaction()
                .add(R.id.frame_layout, fragment)
                .addToBackStack(null)
                .commit()
        }
        if (id == R.id.share) {
            val fragment = ShareTripFragment()
            val bundle = Bundle()
            bundle.putString("trip", trip)
            fragment.arguments = bundle
            supportFragmentManager.beginTransaction()
                .add(R.id.frame_layout, fragment)
                .addToBackStack(null)
                .commit()
        }

        if (id == R.id.edit) {
            val fragment = EditTripFragment()
            val bundle = Bundle()
            bundle.putString("trip", trip)
            fragment.arguments = bundle
            supportFragmentManager.beginTransaction()
                .add(R.id.frame_layout, fragment)
                .addToBackStack(null)
                .commit()
        }

        if (id == android.R.id.home) {
            finish()
            return true
        }


        if (id == R.id.add_from_img) {
            val intent = Intent(applicationContext, CreateFromImgActivity::class.java)
            intent.putExtra("tripID", trip)
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            applicationContext.startActivity(intent)
            finish()

        }

        return true
    }

//    override fun onPause() {
//        super.onPause()
//        viewModel.trip.removeObservers(this)
//        stopViewModel.stopsForTrip.removeObservers(this)
//        finish()
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        viewModel.trip.removeObservers(this)
//        stopViewModel.stopsForTrip.removeObservers(this)
//    }
//
//    override fun onResume() {
//        super.onResume()
//
//        viewModel.trip.observe(this, Observer {
//            if (it != null) {
//                toolbar.title = it.name.toString()
//            }
//        })
//
//        stopViewModel.stopsForTrip.observe(this, Observer {
//            stopAdapter.updateStopList(it)
//            coordinates.clear()
//            it.forEach {
////                val latLng = it.geoPoint?.let {
////                    LatLng(it.latitude, it.longitude)}
//                it.geoPoint?.let { it2 ->
//                    coordinates.add(it2)
//                    LatLng(it2.latitude, it2.longitude).let {
//                        mMap.addMarker(
//                            MarkerOptions()
//                                .position(it)
//                        )
//                    }
//                }
//            }
//        })
//
//    }

}