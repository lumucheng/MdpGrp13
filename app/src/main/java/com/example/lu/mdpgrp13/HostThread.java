package com.example.lu.mdpgrp13;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by Mucheng on 30/9/15.
 */
public class HostThread extends Thread {

    private BluetoothServerSocket mmServerSocket = null;
    private UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private final String NAME = "MDPGRP13";
    private Handler handler;

    public HostThread(Handler handler, BluetoothAdapter mBluetoothAdapter) {

        this.handler = handler;

        try {
            mmServerSocket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, uuid);
        }
        catch (IOException e) {
            handler.obtainMessage(MainActivity.ERROR_OCCURRED, "Error occured while connecting: "
                    + e.getMessage()).sendToTarget();
        }
    }

    public void run() {

        BluetoothSocket socket = null;

        while (true) {
            try {
                socket = mmServerSocket.accept();
                manageConnectedSocket(socket);
                mmServerSocket.close();
                break;
            }
            catch (IOException e) {
                handler.obtainMessage(MainActivity.ERROR_OCCURRED, "Error occured while connecting: "
                        + e.getMessage()).sendToTarget();
                break;
            }
        }
    }

    /** Will cancel the listening socket, and cause the thread to finish */
    public void cancel() {
        try {
            mmServerSocket.close();
        }
        catch (IOException e) {
            handler.obtainMessage(MainActivity.ERROR_OCCURRED, "Cancel thread error: "
                    + e.getMessage()).sendToTarget();
        }
    }

    private void manageConnectedSocket(BluetoothSocket mmSocket) {

        ConnectionThread connectionThread = new ConnectionThread(mmSocket, handler);
        connectionThread.start();
        handler.obtainMessage(MainActivity.SOCKET_CONNECTED, connectionThread).sendToTarget();
    }
}
