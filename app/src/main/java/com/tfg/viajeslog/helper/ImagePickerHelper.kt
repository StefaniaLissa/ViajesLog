import android.Manifest
import android.app.Dialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.tfg.viajeslog.R

class ImagePickerHelper(
    private val context: Context,
    private val singleImageMode: Boolean = true, // Cambiar a `false` para múltiples imágenes
    private val onImagePicked: (List<Uri>) -> Unit // Callback con las imágenes seleccionadas
) {
    private var cameraUri: Uri? = null

    private val galleryPermission =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    private val cameraPermission = Manifest.permission.CAMERA

    fun openGallery(galleryLauncher: ActivityResultLauncher<Intent>) {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
            if (!singleImageMode) putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        galleryLauncher.launch(intent)
    }

    fun openCamera(cameraLauncher: ActivityResultLauncher<Intent>): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.TITLE, "New Picture")
            put(MediaStore.Images.Media.DESCRIPTION, "From Camera")
        }
        cameraUri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, cameraUri)
        }
        cameraLauncher.launch(intent)

        return cameraUri
    }

    fun checkAndRequestPermissions(
        permission: String,
        onPermissionGranted: () -> Unit,
        onPermissionDenied: () -> Unit
    ) {
        when {
            ContextCompat.checkSelfPermission(context, permission) == PermissionChecker.PERMISSION_GRANTED -> {
                onPermissionGranted()
            }
            else -> {
                Toast.makeText(context, "Debe habilitar los permisos en Configuración.", Toast.LENGTH_SHORT).show()
                onPermissionDenied()
            }
        }
    }

    fun handleGalleryResult(data: Intent?) {
        val uris = mutableListOf<Uri>()
        data?.let {
            val clipData = it.clipData
            if (clipData != null) {
                for (i in 0 until clipData.itemCount) {
                    clipData.getItemAt(i).uri?.let { uri -> uris.add(uri) }
                }
            } else {
                it.data?.let { uri -> uris.add(uri) }
            }
        }
        if (uris.isNotEmpty()) {
            onImagePicked(uris)
        } else {
            Toast.makeText(context, "No se seleccionaron imágenes", Toast.LENGTH_SHORT).show()
        }
    }

    fun handleCameraResult(): Uri? {
        cameraUri?.let { onImagePicked(listOf(it)) }
        return cameraUri
    }

    fun showImagePickerDialog(
        galleryLauncher: ActivityResultLauncher<Intent>,
        cameraLauncher: ActivityResultLauncher<Intent>
    ) {
        val dialog = Dialog(context)
        dialog.setContentView(R.layout.select_img)

        val btnGallery: Button = dialog.findViewById(R.id.btn_gallery)
        val btnCamera: Button = dialog.findViewById(R.id.btn_camera)

        btnGallery.setOnClickListener {
            checkAndRequestPermissions(
                permission = galleryPermission,
                onPermissionGranted = { openGallery(galleryLauncher) },
                onPermissionDenied = { Toast.makeText(context, "Permiso denegado.", Toast.LENGTH_SHORT).show() }
            )
            dialog.dismiss()
        }

        btnCamera.setOnClickListener {
            checkAndRequestPermissions(
                permission = cameraPermission,
                onPermissionGranted = { openCamera(cameraLauncher) },
                onPermissionDenied = { Toast.makeText(context, "Permiso denegado.", Toast.LENGTH_SHORT).show() }
            )
            dialog.dismiss()
        }

        dialog.show()
    }
}
