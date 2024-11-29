package com.tfg.viajeslog.view.profile

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.tfg.viajeslog.R
import com.tfg.viajeslog.view.login.LoginActivity
import com.tfg.viajeslog.viewmodel.UserViewModel
import com.google.firebase.auth.FirebaseAuth
import okhttp3.internal.notify

class ProfileFragment : Fragment() {
    private lateinit var tv_name: TextView
    private lateinit var tv_email: TextView
    private lateinit var btn_logout: Button
    private lateinit var viewModel: UserViewModel
    private lateinit var tv_online: TextView
    private lateinit var iv_edit: ImageView
    private lateinit var iv_imagen: ImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        tv_name = view.findViewById(R.id.tv_name)
        tv_email = view.findViewById(R.id.tv_email)
        btn_logout = view.findViewById(R.id.btn_logout)
        tv_online = view.findViewById(R.id.tv_online)
        iv_edit = view.findViewById(R.id.iv_edit)
        iv_imagen = view.findViewById(R.id.iv_imagen)

        btn_logout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(activity, LoginActivity::class.java))
            activity?.finish()
        }

        iv_edit.setOnClickListener {
            val bundle = Bundle()
            bundle.putCharSequence("name", tv_name.text)
            bundle.putCharSequence("email", tv_email.text)
            bundle.putBoolean("public", tv_online.isVisible)
            bundle.putCharSequence("image", iv_imagen.contentDescription)
            val fragmentTransaction = parentFragmentManager.beginTransaction()
            val editProfileFragment = EditProfileFragment()
            editProfileFragment.arguments = bundle
            fragmentTransaction.add(R.id.fragment_container, editProfileFragment)
                .addToBackStack(null).commit()
        }

        viewModel = ViewModelProvider(this)[UserViewModel::class.java]
        viewModel.user.observe(viewLifecycleOwner) { user ->
            if (user == null) {
                // Handle the case where the user data is null (e.g., show a message or logout)
                tv_name.text = getString(R.string.user_not_found)
                tv_email.text = ""
                tv_online.visibility = View.GONE
                iv_imagen.setImageResource(R.drawable.ic_error) // Set a default or error image
                return@observe
            }

            // Populate the UI with user data
            if (!user.image.isNullOrEmpty()) {
                iv_imagen.contentDescription = user.image
                Glide.with(this).load(user.image).placeholder(R.drawable.ic_downloading)
                    .error(R.drawable.ic_error).centerCrop().into(iv_imagen)
            } else {
                iv_imagen.contentDescription = null
                iv_imagen.setImageResource(R.drawable.ic_user_placeholder)
            }

            tv_name.text = user.name
            tv_email.text = user.email
            tv_online.visibility = if (user.public == true) View.VISIBLE else View.GONE
        }

        return view
    }

}