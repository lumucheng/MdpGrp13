package com.example.lu.mdpgrp13;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private Bundle savedInstanceState;
    private BluetoothAdapter bluetoothAdapter;
    private ArrayAdapter<String> bluetoothArrayAdapter;
    private ArrayList<BluetoothDevice> bluetoothDeviceList;
    private ConnectThread clientThread;
    public ConnectedThread connectedThread = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.savedInstanceState = savedInstanceState;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bluetoothAdapter  = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_search) {
            openBluetoothSearch();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void openBluetoothSearch() {

        bluetoothArrayAdapter = new ArrayAdapter<>(getApplicationContext(), R.layout.list_layout, R.id.listTextView);
        bluetoothDeviceList = new ArrayList<>();

        if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(getApplicationContext(),
                    "Turning on Bluetooth",
                    Toast.LENGTH_SHORT).show();
            bluetoothAdapter.enable();
        }

        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName() != null) {
                    bluetoothArrayAdapter.add("Paired: " + device.getName());
                }
                else {
                    bluetoothArrayAdapter.add("Paired: " +  device.getAddress());
                }


                bluetoothDeviceList.add(device);
            }
        }

        bluetoothAdapter.startDiscovery();

//        Intent discoverableIntent = new
//                Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
//        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
//        startActivity(discoverableIntent);

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);

        AlertDialog dialog = onCreateDialog(savedInstanceState);
        dialog.show();
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if (device.getName() != null) {
                    bluetoothArrayAdapter.add(device.getName());
                }
                else {
                    bluetoothArrayAdapter.add(device.getAddress());
                }

                bluetoothDeviceList.add(device);
                bluetoothArrayAdapter.notifyDataSetChanged();
            }
        }
    };

    public AlertDialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Device");
        builder.setAdapter(bluetoothArrayAdapter, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {

                dialog.dismiss();

                bluetoothAdapter.cancelDiscovery();

                BluetoothDevice device = bluetoothDeviceList.get(which);
                clientThread = new ConnectThread(device);
                clientThread.run();
            }
        });
        return builder.create();
    }

    Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            String data = (String)msg.obj;
            Toast.makeText(getApplicationContext(), data, Toast.LENGTH_LONG).show();
        }
    };

    // Connect as CLIENT
    public class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
        //private UUID uuid = UUID.fromString("b87774c0-5b65-484b-9d80-23603008d743");

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            mmDevice = device;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                tmp = device.createRfcommSocketToServiceRecord(uuid);
            }
            catch (IOException e) {
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
            manageConnectedSocket();
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

        public void manageConnectedSocket() {
            ConnectedThread connectedThread = new ConnectedThread(mmSocket, mHandler);
            connectedThread.start();
        }
    }

    // Connection Thread
    public class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket, Handler mHandler) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
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

                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);

                    // Send the obtained bytes to the UI activity
                    String data = new String(buffer, 0, bytes);
                    mHandler.obtainMessage(1, data).sendToTarget();
                }
                catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }
}
