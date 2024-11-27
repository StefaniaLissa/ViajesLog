package com.tfg.viajeslog.view.trip

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tfg.viajeslog.R
import com.tfg.viajeslog.model.data.Photo
import com.tfg.viajeslog.view.adapters.ImageAdapter
import com.google.firebase.firestore.FirebaseFirestore
import com.tfg.viajeslog.viewmodel.TripViewModel

class AlbumFragment : Fragment() {

    private lateinit var rv_images: RecyclerView
    private lateinit var tv_no_fotos: TextView
    private lateinit var adapter: ImageAdapter
    private lateinit var layoutManager: GridLayoutManager
    private lateinit var imagesList: ArrayList<String>
    private lateinit var tripViewModel: TripViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_album, container, false)
        rv_images = view.findViewById(R.id.rv_images)
        tv_no_fotos = view.findViewById(R.id.tv_no_fotos)

        // Configurar el Toolbar
        val toolbar: Toolbar = view.findViewById(R.id.toolbar)
        (activity as AppCompatActivity).setSupportActionBar(toolbar)
        val actionBar = (activity as AppCompatActivity).supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
        actionBar?.title = "Álbum"
        toolbar.setNavigationOnClickListener {
            activity?.onBackPressed()
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar RecyclerView
        imagesList = ArrayList()
        layoutManager = GridLayoutManager(context, 3)
        adapter = ImageAdapter(imagesList)
        rv_images.layoutManager = layoutManager
        rv_images.adapter = adapter

        // Inicializar el ViewModel
        tripViewModel = ViewModelProvider(this).get(TripViewModel::class.java)

        // Obtener Trip ID y cargar fotos
        val tripId = arguments?.getString("trip")!!
        tripViewModel.loadAlbumPhotos(tripId)

        // Observar cambios en las fotos
        tripViewModel.albumPhotos.observe(viewLifecycleOwner) { photos ->
            if (photos.isEmpty()) {
                tv_no_fotos.visibility = View.VISIBLE
                rv_images.visibility = View.GONE
            } else {
                tv_no_fotos.visibility = View.GONE
                rv_images.visibility = View.VISIBLE
                imagesList.clear()
                imagesList.addAll(photos)
                adapter.notifyDataSetChanged()
            }
        }
    }
}