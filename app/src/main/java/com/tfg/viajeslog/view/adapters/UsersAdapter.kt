package com.tfg.viajeslog.view.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tfg.viajeslog.R
import com.tfg.viajeslog.model.data.User
import com.google.firebase.firestore.FirebaseFirestore

class UsersAdapter(
    private val tripID: String,
    private val editorsList: List<User>,
    private val tempSelectedEditors: MutableList<User>,
    private val onUserAdded: (User) -> Unit // Callback para notificar al fragmento
) : RecyclerView.Adapter<UsersAdapter.UsersViewHolder>() {

    private var usersList = ArrayList<User>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsersViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.user_item, parent, false)
        return UsersViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return usersList.size
    }

    override fun onBindViewHolder(holder: UsersViewHolder, position: Int) {
        val user = usersList[position]
        holder.email.text = user.email

        // Cargar la imagen de perfil
        if (!user.image.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(user.image)
                .placeholder(R.drawable.ic_user_placeholder) // Imagen por defecto mientras carga
                .error(R.drawable.ic_error) // Imagen en caso de error
                .circleCrop() // Mostrar como círculo
                .into(holder.ivImage)
        } else {
            holder.ivImage.setImageResource(R.drawable.ic_user_placeholder) // Imagen por defecto
        }

        holder.iv_plus.setOnClickListener {
            val member = hashMapOf(
                "admin" to false,
                "userID" to user.id,
                "tripID" to tripID
            )

            FirebaseFirestore.getInstance().collection("members").add(member).addOnSuccessListener {
                tempSelectedEditors.add(user)
                onUserAdded(user) // Notificar al fragmento
            }
        }
    }

    fun updateUsersList(userList: List<User>) {
        this.usersList.clear()
        this.usersList.addAll(userList)
        notifyDataSetChanged()
    }

    class UsersViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val email: TextView = itemView.findViewById(R.id.tv_email)
        val ivImage: ImageView = itemView.findViewById(R.id.iv_image) // ImageView para la foto de perfil
        val iv_plus: ImageView = itemView.findViewById(R.id.iv_plus)
    }
}