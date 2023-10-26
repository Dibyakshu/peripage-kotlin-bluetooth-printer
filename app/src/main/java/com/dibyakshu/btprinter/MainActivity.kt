package com.dibyakshu.btprinter

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.dibyakshu.peripageprinterlibrary.BluetoothHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val bluetoothFn: BluetoothHelper = BluetoothHelper(this, this.applicationContext)

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        bluetoothFn.onRequestPermissionsResult(requestCode, grantResults)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Layout Views
        val btnReqPerm = findViewById<Button>(R.id.btnReqPerm)
        val btnOnBt = findViewById<Button>(R.id.btnOnBt)
        val btnConnPrinter = findViewById<Button>(R.id.btnConnPrinter)
        val btnPrint = findViewById<Button>(R.id.btnPrint)
        val btnDisconnect = findViewById<Button>(R.id.btnDisconnect)

        // Button Listeners
        btnReqPerm.setOnClickListener {
            bluetoothFn.checkAndRequestBluetoothPermission()
        }

        btnOnBt.setOnClickListener {
            bluetoothFn.turnOnBluetooth()
        }

        btnConnPrinter.setOnClickListener {
            val isConnected = bluetoothFn.connectPrinter()
            if (isConnected) {
                // Printer connected successfully, you can proceed with printing.
            } else {
                // Handle the case where the connection failed.
            }
        }

        btnPrint.setOnClickListener {
            val bitMapText = bluetoothFn.messageToBitmap(message)

            CoroutineScope(Dispatchers.IO).launch {
                val isPrinted = bluetoothFn.printImage(bitMapText)
                //val isPrinted1 = bluetoothFn.printImage(resImg!!, 1.5F, 0.75F)
                if (isPrinted) {
                    // Message printed successfully.
                } else {
                    // Handle the case where printing failed.
                }
            }
        }

        btnDisconnect.setOnClickListener {
            bluetoothFn.disconnectPrinter()
        }
    }
}

const val message = "****************************************\n" +
        "       Sample Fancy Bill Receipt\n" +
        "****************************************\n" +
        "Date: 2023-10-26 15:30:00\n" +
        "Customer: John Doe\n" +
        "Email: johndoe@example.com\n" +
        "\n" +
        "----------------------------------------\n" +
        "Item                  |  Qty  |  Price  \n" +
        "----------------------------------------\n" +
        "Product A            |   2   |  \$10.00 \n" +
        "Product B            |   1   |   \$5.00 \n" +
        "Product C            |   3   |  \$15.00 \n" +
        "----------------------------------------\n" +
        "Subtotal                |       |  \$30.00 \n" +
        "Tax (7%)               |       |   \$2.10 \n" +
        "Total                    |       |  \$32.10 \n" +
        "\n" +
        "----------------------------------------\n" +
        "Payment Method: Credit Card\n" +
        "Transaction ID: 1234567890\n" +
        "\n" +
        "Thank you for your purchase!\n" +
        "Please come again soon!\n" +
        "\n" +
        "****************************************\n"

