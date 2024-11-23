package com.tfg.viajeslog.model.repository

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.tfg.viajeslog.model.data.Photo
import com.tfg.viajeslog.model.data.Stop
import com.tfg.viajeslog.model.data.Trip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.toObject

class StopRepository {
    @Volatile
    private var INSTANCE: StopRepository? = null

    fun getInstance(): StopRepository {
        return (INSTANCE ?: synchronized(this) {
            val instance = StopRepository()
            INSTANCE = instance
            instance
        })
    }

    fun loadStops(documentId: String, mld_stops: MutableLiveData<List<Stop>>) {
        FirebaseFirestore.getInstance()
            .collection("trips")
            .document(documentId)
            .collection("stops")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("Error", "loadStops failed.", e)
                    mld_stops.value = null
                    return@addSnapshotListener
                }
                var _stops = ArrayList<Stop>()
                for (doc in snapshot!!) {
                    val stop = doc.toObject(Stop::class.java)
                        stop.id = doc.id

                        //get Images
                        var photoList = ArrayList<String>()
                        FirebaseFirestore.getInstance()
                            .collection("trips")
                            .document(documentId)
                            .collection("stops")
                            .document(doc.id)
                            .collection("photos")
                            .addSnapshotListener { photosDoc, e ->
                                if (e != null) {
                                    Log.w("Error", "load images failed.", e)
                                    mld_stops.value = null
                                    return@addSnapshotListener
                                }
                                if (photosDoc != null) {
                                    for (doc in photosDoc) {
                                        val photo = doc.toObject(Photo::class.java)
                                            photoList.add(photo.url.toString())
                                    }
                                }
                            }
                        stop.photos = photoList
                        _stops.add(stop)
                        mld_stops.postValue(_stops)
                }
            }
    }

    fun loadSingleStop(tripId: String, stopId: String, mld_stop: MutableLiveData<Stop>) {
        FirebaseFirestore.getInstance()
            .collection("trips")
            .document(tripId)
            .collection("stops")
            .document(stopId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("Error", "loadStops failed.", e)
                    mld_stop.value = null
                    return@addSnapshotListener
                } else if (snapshot != null && snapshot.exists()) {
                    val stop = snapshot.toObject(Stop::class.java)

                    if (stop != null) {
                        //Images
                        var photoList = ArrayList<String>()
                        FirebaseFirestore.getInstance()
                            .collection("trips")
                            .document(tripId)
                            .collection("stops")
                            .document(stopId)
                            .collection("photos")
                            .addSnapshotListener { photosDoc, e ->
                                if (e != null) {
                                    Log.w("Error", "load images failed.", e)
                                    mld_stop.value = null
                                    return@addSnapshotListener
                                }
                                if (photosDoc != null) {
                                    for (doc in photosDoc) {
                                        val photo = doc.toObject(Photo::class.java)
                                            photoList.add(photo.url.toString())
                                    }
                                }
                            }
                        stop.photos = photoList
                        mld_stop.postValue(stop)
                    }
                }
            }
    }

// TODO: Unificar en LoadStops y recuperar con todo
    fun loadCoordinates(documentId: String, al_coord: ArrayList<GeoPoint>) {
        FirebaseFirestore.getInstance()
            .collection("trips")
            .document(documentId)
            .collection("stops")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("Error", "loadStops failed.", e)
                    al_coord.clear()
                    return@addSnapshotListener
                }
                for (doc in snapshot!!) {
                    val stop = doc.toObject(Stop::class.java)
                        stop.id = doc.id
                        if (stop.geoPoint != GeoPoint(0.0, 0.0)) {
                            stop.geoPoint?.let { al_coord.add(it) }
                        }
                }
            }
    }
}