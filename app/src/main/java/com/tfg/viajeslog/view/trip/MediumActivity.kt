package com.tfg.viajeslog.view.trip

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.tfg.viajeslog.R

class MediumActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = intent.getStringExtra("view") ?: return
        if (view == "share") {
            setContentView(R.layout.activity_medium) // Ensure this layout has a FrameLayout
            val tripId = intent.getStringExtra("tripId") ?: return

            // Attach the ShareTripFragment
            val shareTripFragment = ShareTripFragment()
            shareTripFragment.arguments = Bundle().apply {
                putString("trip", tripId)
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, shareTripFragment)
                .commit()
        }
    }
}