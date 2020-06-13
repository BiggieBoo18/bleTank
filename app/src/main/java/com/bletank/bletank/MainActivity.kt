package com.bletank.bletank

import android.Manifest
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private val REQUEST_ENABLE_BLUETOOTH = 1
    private val REQUEST_ENABLE_FINE_LOCATION = 2

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

        checkPermission()
        buttonInit()
    }

    private fun checkPermission() {
        // Check for FINE LOCATION
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {

            } else {
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_ENABLE_FINE_LOCATION)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode) {
            REQUEST_ENABLE_FINE_LOCATION -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                } else {
                    finish()
                }
                return
            }
        }
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
