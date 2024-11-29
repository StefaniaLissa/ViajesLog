package com.tfg.viajeslog.view.tripExtra

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
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

    private val tempSelectedEditors = mutableListOf<User>() // Usuarios seleccionados temporalmente
    private val editorsList = mutableListOf<User>() // Usuarios ya guardados como editores
    private val allUsers = mutableListOf<User>() // Todos los usuarios

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

        // Botón para cerrar el fragmento
        fab_custom.setOnClickListener {
            if (parentFragmentManager.backStackEntryCount > 0) {
                parentFragmentManager.popBackStack()
            } else {
                requireActivity().finish()
            }
        }

        // Configurar RecyclerView de editores
        rv_editors.layoutManager = LinearLayoutManager(context)
        rv_editors.setHasFixedSize(true)
        editorsAdapter = EditorAdapter(tripID) { removedEditor ->
            tempSelectedEditors.remove(removedEditor)
            filterAndRefreshUsers() // Actualizar lista de usuarios
        }
        rv_editors.adapter = editorsAdapter

        editorsViewModel = ViewModelProvider(this).get(UserViewModel::class.java)
        editorsViewModel.loadEditors(tripID)

        editorsViewModel.allEditors.observe(viewLifecycleOwner) { editors ->
            editorsList.clear()
            editorsList.addAll(editors)
            editorsAdapter.updateEditorsList(editors)
            filterAndRefreshUsers() // Actualizar lista de usuarios al cambiar los editores
        }

        // Configurar RecyclerView de usuarios
        rv_users.layoutManager = LinearLayoutManager(context)
        rv_users.setHasFixedSize(true)
        usersAdapter = UsersAdapter(tripID, editorsList, tempSelectedEditors) { addedUser ->
            tempSelectedEditors.add(addedUser)
            filterAndRefreshUsers() // Actualizar lista de usuarios tras agregar
        }
        rv_users.adapter = usersAdapter

        usersViewModel = ViewModelProvider(this).get(UserViewModel::class.java)
        usersViewModel.loadUsersExcludingEditors(tripID)

        // Buscar usuarios
        sv_users.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (!newText.isNullOrEmpty()) {
                    searchUsers(newText)
                } else {
                    usersAdapter.updateUsersList(emptyList())
                }
                return true
            }
        })
    }

    private fun searchUsers(query: String) {
        usersViewModel.searchUsers(query).observe(viewLifecycleOwner) { users ->
            val filteredUsers = users.filter { user ->
                user.id !in editorsList.map { it.id } && // Excluir editores existentes
                        user.id !in tempSelectedEditors.map { it.id } // Excluir seleccionados temporalmente
            }
            usersAdapter.updateUsersList(filteredUsers)
        }
    }

    private fun filterAndRefreshUsers() {
        val filteredUsers = allUsers.filter { user ->
            user.id !in editorsList.map { it.id } &&
                    user.id !in tempSelectedEditors.map { it.id }
        }
        usersAdapter.updateUsersList(filteredUsers)
    }
}