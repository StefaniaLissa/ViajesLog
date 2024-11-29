package com.tfg.viajeslog.view.trip

import com.tfg.viajeslog.helper.ImagePickerHelper
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import com.tfg.viajeslog.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.tfg.viajeslog.view.tripExtra.MediumActivity

class CreateTripActivity : AppCompatActivity() {

    private lateinit var iv_cover: ImageView
    private lateinit var btn_new_image: Button
    private lateinit var iv_delete: ImageView
    private lateinit var et_name: EditText
    private lateinit var cb_online: CheckBox
    private lateinit var cb_share: CheckBox
    private lateinit var btn_create: Button
    private lateinit var db: FirebaseFirestore
    private var uri: Uri? = null
    private lateinit var user: FirebaseUser
    private lateinit var tripId: String

    private lateinit var imagePickerHelper: ImagePickerHelper // com.tfg.viajeslog.helper.ImagePickerHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_trip)
        init()

        // Inicializar com.tfg.viajeslog.helper.ImagePickerHelper
        imagePickerHelper = ImagePickerHelper(context = this,
            singleImageMode = true, // Solo se permite una imagen para la portada
            onImagePicked = { uris ->
                // Manejar imagen seleccionada
                if (uris.isNotEmpty() && uris[0].toString() != "") {
                    uri = uris[0]
                    iv_cover.setImageURI(uri)
                }
                iv_delete.isEnabled = true
                iv_delete.alpha = 1.0f
            })

        // Cambiar imagen (Abrir diálogo)
        btn_new_image.setOnClickListener {
            imagePickerHelper.showImagePickerDialog(
                galleryLauncher = galleryLauncher,
                cameraLauncher = cameraLauncher,
                permissionLauncher = permissionLauncher
            )
        }

        iv_cover.setOnClickListener {
            imagePickerHelper.showImagePickerDialog(
                galleryLauncher = galleryLauncher,
                cameraLauncher = cameraLauncher,
                permissionLauncher = permissionLauncher
            )
        }

        // Handle trip creation
        btn_create.setOnClickListener {
            createTrip()
        }

        iv_delete.setOnClickListener {
            uri = null
            iv_cover.setImageURI(null)
            iv_delete.isEnabled = false
            iv_delete.alpha = 0.5f
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
            if (uri != null) {
                uploadCoverImage(uri.toString())
            }

            if (cb_share.isChecked) {
                openShareTripFragment()
            } else {
                // Directly go to DetailedTripActivity if no sharing is required
                val intent = Intent(this@CreateTripActivity, DetailedTripActivity::class.java)
                intent.putExtra("id", tripId)
                startActivity(intent)
            }

            db.waitForPendingWrites().addOnCompleteListener {
                finish()
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
    }

    private fun uploadCoverImage(imageUri: String) {
        val rutaImagen = "TripCover/" + tripId + "/" + System.currentTimeMillis()
        val referenceStorage = FirebaseStorage.getInstance().getReference(rutaImagen)

        // Subir archivo a Firebase Storage
        referenceStorage.putFile(imageUri.toUri())
            .addOnSuccessListener { task ->
                // Obtener la URL de descarga
                task.storage.downloadUrl
                    .addOnSuccessListener { uri ->
                        val imageUrl = uri.toString()
                        db.collection("trips").document(tripId).update("image", imageUrl)
                            .addOnSuccessListener {
                                Toast.makeText(
                                    applicationContext,
                                    "Imagen de portada subida con éxito.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    applicationContext,
                                    "Error al guardar la URL de la imagen: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            applicationContext,
                            "Error al obtener la URL de descarga: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    applicationContext,
                    "No se ha podido subir la imagen: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun init() {
        iv_cover = findViewById(R.id.iv_cover)
        btn_new_image = findViewById(R.id.btn_new_image)
        et_name = findViewById(R.id.et_name)
        cb_online = findViewById(R.id.cb_online)
        cb_share = findViewById(R.id.cb_share)
        btn_create = findViewById(R.id.btn_create)
        iv_delete = findViewById(R.id.iv_delete)
        db = FirebaseFirestore.getInstance()
        user = FirebaseAuth.getInstance().currentUser!!

        // Fetch user profile to initialize `cb_online`
        db.collection("users").document(user.uid).get().addOnSuccessListener { document ->
            if (document.exists()) {
                val isPublic = document.getBoolean("public") ?: false
                cb_online.isChecked = isPublic
            }
        }

        iv_delete.isEnabled = false
        iv_delete.alpha = 0.5f

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