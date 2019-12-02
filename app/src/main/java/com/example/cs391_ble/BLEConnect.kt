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
import android.text.format.Time
import android.view.View
import androidx.core.content.PermissionChecker
import java.lang.Thread.sleep
import java.util.Date
import kotlin.system.measureNanoTime


private val TAG = BLEConnect::class.java.simpleName

private const val STATE_DISCONNECTED = 0
private const val STATE_CONNECTING = 1
private const val STATE_CONNECTED = 2
private const val SCAN_PERIOD: Long = 5000
const val ACTION_GATT_CONNECTED = "com.example.cs391_ble.ACTION_GATT_CONNECTED"
const val ACTION_GATT_DISCONNECTED = "com.example.cs391_ble.ACTION_GATT_DISCONNECTED"
const val ACTION_GATT_SERVICES_DISCOVERED =
    "com.example.cs391_ble.ACTION_GATT_SERVICES_DISCOVERED"
/**
 * Coordinate-based system!
 */
private val BEACON1_COORD = Pair(1.0,0.0) //beacon 1 on right of diagram
private val BEACON2_COORD = Pair(0.0,2.0) // beacon 2 in middle
private val BEACON3_COORD = Pair(-1.0,0.0) // beacon 3 on left

var rssi1:Int= 0
var rssi2:Int = 0
var rssi3:Int = 0
var distance: Double = 0.0
var txPower : Double = -4.0
val pointA1: Double = 1.0
val pointA2: Double= 0.0
val pointB1: Double = 0.0
val pointB2: Double = 2.0
val pointC1: Double = -1.0
val pointC2: Double = 0.0
var distanceA: Double = 0.0
var distanceB : Double = 0.0
var distanceC : Double = 0.0
var x: Double = 0.0
var y: Double = 0.0



private const val SYS_DELAY = 0.001 // Reading each Device's rssi creates lag...  Subtract from signal received
private const val SIGNAL_S = 0.3    //Speed(m) of signal per ns....

/**
 * As of now, everything will be implemented inside the onCreate function, as there is
 * not enough time to fully implement everything with ease.
 */
class BLEConnect: AppCompatActivity()  {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        SpotifyAPIBUTTON.visibility = View.INVISIBLE
        progressBar.visibility = View.INVISIBLE
        /**
         * Listener to check and see if switch is pressed.  Will change mode from RSSI to TDOA
         */
        on_Switch.setOnCheckedChangeListener{buttonView, isChecked ->
            if(isChecked==false){
                on_Switch.text= getResources().getString(R.string.Beacon_RSSI)
            }
            else
            {
                on_Switch.text= getResources().getString(R.string.Beacon_TDOA)
            }
        }
        initBLE()
    }
    private fun initBLE(){

        isConnectedText.setText("Connected!")
        Toast.makeText(this, "Done", Toast.LENGTH_LONG).show()
        // Represents bluetooth device on phone.
        val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
            val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            bluetoothManager.adapter
        }
        //Now, Initialize a BL Adapter for usage later on...
        // With bluetoothAdapter, one is able to interact with bluetooth devices
        bluetoothAdapter?.takeIf { !it.isEnabled }?.apply {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, 1)
        }


        /**
         * Now, Let's connect to a GATT server, aka the BLE devices...
         * Here is where the fun begins...
         */
        // FIRST BLE DEVICES..........
        var bluetoothLEScanner = bluetoothAdapter?.getBluetoothLeScanner()
        var device: BluetoothDevice? = bluetoothAdapter?.getRemoteDevice("80:6F:B0:6C:94:2B")
        var device2: BluetoothDevice? = bluetoothAdapter?.getRemoteDevice("E0:7D:EA:2D:29:AB")
        var device3: BluetoothDevice? = bluetoothAdapter?.getRemoteDevice("80:6F:B0:6C:8F:B6")
        var connectionState = STATE_DISCONNECTED
        /**
         * value of rssi resorts here to the callback!!
         */
        var bluetoothGatt: BluetoothGatt? = null
        var gattCallback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(
                gatt: BluetoothGatt,
                status: Int,
                newState: Int
            ) {
                /**
                 * INTENTS TO BE IMPLEMENTED
                 */
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
        bluetoothGatt = device?.connectGatt(this, true, gattCallback)
        var bluetoothGatt2 = device2?.connectGatt(this,true,gattCallback)
        var bluetoothGatt3 = device3?.connectGatt(this,true,gattCallback)
        Log.i(TAG, "Trying to connect")

        // Stores all of the important info in this callback
        var scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType : Int, result:ScanResult) {
                gattCallback.onReadRemoteRssi(bluetoothGatt,rssi1,0)
                gattCallback.onReadRemoteRssi(bluetoothGatt2,rssi2,0)
                gattCallback.onReadRemoteRssi(bluetoothGatt3,rssi3,0)
                var isConnected:Boolean? = bluetoothGatt?.readRemoteRssi()
                Log.d("Time1!"," ")
                var isConnected2:Boolean? = bluetoothGatt2?.readRemoteRssi()
                var isConnected3:Boolean? = bluetoothGatt3?.readRemoteRssi()
                if(isConnected==true && isConnected2==true && isConnected3==true)
                    connectionState = STATE_CONNECTED
                else
                    connectionState = STATE_DISCONNECTED
                Log.d("isConnect","${isConnected}, ${isConnected2}, ${isConnected3}.")
                //Setting rssi ..... First implementation...
                if(result?.device?.address == "80:6F:B0:6C:94:2B")
                    Log.d("time1!!!","${measureNanoTime {rssi1 = result.getRssi()}}")
                if(result?.device?.address == "E0:7D:EA:2D:29:AB")
                    Log.d("time2!!!","${measureNanoTime {rssi2 = result.getRssi()}}")
                if(result?.device?.address == "80:6F:B0:6C:8F:B6")
                    Log.d("time3!!!","${measureNanoTime {rssi3 = result.getRssi()}}")
                Beacon1RSSI.text=Integer.toString(rssi1) + " dBm"
                Beacon2RSSI.text=Integer.toString(rssi2) + " dBm"
                Beacon3RSSI.text=Integer.toString(rssi3) + " dBm"
                //angleCalc(bluetoothGatt)
                distanceA = calculateBeaconDistance(rssi1)
                distanceB = calculateBeaconDistance(rssi2)
                distanceC = calculateBeaconDistance(rssi3)
                trilateration()
                Location.text= "x: " + x.toString() + "/n y:" + y.toString()

            }
        }
        /**
         * Allows for user to accept location permission for location...
         */
        when (PermissionChecker.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
            PackageManager.PERMISSION_GRANTED -> {
                bluetoothLEScanner?.startScan(scanCallback)
            }
            else -> requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 1)
        }
        bluetoothGatt?.connect()
        bluetoothGatt2?.connect()
        bluetoothGatt3?.connect()
        sleep(1000)
        Toast.makeText(this, "Connected!", Toast.LENGTH_SHORT)
        gattCallback.onReadRemoteRssi(bluetoothGatt,rssi1,0)
        gattCallback.onReadRemoteRssi(bluetoothGatt2,rssi2,0)
        gattCallback.onReadRemoteRssi(bluetoothGatt3,rssi3,0)
    }

    /**
     * USES TDOA TO CALCULATE LOCATION
     */
    fun angleCalc(gatt:BluetoothGatt?){
        var characteristics: List<BluetoothGattService>? = gatt?.services
        for(service in characteristics!!){
            Log.d("servicesss","${service.instanceId}")
        }
    }



    private fun broadcastUpdate(action: String) {
        val intent = Intent(action)
        sendBroadcast(intent)
    }


}

fun calculateBeaconDistance(rssi: Int): Double {
    var rssiD = rssi.toDouble()
    var ratio : Double
    var accuracy : Double
    // Manufacture set this power in the device
    txPower = -4.0
    val signalPropagationConstant: Double = 2.0;
    var d = Math.pow(((txPower - rssiD) / (10 * signalPropagationConstant)),10.0)


    if (rssiD == 0.0){

       return  -1.0; // if we cannot determine accuracy, return -1.

    }

    return Math.pow(10*d, ( txPower - rssi) / (10.0 * signalPropagationConstant))
}

/**
 * It needs distanceA is distance from beacon 1, distanceB is distance from beacon 2, distanceC is distance from beacon 3
 * , pointA1, pointA2 (location of beacon 1) (1.0,0.0)
 * , pointB1, pointB2  (location of beacon 2) (0.0,2.0)
 * , pointC1, pointC2 (location of beacon 3)  (-1.0,0.0)
 * x, y are the location of the device
 */

fun trilateration() {
    var A: Double
    var B: Double
    var C: Double
    var D: Double
    var E: Double
    var F: Double

    A = 2* pointB1 - 2* pointA1
    B = 2* pointB2 - 2* pointA2
    C = distanceA*distanceA - distanceB*distanceB - pointA1*pointA1 + pointB1*pointB1 - pointA2*pointA2 + pointB2*pointB2
    D = 2*pointC1 - 2*pointB1
    E = 2*pointC2 - 2*pointB2
    F = distanceB*distanceB - distanceC*distanceC - pointB1*pointB1 + pointC1*pointC1 - pointB2*pointB2 + pointC2*pointC2
    x = (C*E - F*B) / (E*A - B*D)
    y = (C*D - A*F) / (B*D - A*E)


}







