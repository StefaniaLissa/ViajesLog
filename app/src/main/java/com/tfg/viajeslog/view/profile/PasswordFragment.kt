package com.tfg.viajeslog.view.profile

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.tfg.viajeslog.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class PasswordFragment : Fragment() {
    private lateinit var et_password: EditText
    private lateinit var btn_passw: Button
    private lateinit var tv_alert: TextView
    private lateinit var auth: FirebaseAuth
    var user: FirebaseUser? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_password, container, false)
        et_password = view.findViewById(R.id.et_password)
        btn_passw = view.findViewById(R.id.btn_passw)
        tv_alert = view.findViewById(R.id.tv_alert)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btn_passw.setOnClickListener {
            val passw = et_password.text.toString()
            if (isPasswValid(passw)) {
                auth = FirebaseAuth.getInstance()
                user = FirebaseAuth.getInstance().currentUser
                auth.signInWithEmailAndPassword(user?.email.toString(), passw)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val fragmentTransaction = parentFragmentManager.beginTransaction()
                            val newPasswordFragment = NewPasswordFragment()
                            fragmentTransaction
                                .add(R.id.fragment_container, newPasswordFragment)
                                .addToBackStack(null)
                                .commit()
                            parentFragmentManager.beginTransaction().remove(this).commit()
                        }
                    }.addOnFailureListener { e ->
                        if (e.message?.contains("INVALID_LOGIN_CREDENTIALS") == true) {
                            alert(getString(R.string.wrong_passw))
                        } else {
                            alert(e.message.toString())
                            //Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                alert("Contraseña Invalida")
                //Toast.makeText(context, "Contraseña Invalida", Toast.LENGTH_SHORT).show()
            }

        }
    }

    private fun isPasswValid(passw: String): Boolean {
        // Mínimo 8 char, una minúscula, una mayúscula, un número
        val passwRegex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}\$"
        return passw.matches(passwRegex.toRegex())
    }

    private fun alert(text: String) {
        val animation = AlphaAnimation(0f, 1f)
        animation.duration = 4000
        tv_alert.setText(text)
        tv_alert.startAnimation(animation)
        tv_alert.setVisibility(View.VISIBLE)
        val animation2 = AlphaAnimation(1f, 0f)
        animation2.duration = 4000
        tv_alert.startAnimation(animation2)
        tv_alert.setVisibility(View.INVISIBLE)
    }


}