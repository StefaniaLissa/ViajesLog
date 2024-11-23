package com.tfg.viajeslog.view.trip

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tfg.viajeslog.R
import com.tfg.viajeslog.model.data.Stop
import com.tfg.viajeslog.view.adapters.ImageAdapter
import com.tfg.viajeslog.view.adapters.StopAdapter
import com.tfg.viajeslog.viewmodel.StopViewModel
import kotlinx.coroutines.newSingleThreadContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class CreateFromImgActivity : AppCompatActivity() {

    private var uri: Uri? = null

    private lateinit var rv_images: RecyclerView
    private lateinit var img_adapter: ImageAdapter
    private lateinit var imagesList: ArrayList<String>
    private lateinit var rv_stops: RecyclerView
    private lateinit var stopViewModel: StopViewModel
    private lateinit var stopAdapter: StopAdapter
    private lateinit var stopList: MutableList<Stop>
    private lateinit var btn_save: Button
    private lateinit var btn_upload: Button
    private lateinit var btn_process: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_create_from_img)
        init()

        btn_upload.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                openGallery()
            } else {
                galleryPermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        btn_process.setOnClickListener {
            for (uri in imagesList) {

                var exifDate: LocalDateTime? = null
                contentResolver.openInputStream(uri.toUri())?.use { stream ->
                    val exif = ExifInterface(stream)
                    val exifDateFormatter = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")
                    var exifDateString = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                    if (exifDateString == null) {
                        exifDateString = exif.getAttribute(ExifInterface.TAG_DATETIME)
                    }
                    if (exifDateString != null) {
                        exifDate = LocalDateTime.parse(exifDateString, exifDateFormatter)
                    }
                    //LOCATION
                    var LatLong = FloatArray(2)
                    exif.getLatLong(LatLong)
                    var test1 = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE)
                    var test2 = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE)

                    //Crear Stop
                    val stop = Stop (
                        "test",
                        "Prueba"
                    )

                    stopList.add(stop)
                }
            }
            stopAdapter.updateStopList(stopList)
        }
    }

    private fun init() {
        btn_upload = findViewById(R.id.btn_upload)
        btn_save = findViewById(R.id.btn_save)
        btn_process = findViewById(R.id.btn_process)

        rv_images = findViewById(R.id.rv_images)
        imagesList = ArrayList()
        img_adapter = ImageAdapter(imagesList)
        rv_images.layoutManager = GridLayoutManager(this, 3) // Display images in a grid
        rv_images.adapter = img_adapter

        rv_stops = findViewById(R.id.rv_stops)
        rv_stops.layoutManager = LinearLayoutManager(this)
        rv_stops.setHasFixedSize(true)
        stopAdapter = StopAdapter()
        stopAdapter.tripID = "test"
        rv_stops.adapter = stopAdapter
        stopViewModel = ViewModelProvider(this).get(StopViewModel::class.java)
        stopList = mutableListOf()
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
        ActivityResultContracts.StartActivityForResult(),
        ActivityResultCallback<ActivityResult> { result ->
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
            } else {
                Toast.makeText(applicationContext, "Cancelado por el usuario", Toast.LENGTH_SHORT)
                    .show()

            }

        }
    )
}