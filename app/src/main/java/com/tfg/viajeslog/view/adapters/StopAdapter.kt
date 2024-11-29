package com.tfg.viajeslog.view.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.Timestamp
import com.tfg.viajeslog.R
import com.tfg.viajeslog.model.data.Stop
import com.tfg.viajeslog.view.stop.DetailedStopActivity
import com.tfg.viajeslog.viewmodel.StopViewModel
import com.tfg.viajeslog.viewmodel.UserViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.tfg.viajeslog.view.adapters.TripAdapter.TripViewHolder
import java.util.Calendar
import java.util.Date
import java.util.Locale

class StopAdapter(
    private val isClickable: Boolean = true,
    private val isReadOnly: Boolean = false
) : RecyclerView.Adapter<StopAdapter.StopViewHolder>() {

    lateinit var tripID: String
    private val stopArrayList = ArrayList<Stop>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StopViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.stop_item, parent, false)
        return StopViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: StopViewHolder, position: Int) {
        val stop = stopArrayList[position]
        // Nombre
        if (stop.name.isNullOrEmpty()) {
            holder.name.visibility = View.GONE
        } else {
            holder.name.visibility = View.VISIBLE
            holder.name.text = stop.name
        }
        // Descripción
        if (stop.text.equals("")) {
            holder.text.visibility = View.GONE
        } else {
            holder.text.visibility = View.VISIBLE
            holder.text.text = stop.text
        }
        //Photos
        if (stop.photos.isNullOrEmpty()) {
            holder.rv_images.adapter = null
        } else {
            val layoutManager =
                LinearLayoutManager(holder.rv_images.context, LinearLayoutManager.HORIZONTAL, false)
            val adapter = ImageAdapter(stop.photos!!)
            holder.rv_images.layoutManager = layoutManager
            holder.rv_images.adapter = adapter
        }
        //Timestamp
        if (stop.timestamp == null) {
            holder.time.visibility = View.GONE
            holder.day.visibility = View.GONE
            holder.month.visibility = View.GONE
            holder.year.visibility = View.GONE
        } else {
            holder.time.visibility = View.VISIBLE
            holder.day.visibility = View.VISIBLE
            holder.month.visibility = View.VISIBLE
            holder.year.visibility = View.VISIBLE
            var calendar = Calendar.getInstance()
            var stopDate = Date(stop.timestamp!!.seconds * 1000)
            calendar.setTime(stopDate)
            holder.time.text =
                String.format("%02d", calendar.get(Calendar.HOUR_OF_DAY)) + ":" + String.format(
                    "%02d", calendar.get(Calendar.MINUTE)
                )
            holder.day.text = String.format("%02d", calendar.get(Calendar.DAY_OF_MONTH))
            holder.month.text =
                calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())
            holder.year.text = calendar.get(Calendar.YEAR).toString()
        }

        //UBICACIÓN
        if (stop.idPlace.isNullOrEmpty()) {
            holder.ubi.visibility = View.GONE
        } else {
            holder.ubi.visibility = View.VISIBLE
            holder.ubi.text = stop.namePlace
            holder.LatLng.text = stop.geoPoint.toString()
        }

        // Click
        if (isClickable) {
            holder.itemView.setOnClickListener {
                val intent = Intent(holder.itemView.context, DetailedStopActivity::class.java)
                intent.putExtra("stopID", stop.id)
                intent.putExtra("tripID", tripID)
                intent.putExtra("isReadOnly", isReadOnly)
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                holder.itemView.context.startActivity(intent)
            }
        }

        //Delete
        holder.delete.visibility = View.GONE
        if (!isReadOnly) {
            holder.itemView.setOnLongClickListener {
                if (holder.delete.isVisible) {
                    holder.delete.visibility = View.GONE
                } else {
                    holder.delete.visibility = View.VISIBLE
                }
                true
            }

            holder.bt_delete.setOnClickListener {

                val builder =
                    AlertDialog.Builder(holder.bt_delete.context, R.style.CustomDialogTheme)
                builder.setTitle("Confirmar eliminación")
                builder.setMessage("¿Estás seguro de que deseas eliminar este punto de interés? Esta acción no se puede deshacer.")
                builder.setPositiveButton("Eliminar") { dialog, _ ->


                    val db = FirebaseFirestore.getInstance()
                    val tripRef = db.collection("trips").document(tripID)
                    val stopRef = tripRef.collection("stops").document(stop.id!!)

                    // Eliminar la parada
                    stopRef.delete().addOnSuccessListener {
                        // Obtener todas las paradas restantes
                        tripRef.collection("stops").get().addOnSuccessListener { stopsSnapshot ->
                            if (!stopsSnapshot.isEmpty) {
                                val stops =
                                    stopsSnapshot.documents.mapNotNull { it.toObject(Stop::class.java) }
                                if (stops.isNotEmpty()) {
                                    // Recalcular initDate y endDate
                                    val newInitDate =
                                        stops.minByOrNull { it.timestamp!! }?.timestamp
                                    val newEndDate = stops.maxByOrNull { it.timestamp!! }?.timestamp

                                    // Actualizar los campos en el documento del viaje
                                    tripRef.update(
                                        mapOf(
                                            "initDate" to newInitDate,
                                            "endDate" to newEndDate,
                                            "duration" to calculateDurationDays(
                                                newInitDate, newEndDate
                                            )
                                        )
                                    )
                                }
                            } else {
                                // Si no quedan paradas, reiniciar initDate, endDate y durationDays
                                tripRef.update(
                                    mapOf(
                                        "initDate" to null, "endDate" to null, "duration" to 0
                                    )
                                )
                            }
                        }
                    }

                    // Actualizar el adaptador de la lista
                    stopArrayList.removeAt(position)
                    this.notifyDataSetChanged()

                    dialog.dismiss()
                }
                builder.setNegativeButton("Cancelar") { dialog, _ ->
                    holder.delete.visibility = View.GONE
                    dialog.dismiss()
                }
                builder.create().show()
            }


        }

    }


    override fun getItemCount(): Int {
        return stopArrayList.size
    }

    fun updateStopList(stopList: List<Stop>) {
        this.stopArrayList.clear()
        this.stopArrayList.addAll(stopList)
        notifyDataSetChanged()
    }

    class StopViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.tvName)
        val text: TextView = itemView.findViewById(R.id.tvText)

        val time: TextView = itemView.findViewById(R.id.tvTime)
        val day: TextView = itemView.findViewById(R.id.tvDay)
        val month: TextView = itemView.findViewById(R.id.tvMonth)
        val year: TextView = itemView.findViewById(R.id.tvYear)

        val rv_images: RecyclerView = itemView.findViewById(R.id.rv_images)

        val ubi: TextView = itemView.findViewById(R.id.tvUbi)
        val LatLng: TextView = itemView.findViewById(R.id.tvLatLng)

        val delete: CardView = itemView.findViewById(R.id.cv_delete)
        val bt_delete: ImageView = itemView.findViewById(R.id.iv_delete)
    }

    private fun calculateDurationDays(initDate: Timestamp?, endDate: Timestamp?): Long {
        return if (initDate != null && endDate != null) {
            val diffInMillis = endDate.toDate().time - initDate.toDate().time
            diffInMillis / (1000 * 60 * 60 * 24) // Convertir milisegundos a días
        } else {
            0L
        }
    }
}