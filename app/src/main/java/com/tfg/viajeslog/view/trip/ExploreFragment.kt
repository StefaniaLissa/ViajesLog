package com.tfg.viajeslog.view.trip

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.tfg.viajeslog.R
import com.tfg.viajeslog.view.adapters.TripAdapter
import com.tfg.viajeslog.viewmodel.TripViewModel
import java.util.ArrayList

class ExploreFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tripAdapter: TripAdapter
    private lateinit var tripViewModel: TripViewModel
    private lateinit var btnDayTrips: Button
    private lateinit var btnShortTrips: Button
    private lateinit var btnLongTrips: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_explore, container, false)

        recyclerView = view.findViewById(R.id.recyclerView)
        btnDayTrips = view.findViewById(R.id.btn_flt_day)
        btnShortTrips = view.findViewById(R.id.btn_flt_short)
        btnLongTrips = view.findViewById(R.id.btn_flt_long)

        tripAdapter = TripAdapter()
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = tripAdapter

        tripViewModel = ViewModelProvider(this).get(TripViewModel::class.java)

        setupFilters()

        setupAutocompleteSearch()

        return view
    }

    private fun setupFilters() {
        btnDayTrips.setOnClickListener {
            filterTripsByDuration(0, 1)
        }

        btnShortTrips.setOnClickListener {
            filterTripsByDuration(1, 3)
        }

        btnLongTrips.setOnClickListener {
            filterTripsByDuration(3, Int.MAX_VALUE)
        }
    }

    private fun filterTripsByDuration(minDays: Int, maxDays: Int) {
        tripViewModel.getTripsByDuration(minDays, maxDays).observe(viewLifecycleOwner) { trips ->
            if (trips.isNotEmpty()) {
                tripAdapter.updateTripList(trips)
            } else {
                Toast.makeText(requireContext(), "No se encontraron viajes", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }


    private fun setupAutocompleteSearch() {

        // Recuperar API KEY
        val ai: ApplicationInfo? = requireContext().packageManager
            ?.getApplicationInfo(
                requireContext().packageName,
                PackageManager.GET_META_DATA
            )
        val apiKey = ai?.metaData?.getString("com.google.android.geo.API_KEY").toString()

        // Initialize Places SDK if not already initialized
        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), apiKey)
        }

        // Get the AutocompleteSupportFragment
        val autocompleteFragment = childFragmentManager.findFragmentById(R.id.fg_autocomplete)
                as AutocompleteSupportFragment

        // Specify the types of place data to return
        autocompleteFragment.setPlaceFields(
            listOf(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.LAT_LNG
            )
        )

        // Set up a PlaceSelectionListener to handle the response
        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                place.latLng?.let { latLng ->
                    filterTripsByLocation(latLng.latitude, latLng.longitude)
                }
            }

            override fun onError(status: com.google.android.gms.common.api.Status) {
                Toast.makeText(requireContext(), "Error al buscar: $status", Toast.LENGTH_SHORT)
                    .show()
            }
        })
    }

    private fun filterTripsByLocation(lat: Double, lng: Double) {
        tripViewModel.getTripsByLocation(lat, lng, 50.0).observe(viewLifecycleOwner) { trips ->
            if (trips.isNotEmpty()) {
                tripAdapter.updateTripList(trips)
            } else {
                Toast.makeText(
                    requireContext(),
                    "No se encontraron viajes cerca",
                    Toast.LENGTH_SHORT
                ).show()
                tripAdapter.updateTripList(emptyList()) // Limpia el RecyclerView
            }
        }
    }
}