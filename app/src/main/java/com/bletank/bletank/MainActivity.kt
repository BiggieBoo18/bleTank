package com.bletank.bletank

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.content.*
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.snackbar.Snackbar


class MainActivity : AppCompatActivity() {
    private val TAG = this::class.java.simpleName
    private val REQUEST_DEVICE = 1
    private val REQUEST_ENABLE_FINE_LOCATION = 1

    private val serviceConnection = object: ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            bluetoothService = (binder as BluetoothService.LocalBinder).getService()
            bound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            bluetoothService = null
            bound = false
        }
    }

    private lateinit var rootLayout:   ConstraintLayout
    private lateinit var btnConnect:   Button
    private lateinit var btnForceDisconnect: Button
    private lateinit var btnEngine:    Button
    private lateinit var btnForward:   Button
    private lateinit var btnBackward:  Button
    private lateinit var btnLeft:      Button
    private lateinit var btnRight:     Button
    private lateinit var btnBrake:     Button
    private lateinit var seekbarSpeed: SeekBar

    private var bound = false
    private var isConnected = false
    private var isEngineStarted = false
    private var bluetoothService: BluetoothService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Hide both the navigation bar and the status bar.
        window.decorView.apply {
            systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        }

        checkPermission()
        serviceStart()
        rootLayout = findViewById(R.id.rootLayout)
        buttonInit()
        seekbarInit()
        buttonsEnableDisable(listOf(
            btnEngine,
            btnForward,
            btnBackward,
            btnLeft,
            btnRight,
            btnBrake,
            seekbarSpeed
        ), false)
    }

    override fun onStart() {
        super.onStart()
        serviceInit()
    }

    override fun onStop() {
        super.onStop()
        if (bound) {
            unbindService(serviceConnection)
            bound = false
        }
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

    private fun serviceInit() {
        Intent(this, BluetoothService::class.java).also {
            bindService(it, serviceConnection, Context.BIND_AUTO_CREATE)
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(statusChangeReceiver, makeGattUpdateIntentFilter());
    }
    private fun makeGattUpdateIntentFilter(): IntentFilter {
        val intentFilter = IntentFilter()
        intentFilter.addAction(BluetoothService.ACTION_GATT_CONNECTED)
        intentFilter.addAction(BluetoothService.ACTION_GATT_DISCONNECTED)
        intentFilter.addAction(BluetoothService.ACTION_GATT_SERVICES_DISCOVERED)
        intentFilter.addAction(BluetoothService.ACTION_DATA_AVAILABLE)
        intentFilter.addAction(BluetoothService.DEVICE_DOES_NOT_SUPPORT_UART)
        return intentFilter
    }

    private fun serviceStart() {
        Intent(this, BluetoothService::class.java).also {
            startService(it)
        }
    }

    private fun buttonInit() {
        // get buttons
        btnConnect         = findViewById<Button>(R.id.buttonConnect) // button connect
        btnForceDisconnect = findViewById<Button>(R.id.buttonForceDisconnect) // button force disconnect
        btnEngine          = findViewById<Button>(R.id.buttonEngine)  // button engine
        btnForward         = findViewById<Button>(R.id.buttonForward)  // button forward
        btnBackward        = findViewById<Button>(R.id.buttonBackward)  // button backward
        btnLeft            = findViewById<Button>(R.id.buttonLeft)  // button left
        btnRight           = findViewById<Button>(R.id.buttonRight)  // button right
        btnBrake           = findViewById<Button>(R.id.buttonBrake)  // button brake
        // set listeners
        btnConnect.setOnClickListener {
            if (!isConnected) {
                val intent = Intent(this, ScanActivity::class.java)
                startActivityForResult(intent, REQUEST_DEVICE)
            } else {
                bluetoothService?.disconnect()
                (findViewById<TextView>(R.id.connectedDeviceName)).text = ""
                isConnected = false
                buttonsEnableDisable(listOf(
                    btnEngine,
                    btnForward,
                    btnBackward,
                    btnLeft,
                    btnRight,
                    btnBrake,
                    seekbarSpeed
                ), false)
            }
        }
        btnForceDisconnect.setOnClickListener {
            bluetoothService?.disconnect()
            (findViewById<TextView>(R.id.connectedDeviceName)).text = ""
            isConnected = false
            buttonsEnableDisable(listOf(
                btnEngine,
                btnForward,
                btnBackward,
                btnLeft,
                btnRight,
                btnBrake,
                seekbarSpeed
            ), false)
        }
        btnEngine.setOnClickListener {
            if (!isEngineStarted) {
                bluetoothService?.writeRXCharacteristic(getString(R.string.cmd_engine_start).toByteArray())
                isEngineStarted = true
                btnEngine.text = getString(R.string.engine_stop)
                buttonsEnableDisable(listOf(
                    btnForward,
                    btnBackward,
                    btnLeft,
                    btnRight,
                    btnBrake,
                    seekbarSpeed
                ), true)
            } else {
                bluetoothService?.writeRXCharacteristic(getString(R.string.cmd_engine_stop).toByteArray())
                isEngineStarted = false
                btnEngine.text = getString(R.string.engine_start)
                buttonsEnableDisable(listOf(
                    btnForward,
                    btnBackward,
                    btnLeft,
                    btnRight,
                    btnBrake,
                    seekbarSpeed
                ), false)
            }
        }
        btnBrake.setOnClickListener {
            bluetoothService?.writeRXCharacteristic(getString(R.string.cmd_braking).toByteArray())
        }
        btnForward.setOnClickListener {
            bluetoothService?.writeRXCharacteristic(getString(R.string.cmd_forward).toByteArray())
        }
        btnBackward.setOnClickListener {
            bluetoothService?.writeRXCharacteristic(getString(R.string.cmd_backward).toByteArray())
        }
        btnLeft.setOnClickListener {
            bluetoothService?.writeRXCharacteristic(getString(R.string.cmd_turn_left).toByteArray())
        }
        btnRight.setOnClickListener {
            bluetoothService?.writeRXCharacteristic(getString(R.string.cmd_turn_right).toByteArray())
        }
        LongClickRepeatAdapter.bless(btnBrake)
        LongClickRepeatAdapter.bless(btnForward)
        LongClickRepeatAdapter.bless(btnBackward)
        LongClickRepeatAdapter.bless(btnLeft)
        LongClickRepeatAdapter.bless(btnRight)
    }

    private fun <T: View> buttonsEnableDisable(buttons: List<T>, enable_disable: Boolean) {
        for (button in buttons) {
            button.isEnabled = enable_disable
        }
    }

    private fun seekbarInit() {
        seekbarSpeed = findViewById(R.id.speedBar)
        seekbarSpeed.progress = 255
        seekbarSpeed.max = 255
        seekbarSpeed.setOnSeekBarChangeListener(
            object: SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    bluetoothService?.writeRXCharacteristic(progress.toString().toByteArray())
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                }
            }
        )


    }

    private val statusChangeReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when(intent.action) {
                BluetoothService.ACTION_GATT_CONNECTED -> {
                    runOnUiThread {
                        Log.d(TAG, "UART_CONNECT_MSG")
                        buttonsEnableDisable(listOf(
                            btnEngine
                        ), true)
                        btnConnect.text = getString(R.string.disconnect_button)
                        isConnected = true
                        (findViewById<TextView>(R.id.connectedDeviceName)).text =
                            bluetoothService?.getDeviceName() ?: "null"
                        showSnackbar(rootLayout, "Connection succeeded!")
                    }
                }

                BluetoothService.ACTION_GATT_DISCONNECTED -> {
                    runOnUiThread {
                        Log.d(TAG, "UART_DISCONNECT_MSG")
                        btnConnect.text = getString(R.string.connect_button)
                        isConnected = false
                        (findViewById<TextView>(R.id.connectedDeviceName)).text = ""
                        showSnackbar(rootLayout, "Disconnected...")
                    }
                }

                BluetoothService.ACTION_GATT_SERVICES_DISCOVERED -> {
                    bluetoothService?.enableTXNotification()
                }

                BluetoothService.ACTION_DATA_AVAILABLE -> {
                    val txValue = intent.getByteArrayExtra(BluetoothService.EXTRA_DATA)
                    runOnUiThread {
                        try {
                            val text = txValue.toString(charset("UTF-8"))
                            Log.d(TAG, "Recieved data: ${text}")
                        } catch (e: Exception) {
                            Log.e(TAG, e.toString())
                        }
                    }
                }

                BluetoothService.DEVICE_DOES_NOT_SUPPORT_UART -> {
                    Log.d(TAG, "Device doesn't support UART. Disconnecting")
                    bluetoothService?.disconnect()
                    (findViewById<TextView>(R.id.connectedDeviceName)).text = ""
                    btnConnect.text = getString(R.string.connect_button)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode) {
            REQUEST_DEVICE -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val address = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE)
                    if (bluetoothService?.connect(address)!!) {
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothService?.disconnect()
        bluetoothService?.unbindService(serviceConnection)
        bluetoothService?.stopSelf()
        bluetoothService = null
    }

    fun showSnackbar(view: View, message: String) {
        val snackbar = Snackbar.make(view, message, Snackbar.LENGTH_SHORT)
        snackbar.show()
    }
}
