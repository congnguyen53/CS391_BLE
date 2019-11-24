package com.example.cs391_ble

import android.app.ActivityManager
import android.bluetooth.*
import android.content.Context
import android.net.ConnectivityManager
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatDrawableManager.get
import kotlinx.android.synthetic.main.activity_main.*

private const val STATE_DISCONNECTED = 0
private const val STATE_CONNECTING = 1
private const val STATE_CONNECTED = 2
private const val SCAN_PERIOD: Long = 5000


class BLEConnect: AppCompatActivity()  {
    var mContext = this
    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Toast.makeText(this,"Done",Toast.LENGTH_LONG).show()
        // Represents bluetooth device on phone.
        //val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
            val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            bluetoothManager.adapter
        }

        //Now, Initialize a BL Adapter for usage later on...
        //val bluetoothAdapter:BluetoothAdapter = bluetoothManager.adapter
        // With bluetoothAdapter, one is able to interact with bluetooth devices


        //Check if bluetooth is enabled.
        //val BluetoothAdapter.isDisabled: Boolean
         //   get() = !isEnabled


        bluetoothAdapter?.takeIf {!it.isEnabled}?.apply {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, 2)
        }

        /**
         * Now, Let's connect to a GATT server, aka the BLE devices...
         * Here is where the fun begins...
         */

        // initialize null
        var bluetoothGatt: BluetoothGatt? = null
        // FIRST BLE DEVICES..........
        var device:BluetoothDevice? = bluetoothAdapter?.getRemoteDevice("80:6F:B0:6C:94:2B")
        //val mGattCallback:BluetoothGattCallback? = null
        // Gatt creation, callback will go to mGattCallback
        //bluetoothGatt = device?.connectGatt(this, false,mGattCallback)
        //bluetoothGatt?.discoverServices()
        //bluetoothGatt?.connect()
        Toast.makeText(this,"Successfully connected to beacon!",Toast.LENGTH_LONG)



    }


}