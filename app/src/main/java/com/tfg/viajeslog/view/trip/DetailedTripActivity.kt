package com.tfg.viajeslog.view.trip

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.tfg.viajeslog.R
import com.tfg.viajeslog.view.adapters.StopAdapter
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.storage.FirebaseStorage
import com.tfg.viajeslog.view.stop.PostStopActivity
import com.tfg.viajeslog.view.tripExtra.AlbumFragment
import com.tfg.viajeslog.view.tripExtra.ShareTripFragment
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
    private lateinit var tripID: String
    private lateinit var initDate: Date
    private lateinit var tv_no_stop: TextView
    private lateinit var coordinates: ArrayList<GeoPoint>
    private var isReadOnly: Boolean = false
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detailed_trip)


        // Configura el SwipeRefreshLayout
        swipeRefreshLayout = findViewById(R.id.swipe)
        swipeRefreshLayout.setOnRefreshListener {
            // Aquí recargas las paradas
            stopViewModel.loadStopsForTrip(tripID)
            stopAdapter.notifyDataSetChanged()
        }

        swipeRefreshLayout.setProgressBackgroundColorSchemeColor(getResources().getColor(R.color.green))
        swipeRefreshLayout.setColorSchemeResources(R.color.yellow)

        //tv_title = findViewById(R.id.tv_title)
        fab_newStop = findViewById(R.id.fab_newStop)
        tv_no_stop = findViewById(R.id.tv_no_stop)
        toolbar = findViewById(R.id.toolbar)

        //Get Trip Intent
        tripID = intent.getStringExtra("id").toString()

        //Get Trip
        viewModel = ViewModelProvider(this).get(TripViewModel::class.java)
        viewModel.loadTrip(tripID)
        viewModel.trip.observe(this) {
            if (it != null) {
                toolbar.title = it.name.toString()
                initDate = it.initDate?.toDate() ?: Date(9999 - 12 - 31)
            }
        }

        setSupportActionBar(toolbar)

        // calling the action bar
        var actionBar = getSupportActionBar()
        actionBar!!.setDisplayHomeAsUpEnabled(true);


        // Obtener el flag de solo lectura
        isReadOnly = intent.getBooleanExtra("isReadOnly", false)

        // Configurar UI y funcionalidades según el modo
        if (isReadOnly) {
            // Ocultar el botón de agregar stop
            fab_newStop.visibility = View.GONE

            // Deshabilitar opciones del toolbar (editar/eliminar)
            //toolbar.menu.clear() // Elimina las opciones del menú si ya están configuradas
        }

        //Get Trip Stops
        stopRecyclerView = findViewById(R.id.rv_stops)
        stopRecyclerView.layoutManager = LinearLayoutManager(this)
        stopRecyclerView.setHasFixedSize(true)
        stopAdapter = StopAdapter(isReadOnly = isReadOnly)
        stopRecyclerView.adapter = stopAdapter

        stopViewModel = ViewModelProvider(this).get(StopViewModel::class.java)
        stopAdapter.tripID = tripID
        stopViewModel.stopsForTrip.observe(this) { stops ->
            if (stops.isEmpty()) {
                tv_no_stop.visibility = View.VISIBLE
                stopAdapter.updateStopList(emptyList())
            } else {
                tv_no_stop.visibility = View.GONE
                stopAdapter.updateStopList(stops)
                coordinates.clear()

                stops.forEach { stop ->
                    stop.geoPoint?.let { geoPoint ->
                        // Verificar que las coordenadas no sean (0.0, 0.0)
                        if (geoPoint.latitude != 0.0 || geoPoint.longitude != 0.0) {
                            coordinates.add(geoPoint)
                            LatLng(geoPoint.latitude, geoPoint.longitude).let { latLng ->
                                mMap.addMarker(
                                    MarkerOptions()
                                        .position(latLng)
                                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker))
                                )
                            }
                        }
                    }
                }
                setupMap()
            }

            // Detener el giro del SwipeRefreshLayout
            swipeRefreshLayout.isRefreshing = false
        }

        stopViewModel.loadStopsForTrip(tripID)
        coordinates = stopViewModel.getCoordinates(tripID)!!

        //New Stop
        fab_newStop.setOnClickListener {
            val intent = Intent(this, PostStopActivity::class.java)
            intent.putExtra("tripID", tripID) // Pasa el ID del viaje
            intent.putExtra("isEditMode", false) // Indica que estamos creando una nueva parada
            startActivity(intent)
        }

        //Map
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun setupMap() {
        if (::mMap.isInitialized) {
            if (coordinates.isNotEmpty()) {
                val bld = LatLngBounds.Builder()
                coordinates.forEach { geoPoint ->
                    val latLng = LatLng(geoPoint.latitude, geoPoint.longitude)
                    bld.include(latLng)
                }

                val bounds = bld.build()
                val mapView = (supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment).view
                mapView?.post {
                    try {
                        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150))
                        if (coordinates.size == 1) {
                            mMap.animateCamera(CameraUpdateFactory.zoomTo(12.5f))
                        }
                    } catch (e: Exception) {
                        Log.e("DetailedTripActivity", "Error adjusting map bounds: ${e.message}")
                    }
                }
            } else {
                // Mostrar una vista global
                val worldCenter = LatLng(0.0, 0.0) // Centro del mundo
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(worldCenter, 1.5f)) // Zoom bajo para ver continentes
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

        if (isReadOnly) {
            menu?.findItem(R.id.edit)?.isVisible = false
            menu?.findItem(R.id.add_from_img)?.isVisible = false
            menu?.findItem(R.id.share)?.isVisible = false
            menu?.findItem(R.id.delete)?.isVisible = false
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == android.R.id.home) {
            finish()
            return true
        }

        if (id == R.id.album) {
            val fragment = AlbumFragment()
            val bundle = Bundle()
            bundle.putString("trip", tripID)
            fragment.arguments = bundle
            supportFragmentManager.beginTransaction()
                .add(R.id.frame_layout, fragment)
                .addToBackStack(null)
                .commit()
        }

        if (id == R.id.edit) {
            val fragment = EditTripFragment()
            val bundle = Bundle()
            bundle.putString("trip", tripID)
            fragment.arguments = bundle
            supportFragmentManager.beginTransaction()
                .add(R.id.frame_layout, fragment)
                .addToBackStack(null)
                .commit()
        }


        if (id == R.id.delete) {
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            val db = FirebaseFirestore.getInstance()

            // Verificar si el usuario actual es administrador
            db.collection("trips").document(tripID).get().addOnSuccessListener { document ->
                if (document.exists()) {
                    val adminId =
                        document.getString("admin") // Asegúrate de que el campo "admin" contenga el UID del administrador
                    if (currentUserId == adminId) {
                        // Usuario es administrador: Mostrar diálogo de confirmación para eliminar
                        showDeleteConfirmationDialog { deleteTripCompletely() }
                    } else {
                        // Usuario no es administrador: Mostrar diálogo para eliminar de "members"
                        showDeleteConfirmationDialog { deleteFromMembers(currentUserId) }
                    }
                }
            }.addOnFailureListener { e ->
                Log.e("DeleteTrip", "Error al verificar rol: ${e.message}")
            }
        }

        if (id == R.id.add_from_img) {
            val intent = Intent(applicationContext, CreateFromImgActivity::class.java)
            intent.putExtra("tripID", tripID)
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            applicationContext.startActivity(intent)

        }

        if (id == R.id.share) {
            val fragment = ShareTripFragment()
            val bundle = Bundle()
            bundle.putString("trip", tripID)
            fragment.arguments = bundle
            supportFragmentManager.beginTransaction()
                .add(R.id.frame_layout, fragment)
                .addToBackStack(null)
                .commit()
        }
        return true
    }

    private fun showDeleteConfirmationDialog(onConfirm: () -> Unit) {
        val builder = AlertDialog.Builder(this, R.style.CustomDialogTheme)
        builder.setTitle("Confirmar eliminación")
        builder.setMessage("¿Estás seguro de que deseas eliminar este viaje? Esta acción no se puede deshacer.")
        builder.setPositiveButton("Eliminar") { dialog, _ ->
            onConfirm()
            dialog.dismiss()
            finish()
        }
        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.dismiss()
        }
        builder.create().show()
    }

    private fun deleteFromMembers(userId: String?) {
        val db = FirebaseFirestore.getInstance()
        if (userId != null) {
            db.collection("members").whereEqualTo("tripID", tripID).whereEqualTo("userID", userId)
                .get().addOnSuccessListener { snapshot ->
                    for (doc in snapshot) {
                        db.collection("members").document(doc.id).delete()
                    }
                }.addOnFailureListener { e ->
                    Log.e("DeleteTrip", "Error al eliminar de members: ${e.message}")
                }
        }
    }

    private fun deleteTripCompletely() {
        val db = FirebaseFirestore.getInstance()

        // 1. Eliminar los documentos relacionados en la colección "members"
        db.collection("members").whereEqualTo("tripID", tripID).get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot) {
                    db.collection("members").document(doc.id).delete()
                }
            }

        // 2. Eliminar las imágenes de "TripCover"
        val storage = FirebaseStorage.getInstance()
        val tripCoverPath = "TripCover/$tripID/"
        val tripCoverRef = storage.reference.child(tripCoverPath)
        tripCoverRef.listAll().addOnSuccessListener { listResult ->
            for (file in listResult.items) {
                file.delete() // Eliminar cada archivo en TripCover
            }
        }

        // 3. Eliminar las imágenes de "Stop_Image" asociadas a cada parada
        db.collection("trips").document(tripID).collection("stops").get()
            .addOnSuccessListener { stopsSnapshot ->
                for (stopDoc in stopsSnapshot) {
                    val stopId = stopDoc.id
                    val stopImagesPath = "Stop_Image/$tripID/$stopId/"
                    val stopImagesRef = storage.reference.child(stopImagesPath)
                    stopImagesRef.listAll().addOnSuccessListener { listResult ->
                        for (file in listResult.items) {
                            file.delete() // Eliminar cada archivo en Stop_Image
                        }
                    }
                }

                // 4. Eliminar el documento del viaje una vez que las imágenes y "members" se eliminaron
                db.collection("trips").document(tripID).delete().addOnSuccessListener {
                }.addOnFailureListener { e ->
                    Log.e("DeleteTrip", "Error al eliminar el viaje: ${e.message}")
                }
            }.addOnFailureListener { e ->
                Log.e("DeleteTrip", "Error al cargar las paradas: ${e.message}")
            }
    }

    override fun onResume() {
        super.onResume()
        stopViewModel.loadStopsForTrip(tripID)
        stopAdapter.notifyDataSetChanged()
    }

}