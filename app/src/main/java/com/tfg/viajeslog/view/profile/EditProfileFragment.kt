package com.tfg.viajeslog.view.profile

import com.tfg.viajeslog.helper.ImagePickerHelper
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.tfg.viajeslog.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.tfg.viajeslog.view.login.LoginActivity

@Suppress("DEPRECATION")
class EditProfileFragment : Fragment() {

    private lateinit var imagePickerHelper: ImagePickerHelper

    private lateinit var iv_imagen: ImageView
    private lateinit var iv_delete: ImageView
    private lateinit var btn_new_image: Button
    private lateinit var et_name: EditText
    private lateinit var et_email: EditText
    private lateinit var tv_alert: TextView
    private lateinit var tv_delete: TextView
    private lateinit var btn_save: Button
    private lateinit var cb_online: CheckBox
    private lateinit var btn_passw: Button
    private lateinit var pb_img: ProgressBar
    private var lv_public_old: Boolean = true
    private var oldName: String = ""
    private lateinit var pb_save: ProgressBar

    private var uri: Uri? = null

    private lateinit var auth: FirebaseAuth
    var user: FirebaseUser? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_edit_profile, container, false)
        iv_imagen = view.findViewById(R.id.iv_imagen)
        iv_delete = view.findViewById(R.id.iv_delete)
        btn_new_image = view.findViewById(R.id.btn_new_image)
        et_name = view.findViewById(R.id.et_name)
        et_email = view.findViewById(R.id.et_email)
        tv_alert = view.findViewById(R.id.tv_alert)
        tv_delete = view.findViewById(R.id.tv_delete)
        btn_save = view.findViewById(R.id.btn_save)
        cb_online = view.findViewById(R.id.cb_online)
        btn_passw = view.findViewById(R.id.btn_passw)
        pb_img = view.findViewById(R.id.pb_img)
        pb_save = view.findViewById(R.id.pb_save)

        auth = FirebaseAuth.getInstance()
        user = FirebaseAuth.getInstance().currentUser

        //Get User Intent
        oldName = arguments?.getString("name").toString()
        et_name.setText(oldName)
        et_email.setText(arguments?.getString("email").toString())

        if (arguments?.getString("image") != null) {
            pb_img.visibility = View.VISIBLE
            Glide.with(this).load(arguments?.getString("image").toString())
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        pb_img.visibility = View.GONE
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable,
                        model: Any,
                        target: Target<Drawable>?,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        pb_img.visibility = View.GONE
                        return false
                    }

                }).placeholder(R.drawable.ic_downloading)
                .error(R.drawable.ic_error)
                .centerCrop()
                .into(iv_imagen)
        }
        cb_online.isChecked = arguments?.getBoolean("public")!!
        lv_public_old = cb_online.isChecked


        imagePickerHelper = ImagePickerHelper(context = requireContext(),
            singleImageMode = true, // Cambiar a `false` si se permiten múltiples imágenes
            onImagePicked = { uris ->
                // Manejo de imágenes seleccionadas
                if (uris.isNotEmpty()) {
                    uri = uris[0]
                    iv_imagen.setImageURI(uri) // Mostrar imagen en la vista
                }
            })

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        super.onCreate(savedInstanceState)

        //New Image
        btn_new_image.setOnClickListener {
            imagePickerHelper.showImagePickerDialog(
                galleryLauncher = galleryLauncher,
                cameraLauncher = cameraLauncher,
                permissionLauncher = permissionLauncher
            )
        }

        //Cambiar Contraseña
        btn_passw.setOnClickListener {
            val fragmentTransaction = parentFragmentManager.beginTransaction()
            val passwordFragment = PasswordFragment()
            fragmentTransaction.add(R.id.fragment_container, passwordFragment).addToBackStack(null)
                .commit()
        }

        //Save Changes
        btn_save.setOnClickListener {
            pb_save.visibility = View.VISIBLE
            btn_save.visibility = View.GONE

            //Validations
            val isNewImage =
                uri != null && uri.toString() != arguments?.getString("image").toString()

            if (isNewImage) {
                //Save Image in Firebase Store
                val path = "UserProfile/" + auth.uid
                val referenceStorage = FirebaseStorage.getInstance().getReference(path)
                referenceStorage.putFile(uri!!).addOnSuccessListener { taskSnapshot ->
                    // Obtiene la URL de descarga de la imagen subida
                    taskSnapshot.storage.downloadUrl.addOnCompleteListener { uriTask ->
                        if (uriTask.isSuccessful) {
                            val url = uriTask.result.toString()
                            UpdateFirestore(url)
                        } else {
                            Toast.makeText(
                                context,
                                "Error al obtener la URL de descarga: ${uriTask.exception?.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }.addOnFailureListener { e ->
                    Toast.makeText(
                        context,
                        "No se ha podido subir la imagen debido a: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            val newEmail = et_email.text.toString()
            val isEmailValid = isEmailValid(newEmail)
            val isEmailNew = (newEmail != user!!.email)
            if (isEmailNew) {
                if (isEmailValid) {

                    val currentEmail = user!!.email
                    val builder = androidx.appcompat.app.AlertDialog.Builder(
                        requireContext(), R.style.CustomDialogTheme
                    )
                    builder.setTitle("Reautenticación requerida")
                    builder.setMessage("Por favor, ingrese su contraseña actual para confirmar los cambios.")

                    val input = EditText(requireContext()).apply {
                        setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.yellow))
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                        setHintTextColor(ContextCompat.getColor(requireContext(), R.color.gray))
                        hint = "Enter text here"
                        textAlignment = View.TEXT_ALIGNMENT_CENTER
                    }

                    input.inputType =
                        android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                    input.hint = "Contraseña actual"
                    builder.setView(input)

                    builder.setPositiveButton("Confirmar") { _, _ ->
                        val currentPassword = input.text.toString()
                        val credential = FirebaseAuth.getInstance()
                            .signInWithEmailAndPassword(currentEmail!!, currentPassword)

                        credential.addOnSuccessListener {
                            // Proceed to update email
                            user!!.updateEmail(newEmail).addOnSuccessListener {
                                // Update in Firestore
                                FirebaseFirestore.getInstance().collection("users")
                                    .document(user!!.uid).update("email", newEmail)
                                    .addOnFailureListener {
                                        Toast.makeText(
                                            context,
                                            "Error al actualizar el correo electrónico: ${it.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                Toast.makeText(
                                    context, "Correo actualizado con éxito", Toast.LENGTH_SHORT
                                ).show()
                            }.addOnFailureListener { e ->
                                Toast.makeText(
                                    context,
                                    "Error al actualizar el correo electrónico: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }.addOnFailureListener { e ->
                            Toast.makeText(
                                context,
                                "La reautenticación falló: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }.addOnCompleteListener {
                            pb_save.visibility = View.GONE
                            btn_save.visibility = View.VISIBLE

                        }
                    }

                    builder.setNegativeButton("Cancelar") { dialog, _ ->
                        dialog.dismiss()
                        pb_save.visibility = View.GONE
                        btn_save.visibility = View.VISIBLE
                    }

                    builder.create().show()

                } else {
                    alert(getString(R.string.wrong_email))
                    pb_save.visibility = View.GONE
                    btn_save.visibility = View.VISIBLE
                }
            }

            if (cb_online.isChecked != lv_public_old) {
                FirebaseFirestore.getInstance().collection("users").document(user!!.uid)
                    .update("public", cb_online.isChecked).addOnCompleteListener {
                        pb_save.visibility = View.GONE
                        btn_save.visibility = View.VISIBLE
                    }
            }


            if (et_name.text.toString().trim().isEmpty()) {
                alert("El nombre no puede estar vacío")
            } else if (et_name.text.toString() != oldName) {
                FirebaseFirestore.getInstance().collection("users").document(user!!.uid)
                    .update("name", et_name.text.toString().trim()).addOnCompleteListener {
                        pb_save.visibility = View.GONE
                        btn_save.visibility = View.VISIBLE
                    }
            }

            parentFragmentManager.beginTransaction().remove(this).commit()
        }

        tv_delete.setOnClickListener {
            val builder = androidx.appcompat.app.AlertDialog.Builder(
                requireContext(), R.style.CustomDialogTheme
            )
            builder.setTitle("Confirmación")
            builder.setMessage("¿Está seguro que quiere eliminar su cuenta? Esta acción no se puede deshacer.")

            builder.setPositiveButton("Eliminar") { dialog, _ ->
                dialog.dismiss()

                val userId = auth.uid!!
                val db = FirebaseFirestore.getInstance()
                val storage = FirebaseStorage.getInstance()

                // Step 1: Delete user's trips if they are an admin
                db.collection("trips").whereEqualTo("admin", userId).get()
                    .addOnSuccessListener { tripsSnapshot ->
                        if (!tripsSnapshot.isEmpty) {
                            for (tripDoc in tripsSnapshot) {
                                val tripId = tripDoc.id

                                // Delete TripCover images
                                val tripCoverRef = storage.reference.child("TripCover/$tripId/")
                                tripCoverRef.listAll().addOnSuccessListener { listResult ->
                                    for (file in listResult.items) {
                                        file.delete().addOnFailureListener { e ->
                                            Log.e(
                                                "Delete Error",
                                                "Failed to delete trip cover: ${e.message}"
                                            )
                                        }
                                    }
                                }.addOnFailureListener { e ->
                                    Log.e("List Error", "Failed to list trip covers: ${e.message}")
                                }

                                // Delete Stop images
                                db.collection("trips").document(tripId).collection("stops").get()
                                    .addOnSuccessListener { stopsSnapshot ->
                                        if (!stopsSnapshot.isEmpty) {
                                            for (stopDoc in stopsSnapshot) {
                                                val stopId = stopDoc.id
                                                val stopImagesRef =
                                                    storage.reference.child("Stop_Image/$tripId/$stopId/")
                                                stopImagesRef.listAll()
                                                    .addOnSuccessListener { listResult ->
                                                        for (file in listResult.items) {
                                                            file.delete()
                                                                .addOnFailureListener { e ->
                                                                    Log.e(
                                                                        "Delete Error",
                                                                        "Failed to delete stop image: ${e.message}"
                                                                    )
                                                                }
                                                        }
                                                    }.addOnFailureListener { e ->
                                                        Log.e(
                                                            "List Error",
                                                            "Failed to list stop images: ${e.message}"
                                                        )
                                                    }
                                            }
                                        }

                                        // Delete trip document after images are removed
                                        db.collection("trips").document(tripId).delete()
                                            .addOnFailureListener { e ->
                                                Log.e(
                                                    "Delete Error",
                                                    "Failed to delete trip document: ${e.message}"
                                                )
                                            }
                                    }.addOnFailureListener { e ->
                                        Log.e("Query Error", "Failed to query stops: ${e.message}")
                                    }
                            }
                        }
                    }.addOnFailureListener { e ->
                        Log.e("Query Error", "Failed to query trips: ${e.message}")
                    }

                // Step 2: Remove user from "members" collection
                db.collection("members").whereEqualTo("userID", userId).get()
                    .addOnSuccessListener { membersSnapshot ->
                        if (!membersSnapshot.isEmpty) {
                            for (memberDoc in membersSnapshot) {
                                db.collection("members").document(memberDoc.id).delete()
                                    .addOnFailureListener { e ->
                                        Log.e(
                                            "Delete Error", "Failed to delete member: ${e.message}"
                                        )
                                    }
                            }
                        }
                    }.addOnFailureListener { e ->
                        Log.e("Query Error", "Failed to query members: ${e.message}")
                    }

                // Step 3: Delete profile image
                val profileImageRef = storage.reference.child("UserProfile/$userId")
                profileImageRef.delete().addOnFailureListener { e ->
                    Log.e("Delete Error", "Failed to delete profile image: ${e.message}")
                }

                // Step 4: Delete user document and authentication account
                db.collection("users").document(userId).delete().addOnSuccessListener {
                    FirebaseAuth.getInstance().currentUser?.delete()
                        ?.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(
                                    context, "Cuenta eliminada correctamente.", Toast.LENGTH_SHORT
                                ).show()
                                startActivity(Intent(activity, LoginActivity::class.java))
                                activity?.finish()
                            }
                        }!!.addOnFailureListener { e ->
                            Log.e(
                                "Delete Error", "Failed to delete Firebase Auth user: ${e.message}"
                            )
                        }
                }.addOnFailureListener { e ->
                    Log.e("Delete Error", "Failed to delete user document: ${e.message}")
                }
            }

            builder.setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }

            builder.create().show()
        }

        iv_delete.setOnClickListener {
            val builder = androidx.appcompat.app.AlertDialog.Builder(
                requireContext(), R.style.CustomDialogTheme
            )
            builder.setTitle("Confirmación")
            builder.setMessage("¿Está seguro que quiere eliminar su imagen? Esta acción no se puede deshacer.")

            builder.setPositiveButton("Eliminar") { dialog, _ ->
                dialog.dismiss()
                // Proceed to delete the account
                //Delete Image in Firebase Store
                val path = "UserProfile/" + auth.uid
                val referenceStorage = FirebaseStorage.getInstance().getReference(path)
                referenceStorage.delete().addOnSuccessListener {
                    FirebaseFirestore.getInstance().collection("users").document(auth.uid!!)
                        .update("image", null).addOnFailureListener { e ->
                            Toast.makeText(
                                context,
                                "No se ha eliminado su imagen debido a: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }.addOnSuccessListener {
                            uri = null
                            Glide.with(this).clear(iv_imagen) // Borra cualquier caché
                            iv_imagen.setImageResource(R.drawable.ic_user_placeholder) // Placeholder predeterminado
                        }
                }.addOnFailureListener { e ->
                    Toast.makeText(
                        context,
                        "No se ha podido eliminar la imagen debido a: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            builder.setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }

            builder.create().show()
        }

    }

    fun isEmailValid(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun alert(text: String) {
        val animation = AlphaAnimation(0f, 1f)
        animation.duration = 4000
        tv_alert.setText(text)
        tv_alert.startAnimation(animation)
        tv_alert.setVisibility(View.VISIBLE)
        val animation2 = AlphaAnimation(1f, 0f)
        animation2.duration = 4000
        tv_alert.startAnimation(animation2)
        tv_alert.setVisibility(View.INVISIBLE)
    }


    private fun UpdateFirestore(url: String) {
        FirebaseFirestore.getInstance().collection("users").document(auth.uid!!)
            .update("image", url).addOnFailureListener { e ->
                Toast.makeText(
                    context,
                    "No se ha actualizado su imagen debido a: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        FirebaseFirestore.getInstance().collection("users").document(auth.uid!!)
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