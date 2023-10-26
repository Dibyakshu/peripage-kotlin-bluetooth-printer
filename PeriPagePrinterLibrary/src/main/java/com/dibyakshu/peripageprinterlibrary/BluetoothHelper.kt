package com.dibyakshu.peripageprinterlibrary

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream
import java.nio.ByteBuffer
import java.util.Arrays
import java.util.UUID

private const val TAG = "APP_DEBUG"

/**
 * A utility class for managing Bluetooth operations, such as connecting to a printer and printing content.
 *
 * @param appActivity The ComponentActivity associated with the application.
 * @param context The application's context.
 */
class BluetoothHelper(private val appActivity: ComponentActivity, private val context: Context) {
    private val bluetoothManager: BluetoothManager = appActivity.getSystemService(BluetoothManager::class.java)
    private var bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    /**
     * The name of the Bluetooth printer to which this BluetoothHelper will attempt to connect.
     */
    private val printerName: String = "PPG_A2_3DC4"

    // Commands for printing
    private val cmdPrintStart = byteArrayOf(0x10.toByte(), 0xFF.toByte(), 0xFE.toByte(), 0x01.toByte())
    private val cmdPrintEnd = byteArrayOf(0x1B.toByte(), 0x4A.toByte(), 0x40.toByte(), 0x10.toByte(), 0xFF.toByte(), 0xFE.toByte(), 0x45.toByte())
    private val cmdSetPrintInfo = byteArrayOf(0x1D.toByte(), 0x76.toByte(), 0x30.toByte(), 0x00.toByte(), 0x30.toByte(), 0x00.toByte())
    private var btPerm: Boolean = false

    private val bitmapHelper = BitmapHelper(context)

    companion object {
        /**
         * A constant used to represent the permission code for requesting Bluetooth connection permission.
         * This code is used when requesting permission to connect to Bluetooth devices.
         */
        private const val BLUETOOTH_CONNECT_PERMISSION_CODE = 1
    }

    init {
        setBtPermissions()
    }

    /**
     * Function to set Bluetooth permissions based on the Android version.
     * It checks and assigns the `btPerm` variable to indicate whether Bluetooth permissions are granted.
     * The behavior differs between Android versions (API levels).
     */
    private fun setBtPermissions() {
        // Check if the device is running Android API level 31 (Android 12) or higher.
        btPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(
                appActivity,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // For Android versions lower than 12 (API level 31), you can handle it differently.
            ActivityCompat.checkSelfPermission(
                appActivity,
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(
                        appActivity,
                        Manifest.permission.BLUETOOTH_ADMIN
                    ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Function to check if Bluetooth permissions are granted and request them if necessary.
     * The behavior differs based on the Android version, and it handles Bluetooth permissions accordingly.
     */
    fun checkAndRequestBluetoothPermission() {
        // Check if the device is running Android API level 31 (Android 12) or higher.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Request BLUETOOTH_CONNECT permission.
            if (ActivityCompat.checkSelfPermission(
                    appActivity,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    appActivity, // Make sure your context is an AppCompatActivity. by adding as AppCompatActivity
                    arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                    BLUETOOTH_CONNECT_PERMISSION_CODE
                )
            }
        } else {
            // For Android versions lower than 12 (API level 31), you can handle it differently.
            // For example, you can request BLUETOOTH and BLUETOOTH_ADMIN permissions here.
            if (ActivityCompat.checkSelfPermission(
                    appActivity,
                    Manifest.permission.BLUETOOTH
                ) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                    appActivity,
                    Manifest.permission.BLUETOOTH_ADMIN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    appActivity, // Make sure your context is an AppCompatActivity. by adding as AppCompatActivity
                    arrayOf(
                        Manifest.permission.BLUETOOTH,
                        Manifest.permission.BLUETOOTH_ADMIN
                    ),
                    BLUETOOTH_CONNECT_PERMISSION_CODE
                )
            }
        }
    }

    /**
     * Handle the result of a permission request, specifically for Bluetooth permissions.
     *
     * @param requestCode The request code for the permission request.
     * @param grantResults The results of the permission request, indicating whether the permission was granted.
     */
    fun onRequestPermissionsResult(
        requestCode: Int,
        grantResults: IntArray
    ) {
        // Check if the device is running Android API level 31 (Android 12) or higher.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (requestCode == BLUETOOTH_CONNECT_PERMISSION_CODE) {
                btPerm =
                    grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            }
        } else {
            // For Android versions lower than 12 (API level 31), you can handle it differently.
            if (requestCode == BLUETOOTH_CONNECT_PERMISSION_CODE) {
                btPerm =
                    grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            }
        }
    }

    /**
     * Function to check if the device supports Bluetooth (whether a BluetoothAdapter is available).
     *
     * @return `true` if the device supports Bluetooth, `false` otherwise.
     */
    fun isBluetoothSupported(): Boolean {
        return bluetoothAdapter != null
    }

    /**
     * Function to check if Bluetooth is enabled (turned on) on the device.
     *
     * @return `true` if Bluetooth is enabled, `false` otherwise.
     */
    fun isBluetoothOn(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    /**
     * Function to check if the printer is currently connected to the application.
     *
     * @return `true` if the printer is connected, `false` otherwise.
     */
    fun isPrinterConnected(): Boolean {
        return bluetoothSocket != null && bluetoothSocket!!.isConnected
    }

    /**
     * Function to turn on Bluetooth for the device if it's not already turned on.
     * This function checks and requests Bluetooth permissions if necessary, and then initiates the Bluetooth activation process.
     */
    fun turnOnBluetooth() {
        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
            val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            if(!btPerm){
                checkAndRequestBluetoothPermission()
                Log.d(TAG,"No permission for bluetooth")
            }
            else if(!isBluetoothOn()){
                context.startActivity(enableBluetoothIntent)
                Log.d(TAG,"Turning on bluetooth")
            }
            else{
                Log.d(TAG,"Bluetooth already on")
            }
        }
    }

    /**
     * Function to connect to the printer by creating a BluetoothSocket and initializing communication with the printer.
     * It will also check and request Bluetooth permissions if necessary.
     *
     * @return `true` if the connection to the printer was successful, `false` otherwise.
     */
    fun connectPrinter(): Boolean {
        if(isPrinterConnected()){
            Log.d(TAG,"Printer is already connected")
            return false
        }

        val device: BluetoothDevice = findBluetoothDevice() ?: return false

        val uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")

        return try {
            if(!btPerm){
                checkAndRequestBluetoothPermission()
                false
            } else{
                bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)
                bluetoothSocket?.connect()
                outputStream = bluetoothSocket?.outputStream
                true
            }
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Function to disconnect from the currently connected printer by closing the output stream and Bluetooth socket.
     * If successful, this function severs the connection to the printer.
     */
    fun disconnectPrinter() {
        try {
            outputStream?.close()
            bluetoothSocket?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * Function to find a paired Bluetooth device with a specific name (the name of the printer).
     * If Bluetooth permissions are not granted, it will attempt to request them.
     *
     * @return The BluetoothDevice instance representing the printer if found, or null if not found or if permissions are not granted.
     */
    private fun findBluetoothDevice(): BluetoothDevice? {
        if(!btPerm){
            checkAndRequestBluetoothPermission()
            return null
        }
        else{
            val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices

            pairedDevices?.forEach { device ->
                if (device.name == printerName) {
                    return device
                }
            }
            return null
        }
    }

    /**
     * Function to create a bitmap representation of a message.
     *
     * @param message The message to convert to a bitmap.
     * @param font The font to use for the text.
     * @param fontSize The font size for the text.
     * @return A bitmap containing the message.
     */
    fun messageToBitmap(message: String,
                     font: Int = R.font.courier_new,
                     fontSize: Float = 20F, ): Bitmap {
        return bitmapHelper.textToMultilineBitmap(message, font, fontSize, 384)
    }

    /**
     * Function to print a feed with a specified number of lines.
     *
     * @param lines The number of lines to feed.
     * @param blank If `true`, the feed lines are blank; otherwise, they are filled.
     * @return `true` if the operation was successful, `false` otherwise.
     */
    suspend fun printFeed(lines: Int, blank: Boolean = true): Boolean {
        if (!isPrinterConnected()) {
            Log.d(TAG,"No Printer is connected")
            return false
        }

        try {
            if (lines > 65535) {
                return false
            }

            val heightBytes = ByteBuffer.allocate(2).putShort(lines.toShort()).array()

            // A chunk is one line, 48 bytes * 8 = 384 bits
            val line = ByteArray(48)
            Arrays.fill(line, if (blank) 0.toByte() else 0xFF.toByte())

            withContext(Dispatchers.IO) {
                outputStream?.write(cmdPrintStart)
                outputStream?.write(cmdSetPrintInfo + heightBytes)

                for (i in 0 until lines) {
                    outputStream?.write(line)
                    Thread.sleep(20)
                }

                outputStream?.write(cmdPrintEnd)
                outputStream?.flush()
            }

            return true
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * Function to print an image.
     *
     * @param bitmap The image to print.
     * @return `true` if the image was successfully printed, `false` otherwise.
     */
    suspend fun printImage(bitmap: Bitmap): Boolean {
        if (!isPrinterConnected()) {
            Log.d(TAG,"No Printer is connected")
            return false
        }

        try {
            val width = 384 // PeriPage  image width
            val scale = width.toFloat() / bitmap.width
            val height = (bitmap.height * scale).toInt()

            if (height > 65535) {
                return false
            }

            val grayBitmap = bitmapHelper.convertToGrayscale(bitmap)
            val resizedBitmap = Bitmap.createScaledBitmap(grayBitmap, width, height, true)
            val imageBytes = bitmapHelper.bitmapToByteArray(resizedBitmap)

            val heightBytes = ByteBuffer.allocate(2).putShort(height.toShort()).array()

            // A chunk is one line, 48 bytes * 8 = 384 bits
            val chunkSize = 48

            withContext(Dispatchers.IO) {
                outputStream?.write(cmdPrintStart)
                outputStream?.write(cmdSetPrintInfo + heightBytes)

                for (i in imageBytes.indices step chunkSize) {
                    val max = if ((i + chunkSize) >= imageBytes.size) {
                        imageBytes.size
                    } else {
                        i + chunkSize
                    }
                    val chunk = imageBytes.copyOfRange(i, max)
                    outputStream?.write(chunk)
                    Thread.sleep(20)
                }

                val lines = 30
                val lineHeightBytes = ByteBuffer.allocate(2).putShort(lines.toShort()).array()
                outputStream?.write(cmdSetPrintInfo + lineHeightBytes)
                val line = ByteArray(48)

                Arrays.fill(line, 0.toByte())
                for (i in 0 until lines) {
                    outputStream?.write(line)
                    Thread.sleep(20)
                }

                outputStream?.write(cmdPrintEnd)
                outputStream?.flush()
            }

            return true
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
    }

}