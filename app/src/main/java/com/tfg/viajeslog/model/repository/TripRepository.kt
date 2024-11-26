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

    fun loadTrips(trips: MutableLiveData<List<Trip>>) {
        FirebaseFirestore.getInstance().collection("members")
            .whereEqualTo("userID", FirebaseAuth.getInstance().currentUser!!.uid)
            .addSnapshotListener { snapshot, e ->

                if (e != null) {
                    Log.w("Error", "loadTrips failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    var _trips = ArrayList<Trip>()
                    val db = FirebaseFirestore.getInstance()
                    val tripsRef = db.collection("trips")
                    tripsRef.orderBy("initDate", Query.Direction.ASCENDING)
                        .orderBy("name", Query.Direction.ASCENDING)

                    for (doc in snapshot) {
                        tripsRef.document(doc.get("tripID").toString()).get()
                            .addOnSuccessListener() {
                                val trip = it.toObject(Trip::class.java)
                                if (trip != null) {
                                    trip.id = it.id
                                    _trips.add(trip)
                                    trips.postValue(_trips)
                                }
                            }
                    }
                } else {
                    trips.postValue(emptyList())
                }

//                _trips.sortWith(
//                    compareByDescending<Trip> { it.initDate } .thenBy { it.name }
//                )

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

        val snapshot = db.collection("trips").whereEqualTo("public", true).get().await()

        for (document in snapshot.documents) {
            val trip = document.toObject(Trip::class.java)
            val duration = trip?.duration ?: 0
            if (duration in minDays..maxDays) {
                trip?.let { trips.add(it) }
            }
        }
        return trips
    }

    suspend fun getTripsByLocation(lat: Double, lng: Double, radius: Double): List<Trip> {
        val db = FirebaseFirestore.getInstance()
        val trips = mutableListOf<Trip>()

        // Fetch all public trips
        val tripsSnapshot = db.collection("trips").whereEqualTo("public", true).get().await()

        // Calculate the approximate bounds for the radius
        val centerGeoPoint = GeoPoint(lat, lng)

        for (tripDoc in tripsSnapshot.documents) {
            val tripId = tripDoc.id
            val trip = tripDoc.toObject(Trip::class.java)
            if (trip != null) {
                // Check the stops within the trip
                val stopsSnapshot =
                    db.collection("trips").document(tripId).collection("stops").get().await()

                var found = false
                for (stopDoc in stopsSnapshot.documents) {
                    if (found) break // Exit the outer loop if a match was found

                    val stop = stopDoc.toObject(Stop::class.java)
                    stop?.geoPoint?.let { stopGeoPoint ->
                        val distance = calculateDistance(
                            lat, lng, stopGeoPoint.latitude, stopGeoPoint.longitude
                        )
                        if (distance <= radius) {
                            trips.add(trip)
                            found = true // Set the flag to exit the loop
                        }
                    }
                }
            }
        }

        return trips
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

//    fun loadTripLive(tripId: String) : Flow<Trip?> = callbackFlow {
//        val listener = EventListener<DocumentSnapshot> { documentSnapshot, e ->
//            if (e != null) {
//                cancel()
//                return@EventListener
//            }
//
//            if (documentSnapshot != null && documentSnapshot.exists()) {
//                val trip = documentSnapshot.toObject(Trip::class.java)
//                trySend(trip)
//            } else {
//                // The user document does not exist or has no data
//            }
//        }
//
//
//        val registration =  FirebaseFirestore.getInstance()
//            .collection("trips")
//            .document(tripId).addSnapshotListener(listener)
//        awaitClose { registration.remove() }
//
//    }

}