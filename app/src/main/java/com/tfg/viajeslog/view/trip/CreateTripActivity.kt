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
import com.tfg.viajeslog.R
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class CreateTripActivity : AppCompatActivity() {

    private lateinit var iv_cover: ImageView
    private lateinit var btn_new_image: Button
    private lateinit var et_name: EditText
    private lateinit var cb_online: CheckBox
    private lateinit var ll_cb_share: LinearLayout
    private lateinit var sv_user: SearchView
    private lateinit var ll_users: LinearLayout
    private lateinit var cb_share: CheckBox
    private lateinit var btn_create: Button
    private lateinit var db: FirebaseFirestore
    private var uri: Uri? = null
    private var uriString: String? = null
    private lateinit var user: FirebaseUser
    private lateinit var tripId: String

    //private lateinit var photoUpload: PhotosUpload

// TODO: Compartimentar con clase photoUpload
//    private val photoUpload by lazy { PhotosUpload(this, tripId, "") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_trip)
        init()

//        //Crear Trip Vacío
//        val trip = hashMapOf(
//            "name" to "",
//            "public" to ""
//        )
//        db.collection("trips").add(trip).addOnSuccessListener {
//            tripId = it.id
//        }

        //Cambiar imagen
//        btn_new_image.setOnClickListener {
////            CameraOrGalleryDialog()
//            val fragment = PhotoUploadFragment()
//            val bundle = Bundle()
//            bundle.putString("tripID", tripId)
//            bundle.putString("stopID", "")
//            fragment.arguments = bundle
//            supportFragmentManager.beginTransaction()
//                .add(R.id.frame_layout, fragment)
//                .addToBackStack(null)
//                .commit()
//        }

        //Cambiar imagen
        btn_new_image.setOnClickListener {
            CameraOrGalleryDialog()
        }
        iv_cover.setOnClickListener {
            CameraOrGalleryDialog()
        }

        // Compartir
        cb_share.setOnCheckedChangeListener { compoundButton, b ->
            if (b == true) {
                ll_cb_share.isVisible = true
            }
        }
// Crear
        btn_create.setOnClickListener {
            val trip = hashMapOf(
                "name" to et_name.text.toString(), "public" to cb_online.isChecked
            )
            // Agregar a la colección con nuevo ID
            db.collection("trips").add(trip).addOnSuccessListener { documentReference ->
                Toast.makeText(
                    applicationContext, "Se ha registrado con éxito", Toast.LENGTH_SHORT
                ).show()
                // Agregar a Members
                val member = hashMapOf(
                    "admin" to true, "tripID" to documentReference.id, "userID" to user.uid
                )
                db.collection("members").add(member).addOnSuccessListener {
                    Toast.makeText(
                        applicationContext, "Se ha agregado con éxito", Toast.LENGTH_SHORT
                    ).show()
                }.addOnFailureListener {
                    Toast.makeText(
                        applicationContext, "No se ha podido agregar", Toast.LENGTH_SHORT
                    ).show()
                }
                // Guardar Cover
                if (uriString != null) {
                    //Save Image in Firebase Store
                    tripId = documentReference.id
                    val rutaImagen = "TripCover/" + tripId + "/" + System.currentTimeMillis()
                    val referenceStorage = FirebaseStorage.getInstance().getReference(rutaImagen)
                    referenceStorage.putFile(uriString!!.toUri()).addOnSuccessListener { task ->
                        val uriTask: Task<Uri> = task.storage.downloadUrl
                        while (!uriTask.isSuccessful);
                        val url = "${uriTask.result}"
                        UpdateFirestore(url)
                    }.addOnFailureListener { e ->
                        Toast.makeText(
                            applicationContext,
                            "No se ha podido subir la imagen debido a: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                val intent = Intent(this@CreateTripActivity, DetailedTripActivity::class.java)
                intent.putExtra("id", tripId)
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            }.addOnFailureListener { e ->
                Toast.makeText(applicationContext, "${e.message}", Toast.LENGTH_SHORT).show()
            }
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

//        btn_camera.setOnClickListener {
//            //Toast.makeText(applicationContext, "Abrir cámara", Toast.LENGTH_SHORT).show()
//            if (ContextCompat.checkSelfPermission(
//                    applicationContext,
//                    Manifest.permission.CAMERA
//                ) == PackageManager.PERMISSION_GRANTED
//            ) {
//                openCamera()
//                dialog.dismiss()
//            } else {
//                cameraPermission.launch(Manifest.permission.CAMERA)
//            }
////            photoUpload.uploadTripCover(true)
//
//
//        }
        dialog.show()
    }

    private fun UpdateFirestore(url: String) {
        FirebaseFirestore.getInstance().collection("trips").document(tripId).update("image", url)
            .addOnFailureListener { e ->
                Toast.makeText(
                    applicationContext,
                    "No se ha actualizado su imagen debido a: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        galleryActivityResultLauncher.launch(intent)
    }

    private fun openCamera() {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "Titulo")
        values.put(MediaStore.Images.Media.DESCRIPTION, "Descripcion")
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

    private val galleryActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult(),
            ActivityResultCallback<ActivityResult> { result ->
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

            })

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
        ll_cb_share = findViewById(R.id.ll_cb_share)
        sv_user = findViewById(R.id.sv_user)
        ll_users = findViewById(R.id.ll_users)
        cb_share = findViewById(R.id.cb_share)
        btn_create = findViewById(R.id.btn_create)
        db = FirebaseFirestore.getInstance()
        user = FirebaseAuth.getInstance().currentUser!!
        uriString = String()
    }

//    override fun onDestroy() {
//        super.onDestroy()
//
//        // Check if the trip is not created (i.e., the name is still empty)
//        if (et_name.text.isEmpty()) {
//            // Delete the trip from Firestore
//            db.collection("trips")
//                .document(tripId)
//                .delete()
//                .addOnSuccessListener {
//                    Toast.makeText(this, "Unsaved trip deleted", Toast.LENGTH_SHORT).show()
//                }
//                .addOnFailureListener { e ->
//                    Toast.makeText(
//                        this,
//                        "Failed to delete unsaved trip: ${e.message}",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                }
//        }
//    }

}