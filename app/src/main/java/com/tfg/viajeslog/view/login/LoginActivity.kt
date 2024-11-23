package com.tfg.viajeslog.view.login

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.view.animation.AlphaAnimation
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.tfg.viajeslog.R
import com.tfg.viajeslog.view.MainActivity


class LoginActivity : AppCompatActivity() {

    private lateinit var et_email: EditText
    private lateinit var et_password: EditText
    private lateinit var tv_alert: TextView
    private lateinit var btn_login: Button
    private lateinit var tv_forgot: TextView
    private lateinit var btn_login_google: LinearLayout
    private lateinit var btn_signup: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var pb_login: ProgressBar
    private lateinit var pb_login_google: ProgressBar


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        init();

        btn_login.setOnClickListener {
            login(et_email.text.toString(), et_password.text.toString())
        }

        tv_forgot.setOnClickListener {
            startActivity(Intent(this@LoginActivity, RecoverActivity::class.java))
        }

        btn_signup.setOnClickListener {
            startActivity(Intent(this@LoginActivity, SignupActivity::class.java))
        }

        btn_login_google.setOnClickListener {
            google()
        }

    }

    private fun login(email: String, passw: String) {
        if (!isEmailValid(email)) {
            alert(getString(R.string.wrong_email))
        } else if (!isPasswValid(passw)) {
            alert(getString(R.string.wrong_passw))
        } else {
            // ProgressBar
            pb_login.visibility = View.VISIBLE
            btn_login.visibility = View.GONE

            auth.signInWithEmailAndPassword(email, passw).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // ProgressBar
                    pb_login.visibility = View.GONE
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                }
            }.addOnFailureListener { e ->
                // ProgressBar
                pb_login.visibility = View.GONE
                btn_login.visibility = View.VISIBLE
                if (e.message?.contains("INVALID_LOGIN_CREDENTIALS") == true) {
                    alert(getString(R.string.wrong_login))
                } else {
                    Toast.makeText(applicationContext, e.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun google() {
        // ProgressBar
        pb_login_google.visibility = View.VISIBLE
        btn_login_google.visibility = View.GONE
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        val googleClient = GoogleSignIn.getClient(this, gso)
        val googleSignIntent = googleClient.signInIntent
        googleSignInARL.launch(googleSignIntent)
    }

    private val googleSignInARL = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { resultado ->
        if (resultado.resultCode == RESULT_OK) {
            val data = resultado.data
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                verificarFirebase(account.idToken)
            } catch (e: Exception) {
                Toast.makeText(applicationContext, e.message, Toast.LENGTH_SHORT).show()
            }
        } else {
            // ProgressBar
            pb_login_google.visibility = View.GONE
            btn_login_google.visibility = View.VISIBLE
            Toast.makeText(applicationContext, "Cancelado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun verificarFirebase(idToken: String?) {
        val credencial = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credencial)
            .addOnSuccessListener { authResult ->
                //El usuario es nuevo
                if (authResult.additionalUserInfo!!.isNewUser) {
                    insertFirebase()
                }
                //El usuario ya se registró previamente
                else {
                    // ProgressBar
                    pb_login_google.visibility = View.GONE
                    btn_login_google.visibility = View.VISIBLE
                    startActivity(Intent(this, MainActivity::class.java))
                    finishAffinity()
                }

            }.addOnFailureListener { e ->
                Toast.makeText(applicationContext, e.message, Toast.LENGTH_SHORT).show()

            }

    }

    private fun insertFirebase() {
        var db = FirebaseFirestore.getInstance()
        //Obtener información de la cuenta de Google
        val email = auth.currentUser?.email
        val name = auth.currentUser?.displayName.toString()

        //Registrar en BD
        val user = hashMapOf(
            "email" to email,
            "name" to name,
            "public" to true,
            "googleProvieded" to true
        )

        // ProgressBar
        pb_login_google.visibility = View.GONE
        btn_login_google.visibility = View.VISIBLE

        // Agregar a la colección con nuevo ID
        db.collection("users")
            .document(auth.currentUser!!.uid)
            .set(user)
            .addOnSuccessListener {
                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
            }
            .addOnFailureListener { e ->
                Toast.makeText(applicationContext, e.message, Toast.LENGTH_SHORT).show()
            }
    }

    fun isEmailValid(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
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

    private fun init() {
        et_email = findViewById(R.id.et_email)
        et_password = findViewById(R.id.et_password)
        tv_alert = findViewById(R.id.tv_alert)
        btn_login = findViewById(R.id.btn_login)
        tv_forgot = findViewById(R.id.tv_forgot)
        btn_login_google = findViewById(R.id.btn_login_google)
        btn_signup = findViewById(R.id.btn_signup)
        auth = FirebaseAuth.getInstance()
        pb_login = findViewById(R.id.pb_login)
        pb_login_google = findViewById(R.id.pb_login_google)
    }
}