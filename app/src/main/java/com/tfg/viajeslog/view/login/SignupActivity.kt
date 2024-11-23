package com.tfg.viajeslog.view.login

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.view.animation.AlphaAnimation
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tfg.viajeslog.R
import com.tfg.viajeslog.view.MainActivity


class SignupActivity : AppCompatActivity() {

    private lateinit var et_email: EditText
    private lateinit var et_name: EditText
    private lateinit var et_password: EditText
    private lateinit var et_re_password: EditText
    private lateinit var tv_alert: TextView
    private lateinit var signup: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var pb_signup: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)
        init()

        signup.setOnClickListener {
            pb_signup.visibility = View.VISIBLE
            signup.visibility = View.GONE

            val email = et_email.text.toString()
            val name = et_name.text.toString()
            val passw = et_password.text.toString()
            val re_passw = et_re_password.text.toString()

            if (name.isEmpty()) {
                alert(getString(R.string.signup_user_name), 2)
            } else if (!isEmailValid(email)) {
                alert(getString(R.string.wrong_email), 2)
            } else if (!isPasswValid(passw)) {
                alert(getString(R.string.wrong_passw), 3)
            } else if (re_passw.isEmpty()) {
                alert(getString(R.string.repete), 2)
            } else if (passw != re_passw) {
                alert(getString(R.string.passw_dont_match), 2)
            } else {
                signup(email, passw, name)
            }
        }
    }

    private fun signup(email: String, passw: String, name: String) {
        auth.createUserWithEmailAndPassword(email, passw)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    //Registrar en BD
                    val user = hashMapOf(
                        "email" to email,
                        "name" to name,
                        "public" to true,
                        "googleProvieded" to false
                    )

                    if (task.isComplete) {

                        // Agregar a la colección con nuevo ID
                        db.collection("users")
                            .document(auth.currentUser!!.uid)
                            .set(user)
                            .addOnSuccessListener { documentReference ->
                                Toast.makeText(
                                    applicationContext,
                                    "Se ha registrado con éxito",
                                    Toast.LENGTH_SHORT
                                ).show()
                                startActivity(Intent(this@SignupActivity, MainActivity::class.java))
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    applicationContext,
                                    "${e.message}",
                                    Toast.LENGTH_SHORT
                                )
                                    .show()
                            }

                    } else {
                        Toast.makeText(applicationContext, "Error", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(applicationContext, "${e.message}", Toast.LENGTH_SHORT).show()
            }
        pb_signup.visibility = View.GONE
        signup.visibility = View.VISIBLE
    }

    fun isEmailValid(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isPasswValid(passw: String): Boolean {
        // Mínimo 8 char, una minúscula, una mayúscula, un número
        val passwRegex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}\$"
        return passw.matches(passwRegex.toRegex())
    }

    private fun alert(text: String, duration: Long) {
        pb_signup.visibility = View.GONE
        signup.visibility = View.VISIBLE
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

    private fun init() {
        et_email = findViewById(R.id.et_email)
        et_name = findViewById(R.id.et_name)
        et_password = findViewById(R.id.et_password)
        et_re_password = findViewById(R.id.et_re_password)
        tv_alert = findViewById(R.id.tv_alert)
        signup = findViewById(R.id.signup)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        pb_signup = findViewById(R.id.progress_bar)
    }
}