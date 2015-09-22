package com.example.lu.mdpgrp13;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;

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

//        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
//        if (pairedDevices.size() > 0) {
//            for (BluetoothDevice device : pairedDevices) {
//                if (device.getName() != null) {
//                    bluetoothArrayAdapter.add("Paired: " + device.getName());
//                }
//                else {
//                    bluetoothArrayAdapter.add("Paired: " +  device.getAddress());
//                }
//
//
//                bluetoothDeviceList.add(device);
//            }
//        }

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
}
