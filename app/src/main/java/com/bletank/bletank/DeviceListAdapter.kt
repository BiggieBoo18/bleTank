package com.bletank.bletank

import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DeviceListAdapter(private val deviceList: ArrayList<BluetoothDevice>):
    RecyclerView.Adapter<DeviceListAdapter.ViewHolder>() {
    lateinit var listener: OnItemClickListener

    inner class ViewHolder(val deviceListView: View): RecyclerView.ViewHolder(deviceListView) {
        val deviceName = deviceListView.findViewById<TextView>(R.id.deviceName)
        val deviceAddress = deviceListView.findViewById<TextView>(R.id.deviceAddress)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceListAdapter.ViewHolder {
        val context = parent.context
        val inflater = LayoutInflater.from(context)
        val deviceListView = inflater.inflate(R.layout.row, parent, false)
        return ViewHolder(deviceListView)
    }

    override fun onBindViewHolder(holder: DeviceListAdapter.ViewHolder, position: Int) {
        val device = deviceList[position]
        holder.deviceName.text = if (device.name.isNullOrEmpty()) "null" else device.name
        holder.deviceAddress.text = device.address
        holder.deviceListView.setOnClickListener {
            listener.onItemClickListener(it, position, device)
        }
    }

    override fun getItemCount(): Int {
        return deviceList.size
    }

    interface OnItemClickListener {
        fun onItemClickListener(view: View, position: Int, device: BluetoothDevice)
    }

    fun setOnItemClickListener(listener: OnItemClickListener){
        this.listener = listener
    }
}