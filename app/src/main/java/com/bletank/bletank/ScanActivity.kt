package com.bletank.bletank

import android.app.ListActivity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.Toast

private const val SCAN_PERIOD: Long = 10000

class ScanActivity : AppCompatActivity() {
    private var mScanning: Boolean = false
    private lateinit var handler: Handler
    private val REQUEST_ENABLE_BT = 1
    private fun PackageManager.missingSystemFeature(name: String): Boolean = !hasSystemFeature(name)
    // Get BluetoothAdapter
    private val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }
    private val BluetoothAdapter.isDisabled: Boolean
        get() = !isEnabled

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)

        // Hide both the navigation bar and the status bar.
        window.decorView.apply {
            systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        }

        bleInit()
    }

//    private fun scanLeDevice(enable: Boolean) {
//        when (enable) {
//            true -> {
//                // Stops scanning after a pre-defined scan period.
//                handler.postDelayed({
//                    mScanning = false
//                    bluetoothAdapter.stopLeScan(leScanCallback)
//                }, SCAN_PERIOD)
//                mScanning = true
//                bluetoothAdapter.startLeScan(leScanCallback)
//            }
//            else -> {
//                mScanning = false
//                bluetoothAdapter.stopLeScan(leScanCallback)
//            }
//        }
//    }

    private fun bleInit() {
        // Check for BLE availability
        packageManager.takeIf { it.missingSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) }?.also {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show()
            finish()
        }

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        bluetoothAdapter?.takeIf { it.isDisabled }?.apply {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
    }
}