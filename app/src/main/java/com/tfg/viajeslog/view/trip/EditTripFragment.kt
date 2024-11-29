package com.tfg.viajeslog.view.trip

import com.tfg.viajeslog.helper.ImagePickerHelper
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SearchView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity.RESULT_OK
import androidx.core.net.toUri
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.tfg.viajeslog.R
import com.tfg.viajeslog.viewmodel.TripViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class EditTripFragment : Fragment() {

    private lateinit var iv_cover: ImageView
    private lateinit var btn_new_image: Button
    private lateinit var iv_delete: ImageView
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
    private lateinit var viewModel: TripViewModel
    private lateinit var tripID: String
    private var originalImageUrl: String? = null // Guardar URL original de la imagen


    private lateinit var imagePickerHelper: ImagePickerHelper // com.tfg.viajeslog.helper.ImagePickerHelper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_edit_trip, container, false)
        iv_cover = view.findViewById(R.id.iv_cover)
        btn_new_image = view.findViewById(R.id.btn_new_image)
        iv_delete = view.findViewById(R.id.iv_delete)
        et_name = view.findViewById(R.id.et_name)
        cb_online = view.findViewById(R.id.cb_online)
        ll_cb_share = view.findViewById(R.id.ll_cb_share)
        sv_user = view.findViewById(R.id.sv_user)
        ll_users = view.findViewById(R.id.ll_users)
        cb_share = view.findViewById(R.id.cb_share)
        btn_create = view.findViewById(R.id.btn_create)
        db = FirebaseFirestore.getInstance()
        user = FirebaseAuth.getInstance().currentUser!!
        uriString = String()

        //Get Trip Intent
        tripID = arguments?.getString("trip")!!

        //Get Trip
        viewModel = ViewModelProvider(this).get(TripViewModel::class.java)
        viewModel.loadTrip(tripID)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configurar com.tfg.viajeslog.helper.ImagePickerHelper
        imagePickerHelper = ImagePickerHelper(context = requireContext(),
            singleImageMode = true,
            onImagePicked = { uris ->
                if (uris.isNotEmpty()) {
                    uri = uris[0]
                    updateCoverImage(uri)
                    iv_delete.isEnabled = true
                    iv_delete.alpha = 1.0f
                }
            })

        // Observador del ViewModel para cargar los datos del viaje
        viewModel = ViewModelProvider(this).get(TripViewModel::class.java)
        viewModel.loadTrip(tripID)

        viewModel.trip.observe(viewLifecycleOwner) {
            if (it != null) {
                et_name.setText(it.name)
                if (it.image != null) {
                    originalImageUrl = it.image // Guardar URL original
                    iv_cover.contentDescription = it.image
                    Glide.with(this).load(it.image).placeholder(R.drawable.ic_downloading)
                        .error(R.drawable.ic_error).centerCrop().into(iv_cover)
                    iv_delete.isEnabled = true
                    iv_delete.alpha = 1.0f
                } else {
                    iv_delete.isEnabled = false
                    iv_delete.alpha = 0.5f
                }
                viewModel.trip.removeObservers(viewLifecycleOwner)
            }
        }

        // Imagen como botón para cambiarla
        iv_cover.setOnClickListener {
            imagePickerHelper.showImagePickerDialog(
                galleryLauncher = galleryLauncher,
                cameraLauncher = cameraLauncher,
                permissionLauncher = permissionLauncher
            )
        }
        btn_new_image.setOnClickListener {
            imagePickerHelper.showImagePickerDialog(
                galleryLauncher = galleryLauncher,
                cameraLauncher = cameraLauncher,
                permissionLauncher = permissionLauncher
            )
        }

        // Botón para eliminar la imagen
        iv_delete.setOnClickListener {
            clearCoverImage()

        }

        // Botón para guardar los cambios
        btn_create.setOnClickListener {
            updateTrip()
        }

    }

    private fun updateTrip() {
        val trip = hashMapOf(
            "name" to et_name.text.toString(), "public" to cb_online.isChecked
        )

        // Actualizar los datos en Firestore
        val docRef = db.collection("trips").document(tripID)
        docRef.update(trip as Map<String, Any>).addOnSuccessListener {
            // Subir la imagen si es necesario
            uri?.let { imageUri ->
                uploadCoverImage(imageUri.toString())
            }

            // Eliminar la imagen anterior si fue eliminada
            if (originalImageUrl != null && uri == null) {
                deleteImageFromStorage(originalImageUrl!!)
            }

            Toast.makeText(context, "Viaje actualizado correctamente", Toast.LENGTH_SHORT).show()
            parentFragmentManager.beginTransaction().remove(this).commit()
        }.addOnFailureListener { e ->
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteImageFromStorage(imageUrl: String) {
        val referenceStorage = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl)
        referenceStorage.delete().addOnSuccessListener {
            db.collection("trips").document(tripID).update("image", null) // Actualizar Firestore
            Toast.makeText(context, "Imagen eliminada correctamente", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener { e ->
            Toast.makeText(context, "Error al eliminar la imagen: ${e.message}", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun uploadCoverImage(imageUri: String) {
        val rutaImagen = "TripCover/$tripID/${System.currentTimeMillis()}"
        val referenceStorage = FirebaseStorage.getInstance().getReference(rutaImagen)
        referenceStorage.putFile(imageUri.toUri()).addOnSuccessListener { task ->
            task.storage.downloadUrl.addOnSuccessListener { uri ->
                db.collection("trips").document(tripID).update("image", uri.toString())
            }
        }.addOnFailureListener { e ->
            Toast.makeText(context, "No se pudo subir la imagen: ${e.message}", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun clearCoverImage() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(
            requireContext(), R.style.CustomDialogTheme
        )
        builder.setTitle("Confirmación")
        builder.setMessage("¿Está seguro de que desea eliminar la imagen? Esta acción no se puede deshacer.")

        builder.setPositiveButton("Eliminar") { dialog, _ ->
            dialog.dismiss()
            // Actualizar estado de la imagen
            uri = null
            iv_cover.setImageResource(R.drawable.ic_cover_background) // Imagen por defecto
            iv_cover.contentDescription = null

            // Si había una imagen original en Firestore, actualizar el modelo
            if (!originalImageUrl.isNullOrEmpty()) {
                deleteImageFromStorage(originalImageUrl!!)
                originalImageUrl = null
            }

            iv_delete.isEnabled = false
            iv_delete.alpha = 0.5f
        }

        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.dismiss()
        }

        builder.create().show()
    }

    private fun updateCoverImage(uri: Uri?) {
        iv_cover.setImageURI(uri)
    }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                Toast.makeText(context, "Permiso denegado. Vuelva a intentarlo.", Toast.LENGTH_SHORT).show()
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