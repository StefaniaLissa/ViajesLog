package com.tfg.viajeslog.view.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tfg.viajeslog.R
import com.tfg.viajeslog.model.data.User
import com.google.firebase.firestore.FirebaseFirestore

class UsersAdapter(tripID: String) : RecyclerView.Adapter<UsersAdapter.UsersViewHolder>() {

    private var usersList = ArrayList<User>()
    private val tripID = tripID

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsersViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.user_item, parent, false)
        return UsersAdapter.UsersViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return usersList.size
    }

    override fun onBindViewHolder(holder: UsersViewHolder, position: Int) {
        val user = usersList[position]
        holder.email.text = user.email

        holder.iv_plus.setOnClickListener {
            val member = hashMapOf(
                "admin" to false,
                "userID" to user.id,
                "tripID" to tripID)
            FirebaseFirestore.getInstance().collection("members").add(member)
        }
    }

    fun updateUsersList(userList: List<User>) {
        this.usersList.clear()
        this.usersList.addAll(userList)
        notifyDataSetChanged()
    }

    class UsersViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val email: TextView = itemView.findViewById(R.id.tv_email)
        val iv_plus: ImageView = itemView.findViewById(R.id.iv_plus)
    }

}