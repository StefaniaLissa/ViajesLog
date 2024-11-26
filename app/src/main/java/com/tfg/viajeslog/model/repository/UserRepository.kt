package com.tfg.viajeslog.model.repository

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.tfg.viajeslog.model.data.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UserRepository {
    @Volatile
    private var INSTANCE: UserRepository? = null


    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid


    fun getInstance(): UserRepository {
        return (INSTANCE ?: synchronized(this) {
            val instance = UserRepository()
            INSTANCE = instance
            instance
        })
    }

    //Carga a todos los usuarios menos al actual
    fun loadAllUsers(users: MutableLiveData<List<User>>) {
        FirebaseFirestore.getInstance().collection("users").addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("Error", "loadAllUsers failed.", e)
                    return@addSnapshotListener
                }

                var _users = ArrayList<User>()
                for (doc in snapshot!!) {
                    val user = doc.toObject(User::class.java)
                    if (doc.id != currentUserId) {
                        user.id = doc.id
                        _users.add(user)
                        users.postValue(_users)
                    }
                }
            }
    }

    fun loadEditors(tripID: String, editors: MutableLiveData<List<User>>) {
        FirebaseFirestore.getInstance().collection("members").whereEqualTo("tripID", tripID)
            .whereEqualTo("admin", false).addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("Error", "loadEditors failed.", e)
                    return@addSnapshotListener
                }

                var _editors = ArrayList<User>()
                for (doc in snapshot!!) {

                    FirebaseFirestore.getInstance().collection("users")
                        .document(doc.get("userID").toString()).get().addOnSuccessListener() {
                            val editor = it.toObject(User::class.java)
                            if (editor != null) {
                                editor.id = it.id
                                _editors.add(editor)
                                editors.postValue(_editors)
                            }
                        }

                }
            }
    }

    fun loadUser(user: MutableLiveData<User>) {
        FirebaseFirestore.getInstance().collection("users").document(currentUserId!!)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("Error", "Listen failed.", e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val usr = snapshot.toObject(User::class.java)
                    usr!!.id = snapshot.id
                    user.postValue(usr)
                }
            }
    }

    fun loadUsersExcludingEditors(tripId: String, users: MutableLiveData<List<User>>) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        // Obtener los IDs de los editores del viaje
        FirebaseFirestore.getInstance()
            .collection("members")
            .whereEqualTo("tripID", tripId)
            .get()
            .addOnSuccessListener { memberSnapshot ->
                val editorIds = memberSnapshot.documents.map { it.getString("userID") }

                // Obtener todos los usuarios excepto los editores y el usuario actual
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .get()
                    .addOnSuccessListener { userSnapshot ->
                        val filteredUsers = userSnapshot.documents
                            .filter { it.id != currentUserId && !editorIds.contains(it.id) }
                            .map { it.toObject(User::class.java)!!.apply { id = it.id } }

                        users.postValue(filteredUsers)
                    }
                    .addOnFailureListener {
                        // Manejar errores al obtener usuarios
                        users.postValue(emptyList())
                    }
            }
            .addOnFailureListener {
                // Manejar errores al obtener editores
                users.postValue(emptyList())
            }
    }
}