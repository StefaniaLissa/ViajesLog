package com.tfg.viajeslog.view.trip

import android.Manifest
import android.app.Dialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SearchView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tfg.viajeslog.R
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.tfg.viajeslog.model.data.User
import com.tfg.viajeslog.view.adapters.EditorAdapter
import com.tfg.viajeslog.view.adapters.UsersAdapter
import com.tfg.viajeslog.viewmodel.UserViewModel

class CreateTripActivity : AppCompatActivity() {

    private lateinit var iv_cover: ImageView
    private lateinit var btn_new_image: Button
    private lateinit var et_name: EditText
    private lateinit var cb_online: CheckBox
    private lateinit var ll_cb_share: LinearLayout
    private lateinit var cb_share: CheckBox
    private lateinit var btn_create: Button
    private lateinit var db: FirebaseFirestore
    private var uri: Uri? = null
    private var uriString: String? = null
    private lateinit var user: FirebaseUser
    private lateinit var tripId: String

    private val selectedEditors = mutableListOf<User>() // Dummy list for selected editors

// TODO: Compartimentar con clase photoUpload
//    private val photoUpload { PhotosUpload(this, tripId, "") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_trip)
        init()

        //Cambiar imagen
        btn_new_image.setOnClickListener { CameraOrGalleryDialog() }
        iv_cover.setOnClickListener { CameraOrGalleryDialog() }

        // Handle trip creation
        btn_create.setOnClickListener {
            createTrip()
        }

    }

    private fun createTrip() {
        val trip = hashMapOf(
            "name" to et_name.text.toString(), "public" to cb_online.isChecked
        )

        // Agregar a la colección con nuevo ID
        db.collection("trips").add(trip).addOnSuccessListener { documentReference ->
            Toast.makeText(
                applicationContext, "Se ha registrado con éxito", Toast.LENGTH_SHORT
            ).show()

            tripId = documentReference.id

            // Agregar usuario actual como admin
            val member = hashMapOf(
                "admin" to true, "tripID" to documentReference.id, "userID" to user.uid
            )
            db.collection("members").add(member)

            // Guardar Cover
            if(uriString.toString() != ""){
                uploadCoverImage(uriString.toString())
            }

            if (cb_share.isChecked) {
                openShareTripFragment()
            } else {
                // Directly go to DetailedTripActivity if no sharing is required
                val intent = Intent(this@CreateTripActivity, DetailedTripActivity::class.java)
                intent.putExtra("id", tripId)
                startActivity(intent)
                finish() // Close CreateTripActivity
            }

        }.addOnFailureListener { e ->
            Toast.makeText(applicationContext, "${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openShareTripFragment() {
        val intent = Intent(this, MediumActivity::class.java)
        intent.putExtra("view", "share")
        intent.putExtra("tripId", tripId)
        startActivity(intent)
        finish()
    }

    private fun uploadCoverImage(imageUri: String) {
        val rutaImagen = "TripCover/" + tripId + "/" + System.currentTimeMillis()
        val referenceStorage = FirebaseStorage.getInstance().getReference(rutaImagen)
        referenceStorage.putFile(imageUri.toUri()).addOnSuccessListener { task ->
            task.storage.downloadUrl.addOnSuccessListener { uri ->
                val imageUrl = uri.toString()
                db.collection("trips").document(tripId).update("image", imageUrl)
            }
        }.addOnFailureListener { e ->
            Toast.makeText(
                applicationContext,
                "No se ha podido subir la imagen debido a: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun CameraOrGalleryDialog() {
        iv_cover.background = null
        val btn_gallery: Button
        val btn_camera: Button

        val dialog = Dialog(this@CreateTripActivity)

        dialog.setContentView(R.layout.select_img)

        btn_gallery = dialog.findViewById(R.id.btn_gallery)
        btn_camera = dialog.findViewById(R.id.btn_camera)

        btn_gallery.setOnClickListener {
            //Toast.makeText(applicationContext, "Abrir galería", Toast.LENGTH_SHORT).show()
            if (ContextCompat.checkSelfPermission(
                    applicationContext, Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                openGallery()
                dialog.dismiss()
            } else {
                galleryPermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }

        }

        btn_camera.setOnClickListener {
            //Toast.makeText(applicationContext, "Abrir cámara", Toast.LENGTH_SHORT).show()
            if (ContextCompat.checkSelfPermission(
                    applicationContext, Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                openCamera()
                dialog.dismiss()
            } else {
                cameraPermission.launch(Manifest.permission.CAMERA)
            }
        }
        dialog.show()
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        galleryActivityResultLauncher.launch(intent)
    }

    private fun openCamera() {
        val values = ContentValues()
        uri = contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
        )

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
        cameraActivityResultLauncher.launch(intent)
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

    private val galleryActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            uri = data!!.data
            uriString = data.clipData!!.getItemAt(0).uri.toString()
            iv_cover.setImageURI(uri)

        } else {
            Toast.makeText(
                applicationContext, "Cancelado por el usuario", Toast.LENGTH_SHORT
            ).show()

        }

    }

    private val cameraActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK) {
                iv_cover.setImageURI(uri)
            } else {
                Toast.makeText(applicationContext, "Cancelado por el usuario", Toast.LENGTH_SHORT)
                    .show()
            }
        }


    private fun init() {
        iv_cover = findViewById(R.id.iv_cover)
        btn_new_image = findViewById(R.id.btn_new_image)
        et_name = findViewById(R.id.et_name)
        cb_online = findViewById(R.id.cb_online)
        cb_share = findViewById(R.id.cb_share)
        btn_create = findViewById(R.id.btn_create)
        db = FirebaseFirestore.getInstance()
        user = FirebaseAuth.getInstance().currentUser!!
        uriString = String()

        // Fetch user profile to initialize `cb_online`
        db.collection("users").document(user.uid).get().addOnSuccessListener { document ->
            if (document.exists()) {
                val isPublic = document.getBoolean("public") ?: false
                cb_online.isChecked = isPublic
            }
        }
    }


}