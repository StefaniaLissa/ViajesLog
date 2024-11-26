package com.tfg.viajeslog.view.profile

import android.Manifest
import android.app.Dialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
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
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.marginRight
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.tfg.viajeslog.R
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class EditProfileFragment : Fragment() {

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

                }).placeholder(R.drawable.ic_downloading).error(R.drawable.ic_error).centerCrop()
                .into(iv_imagen)
        }
        cb_online.isChecked = arguments?.getBoolean("public")!!
        lv_public_old = cb_online.isChecked
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        super.onCreate(savedInstanceState)

        //New Image
        btn_new_image.setOnClickListener {
            CameraOrGalleryDialog()
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
                referenceStorage.putFile(uri!!).addOnSuccessListener { task ->
                    val uriTask: Task<Uri> = task.storage.downloadUrl
                    while (!uriTask.isSuccessful);
                    val url = "${uriTask.result}"
                    UpdateFirestore(url)
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

                    builder.setPositiveButton("Confirmar") { dialog, _ ->
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
                // Proceed to delete the account
                FirebaseFirestore.getInstance().collection("users").document(auth.uid!!).delete()
                    .addOnSuccessListener {
                        FirebaseAuth.getInstance().currentUser?.delete()!!
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    iv_delete.performClick()
                                    parentFragmentManager.beginTransaction().remove(this).commit()
                                }
                            }
                    }.addOnFailureListener { e ->
                        Toast.makeText(
                            context,
                            "No se ha eliminado su cuenta debido a: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
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
                            iv_imagen.setImageDrawable(null)
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

    private fun CameraOrGalleryDialog() {
        val btn_gallery: Button
        val btn_camera: Button

        val dialog = Dialog(requireContext())

        dialog.setContentView(R.layout.select_img)

        btn_gallery = dialog.findViewById(R.id.btn_gallery)
        btn_camera = dialog.findViewById(R.id.btn_camera)

        btn_gallery.setOnClickListener {
            //Toast.makeText(applicationContext, "Abrir galería", Toast.LENGTH_SHORT).show()
            if (ContextCompat.checkSelfPermission(
                    requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE
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
                    requireContext(), Manifest.permission.CAMERA
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

    private fun UpdateFirestore(url: String) {
        FirebaseFirestore.getInstance().collection("users").document(auth.uid!!)
            .update("image", url).addOnFailureListener { e ->
                Toast.makeText(
                    context,
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
        uri = requireActivity().contentResolver.insert(
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
                    context,
                    "Permiso denegado. Actívelo en la configuración para continuar.",
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
                    context,
                    "Permiso denegado. Actívelo en la configuración para continuar.",
                    Toast.LENGTH_SHORT
                ).show()
            }

        }

    private val galleryActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult(),
            ActivityResultCallback<ActivityResult> { result ->
                if (result.resultCode == AppCompatActivity.RESULT_OK) {
                    val data = result.data
                    uri = data!!.data
                    iv_imagen.setImageURI(uri)
                    Glide.with(this).load(uri).placeholder(R.drawable.ic_downloading)
                        .error(R.drawable.ic_error).centerCrop().into(iv_imagen)
                } else {
                    Toast.makeText(context, "Cancelado por el usuario", Toast.LENGTH_SHORT).show()

                }

            })

    private val cameraActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK) {
                iv_imagen.setImageURI(uri)
                Glide.with(this).load(uri).into(iv_imagen)
            } else {
                Toast.makeText(context, "Cancelado por el usuario", Toast.LENGTH_SHORT).show()
            }
        }

}