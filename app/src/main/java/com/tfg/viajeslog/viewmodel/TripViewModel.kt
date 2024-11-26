package com.tfg.viajeslog.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tfg.viajeslog.model.data.Trip
import com.tfg.viajeslog.model.repository.TripRepository
import kotlinx.coroutines.launch

class TripViewModel : ViewModel() {

    private val repository: TripRepository = TripRepository().getInstance()
    private val _allTrips = MutableLiveData<List<Trip>>()
    val allTrips: LiveData<List<Trip>> = _allTrips

    fun loadTrips() {
        viewModelScope.launch {
                try {
                    repository.loadTrips(_allTrips)
                } catch (e: Exception) {
                    throw e
                }
        }
    }

    private val _trip = MutableLiveData<Trip>()
    val trip: LiveData<Trip> = _trip

    fun loadTrip(documentId: String) {
        viewModelScope.launch {
            try {
                repository.loadTrip(documentId, _trip)
            } catch (e: Exception) {
                throw e
            }
        }
    }

    private val tripRepository = TripRepository()
    fun getTripsByDuration(minDays: Int, maxDays: Int): LiveData<List<Trip>> {
        val tripsLiveData = MutableLiveData<List<Trip>>()
        viewModelScope.launch {
            val trips = tripRepository.getTripsByDuration(minDays, maxDays)
            tripsLiveData.postValue(trips)
        }
        return tripsLiveData
    }

    fun getTripsByLocation(lat: Double, lng: Double, radius: Double): LiveData<List<Trip>> {
        val tripsLiveData = MutableLiveData<List<Trip>>()
        viewModelScope.launch {
            try {
                val trips = tripRepository.getTripsByLocation(lat, lng, radius)
                tripsLiveData.postValue(trips)
            } catch (e: Exception) {
                Log.e("TripViewModel", "Error fetching trips: ${e.message}")
            }
        }
        return tripsLiveData
    }
}