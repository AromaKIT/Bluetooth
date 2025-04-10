package com.example.myapplication

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


const val TAG1 = "MainActivity"
const val TAG2 = "Bluetooth"
const val TAG3 = "DataSend"
const val TAG4 = "DataReceive"

private val bluetoothPermissions = mutableListOf(
    Manifest.permission.BLUETOOTH,
    Manifest.permission.BLUETOOTH_ADMIN,
    Manifest.permission.ACCESS_FINE_LOCATION
).apply {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        add(Manifest.permission.BLUETOOTH_CONNECT)
        add(Manifest.permission.BLUETOOTH_SCAN)
    }
}.toTypedArray()

private val PERMISSION_REQUEST_CODE = 1001

@SuppressLint("MissingPermission")      // Currently suppressing missing permissions
class MainActivity : AppCompatActivity() {

    // setup the connection button and the Bluetooth Modules
    private lateinit var connectButton: Button
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter

    private val ENABLE_BLUETOOTH_REQUEST_CODE = 2001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        checkAndRequestPermissions()

        bluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager.adapter

        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            Toast.makeText(this, "Device doesn't support Bluetooth", Toast.LENGTH_SHORT).show()
        }

        connectButton = findViewById(R.id.connect)

        connectButton.setOnClickListener {
            Log.d(TAG2, "Connect button clicked")
            val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
            if (pairedDevices != null) {
                Log.i(TAG2, "Paired devices: $pairedDevices")
                if (pairedDevices.size != 0) {
                    pairedDevices?.forEach { device ->
                        val deviceName = device.name
                        val deviceHardwareAddress = device.address // MAC address

                        if (deviceName.contains("Pico")) {
                            Log.i(TAG2, "Device name: $deviceName, MAC address: $deviceHardwareAddress")
                            Toast.makeText(this, "Pico found", Toast.LENGTH_SHORT).show()
                            val thread = ConnectThread(device)
                            thread.start()
                        } else{
                            Log.i(TAG2, "Device name: $deviceName, MAC address: $deviceHardwareAddress")
                            Log.i(TAG2, "No Pico found")
                            Toast.makeText(this, "No Pico found", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    val bluetoothPermissions = arrayOf(
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.ACCESS_FINE_LOCATION,
        // For Android 12+:
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_SCAN
    )

    // Check and request permissions for bluetooth
    private fun checkAndRequestPermissions() {
        val missingPermissions = bluetoothPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                missingPermissions.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        } else {
            // All permissions granted, continue Bluetooth work
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            ensureBluetoothIsOn()
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // All permissions granted
            } else {
                // Some permissions denied, show a warning or guide user
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // check that Bluetooth is on
    private fun ensureBluetoothIsOn() {
        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported on this device", Toast.LENGTH_LONG).show()
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, ENABLE_BLUETOOTH_REQUEST_CODE)
        } else {
            // Bluetooth is already on — continue with your logic
        }
    }

    // Connect to the Bluetooth device
    private inner class ConnectThread(device: BluetoothDevice) : Thread() {

        private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            device.createRfcommSocketToServiceRecord(device.uuids[0].uuid)
        }

        override fun run() {
            // Cancel discovery because it otherwise slows down the connection.
            bluetoothAdapter?.cancelDiscovery()

            mmSocket?.let { socket ->
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                socket.connect()

                // The connection attempt succeeded. Perform work associated with
                // the connection in a separate thread.
                manageMyConnectedSocket(socket)
            }
        }

        // Sample of how to send and receive data
        fun manageMyConnectedSocket(it: BluetoothSocket) {

            val mmInStream: InputStream = it.inputStream
            val mmOutStream: OutputStream = it.outputStream
            val mmBuffer: ByteArray = ByteArray(1024) // mmBuffer store for the stream

            mmOutStream.write("HELLO \n".toByteArray())
            Log.i(TAG3, "Sent: HELLO")

            try {
                val reader = BufferedReader(InputStreamReader(mmInStream))
                val response = reader.readLine()
                Log.i(TAG4, "Response: $response")
            } catch (e: IOException) {
                Log.e(TAG4, "Error reading from input stream")
            }

            it.close()
        }

        // Closes the client socket and causes the thread to finish.
        fun cancel() {
            try {
                mmSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG2, "Could not close the client socket", e)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == ENABLE_BLUETOOTH_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth enabled
            } else {
                // User declined to enable Bluetooth
                Toast.makeText(this, "Bluetooth not enabled", Toast.LENGTH_SHORT).show()

            }
        }
    }

}
