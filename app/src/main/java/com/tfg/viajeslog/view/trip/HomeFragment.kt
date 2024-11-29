package com.tfg.viajeslog.view.trip

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tfg.viajeslog.R
import com.tfg.viajeslog.view.adapters.TripAdapter
import com.tfg.viajeslog.viewmodel.TripViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton

class HomeFragment : Fragment() {

    private lateinit var tripViewModel : TripViewModel
    private lateinit var tripRecyclerView: RecyclerView
    lateinit var tripAdapter: TripAdapter
    private lateinit var  fab_create : FloatingActionButton
    private lateinit var tv_no_trip : TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        fab_create = view.findViewById(R.id.fab_custom)
        tv_no_trip = view.findViewById(R.id.tv_no_trip)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tripRecyclerView = view.findViewById(R.id.recyclerView)
        tripRecyclerView.layoutManager = LinearLayoutManager(context)
        tripRecyclerView.setHasFixedSize(true)
        tripAdapter = TripAdapter()
        tripRecyclerView.adapter = tripAdapter

        tripViewModel = ViewModelProvider(this).get(TripViewModel::class.java)
        tripViewModel.loadTrips()

        tripViewModel.allTrips.observe(viewLifecycleOwner) {
            tripAdapter.updateTripList(it.toMutableList())
            if (it.isEmpty()) {
                tv_no_trip.visibility = View.VISIBLE
            } else {
                tv_no_trip.visibility = View.GONE
            }
        }

        fab_create.setOnClickListener {
            val intent = Intent(activity, CreateTripActivity::class.java)
            startActivity(intent)
        }

    }

}