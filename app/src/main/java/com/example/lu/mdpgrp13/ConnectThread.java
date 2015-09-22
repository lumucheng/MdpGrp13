package com.example.lu.mdpgrp13;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by Mucheng on 18/9/15.
 */
public class ConnectThread extends Thread {
    private final BluetoothSocket mmSocket;
    private final BluetoothDevice mmDevice;
    private UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    public ConnectThread(BluetoothDevice device) {
        // Use a temporary object that is later assigned to mmSocket,
        // because mmSocket is final
        BluetoothSocket tmp = null;
        mmDevice = device;

        // Get a BluetoothSocket to connect with the given BluetoothDevice
        try {
            // MY_UUID is the app's UUID string, also used by the server code
            tmp = device.createRfcommSocketToServiceRecord(uuid);
        } catch (IOException e) {
            Log.w("Exception", e.getMessage());

        }
        mmSocket = tmp;
    }

    public void run() {

        try {
            // Connect the device through the socket. This will block
            // until it succeeds or throws an exception
            mmSocket.connect();
        }
        catch (IOException connectException) {
            // Unable to connect; close the socket and get out
            try {
                mmSocket.close();
            }
            catch (IOException closeException) {
                Log.w("Exception", closeException.getMessage());
            }
            return;
        }

        // Do work to manage the connection (in a separate thread)
        ConnectedThread connectedThread = new ConnectedThread(mmSocket);
        connectedThread.run();
    }

    /** Will cancel an in-progress connection, and close the socket */
    public void cancel() {
        try {
            mmSocket.close();
        }
        catch (IOException e) {
            Log.w("Exception", e.getMessage());
        }
    }
}
