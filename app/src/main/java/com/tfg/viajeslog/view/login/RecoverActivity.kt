package com.tfg.viajeslog.view.login

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AlphaAnimation
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tfg.viajeslog.R

class RecoverActivity : AppCompatActivity() {

    private lateinit var et_email: EditText
    private lateinit var btn_recover: Button
    private lateinit var tv_alert: TextView
    private lateinit var auth: FirebaseAuth
    private lateinit var tv_forgot: TextView
    private lateinit var til_email: TextInputLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recover)
        et_email = findViewById(R.id.et_email)
        btn_recover = findViewById(R.id.btn_recover)
        tv_alert = findViewById(R.id.tv_alert)
        tv_forgot = findViewById(R.id.tv_forgot)
        til_email = findViewById(R.id.til_email)

        auth = FirebaseAuth.getInstance()
        var db = FirebaseFirestore.getInstance()

        btn_recover.setOnClickListener {
            var email = et_email.text.toString()
            if (email.isEmpty()) {
                alert(getString(R.string.empty_email), 1)

            } else {
                auth.fetchSignInMethodsForEmail(email).addOnCompleteListener { task ->
                    if (task.isSuccessful()) {

                        // Enviar correo de recuperación
                        auth.setLanguageCode("es")
                        auth.sendPasswordResetEmail(email)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    alert(getString(R.string.sended), 1)

                                    //Correo enviado, reutilizar vista
                                    et_email.visibility = View.GONE
                                    til_email.visibility = View.GONE
                                    tv_forgot.setText("Se ha enviado un enlace de recuperación a su correo electrónico.")
                                    btn_recover.setText("INICIAR SESIÓN")
                                    btn_recover.setOnClickListener {
                                        startActivity(Intent(this, LoginActivity::class.java))
                                        finish()
                                    }
                                } else {
                                    alert(task.exception!!.message.toString(), 1)
                                }
                            }
                            .addOnFailureListener { e -> alert(e.message.toString(), 1) }

                    } else {
                        alert("Correo electrónico sin registrar", 2)
                    }
                }
            }
        }
    }

    private fun alert(text: String, duration: Long) {
        val animation = AlphaAnimation(0f, 1f)
        animation.duration = 4000 * duration
        tv_alert.setText(text)
        tv_alert.startAnimation(animation)
        tv_alert.setVisibility(View.VISIBLE)
        val animation2 = AlphaAnimation(1f, 0f)
        animation2.duration = 4000 * duration
        tv_alert.startAnimation(animation2)
        tv_alert.setVisibility(View.INVISIBLE)
    }
}