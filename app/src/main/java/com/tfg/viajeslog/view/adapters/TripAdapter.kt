package com.tfg.viajeslog.view.adapters

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.tfg.viajeslog.R
import com.tfg.viajeslog.model.data.Trip
import com.tfg.viajeslog.view.trip.DetailedTripActivity
import com.tfg.viajeslog.viewmodel.TripViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TripAdapter() : RecyclerView.Adapter<TripAdapter.TripViewHolder>() {
    //    private val tripArrayList = ArrayList<Trip>()
    private val tripArrayList = mutableListOf<Trip>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.trip_item, parent, false)
        return TripViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        val trip = tripArrayList[position]
        holder.name.text = trip.name

        // Fecha del Viaje
        if (trip.initDate != null) {
            val date = Date(trip.initDate!!.seconds * 1000)
            val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            holder.initDate.text = dateFormat.format(date).replaceFirstChar { it -> it.uppercase() }
            holder.initDate.visibility = View.VISIBLE
        } else {
            holder.initDate.visibility = View.GONE
        }

        // Lugar del Viaje
        if (trip.globalPlace != null) {
            holder.place.text = trip.globalPlace
            holder.place.visibility = View.VISIBLE
        } else {
            holder.place.visibility = View.GONE
        }

        if (trip.image != null) {
            // Imagen Cover del Viaje
            Glide.with(holder.itemView.context).load(trip.image)
                .placeholder(R.drawable.ic_downloading).error(R.drawable.ic_error).centerCrop()
                .into(holder.image)
        }

        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, DetailedTripActivity::class.java)
            intent.putExtra("id", trip.id)
            intent.putExtra("initDate", trip.initDate)
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            holder.itemView.context.startActivity(intent)
        }


        holder.delete.visibility = View.GONE
        holder.itemView.setOnLongClickListener {
            if (holder.delete.isVisible) {
                holder.delete.visibility = View.GONE
            } else {
                holder.delete.visibility = View.VISIBLE
            }
            true
        }

        holder.bt_delete.setOnClickListener {
            val tripId = trip.id!!
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            val db = FirebaseFirestore.getInstance()

            // Verificar si el usuario actual es administrador
            db.collection("trips").document(tripId).get().addOnSuccessListener { document ->
                    if (document.exists()) {
                        val adminId =
                            document.getString("admin") // Asegúrate de que el campo "admin" contenga el UID del administrador
                        if (currentUserId == adminId) {
                            // Usuario es administrador: Eliminar todo
                            deleteTripCompletely(tripId, position)
                        } else {
                            // Usuario no es administrador: Solo eliminar de "members"
                            deleteFromMembers(tripId, currentUserId, position)
                        }
                    }
                }.addOnFailureListener { e ->
                    Log.e("DeleteTrip", "Error al verificar rol: ${e.message}")
                }


        }
    }

    private fun deleteFromMembers(tripId: String, userId: String?, position: Int) {
        val db = FirebaseFirestore.getInstance()
        if (userId != null) {
            db.collection("members").whereEqualTo("tripID", tripId).whereEqualTo("userID", userId)
                .get().addOnSuccessListener { snapshot ->
                    for (doc in snapshot) {
                        db.collection("members").document(doc.id).delete()
                    }
                    tripArrayList.removeAt(position)
                    this.notifyDataSetChanged()
                }.addOnFailureListener { e ->
                    Log.e("DeleteTrip", "Error al eliminar de members: ${e.message}")
                }
        }
    }

    private fun deleteTripCompletely(tripId: String, position: Int) {
        val db = FirebaseFirestore.getInstance()

        // 1. Eliminar los documentos relacionados en la colección "members"
        db.collection("members").whereEqualTo("tripID", tripId).get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot) {
                    db.collection("members").document(doc.id).delete()
                }
            }

        // 2. Eliminar las imágenes de "TripCover"
        val storage = FirebaseStorage.getInstance()
        val tripCoverPath = "TripCover/$tripId/"
        val tripCoverRef = storage.reference.child(tripCoverPath)
        tripCoverRef.listAll().addOnSuccessListener { listResult ->
            for (file in listResult.items) {
                file.delete() // Eliminar cada archivo en TripCover
            }
        }

        // 3. Eliminar las imágenes de "Stop_Image" asociadas a cada parada
        db.collection("trips").document(tripId).collection("stops").get()
            .addOnSuccessListener { stopsSnapshot ->
                for (stopDoc in stopsSnapshot) {
                    val stopId = stopDoc.id
                    val stopImagesPath = "Stop_Image/$tripId/$stopId/"
                    val stopImagesRef = storage.reference.child(stopImagesPath)
                    stopImagesRef.listAll().addOnSuccessListener { listResult ->
                        for (file in listResult.items) {
                            file.delete() // Eliminar cada archivo en Stop_Image
                        }
                    }
                }

                // 4. Eliminar el documento del viaje una vez que las imágenes y "members" se eliminaron
                db.collection("trips").document(tripId).delete().addOnSuccessListener {
//                            tripArrayList.removeAt(position)
//                            this.notifyDataSetChanged()
                }.addOnFailureListener { e ->
                    Log.e("DeleteTrip", "Error al eliminar el viaje: ${e.message}")
                }
            }.addOnFailureListener { e ->
                Log.e("DeleteTrip", "Error al cargar las paradas: ${e.message}")
            }
    }

    override fun getItemCount(): Int {
        return tripArrayList.size
    }

    fun updateTripList(tripList: List<Trip>) {
        this.tripArrayList.clear()
        this.tripArrayList.addAll(tripList)
        Log.w("BD", "loadTripsAdapter")
        this.notifyDataSetChanged()
    }

    class TripViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.tvName)
        val image: ImageView = itemView.findViewById(R.id.ivImage)
        val initDate: TextView = itemView.findViewById(R.id.tvDates)
        val place: TextView = itemView.findViewById(R.id.tvPlace)
        val delete: CardView = itemView.findViewById(R.id.cv_delete)
        val bt_delete: ImageView = itemView.findViewById(R.id.iv_delete)
    }

}