package com.bletank.bletank

import android.Manifest
import android.app.ListActivity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

private const val SCAN_PERIOD: Long = 10000

class ScanActivity : AppCompatActivity() {
    private val TAG = "ScanActivity"
    private val REQUEST_ENABLE_BT = 1
    private var mScanning: Boolean = false
    private lateinit var handler: Handler
    private lateinit var recyclerView: RecyclerView
    private lateinit var bondedDeviceList: ArrayList<BluetoothDevice>
    private lateinit var scanDeviceList: ArrayList<BluetoothDevice>
    private lateinit var scanDeviceListAdapter: DeviceListAdapter
    private fun PackageManager.missingSystemFeature(name: String): Boolean = !hasSystemFeature(name)

    // Get BluetoothAdapter
    private val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }
    private val BluetoothAdapter.isDisabled: Boolean
        get() = !isEnabled
    private val bluetoothLeScanner by lazy(LazyThreadSafetyMode.NONE) {
        bluetoothAdapter?.bluetoothLeScanner
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)

        // Hide both the navigation bar and the status bar.
        window.decorView.apply {
            systemUiVisibility =
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        }

        handler = Handler()

        recyclerViewInit()
        bleInit()
        scanLeDevice(true)
    }

    private val leScanCallback = object : ScanCallback() {
        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            Log.d(TAG, "LE batch scan result $results")
        }

        override fun onScanResult(callbackType: Int, result: ScanResult) {
            Log.d(TAG, "LE scan result $callbackType $result")
            scanDeviceList.add(result.device)
            scanDeviceListAdapter.notifyItemInserted(scanDeviceList.size-1);
        }

        override fun onScanFailed(errorCode: Int) {
            Log.w(TAG, "LE scan failed $errorCode")
        }
    }

    private fun scanLeDevice(enable: Boolean) {
        if (bluetoothAdapter?.isEnabled!!) {
            when (enable) {
                true -> {
                    // Stops scanning after a pre-defined scan period.
                    handler.postDelayed({
                        mScanning = false
                        bluetoothLeScanner?.stopScan(leScanCallback)
                    }, SCAN_PERIOD)
                    mScanning = true
                    bluetoothLeScanner?.startScan(leScanCallback)
                }
                else -> {
                    mScanning = false
                    bluetoothLeScanner?.stopScan(leScanCallback)
                }
            }
        } else {
            checkPermission()
        }
    }

    private fun recyclerViewInit() {
//        // bonded device list
//        bondedDeviceList = arrayListOf()
//        bondedDeviceList.addAll(bluetoothAdapter?.bondedDevices!!)
        // scan device list
        scanDeviceList = arrayListOf()
        scanDeviceListAdapter = DeviceListAdapter(scanDeviceList)
        // get recyclerView for device list
        val viewManager = LinearLayoutManager(this)
        val divider = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        recyclerView = findViewById<RecyclerView>(R.id.deviceList).apply {
            setHasFixedSize(true)
            // set layout manager
            layoutManager = viewManager
            // divider
            addItemDecoration(divider)
            // set click listener
            scanDeviceListAdapter.setOnItemClickListener(object:DeviceListAdapter.OnItemClickListener{
                override fun onItemClickListener(view: View, position: Int, device: BluetoothDevice) {
                    Log.d(TAG, "Clicked device: ${device.name}")
                }
            })
            // set adapter
            adapter = scanDeviceListAdapter
        }
    }

    private fun bleInit() {
        checkPermission()
    }

    private fun checkPermission() {
        // Check for BLE availability
        packageManager.takeIf { it.missingSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) }
            ?.also {
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
