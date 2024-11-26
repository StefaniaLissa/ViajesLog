package com.tfg.viajeslog.view.trip

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.tfg.viajeslog.R
import com.tfg.viajeslog.model.data.User
import com.tfg.viajeslog.view.adapters.EditorAdapter
import com.tfg.viajeslog.view.adapters.UsersAdapter
import com.tfg.viajeslog.viewmodel.UserViewModel

class ShareTripFragment : Fragment() {

    lateinit var sv_users: SearchView
    lateinit var usersViewModel: UserViewModel
    lateinit var usersAdapter: UsersAdapter
    lateinit var rv_users: RecyclerView

    lateinit var rv_editors: RecyclerView
    lateinit var editorsViewModel: UserViewModel
    lateinit var editorsAdapter: EditorAdapter

    lateinit var fab_custom: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_share_trip, container, false)
        sv_users = view.findViewById(R.id.sv_users)
        rv_users = view.findViewById(R.id.rv_users)
        rv_editors = view.findViewById(R.id.rv_editors)
        fab_custom = view.findViewById(R.id.fab_custom)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tripID = arguments?.getString("trip")!!

        rv_editors.layoutManager = LinearLayoutManager(context)
        rv_editors.setHasFixedSize(true)
        editorsAdapter = EditorAdapter(tripID)
        rv_editors.adapter = editorsAdapter
        editorsViewModel = ViewModelProvider(this).get(UserViewModel::class.java)
        editorsViewModel.loadEditors(tripID)
        editorsViewModel.allEditors.observe(viewLifecycleOwner, Observer {
            editorsAdapter.updateEditorsList(it)
        })

        rv_users.layoutManager = LinearLayoutManager(context)
        rv_users.setHasFixedSize(true)
        usersAdapter = UsersAdapter(tripID)
        rv_users.adapter = usersAdapter
        usersViewModel = ViewModelProvider(this).get(UserViewModel::class.java)
        usersViewModel.loadUsersExcludingEditors(tripID)

        usersViewModel.allUsers.observe(viewLifecycleOwner, Observer {
            //usersAdapter.updateUsersList(it)
            sv_users.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    TODO("Not yet implemented")
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    filterText(newText, it)
                    return true
                }

            })
        })

        fab_custom.setOnClickListener {
            // Check if the fragment is part of a transaction and pop it from the back stack
            if (parentFragmentManager.backStackEntryCount > 0) {
                parentFragmentManager.popBackStack() // Go back to the previous fragment/activity
            } else {
                // If no fragments are on the back stack, finish the activity
                requireActivity().finish()
            }
        }
    }

    private fun filterText(
        newText: String?, it: List<User>
    ) {
        if (newText != null) {
            val filterList = ArrayList<User>()
            for (i in it) {
                if (i.email.toString().lowercase().contains(newText)) {
                    filterList.add(i)
                }
            }
            if (filterList.isEmpty()) {

            } else {
                usersAdapter.updateUsersList(filterList)
//                usersAdapter.setFilteredList(filterList)
            }
        }
    }

}