package com.tfg.viajeslog.view.stop

import com.tfg.viajeslog.helper.ImagePickerHelper
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
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
    private lateinit var btnUpload: Button
    private lateinit var rvImages: RecyclerView
    private lateinit var tvDate: TextView
    private lateinit var tvTime: TextView
    private lateinit var fabDone: FloatingActionButton
    private lateinit var sv_images: ScrollView
    private lateinit var pb_load: ProgressBar

    // Firebase
    private lateinit var db: FirebaseFirestore
    private var imagesList: ArrayList<String> = ArrayList() // Imágenes mostradas en el RecyclerView
    private var newImages: MutableList<String> = mutableListOf() // Nuevas imágenes para subir
    private var imagesToDelete: MutableList<String> = mutableListOf() // Imágenes para eliminar

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
    private var isEditMode: Boolean = false
    private var calendar = Calendar.getInstance()

    // Helper
    private lateinit var imagePickerHelper: ImagePickerHelper

    // Places
    private var placeFragment: AutocompleteSupportFragment? = null
    private var idPlace: String = ""
    private var namePlace: String = ""
    private var addressPlace: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_stop)
        init()

        isEditMode = intent.getBooleanExtra("isEditMode", false)
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

        // Inicializar com.tfg.viajeslog.helper.ImagePickerHelper
        imagePickerHelper = ImagePickerHelper(
            context = this,
            singleImageMode = false, // Permitimos múltiples imágenes para los stops
            onImagePicked = { uris ->
                // Manejo de imágenes seleccionadas
                uris.forEach { uri ->
                    if (!imagesList.contains(uri.toString())) {
                        newImages.add(uri.toString())
                        imagesList.add(uri.toString())
                    }
                }
                sv_images.isVisible = imagesList.isNotEmpty()
                adapter.notifyDataSetChanged()
            }
        )
    }

    private fun checkEditingState(tripID: String, stopID: String, userName: String) {
        val stopDoc = db.collection("trips").document(tripID).collection("stops").document(stopID)

        stopDoc.get().addOnSuccessListener { document ->
            val editingUser = document.getString("editing")
            if (editingUser != null && editingUser != userName) {
                // Otro usuario está editando
                Toast.makeText(
                    this, "Esta parada está siendo editada por $editingUser.", Toast.LENGTH_LONG
                ).show()
                finish() // Salir de la actividad
            } else {
                // No hay nadie editando, actualizar el campo
                stopDoc.update("editing", userName).addOnSuccessListener {
                    loadStopData(tripID, stopID)
                    Toast.makeText(
                        this, "Punto de Interés reservado para edición.", Toast.LENGTH_LONG
                    ).show()
                }.addOnFailureListener {
                    Toast.makeText(
                        this, "No se pudo establecer el estado de edición.", Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Error al verificar el estado de edición.", Toast.LENGTH_LONG)
                .show()
            finish()
        }
    }

    private fun init() {
        etName = findViewById(R.id.et_name)
        etDescription = findViewById(R.id.et_description)
        btnUpload = findViewById(R.id.btn_upload)
        rvImages = findViewById(R.id.rv_images)
        sv_images = findViewById(R.id.sv_images)
        tvDate = findViewById(R.id.tv_date)
        tvTime = findViewById(R.id.tv_time)
        fabDone = findViewById(R.id.fab_done)
        pb_load = findViewById(R.id.pb_load)
        db = FirebaseFirestore.getInstance()

        // Setup RecyclerView
        layoutManager = GridLayoutManager(this, 3)


        // Manejar la eliminación de imagenes
        adapter = ImageAdapter(imagesList) { imageUrl ->
            if (!newImages.remove(imageUrl)) { // Si no está en las nuevas, está en las existentes
                imagesToDelete.add(imageUrl) // Marcar para eliminar
            }
            imagesList.remove(imageUrl) // Eliminar de la lista general
            adapter.notifyDataSetChanged()
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
        calendar = Calendar.getInstance()
        tvDate.text = SimpleDateFormat(
            "dd 'de' MMMM 'de' yyyy",
            Locale.getDefault()
        ).format(calendar.timeInMillis)
        tvTime.text = SimpleDateFormat("HH:mm").format(calendar.timeInMillis)
        timestampFb = Timestamp(calendar.time)
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
            val currentYear = calendar.get(Calendar.YEAR)
            val currentMonth = calendar.get(Calendar.MONTH)
            val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(
                this,
                R.style.CustomDatePickerTheme,
                { _, year, month, day ->
                    calendar.set(Calendar.YEAR, year)
                    calendar.set(Calendar.MONTH, month)
                    calendar.set(Calendar.DAY_OF_MONTH, day)

                    tvDate.text = SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale.getDefault())
                        .format(calendar.time)

                    // Actualizar el Timestamp global
                    timestampFb = Timestamp(calendar.time)
                },
                currentYear, currentMonth, currentDay
            )
            datePickerDialog.show()
        }

        // Time Picker
        tvTime.setOnClickListener {
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            val currentMinute = calendar.get(Calendar.MINUTE)

            TimePickerDialog(
                this,
                R.style.CustomTimePickerTheme,
                { _, hour, minute ->
                    calendar.set(Calendar.HOUR_OF_DAY, hour)
                    calendar.set(Calendar.MINUTE, minute)

                    tvTime.text =
                        SimpleDateFormat("HH:mm", Locale.getDefault()).format(calendar.time)

                    // Actualizar el Timestamp global
                    timestampFb = Timestamp(calendar.time)
                },
                currentHour, currentMinute,
                true
            ).show()
        }

        btnUpload.setOnClickListener {
            imagePickerHelper.showImagePickerDialog(
                galleryLauncher = galleryLauncher,
                cameraLauncher = cameraLauncher,
                permissionLauncher = permissionLauncher
            )
        }

        // Save or Update
        fabDone.setOnClickListener {
            pb_load.visibility = View.VISIBLE
            if (mode == MODE_CREATE) {
                createStop()
            } else if (mode == MODE_EDIT) {
                updateStop()
            }
            updateTrip()
            pb_load.visibility = View.GONE
            db.waitForPendingWrites().addOnCompleteListener {
                finish()
            }
        }
    }

    private fun updateTrip() {
        // Actualizar las fechas del viaje (initDate, endDate, duración)
        db.collection("trips").document(tripID).get().addOnSuccessListener { document ->
            val trip = document.toObject(Trip::class.java)

            // Crear una fecha máxima válida usando Calendar
            val maxDate = Calendar.getInstance().apply {
                set(9999, Calendar.DECEMBER, 31, 23, 59, 59)
                // Año 9999, mes diciembre (0-based), día 31, último segundo del día
            }.time

            // Convertir la fecha a un Timestamp
            val currentInitDate = trip?.initDate ?: Timestamp(maxDate)
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
            db.collection("trips").document(tripID).update(
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

        // Actualizar la fecha y hora desde el Timestamp
        stop.timestamp?.let { timestamp ->
            calendar.time = timestamp.toDate() // Sincroniza el Calendar global

            tvDate.text = SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale.getDefault())
                .format(calendar.time)
            tvTime.text = SimpleDateFormat("HH:mm", Locale.getDefault())
                .format(calendar.time)

            // Actualiza el timestampFb global
            timestampFb = stop.timestamp!!
        }

        // Actualizar Google Places (si está inicializado)
        if (Places.isInitialized()) {
            placeFragment?.setText(stop.namePlace)
        }

        // Actualizar datos de Google Places
        idPlace = stop.idPlace!!
        namePlace = stop.namePlace!!
        addressPlace = stop.addressPlace!!
        geoPoint = stop.geoPoint!!

        // Actualizar imágenes
        imagesList.clear()
        imagesList.addAll(stop.photos ?: emptyList()) // Imágenes existentes en Edit Mode
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
                    uploadImages(id) // Manejar las imágenes
                    Toast.makeText(this, "Parada actualizada con éxito", Toast.LENGTH_SHORT).show()
                }
        }

        Toast.makeText(
            this, "Punto de Interés liberado.", Toast.LENGTH_LONG
        ).show()
    }

    private fun uploadImages(stopId: String) {

        // Modo Creación
        if (!isEditMode) {
            newImages.addAll(imagesList)
            imagesToDelete.clear()
        }

        // Asegurar que no hay duplicados
        newImages = newImages.distinct().toMutableList()
        imagesToDelete = imagesToDelete.distinct().toMutableList()


        // Subir nuevas imágenes
        newImages.forEach { uriString ->
            val storageRef = FirebaseStorage.getInstance()
                .getReference("Stop_Image/$tripID/$stopId/${System.currentTimeMillis()}")
            storageRef.putFile(uriString.toUri()).addOnSuccessListener { taskSnapshot ->
                taskSnapshot.storage.downloadUrl.addOnSuccessListener { uri ->
                    val photoData = hashMapOf("url" to uri.toString())
                    db.collection("trips").document(tripID).collection("stops").document(stopId)
                        .collection("photos").add(photoData)
                }
            }
        }

        // Eliminar imágenes marcadas
        imagesToDelete.forEach { url ->
            val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(url)
            storageRef.delete().addOnSuccessListener {
                db.collection("trips").document(tripID).collection("stops").document(stopId)
                    .collection("photos").whereEqualTo("url", url).get()
                    .addOnSuccessListener { querySnapshot ->
                        querySnapshot.documents.forEach { it.reference.delete() }
                    }
            }
        }

        // Limpiar las listas
        newImages.clear()
        imagesToDelete.clear()
    }


    override fun onDestroy() {
        super.onDestroy()
        if (mode == MODE_EDIT) {
            clearEditingState(tripID, stopID)
            Toast.makeText(
                this, "Punto de Interés liberado.", Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        if (mode == MODE_EDIT) {
            clearEditingState(tripID, stopID)
            Toast.makeText(
                this, "Punto de Interés liberado.", Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun clearEditingState(tripID: String, stopID: String?) {
        val stopDoc = db.collection("trips").document(tripID).collection("stops").document(stopID!!)
        stopDoc.update("editing", null).addOnFailureListener {
            Log.e("PostStopActivity", "No se pudo limpiar el estado de edición.")
        }
    }


    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                Toast.makeText(this, "Permiso denegado. Vuelva a intentarlo.", Toast.LENGTH_SHORT).show()
            }
        }

    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                imagePickerHelper.handleGalleryResult(result.data)
            }
        }

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                imagePickerHelper.handleCameraResult()
            }
        }


}
