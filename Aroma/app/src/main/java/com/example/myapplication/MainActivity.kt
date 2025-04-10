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

const val TAG1 = "MainActivity"
const val TAG2 = "Bluetooth"
const val TAG3 = "DataSend"
const val TAG4 = "DataReceive"

class MainActivity : AppCompatActivity() {
    private lateinit var connectButton: Button
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

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

        fun manageMyConnectedSocket(it: BluetoothSocket) {

            val mmInStream: InputStream = it.inputStream
            val mmOutStream: OutputStream = it.outputStream
            val mmBuffer: ByteArray = ByteArray(1024) // mmBuffer store for the stream

            mmOutStream.write("HELLO \n".toByteArray())

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

}
