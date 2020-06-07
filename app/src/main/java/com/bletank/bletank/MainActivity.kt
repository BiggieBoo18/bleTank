package com.bletank.bletank

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast

class MainActivity : AppCompatActivity() {
    private lateinit var btnConnect:  Button
    private lateinit var btnEngine:   Button
    private lateinit var btnForward:  Button
    private lateinit var btnBackward: Button
    private lateinit var btnLeft:     Button
    private lateinit var btnRight:    Button
    private lateinit var btnBrake:    Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Hide both the navigation bar and the status bar.
        window.decorView.apply {
            systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        }

        buttonInit()
    }

    private fun buttonInit() {
        // get buttons
        btnConnect   = findViewById<Button>(R.id.buttonConnect) // button connect
        btnEngine    = findViewById<Button>(R.id.buttonEngine)  // button engine
        btnForward   = findViewById<Button>(R.id.buttonForward)  // button forward
        btnBackward  = findViewById<Button>(R.id.buttonBackward)  // button backward
        btnLeft      = findViewById<Button>(R.id.buttonLeft)  // button left
        btnRight     = findViewById<Button>(R.id.buttonRight)  // button right
        btnBrake     = findViewById<Button>(R.id.buttonBrake)  // button brake
        // set listeners
        btnConnect.setOnClickListener {
            val intent = Intent(this, ScanActivity::class.java)
            startActivity(intent)
        }
    }
}
