package com.tfg.viajeslog.view.stop

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.*
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.common.api.Status
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.storage.FirebaseStorage
import com.tfg.viajeslog.R
import com.tfg.viajeslog.model.data.Stop
import com.tfg.viajeslog.model.data.Trip
import com.tfg.viajeslog.view.adapters.ImageAdapter
import com.tfg.viajeslog.viewmodel.StopViewModel
import java.text.SimpleDateFormat
import java.util.*

class PostStopActivity : AppCompatActivity() {

    companion object {
        const val MODE_CREATE = "CREATE"
        const val MODE_EDIT = "EDIT"
    }

    // Views
    private lateinit var etName: EditText
    private lateinit var etDescription: EditText
    private lateinit var btnGallery: Button
    private lateinit var btnCamera: Button
    private lateinit var rvImages: RecyclerView
    private lateinit var tvDate: TextView
    private lateinit var tvTime: TextView
    private lateinit var fabDone: FloatingActionButton
    private lateinit var sv_images: ScrollView

    // Firebase
    private lateinit var db: FirebaseFirestore
    private var imagesList: ArrayList<String> = ArrayList()

    // Variables
    private lateinit var adapter: ImageAdapter
    private lateinit var layoutManager: GridLayoutManager
    private var uri: Uri? = null
    private lateinit var mode: String
    private lateinit var tripID: String
    private var stopID: String? = null
    private var stop: Stop? = null
    private var timestampFb: Timestamp = Timestamp(Date())
    private var geoPoint: GeoPoint = GeoPoint(0.0, 0.0)

    // Places
    private var placeFragment: AutocompleteSupportFragment? = null
    private var idPlace: String = ""
    private var namePlace: String = ""
    private var addressPlace: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_stop)
        init()

        val isEditMode = intent.getBooleanExtra("isEditMode", false)
        tripID = intent.getStringExtra("tripID") ?: ""
        stopID = intent.getStringExtra("stopID") ?: ""
        val userName = FirebaseAuth.getInstance().currentUser?.email ?: ""

        if (isEditMode) {
            mode = MODE_EDIT
            val stopID = intent.getStringExtra("stopID") ?: ""
            checkEditingState(tripID, stopID, userName)
            loadStopData(tripID, stopID) // Cargar datos de la parada existente
        } else {
            mode = MODE_CREATE
            setupForNewStop() // Preparar para crear una nueva parada
        }
    }
    private fun checkEditingState(tripID: String, stopID: String, userName: String) {
        val stopDoc = db.collection("trips").document(tripID).collection("stops").document(stopID)

        stopDoc.get().addOnSuccessListener { document ->
            val editingUser = document.getString("editing")
            if (editingUser != null && editingUser != userName) {
                // Otro usuario está editando
                Toast.makeText(
                    this,
                    "Esta parada está siendo editada por $editingUser.",
                    Toast.LENGTH_LONG
                ).show()
                finish() // Salir de la actividad
            } else {
                // No hay nadie editando, actualizar el campo
                stopDoc.update("editing", userName).addOnSuccessListener {
                    loadStopData(tripID, stopID)
                }.addOnFailureListener {
                    Toast.makeText(this, "No se pudo establecer el estado de edición.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Error al verificar el estado de edición.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun init() {
        etName = findViewById(R.id.et_name)
        etDescription = findViewById(R.id.et_description)
        btnGallery = findViewById(R.id.btn_gallery)
        btnCamera = findViewById(R.id.btn_camera)
        rvImages = findViewById(R.id.rv_images)
        sv_images = findViewById(R.id.sv_images)
        tvDate = findViewById(R.id.tv_date)
        tvTime = findViewById(R.id.tv_time)
        fabDone = findViewById(R.id.fab_done)
        db = FirebaseFirestore.getInstance()

        // Setup RecyclerView
        layoutManager = GridLayoutManager(this, 3)
        //adapter = ImageAdapter(imagesList)
        adapter = ImageAdapter(imagesList) { imageUrl ->
            // Eliminar la imagen de la lista
            imagesList.remove(imageUrl)
            adapter.notifyDataSetChanged() // Actualizar el RecyclerView

            // Opcional: Mostrar un mensaje
            Toast.makeText(this, "Imagen eliminada", Toast.LENGTH_SHORT).show()
        }
        rvImages.layoutManager = layoutManager
        rvImages.adapter = adapter

        placeFragment =
            supportFragmentManager.findFragmentById(R.id.fg_autocomplete) as AutocompleteSupportFragment?

        //Personalizando el Fragment de Google Places
        placeFragment!!.view?.findViewById<EditText>(com.google.android.libraries.places.R.id.places_autocomplete_search_input)
            ?.setTextColor(getResources().getColor(R.color.black))
        placeFragment!!.view?.findViewById<ImageButton>(com.google.android.libraries.places.R.id.places_autocomplete_search_button)
            ?.setColorFilter(getResources().getColor(R.color.black))
        placeFragment!!.view?.findViewById<ImageButton>(com.google.android.libraries.places.R.id.places_autocomplete_clear_button)
            ?.setColorFilter(getResources().getColor(R.color.black))

        // Recuperar API KEY
        val ai: ApplicationInfo? = applicationContext.packageManager?.getApplicationInfo(
            applicationContext.packageName, PackageManager.GET_META_DATA
        )
        val apiKey = ai?.metaData?.getString("com.google.android.geo.API_KEY").toString()

        // Initialize Places API
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, apiKey)
        }

        placeFragment!!.setPlaceFields(
            listOf(
                Place.Field.NAME,
                Place.Field.ID,
                Place.Field.LAT_LNG,
                Place.Field.ADDRESS,
                Place.Field.ADDRESS_COMPONENTS
            )
        )

        setupListeners()

    }

    private fun setupForNewStop() {
        //Cargar Fecha y Hora actual
        tvDate.text = SimpleDateFormat(
            "dd 'de' MMMM 'de' yyyy", Locale.getDefault()
        ).format(Calendar.getInstance().timeInMillis)
        tvTime.text = SimpleDateFormat("HH:mm").format(Calendar.getInstance().timeInMillis)
        timestampFb = Timestamp(Calendar.getInstance().time)
    }

    private fun setupListeners() {

        // Places Display the fetched information after clicking on one of the options
        placeFragment!!.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                idPlace = place.id!!.toString()
                namePlace = place.name!!.toString()
                addressPlace = place.address!!.toString()
                geoPoint = GeoPoint(place.latLng!!.latitude, place.latLng!!.longitude)

            }

            override fun onError(status: Status) {
                Toast.makeText(applicationContext, "Some error occurred", Toast.LENGTH_SHORT).show()
            }
        })

        // Date Picker
        tvDate.setOnClickListener {
            val datePickerDialog = DatePickerDialog(
                this,
                R.style.CustomDatePickerTheme,
                { _, year, month, day ->
                    val calendar = Calendar.getInstance()
                    calendar.set(year, month, day)
                    tvDate.text =
                        SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale.getDefault()).format(
                            calendar.time
                        )
                    timestampFb = Timestamp(calendar.time)
                },
                Calendar.getInstance().get(Calendar.YEAR),
                Calendar.getInstance().get(Calendar.MONTH),
                Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        }

        // Time Picker
        tvTime.setOnClickListener {
            TimePickerDialog(
                this,
                R.style.CustomTimePickerTheme,
                { _, hour, minute ->
                    val calendar = Calendar.getInstance()
                    calendar.set(Calendar.HOUR_OF_DAY, hour)
                    calendar.set(Calendar.MINUTE, minute)
                    tvTime.text =
                        SimpleDateFormat("HH:mm", Locale.getDefault()).format(calendar.time)
                    timestampFb = Timestamp(calendar.time)
                },
                Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
                Calendar.getInstance().get(Calendar.MINUTE),
                true
            ).show()
        }

        // Gallery
        btnGallery.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    applicationContext, Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                openGallery()
            } else {
                galleryPermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        // Camera
        btnCamera.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    applicationContext, Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                openCamera()
            } else {
                cameraPermission.launch(Manifest.permission.CAMERA)
            }
        }

        // Save or Update
        fabDone.setOnClickListener {
            if (mode == MODE_CREATE) {
                createStop()
            } else if (mode == MODE_EDIT) {
                updateStop()
            }
            updateTrip()
        }
    }

    private fun updateTrip() {
        // Actualizar las fechas del viaje (initDate, endDate, duración)
        db.collection("trips").document(tripID).get().addOnSuccessListener { document ->
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
            val durationInDays = (
                    (newEndDate.seconds - newInitDate.seconds) / (60 * 60 * 24)
                    ).toInt()

            // Actualizar en la base de datos
            db.collection("trips").document(tripID)
                .update(
                    mapOf(
                        "initDate" to newInitDate,
                        "endDate" to newEndDate,
                        "duration" to durationInDays
                    )
                )
                .addOnSuccessListener {
                    Toast.makeText(
                        applicationContext,
                        "Fechas y duración actualizadas",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .addOnFailureListener { ex ->
                    Toast.makeText(
                        applicationContext,
                        "Error al actualizar fechas: ${ex.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    private fun loadStopData(tripID: String, stopID: String) {
        val stopViewModel: StopViewModel = ViewModelProvider(this).get(StopViewModel::class.java)

        // Cargar los datos desde el ViewModel
        stopViewModel.loadStop(tripID, stopID)

        // Observar el resultado del LiveData
        stopViewModel.stop.observe(this) { stop ->
            stop?.let {
                updateUIWithStopData(it)
            } ?: run {
                showError("Error loading stop data")
            }
        }

        // Observar errores del ViewModel
        stopViewModel.error.observe(this) { errorMessage ->
            errorMessage?.let {
                showError(it)
            }
        }
    }

    // Actualizar los datos de la UI
    private fun updateUIWithStopData(stop: Stop) {
        this.stop = stop

        // Actualizar los campos de texto
        etName.setText(stop.name)
        etDescription.setText(stop.text)

        // Actualizar la fecha y hora
        stop.timestamp?.let { timestamp ->
            tvDate.text = SimpleDateFormat(
                "dd 'de' MMMM 'de' yyyy", Locale.getDefault()
            ).format(timestamp.toDate())
            tvTime.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(timestamp.toDate())
        }

        // Actualizar Google Places (si está inicializado)
        if (Places.isInitialized()) {
            placeFragment?.setText(stop.namePlace)
        }

        // Actualizar las imágenes
        updateImages(stop.photos)
    }

    // Actualizar el RecyclerView de imágenes
    private fun updateImages(photos: List<String>?) {
        imagesList.clear()
        if (photos != null) {
            imagesList.addAll(photos)
        }
        adapter.notifyDataSetChanged()
    }

    // Mostrar errores
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun createStop() {
        val stopData = hashMapOf(
            "name" to etName.text.toString(),
            "text" to etDescription.text.toString(),
            "timestamp" to timestampFb,
            "geoPoint" to geoPoint,
            "idPlace" to idPlace,
            "namePlace" to namePlace,
            "addressPlace" to addressPlace
        )

        db.collection("trips").document(tripID).collection("stops").add(stopData)
            .addOnSuccessListener { documentReference ->
                uploadImages(documentReference.id)
                Toast.makeText(this, "Parada creada con éxito", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun updateStop() {
        val stopData = hashMapOf(
            "name" to etName.text.toString(),
            "text" to etDescription.text.toString(),
            "timestamp" to timestampFb,
            "geoPoint" to geoPoint,
            "idPlace" to idPlace,
            "namePlace" to namePlace,
            "addressPlace" to addressPlace
        )

        stopID?.let { id ->
            db.collection("trips").document(tripID).collection("stops").document(id)
                .update(stopData as Map<String, Any>).addOnSuccessListener {
                    uploadImages(id)
                    Toast.makeText(this, "Parada actualizada con éxito", Toast.LENGTH_SHORT).show()
                    finish()
                }
        }
    }

    private fun uploadImages(stopId: String) {
        imagesList.forEach { uriString ->
            val storageRef = FirebaseStorage.getInstance()
                .getReference("Stop_Image/$tripID/$stopId/${System.currentTimeMillis()}")

            storageRef.putFile(uriString.toUri()).addOnSuccessListener { taskSnapshot ->
                taskSnapshot.storage.downloadUrl.addOnSuccessListener { uri ->
                    val photoData = hashMapOf("url" to uri.toString())
                    db.collection("trips").document(tripID).collection("stops").document(stopId)
                        .collection("photos").add(photoData)
                }
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Error al subir imagen: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mode == MODE_EDIT) {
            clearEditingState(tripID, stopID)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        if (mode == MODE_EDIT) {
            clearEditingState(tripID, stopID)
        }
    }
    override fun onStop() {
        super.onStop()
        // Handle clearing the "editing" field only if the user is editing a stop
        if (mode == MODE_EDIT) {
            clearEditingState(tripID, stopID)
            finish()
        }
    }

    private fun clearEditingState(tripID: String, stopID: String?) {
        val stopDoc = db.collection("trips").document(tripID).collection("stops").document(stopID!!)
        stopDoc.update("editing", null).addOnFailureListener {
            Log.e("PostStopActivity", "No se pudo limpiar el estado de edición.")
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        galleryActivityResultLauncher.launch(intent)
    }

    private fun openCamera() {
        val values = ContentValues()
        uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
        cameraActivityResultLauncher.launch(intent)
    }

    private val galleryActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult(),
            ActivityResultCallback<ActivityResult> { result ->
                if (result.resultCode == RESULT_OK) {
                    result.data?.let { data ->
                        data.clipData?.let {
                            for (i in 0 until it.itemCount) {
                                val imageUri = it.getItemAt(i).uri.toString()
                                imagesList.add(imageUri)
                            }
                        } ?: data.data?.let { uri ->
                            imagesList.add(uri.toString())
                        }
                    }
                    sv_images.isVisible = true // Make the ScrollView visible
                    adapter.notifyDataSetChanged()
                }
            })

    private val cameraActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            uri?.let { imagesList.add(it.toString()) }
            sv_images.isVisible = true // Make the ScrollView visible
            adapter.notifyDataSetChanged()
        }
    }

    private val galleryPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { permission ->
            if (permission) {
                openGallery()
            } else {
                Toast.makeText(
                    applicationContext,
                    "El permiso para acceder a la galería no ha sido concedido",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }


    private val cameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { permission ->
            if (permission) {
                openCamera()
            } else {
                Toast.makeText(
                    applicationContext,
                    "El permiso para acceder a la cámara no ha sido concedido",
                    Toast.LENGTH_SHORT
                ).show()
            }

        }

}
