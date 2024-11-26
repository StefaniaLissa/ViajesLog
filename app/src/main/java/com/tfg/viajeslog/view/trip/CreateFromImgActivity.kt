package com.tfg.viajeslog.view.trip

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.tasks.Task
import com.google.android.libraries.places.api.Places
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.tfg.viajeslog.R
import com.tfg.viajeslog.model.data.Trip
import com.tfg.viajeslog.view.adapters.ImageAdapter
import com.tfg.viajeslog.view.adapters.StopAdapter
import com.tfg.viajeslog.viewmodel.StopViewModel
import java.util.Calendar
import java.util.Date


class CreateFromImgActivity : AppCompatActivity() {

    private var uri: Uri? = null

    private lateinit var rv_images: RecyclerView
    private lateinit var img_adapter: ImageAdapter
    private lateinit var imagesList: ArrayList<String>
    private lateinit var rv_stops: RecyclerView
    private lateinit var stopViewModel: StopViewModel
    private lateinit var stopAdapter: StopAdapter
    private lateinit var btn_save: Button
    private lateinit var btn_upload: Button
    private lateinit var btn_process: Button
    private lateinit var sv_images: ScrollView
    private lateinit var tv_instructions: TextView
    private lateinit var tv_instructions2: TextView
    private lateinit var tripID: String
    private lateinit var db: FirebaseFirestore


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_create_from_img)
        init()

        btn_upload.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    applicationContext, Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                openGallery()
            } else {
                galleryPermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        btn_process.setOnClickListener {
            tv_instructions.visibility = View.GONE
            tv_instructions2.visibility = View.VISIBLE
            btn_process.visibility = View.GONE
            btn_save.visibility = View.VISIBLE

            // Add stops from selected images
            val apiKey = applicationContext.packageManager.getApplicationInfo(
                applicationContext.packageName, PackageManager.GET_META_DATA
            ).metaData.getString("com.google.android.geo.API_KEY").toString()

            if (!Places.isInitialized()) {
                Places.initialize(applicationContext, apiKey)
            }

            for (uri in imagesList) {
                stopViewModel.addStopFromUri(
                    uri.toUri(), contentResolver, apiKey, Places.createClient(this)
                )
            }
            sv_images.visibility = View.GONE
            btn_upload.visibility = View.GONE
            btn_process.visibility = View.GONE

        }

        btn_save.setOnClickListener {
            for (stop in stopViewModel.stops.value!!) {
                val hm_stop = hashMapOf(
                    "name" to stop.name,
                    "text" to stop.text,
                    "timestamp" to stop.timestamp,
                    "idPlace" to stop.idPlace,
                    "namePlace" to stop.namePlace,
                    "addressPlace" to stop.addressPlace,
                    "geoPoint" to stop.geoPoint
                )
                // Agregar a la colección con nuevo ID
                db.collection("trips").document(tripID).collection("stops").add(hm_stop)
                    .addOnSuccessListener { documentReference ->
                        Toast.makeText(
                            applicationContext, "Se ha registrado con éxito", Toast.LENGTH_SHORT
                        ).show()
                        //Subir a Storage
                        val rutaImagen =
                            "Stop_Image/" + tripID + "/" + documentReference.id + "/" + System.currentTimeMillis()
                        val referenceStorage =
                            FirebaseStorage.getInstance().getReference(rutaImagen)
                        referenceStorage.putFile(stop.photos!![0].toUri())
                            .addOnSuccessListener { tarea ->
                                val uriTarea: Task<Uri> = tarea.storage.downloadUrl
                                while (!uriTarea.isSuccessful);
                                val url = "${uriTarea.result}"
                                UpdateFirestore(url, documentReference.id)

                            }.addOnFailureListener { e ->
                                Toast.makeText(
                                    applicationContext,
                                    "No se ha podido subir la imagen: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                        //Fecha de Inicio del Viaje Portada
                        val calendar = Calendar.getInstance()
                        calendar.set(9999, Calendar.DECEMBER, 31, 23, 59, 59) // Año 9999, último día
                        calendar.set(Calendar.MILLISECOND, 999) // Último milisegundo
                        val endDate = Timestamp(calendar.time)
                        db.collection("trips").document(tripID).get().addOnCompleteListener {
                            val initDate =
                                if (it.result.get("initDate") == null) endDate else it.result.toObject(
                                    Trip::class.java
                                )!!.initDate
                            if (initDate!! > stop.timestamp!!) {
                                db.collection("trips").document(tripID)
                                    .update("initDate", stop.timestamp).addOnFailureListener { ex ->
                                        Toast.makeText(
                                            applicationContext,
                                            "No se actualizó la fecha de inicio: ${ex.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                            }
                        }
                    }.addOnFailureListener { e ->
                        Toast.makeText(applicationContext, "${e.message}", Toast.LENGTH_SHORT)
                            .show()
                    }

                finish()
            }
        }
    }

    private fun init() {
        tv_instructions = findViewById(R.id.tv_instructions)
        tv_instructions2 = findViewById(R.id.tv_instructions2)
        btn_upload = findViewById(R.id.btn_upload)
        btn_save = findViewById(R.id.btn_save)
        btn_process = findViewById(R.id.btn_process)

        sv_images = findViewById(R.id.sv_images)
        rv_images = findViewById(R.id.rv_images)
        imagesList = ArrayList()
        img_adapter = ImageAdapter(imagesList)
        rv_images.layoutManager = GridLayoutManager(this, 3)
        rv_images.adapter = img_adapter

        rv_stops = findViewById(R.id.rv_stops)
        rv_stops.layoutManager = LinearLayoutManager(this)
        rv_stops.setHasFixedSize(true)
        stopAdapter = StopAdapter()
        rv_stops.adapter = stopAdapter

        stopViewModel = ViewModelProvider(this).get(StopViewModel::class.java)
        stopViewModel.stops.observe(this) { stops ->
            stopAdapter.updateStopList(stops)
        }
        stopViewModel.error.observe(this) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }
        }

        //Get Trip Intent
        tripID = intent.getStringExtra("tripID").toString()
        db = FirebaseFirestore.getInstance()
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        galleryActivityResultLauncher.launch(intent)
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

    private val galleryActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            uri = data!!.data
            if (data == null) {
                Toast.makeText(this, "No se seleccionaron imágenes", Toast.LENGTH_SHORT).show()
            }
            //iv_cover.setImageURI(uri)
            if (data.clipData != null) {
                for (i in 0 until data.clipData!!.itemCount) {
                    val imageUri = data.clipData!!.getItemAt(i).uri.toString()
                    imagesList.add(imageUri)
                }
            } else {
                val imageUri = data.data.toString()
                imagesList.add(imageUri)
            }
            img_adapter.notifyDataSetChanged() // Notify adapter of dataset changes
            btn_process.visibility = View.VISIBLE
        } else {
            Toast.makeText(
                applicationContext, "Cancelado por el usuario", Toast.LENGTH_SHORT
            ).show()

        }

    }

    private fun UpdateFirestore(url: String, stopID: String) {
        val photo = hashMapOf(
            "url" to url
        )
        FirebaseFirestore.getInstance().collection("trips")
            .document(intent.getStringExtra("trip").toString()).collection("stops").document(stopID)
            .collection("photos").add(photo).addOnFailureListener { e ->
                Toast.makeText(
                    applicationContext,
                    "No se ha actualizado su imagen debido a: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

}