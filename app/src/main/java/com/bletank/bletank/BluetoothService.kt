package com.bletank.bletank

import android.app.Service
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import java.util.*


class BluetoothService: Service() {
    companion object {
        val ACTION_GATT_CONNECTED = "com.example.bluetooth.le.ACTION_GATT_CONNECTED"
        val ACTION_GATT_DISCONNECTED = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED"
        val ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED"
        val ACTION_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE"
        val DEVICE_DOES_NOT_SUPPORT_UART = "com.example.bluetooth.le.DEVICE_DOES_NOT_SUPPORT_UART"
        val EXTRA_DATA = "com.example.bluetooth.le.EXTRA_DATA"
    }

    private val TAG = BluetoothService::class.java.simpleName
    private val STATE_DISCONNECTED = 0
    private val STATE_CONNECTING = 1
    private val STATE_CONNECTED = 2

    private val UART_SERVICE_UUID: UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
    private val RX_CHARACTERISTIC_UUID: UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e")
    private val TX_CHARACTERISTIC_UUID: UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e")
    private val CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    private val binder = LocalBinder()

    // Get BluetoothManager
    private val bluetoothManager: BluetoothManager? by lazy(LazyThreadSafetyMode.NONE) {
        bluetoothManager
    }
    // Get BluetoothAdapter
    private val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }
    private val BluetoothAdapter.isDisabled: Boolean
        get() = !isEnabled

    private var connectionState = STATE_DISCONNECTED
    private var bluetoothGatt: BluetoothGatt? = null

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(
            gatt: BluetoothGatt,
            status: Int,
            newState: Int
        ) {
            val intentAction: String
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    intentAction = ACTION_GATT_CONNECTED
                    connectionState = STATE_CONNECTED
                    broadcastUpdate(intentAction)
                    Log.i(TAG, "Connected to GATT server.")
                    Log.i(
                        TAG, "Attempting to start service discovery: " +
                                bluetoothGatt?.discoverServices()
                    )
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    intentAction = ACTION_GATT_DISCONNECTED
                    connectionState = STATE_DISCONNECTED
                    Log.i(TAG, "Disconnected from GATT server.")
                    broadcastUpdate(intentAction)
                }
            }
        }

        // New services discovered
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            when (status) {
                BluetoothGatt.GATT_SUCCESS -> broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED)
                else -> Log.w(TAG, "onServicesDiscovered received: $status")
            }
        }

        // Result of a characteristic read operation
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            when (status) {
                BluetoothGatt.GATT_SUCCESS -> {
                    broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
                }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            super.onCharacteristicChanged(gatt, characteristic)
            if (characteristic != null) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
            }
        }
    }

    private fun broadcastUpdate(action: String) {
        val intent = Intent(action)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun broadcastUpdate(
        action: String,
        characteristic: BluetoothGattCharacteristic
    ) {
        val intent = Intent(action)

        // This is handling for the notification on TX Character of NUS service
        if (TX_CHARACTERISTIC_UUID.equals(characteristic.uuid)) {

            // Log.d(TAG, String.format("Received TX: %d",characteristic.getValue() ));
            intent.putExtra(EXTRA_DATA, characteristic.value)
        } else {
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    fun connect(address: String): Boolean {
        if (bluetoothAdapter == null || address.isEmpty()) {
            return false
        }
        val device = bluetoothAdapter!!.getRemoteDevice(address) ?: return false
        bluetoothGatt = device.connectGatt(this, false, gattCallback)
        Log.d(TAG, "Trying to create a new connection.")
        connectionState = STATE_CONNECTING
        return true
    }

    fun disconnect() {
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            return
        }
        bluetoothGatt?.disconnect()
    }

    fun close() {
        if (bluetoothGatt == null) {
            return
        }
        bluetoothGatt?.close()
        bluetoothGatt = null;
    }

    fun readCharacteristic(characteristic: BluetoothGattCharacteristic?) {
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized")
            return
        }
        bluetoothGatt!!.readCharacteristic(characteristic)
    }

    fun enableTXNotification() {
        val RxService: BluetoothGattService? = bluetoothGatt?.getService(UART_SERVICE_UUID)
        if (RxService == null) {
            Log.d(TAG, "Rx service not found!")
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART)
            return
        }
        val TxChar = RxService.getCharacteristic(TX_CHARACTERISTIC_UUID)
        if (TxChar == null) {
            Log.d(TAG, "Tx charateristic not found!")
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART)
            return
        }
        bluetoothGatt?.setCharacteristicNotification(TxChar, true)
        val descriptor = TxChar.getDescriptor(CCCD)
        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        bluetoothGatt?.writeDescriptor(descriptor)
    }

    fun writeRXCharacteristic(value: ByteArray?) {
        val RxService: BluetoothGattService? = bluetoothGatt?.getService(UART_SERVICE_UUID)
        Log.d(TAG, "bluetoothGatt : $bluetoothGatt")
        if (RxService == null) {
            Log.d(TAG, "Rx service not found!")
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART)
            return
        }
        val RxChar = RxService.getCharacteristic(RX_CHARACTERISTIC_UUID)
        if (RxChar == null) {
            Log.d(TAG, "Rx charateristic not found!")
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART)
            return
        }
        RxChar.value = value
        val status: Boolean? = bluetoothGatt?.writeCharacteristic(RxChar)
        Log.d(TAG, "write TXchar - status=$status")
    }

    inner class LocalBinder: Binder() {
        fun getService(): BluetoothService = this@BluetoothService
    }
    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }
    override fun onUnbind(intent: Intent?): Boolean {
        return super.onUnbind(intent)
    }

}