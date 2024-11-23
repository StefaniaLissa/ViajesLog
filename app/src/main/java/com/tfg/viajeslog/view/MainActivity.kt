package com.tfg.viajeslog.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.tfg.viajeslog.R
import com.tfg.viajeslog.databinding.ActivityMainBinding
import com.tfg.viajeslog.view.profile.ProfileFragment
import com.tfg.viajeslog.view.trip.ExploreFragment
import com.tfg.viajeslog.view.trip.HomeFragment


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Load Home Fragment
        replaceFragment(HomeFragment())
        binding.navbar.selectedItemId = R.id.homeFragment

        //NavBar Listener for Fragments
        binding.navbar.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.profileFragment -> replaceFragment(ProfileFragment())
                R.id.homeFragment -> replaceFragment(HomeFragment())
                R.id.exploreFragment -> replaceFragment(ExploreFragment())
                else -> {
                    replaceFragment(HomeFragment())
                }
            }
            true
        }
    }

    //Método para cambiar de Fragment
    private fun replaceFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.frame_layout, fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }

}