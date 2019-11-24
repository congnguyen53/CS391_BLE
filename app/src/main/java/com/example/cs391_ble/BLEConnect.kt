package com.example.cs391_ble

import android.Manifest
import android.app.ActivityManager
import android.bluetooth.*
import android.content.Context
import android.net.ConnectivityManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatDrawableManager.get
import kotlinx.android.synthetic.main.activity_main.*
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.pm.PackageManager
import androidx.core.app.ComponentActivity
import androidx.core.app.ComponentActivity.ExtraData
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.view.View
import androidx.core.content.PermissionChecker


private val TAG = BLEConnect::class.java.simpleName

private const val STATE_DISCONNECTED = 0
private const val STATE_CONNECTING = 1
private const val STATE_CONNECTED = 2
private const val SCAN_PERIOD: Long = 5000
const val ACTION_GATT_CONNECTED = "com.example.cs391_ble.ACTION_GATT_CONNECTED"
const val ACTION_GATT_DISCONNECTED = "com.example.cs391_ble.ACTION_GATT_DISCONNECTED"
const val ACTION_GATT_SERVICES_DISCOVERED =
    "com.example.cs391_ble.ACTION_GATT_SERVICES_DISCOVERED"

class BLEConnect: AppCompatActivity()  {
    var mContext = this
    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        SpotifyAPIBUTTON.visibility= View.INVISIBLE
        isConnectedText.setText("Connected!")
        Toast.makeText(this,"Done",Toast.LENGTH_LONG).show()
        // Represents bluetooth device on phone.
        //val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
            val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            bluetoothManager.adapter
        }

        //Now, Initialize a BL Adapter for usage later on...
        // With bluetoothAdapter, one is able to interact with bluetooth devices
        bluetoothAdapter?.takeIf {!it.isEnabled}?.apply {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, 1)
        }


        /**
         * Now, Let's connect to a GATT server, aka the BLE devices...
         * Here is where the fun begins...
         */

        // initialize null
        //var bluetoothGatt: BluetoothGatt? = null
        // FIRST BLE DEVICES..........
        var bluetoothLEScanner = bluetoothAdapter?.getBluetoothLeScanner()
        val bleScanner = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                Log.d("ScanDeviceActivity", "onScanResult(): ${result?.device?.address} - ${result?.device?.name}")
            }
        }
        var device:BluetoothDevice? = bluetoothAdapter?.getRemoteDevice("80:6F:B0:6C:94:2B")
        var connectionState = STATE_DISCONNECTED

        // initialize a callback in order to connect to a Gatt

        /**
         * value of rssi resorts here to the callback!!
         */
        var bluetoothGatt:BluetoothGatt? = null
        val gattCallback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(
                gatt: BluetoothGatt,
                status: Int,
                newState: Int
            ) {
                val intentAction: String
                when (newState) {
                    //if conected state, change variables
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
        }

        // retrieves rssi below and device info...
        connectionState= STATE_CONNECTED
        bluetoothGatt = device?.connectGatt(this, true,gattCallback)
        Log.i(TAG,"Trying to connect")


        bluetoothGatt?.connect()


        var rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, java.lang.Short.MIN_VALUE)
        Toast.makeText(this,"Successfully connected to beacon!",Toast.LENGTH_LONG)
        var isConnected:Boolean? = bluetoothGatt?.readRemoteRssi()
        gattCallback.onReadRemoteRssi(bluetoothGatt,rssi.toInt(),1)
        //Beacon1RSSI.setText(Integer.toString(rssi))
        //bluetoothGatt?.connect()
        /**
         * Allows for user to accept location permission for location...
         */
        when (PermissionChecker.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
            PackageManager.PERMISSION_GRANTED -> bluetoothLEScanner?.startScan(bleScanner)
            else -> requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 1)
        }
        Beacon1RSSI.setText( device?.address +" " +  isConnected + rssi.toString())

    }
    private fun broadcastUpdate(action: String) {
        val intent = Intent(action)
        sendBroadcast(intent)
    }
}

private class LeScanRecord(val device: BluetoothDevice, val rssi: Int)

