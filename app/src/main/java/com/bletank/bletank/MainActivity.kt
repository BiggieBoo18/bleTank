package com.bletank.bletank

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity


class MainActivity : AppCompatActivity() {
    private val TAG = this::class.java.simpleName
    private val REQUEST_DEVICE = 1
    private val REQUEST_ENABLE_FINE_LOCATION = 1

    private lateinit var btnConnect:  Button
    private lateinit var btnEngine:   Button
    private lateinit var btnForward:  Button
    private lateinit var btnBackward: Button
    private lateinit var btnLeft:     Button
    private lateinit var btnRight:    Button
    private lateinit var btnBrake:    Button

    private var isConnected = false
    private var bluetoothService: BluetoothService? = null

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
                if (!(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    finish()
                }
                return
            }
        }
    }

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, rawBinder: IBinder) {
            bluetoothService = (rawBinder as BluetoothService.LocalBinder).getService()
            Log.d(TAG, "onServiceConnected service= ${bluetoothService}")
        }

        override fun onServiceDisconnected(classname: ComponentName) {
            bluetoothService = null
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
            if (!isConnected) {
                val intent = Intent(this, ScanActivity::class.java)
                startActivityForResult(intent, REQUEST_DEVICE)
            } else {
                btnConnect.text = getString(R.string.connect_button)
                isConnected = false
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode) {
            REQUEST_DEVICE -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val address = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE)
                    bluetoothService?.connect(address)
                    Log.d(TAG, address)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothService?.unbindService(serviceConnection)
        bluetoothService?.stopSelf()
        bluetoothService = null
    }
}
