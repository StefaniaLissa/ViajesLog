package com.tfg.viajeslog.view.adapters

import android.app.Dialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tfg.viajeslog.R

class ImageAdapter(
    private val images: ArrayList<String>,
    private val onImageDelete: ((String) -> Unit)? = null // Callback opcional
) : RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.image_item, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        Glide.with(holder.itemView.context).load(images[position])
            .placeholder(R.drawable.ic_downloading)
            .error(R.drawable.ic_error)
            .centerCrop() // Scale image to fill ImageView while maintaining aspect ratio
            .into(holder.image)

        // Botón eliminar
        holder.btnDelete.visibility = if (onImageDelete != null) View.VISIBLE else View.GONE
        holder.btnDelete.setOnClickListener {
            onImageDelete?.invoke(images[position]) // Notificar al callback
        }

        // Dialogo para mostrar la imagen en pantalla completa
        holder.image.setOnClickListener {
            showImageDialog(holder.itemView, images[position])
        }
    }

    override fun getItemCount(): Int {
        return images.size
    }

    class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.imageView)
        val btnDelete: ImageView = itemView.findViewById(R.id.btn_delete) // Botón de eliminar
    }

    // Function to display image in a Dialog
    private fun showImageDialog(view: View, imageUrl: String) {
        val dialog = Dialog(view.context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(R.layout.dialog_image_preview)

        val imagePreview: ImageView = dialog.findViewById(R.id.imagePreview)
        val btnClose: ImageView = dialog.findViewById(R.id.btnClose)

        // Load the image into the ImageView using Glide
        Glide.with(view.context).load(imageUrl).placeholder(R.drawable.ic_downloading)
            .error(R.drawable.ic_error).into(imagePreview)

        // Close the dialog when the close button is clicked
        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}