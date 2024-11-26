package com.tfg.viajeslog.model.repository

import android.content.ContentResolver
import android.content.Context
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FetchPlaceResponse
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.firebase.Timestamp
import com.tfg.viajeslog.model.data.Photo
import com.tfg.viajeslog.model.data.Stop
import com.tfg.viajeslog.model.data.Trip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.tasks.await
import okhttp3.OkHttpClient
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import kotlin.coroutines.coroutineContext

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

//    fun loadSingleStop(tripId: String, stopId: String, mld_stop: MutableLiveData<Stop>) {
//        FirebaseFirestore.getInstance()
//            .collection("trips")
//            .document(tripId)
//            .collection("stops")
//            .document(stopId)
////            .get()
////            .addOnSuccessListener { snapshot ->
//            .addSnapshotListener { snapshot, e ->
//                if (e != null) {
//                    Log.w("Error", "loadStops failed.", e)
//                    mld_stop.value = null
//                    return@addSnapshotListener
//                } else
//                    if (snapshot != null && snapshot.exists()) {
//                    val stop = snapshot.toObject(Stop::class.java)
//
//                    if (stop != null) {
//                        //Images
//                        var photoList = ArrayList<String>()
//                        FirebaseFirestore.getInstance()
//                            .collection("trips")
//                            .document(tripId)
//                            .collection("stops")
//                            .document(stopId)
//                            .collection("photos")
//                            .get()
//                            .addOnCompleteListener { photosDoc ->
//                                for (doc in photosDoc.result) {
//                                    val photo = doc.toObject(Photo::class.java)
//                                    photoList.add(photo.url.toString())
//                                }
//                            }
//                        stop.photos = photoList
//                        mld_stop.postValue(stop)
//                    }
//                }
//            }
//    }

    fun loadSingleStop(tripId: String, stopId: String, mld_stop: MutableLiveData<Stop>) {
        // Escuchar cambios en el documento de la parada
        FirebaseFirestore.getInstance()
            .collection("trips")
            .document(tripId)
            .collection("stops")
            .document(stopId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("StopRepository", "Error al cargar la parada.", e)
                    mld_stop.value = null
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val stop = snapshot.toObject(Stop::class.java)
                    if (stop != null) {
                        stop.id = stopId

                        // Escuchar cambios en la colección de fotos de la parada
                        FirebaseFirestore.getInstance()
                            .collection("trips")
                            .document(tripId)
                            .collection("stops")
                            .document(stopId)
                            .collection("photos")
                            .addSnapshotListener { photosSnapshot, photosError ->
                                if (photosError != null) {
                                    Log.w(
                                        "StopRepository",
                                        "Error al cargar fotos de la parada.",
                                        photosError
                                    )
                                    return@addSnapshotListener
                                }

                                val photoList = ArrayList<String>()
                                if (photosSnapshot != null) {
                                    for (photoDoc in photosSnapshot) {
                                        val photo = photoDoc.toObject(Photo::class.java)
                                        photoList.add(photo.url ?: "")
                                    }
                                }
                                // Asigna la lista de fotos a la parada
                                stop.photos = photoList
                                mld_stop.postValue(stop) // Actualiza la parada con fotos
                            }
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

    fun extractExifData(uri: Uri, contentResolver: ContentResolver): Stop? {
        contentResolver.openInputStream(uri)?.use { stream ->
            val exif = ExifInterface(stream)

            // Extract timestamp
            val exifDateFormatter = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")
            val exifDateString = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
            val exifDate = exifDateString?.let {
                LocalDateTime.parse(it, exifDateFormatter)
            }
            val timestamp = exifDate?.let {
                Timestamp(
                    Date.from(
                        it.atZone(ZoneId.systemDefault()).toInstant()
                    )
                )
            }

            // Extract location
            val latLong = FloatArray(2)
            if (exif.getLatLong(latLong)) {
                var geoPoint = GeoPoint(latLong[0].toDouble(), latLong[1].toDouble())
                return Stop(
                    id = "temporary",
                    name = "Prueba",
                    timestamp = timestamp,
                    geoPoint = geoPoint
                )
            }
        }
        return null
    }

    fun fetchPlaceDetails(
        lat: Double,
        lng: Double,
        apiKey: String,
        placesClient: PlacesClient,
        callback: (Stop?) -> Unit
    ) {
        val url = "https://maps.googleapis.com/maps/api/geocode/json?latlng=$lat,$lng&key=$apiKey"
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(null) // Notify failure
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let { json ->
                    val jsonObject = JSONObject(json)
                    val results = jsonObject.getJSONArray("results")
                    if (results.length() > 0) {
                        val firstResult = results.getJSONObject(0)
                        val placeId = firstResult.optString("place_id", "Unknown")
                        val placeAddress = firstResult.optString("formatted_address", "Unknown")
                        var placeName = firstResult.optString("name", "Unknown")

                        if (placeName == "Unknown") {
                            val placeFields = listOf(Place.Field.ID, Place.Field.NAME)
                            val request = FetchPlaceRequest.newInstance(placeId, placeFields)
                            placesClient.fetchPlace(request)
                                .addOnSuccessListener { response: FetchPlaceResponse ->
                                    placeName = response.place.name!!.toString()

                                }.addOnFailureListener { exception: Exception ->
                                    if (exception is ApiException) {
                                        val statusCode = exception.statusCode
                                        TODO("Handle error with given status code")
                                    }
                                }
                        }

                        if (placeName == "Unknown") {
                            placeName = placeAddress.substringBefore(",")
                        }

                        // Create Stop with place details
                        val stop = Stop(
                            id = "temporary",
                            name = placeName,
                            idPlace = placeId,
                            namePlace = placeName,
                            addressPlace = placeAddress
                        )
                        callback(stop)
                    } else {
                        callback(null) // Notify no results
                    }
                }
            }
        })
    }

}