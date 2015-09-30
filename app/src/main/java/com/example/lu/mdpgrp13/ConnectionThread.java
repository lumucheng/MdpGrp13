package com.example.lu.mdpgrp13;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by Mucheng on 24/9/15.
 */
// Connection Thread
public class ConnectionThread extends Thread {
    private final BluetoothSocket mmSocket;
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;
    private Handler handler;

    public ConnectionThread(BluetoothSocket socket, Handler handler) {
        mmSocket = socket;
        this.handler = handler;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;

        try {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException e) { }

        mmInStream = tmpIn;
        mmOutStream = tmpOut;
    }

    public void run() {
        byte[] buffer = new byte[1024];  // buffer store for the stream
        int bytes; // bytes returned from read()

        // Keep listening to the InputStream until an exception occurs
        while (true) {
            try {

                bytes = mmInStream.read(buffer);

                String data = new String(buffer, 0, bytes);

                Log.w("DATA:" , data);

                if (data.charAt(0) == '{') {
                    try {
                        JSONObject jsonObject = new JSONObject(data);

                        if (!jsonObject.isNull("status")) {
                            String status = jsonObject.getString("status");
                            handler.obtainMessage(MainActivity.STATUS_RECEIVED, status).sendToTarget();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                else {
                    handler.obtainMessage(MainActivity.DATA_RECEIVED, data).sendToTarget();
                }
            }
            catch (IOException e) {
                handler.obtainMessage(MainActivity.ERROR_OCCURED,
                        "Error occured while receiving data: " + e.getMessage()).sendToTarget();
                break;
            }
        }
    }

    /* Call this from the main activity to send data to the remote device */
    public void write(byte[] bytes) {
        try {
            mmOutStream.write(bytes);
        }
        catch (IOException e) {
            handler.obtainMessage(MainActivity.ERROR_OCCURED,
                    "Error occured while sending data").sendToTarget();
        }
    }

    /* Call this from the main activity to shutdown the connection */
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) { }
    }
}