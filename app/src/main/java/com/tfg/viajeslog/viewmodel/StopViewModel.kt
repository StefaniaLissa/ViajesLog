package com.tfg.viajeslog.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tfg.viajeslog.model.data.Stop
import com.tfg.viajeslog.model.repository.StopRepository
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.launch

class StopViewModel : ViewModel() {
    private val stopRepository: StopRepository = StopRepository().getInstance()
    private val _stopsForTrip = MutableLiveData<List<Stop>>()
    val stopsForTrip: LiveData<List<Stop>> = _stopsForTrip
    private val _stop = MutableLiveData<Stop>()
    val stop: LiveData<Stop> = _stop
    val stopsCoordinates = ArrayList<GeoPoint>()

    fun loadStopsForTrip(tripId: String) {
        viewModelScope.launch {
            try {
                stopRepository.loadStops(tripId, _stopsForTrip)
            } catch (e: Exception) {
                throw e
            }
        }
    }

    fun loadStop(tripId: String, stopId: String) {
        viewModelScope.launch {
            try {
                stopRepository.loadSingleStop(tripId, stopId, _stop)
            } catch (e: Exception) {
                throw e
            }
        }
    }

    fun getCoordinates(tripId: String): ArrayList<GeoPoint>? {
//        val coordinates = ArrayList<GeoPoint>()
//        _stopsForTrip.value?.forEach { stop ->
//            stop.geoPoint?.let { coordinates.add(it) }
//        }
//        return coordinates
        viewModelScope.launch {
            try {
                stopRepository.loadCoordinates(tripId, stopsCoordinates)
            } catch (e: Exception) {
                throw e
            }
        }
        return stopsCoordinates
    }

    fun getStops(): List<Stop>? {
        return _stopsForTrip.value
    }
}