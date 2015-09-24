package com.example.lu.mdpgrp13;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by Mucheng on 24/9/15.
 */
// Connect as CLIENT
public class ConnectThread extends Thread {
    private final BluetoothSocket mmSocket;
    private final BluetoothDevice mmDevice;
    private UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private Handler handler;

    public ConnectThread(BluetoothDevice device, Handler handler) {

        BluetoothSocket tmp = null;
        mmDevice = device;
        this.handler = handler;

        try {
            tmp = device.createRfcommSocketToServiceRecord(uuid);
        }
        catch (IOException e) {
            Log.w("Exception", e.getMessage());
        }
        mmSocket = tmp;
    }

    public void run() {

        try {
            mmSocket.connect();
            manageConnectedSocket();
        }
        catch (IOException connectException) {

            handler.obtainMessage(MainActivity.ERROR_OCCURED, "Error occured while connecting: "
                    + connectException.getMessage()).sendToTarget();

            try {
                mmSocket.close();
            }
            catch (IOException closeException) {
                Log.w("Exception", closeException.getMessage());
            }
            return;
        }
    }

    public void cancel() {
        try {
            mmSocket.close();
        }
        catch (IOException e) {
            Log.w("Exception", e.getMessage());
        }
    }

    private void manageConnectedSocket() {
        ConnectionThread connectionThread = new ConnectionThread(mmSocket, handler);
        connectionThread.start();
        handler.obtainMessage(MainActivity.SOCKET_CONNECTED, connectionThread).sendToTarget();
    }
}
