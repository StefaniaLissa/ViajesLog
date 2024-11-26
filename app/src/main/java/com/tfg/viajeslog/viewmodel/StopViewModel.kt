package com.tfg.viajeslog.viewmodel

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.places.api.net.PlacesClient
import com.tfg.viajeslog.model.data.Stop
import com.tfg.viajeslog.model.repository.StopRepository
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.launch

class StopViewModel() : ViewModel() {
    private val stopRepository: StopRepository = StopRepository().getInstance()
    private val _stopsForTrip = MutableLiveData<List<Stop>>()
    val stopsForTrip: LiveData<List<Stop>> = _stopsForTrip
    private val _stop = MutableLiveData<Stop>()
    val stop: LiveData<Stop> = _stop

    private val _error = MutableLiveData<String>() // Define el estado de error
    val error: LiveData<String> = _error // Exponer el error como LiveData

    val stopsCoordinates = ArrayList<GeoPoint>()

    fun loadStopsForTrip(tripId: String) {
        viewModelScope.launch {
            try {
                stopRepository.loadStops(tripId, _stopsForTrip)
            } catch (e: Exception) {
                _error.postValue("Error loading stops: ${e.message}")
            }
        }
    }

    fun loadStop(tripId: String, stopId: String) {
        viewModelScope.launch {
            try {
                stopRepository.loadSingleStop(tripId, stopId, _stop)
            } catch (e: Exception) {
                postError("Error loading stops: ${e.message}")
            }
        }
    }

    fun getCoordinates(tripId: String): ArrayList<GeoPoint>? {
        viewModelScope.launch {
            try {
                stopRepository.loadCoordinates(tripId, stopsCoordinates)
            } catch (e: Exception) {
                postError("Error loading stops: ${e.message}")
            }
        }
        return stopsCoordinates
    }

    private val _coordinates = MutableLiveData<List<GeoPoint>>()
    val coordinates: LiveData<List<GeoPoint>> = _coordinates

    fun loadCoordinates(tripId: String) {
        viewModelScope.launch {
            try {
                val coordList = ArrayList<GeoPoint>()
                stopRepository.loadCoordinates(tripId, coordList)
                _coordinates.postValue(coordList)
            } catch (e: Exception) {
                postError("Error loading coordinates: ${e.message}")
            }
        }
    }

    val stops = MutableLiveData<List<Stop>>()
    private val stopList = mutableListOf<Stop>()

    fun addStopFromUri(uri: Uri, contentResolver: ContentResolver, apiKey: String, placesClient: PlacesClient) {
        viewModelScope.launch {
            try {
                val stop = stopRepository.extractExifData(uri, contentResolver)
                stop?.let {
                    if (it.geoPoint != null) {
                        // Fetch Place Details
                        stopRepository.fetchPlaceDetails(
                            it.geoPoint!!.latitude,
                            it.geoPoint!!.longitude,
                            apiKey,
                            placesClient
                        ) { fetchedStop ->
                            fetchedStop?.let { updatedStop ->
                                updatedStop.timestamp = it.timestamp // Add timestamp from EXIF
                                updatedStop.geoPoint = it.geoPoint // Add geoPoint from EXIF
                                updatedStop.photos = ArrayList<String>() // Initialize photos list
                                updatedStop.photos!!.add(uri.toString())
                                stopList.add(updatedStop)
                                stops.postValue(stopList) // Notify observers
                            }
                        }
                    } else {
//                        stopList.add(it)
//                        stops.postValue(stopList)
                    }
                } ?: run {
                    _error.postValue("Failed to extract EXIF data")
                }
            } catch (e: Exception) {
                postError("Error loading stops: ${e.message}")
            }
        }
    }

    private fun postError(message: String) {
        _error.postValue(message)
    }
}