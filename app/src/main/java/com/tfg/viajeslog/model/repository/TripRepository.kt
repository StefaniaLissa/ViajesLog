package com.tfg.viajeslog.model.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import com.tfg.viajeslog.model.data.Trip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.firestore.toObject
import com.tfg.viajeslog.model.data.Photo
import com.tfg.viajeslog.model.data.Stop
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class TripRepository {

    @Volatile
    private var INSTANCE: TripRepository? = null

    fun getInstance(): TripRepository {
        return (INSTANCE ?: synchronized(this) {
            val instance = TripRepository()
            INSTANCE = instance
            instance
        })
    }

//    fun loadTrips(trips: MutableLiveData<List<Trip>>) {
//        FirebaseFirestore.getInstance().collection("members")
//            .whereEqualTo("userID", FirebaseAuth.getInstance().currentUser!!.uid)
//            //.orderBy("initDate", Query.Direction.ASCENDING)
//            .addSnapshotListener { snapshot, e ->
//
//                if (e != null) {
//                    Log.w("Error", "loadTrips failed.", e)
//                    return@addSnapshotListener
//                }
//
//                if (snapshot != null && !snapshot.isEmpty) {
//                    var _trips = ArrayList<Trip>()
//                    val db = FirebaseFirestore.getInstance()
//                    val tripsRef = db.collection("trips")
//                    tripsRef.orderBy("initDate", Query.Direction.DESCENDING)
//
//                    for (doc in snapshot) {
//                        tripsRef.document(doc.get("tripID").toString()).get()
//                            .addOnSuccessListener() {
//                                val trip = it.toObject(Trip::class.java)
//                                if (trip != null) {
//                                    trip.id = it.id
//                                    _trips.add(trip)
//                                    // Ordenar por fecha de inicio
//                                    _trips.sortWith( compareBy <Trip> { it.initDate } )
//                                    trips.postValue(_trips)
//                                }
//                            }
//                    }
//                } else {
//                    trips.postValue(emptyList())
//                }
//
//            }
//    }

//    fun loadTrips(trips: MutableLiveData<List<Trip>>) {
//        val currentUserID = FirebaseAuth.getInstance().currentUser!!.uid
//        val db = FirebaseFirestore.getInstance()
//        val tripsRef = db.collection("trips")
//        val membersRef = db.collection("members")
//
//        // Consulta los trips asociados al usuario
//        membersRef.whereEqualTo("userID", currentUserID).get()
//            .addOnSuccessListener { snapshot ->
//                if (snapshot != null && !snapshot.isEmpty) {
//                    val tripIDs = snapshot.documents.mapNotNull { it.getString("tripID") }
//                    if (tripIDs.isEmpty()) {
//                        trips.postValue(emptyList())
//                        return@addOnSuccessListener
//                    }
//
//                    // Obtiene los trips en una sola consulta usando `whereIn`
//                    tripsRef.whereIn(FieldPath.documentId(), tripIDs).get()
//                        .addOnSuccessListener { tripSnapshot ->
//                            val tripList = tripSnapshot.documents.mapNotNull { doc ->
//                                val trip = doc.toObject(Trip::class.java)
//                                trip?.apply { id = doc.id }
//                            }
//                            // Ordenar localmente por initDate
//                            val sortedTrips = tripList.sortedByDescending { it.initDate?.toDate() }
//                            trips.postValue(sortedTrips)
//                        }
//                        .addOnFailureListener { e ->
//                            Log.w("Error", "Error loading trips: ${e.message}", e)
//                            trips.postValue(emptyList())
//                        }
//                } else {
//                    trips.postValue(emptyList())
//                }
//            }
//            .addOnFailureListener { e ->
//                Log.w("Error", "Error loading member trips: ${e.message}", e)
//                trips.postValue(emptyList())
//            }
//    }

    fun loadTrips(trips: MutableLiveData<List<Trip>>) {
        val currentUserID = FirebaseAuth.getInstance().currentUser!!.uid
        val db = FirebaseFirestore.getInstance()
        val tripsRef = db.collection("trips")
        val membersRef = db.collection("members")

        // Real-time listener for trips associated with the user
        membersRef.whereEqualTo("userID", currentUserID)
            .addSnapshotListener { memberSnapshot, memberError ->
                if (memberError != null) {
                    Log.w("Error", "Error fetching member trips: ${memberError.message}", memberError)
                    trips.postValue(emptyList())
                    return@addSnapshotListener
                }

                if (memberSnapshot != null && !memberSnapshot.isEmpty) {
                    val tripIDs = memberSnapshot.documents.mapNotNull { it.getString("tripID") }
                    if (tripIDs.isEmpty()) {
                        trips.postValue(emptyList())
                        return@addSnapshotListener
                    }

                    // Real-time listener for trips data
                    tripsRef.whereIn(FieldPath.documentId(), tripIDs)
                        .addSnapshotListener { tripSnapshot, tripError ->
                            if (tripError != null) {
                                Log.w("Error", "Error fetching trips: ${tripError.message}", tripError)
                                trips.postValue(emptyList())
                                return@addSnapshotListener
                            }

                            if (tripSnapshot != null && !tripSnapshot.isEmpty) {
                                val tripList = tripSnapshot.documents.mapNotNull { doc ->
                                    val trip = doc.toObject(Trip::class.java)
                                    trip?.apply { id = doc.id }
                                }
                                // Sort trips by `initDate` descending (most recent first)
                                val sortedTrips = tripList.sortedByDescending { it.initDate?.toDate() }
                                trips.postValue(sortedTrips)
                            } else {
                                trips.postValue(emptyList())
                            }
                        }
                } else {
                    trips.postValue(emptyList())
                }
            }
    }

    fun loadTrip(documentId: String, mld_trip: MutableLiveData<Trip>) {
        FirebaseFirestore.getInstance().collection("trips").document(documentId)
            .addSnapshotListener { documentSnapshot, e ->
                if (e != null) {
                    Log.w("Error", "loadTrip failed.", e)
                    mld_trip.value = null
                    return@addSnapshotListener
                }
                if (documentSnapshot != null && documentSnapshot.exists()) {
                    Log.w("BD", "loadTrip")
                    val trip = documentSnapshot.toObject(Trip::class.java)
                    mld_trip.value = trip
                } else {
                    mld_trip.value = null
                }
            }
    }

    suspend fun getTripsByDuration(minDays: Int, maxDays: Int): List<Trip> {
        val db = FirebaseFirestore.getInstance()
        val trips = mutableListOf<Trip>()

        val snapshot = db.collection("trips")
            .whereEqualTo("public", true)
            .get().await()

        for (document in snapshot.documents) {
            val trip = document.toObject(Trip::class.java)
            val duration = trip?.duration ?: 0
            if (duration in minDays..maxDays) {
                trip?.let {
                    trips.add(it)
                }
            }
        }

        // Sort trips by most recent initDate
        return trips.sortedByDescending { it.initDate?.toDate() }
    }

    suspend fun getTripsByLocation(lat: Double, lng: Double, radius: Double): List<Trip> {
        val db = FirebaseFirestore.getInstance()
        val trips = mutableListOf<Trip>()

        // Fetch all public trips
        val tripsSnapshot = db.collection("trips")
            .whereEqualTo("public", true)
            .get().await()

        val centerGeoPoint = GeoPoint(lat, lng)

        for (tripDoc in tripsSnapshot.documents) {
            val tripId = tripDoc.id
            val trip = tripDoc.toObject(Trip::class.java)

            if (trip != null) {
                // Check stops within the trip
                val stopsSnapshot = db.collection("trips")
                    .document(tripId).collection("stops")
                    .get().await()

                val hasStopWithinRadius = stopsSnapshot.documents.any { stopDoc ->
                    val stop = stopDoc.toObject(Stop::class.java)
                    stop?.geoPoint?.let { stopGeoPoint ->
                        val distance = calculateDistance(
                            lat, lng, stopGeoPoint.latitude, stopGeoPoint.longitude
                        )
                        distance <= radius
                    } ?: false
                }

                if (hasStopWithinRadius) {
                    trips.add(trip)
                }
            }
        }

        // Sort trips by most recent initDate
        return trips.sortedByDescending { it.initDate?.toDate() }
    }

    private fun calculateDistance(
        lat1: Double, lng1: Double, lat2: Double, lng2: Double
    ): Double {
        val earthRadius = 6371.0 // Radius of the Earth in kilometers

        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)

        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1)) * Math.cos(
            Math.toRadians(lat2)
        ) * Math.sin(dLng / 2) * Math.sin(dLng / 2)

        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        return earthRadius * c // Distance in kilometers
    }

    fun loadAlbumPhotos(tripId: String, photosLiveData: MutableLiveData<List<String>>) {
        val db = FirebaseFirestore.getInstance()
        val imagesList = mutableListOf<String>()

        db.collection("trips")
            .document(tripId)
            .collection("stops")
            .addSnapshotListener { stopsSnapshot, e ->
                if (e != null) {
                    Log.e("TripRepository", "Error loading stops: ${e.message}")
                    photosLiveData.postValue(emptyList()) // Enviar lista vacía si hay un error
                    return@addSnapshotListener
                }

                if (stopsSnapshot == null || stopsSnapshot.isEmpty) {
                    photosLiveData.postValue(emptyList()) // No hay paradas
                    return@addSnapshotListener
                }

                var stopsProcessed = 0
                for (stop in stopsSnapshot.documents) {
                    db.collection("trips")
                        .document(tripId)
                        .collection("stops")
                        .document(stop.id)
                        .collection("photos")
                        .addSnapshotListener { photosSnapshot, e ->
                            if (e != null) {
                                Log.e("TripRepository", "Error loading photos: ${e.message}")
                                return@addSnapshotListener
                            }

                            if (photosSnapshot != null) {
                                for (photoDoc in photosSnapshot.documents) {
                                    val photo = photoDoc.toObject(Photo::class.java)
                                    photo?.url?.let { imagesList.add(it) }
                                }
                            }

                            stopsProcessed++
                            if (stopsProcessed == stopsSnapshot.size()) {
                                // Enviar lista actualizada de fotos
                                photosLiveData.postValue(imagesList)
                            }
                        }
                }
            }
    }

}