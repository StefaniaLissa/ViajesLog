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
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[UserViewModel::class.java]
        viewModel.user.observe(viewLifecycleOwner) {
            if (it.image != null) {
                iv_imagen.contentDescription = it.image
                Glide.with(this).load(it.image).placeholder(R.drawable.ic_downloading)
                    .error(R.drawable.ic_error).centerCrop().into(iv_imagen)
            }
            tv_name.text = it.name
            tv_email.text = it.email
            if (it.public == false) {
                tv_online.visibility = View.GONE
            } else {
                tv_online.visibility = View.VISIBLE
            }
        }
    }
}