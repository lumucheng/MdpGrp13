package com.example.lu.mdpgrp13;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.Set;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {

    private final static int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter bluetoothAdapter ;
    private ArrayAdapter<String> bluetoothArrayAdapter;

    private ImageView imgViewBluetooth;
    private TextView txtViewBluetooth;
    private Button btnBluetooth;

    public MainActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        initComponents(rootView);

        bluetoothAdapter  = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter  == null) {
            new AlertDialog.Builder(getActivity())
                    .setTitle("Error")
                    .setMessage("This device does not support Bluetooth")
                    .setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            System.exit(0);
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }
        else {
            IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            getActivity().registerReceiver(btStatusReceiver, filter);

            if (bluetoothAdapter.isEnabled()) {
                displayBluetoothON();
            }
            else {
                displayBluetoothOFF();
            }
        }
        return rootView;
    }

    private void initComponents(View rootView) {
        imgViewBluetooth = (ImageView)rootView.findViewById(R.id.imgViewBluetooth);
        txtViewBluetooth = (TextView)rootView.findViewById(R.id.txtViewBluetooth);
        btnBluetooth = (Button)rootView.findViewById(R.id.btnBluetooth);

        btnBluetooth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bluetoothAdapter.isEnabled()) {
                    bluetoothAdapter.disable();
                }
                else {
                    bluetoothAdapter.enable();
                }
            }
        });
    }

    private void displayBluetoothON() {
        imgViewBluetooth.setImageResource(R.drawable.bluetooth);
        txtViewBluetooth.setText("Bluetooth Status: ON");
        btnBluetooth.setText("TURN OFF BLUETOOTH");
    }

    private void displayBluetoothOFF() {
        imgViewBluetooth.setImageResource(R.drawable.bluetooth_bw);
        txtViewBluetooth.setText("Bluetooth Status: OFF");
        btnBluetooth.setText("TURN ON BLUETOOTH");
    }

    private final BroadcastReceiver btStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        displayBluetoothOFF();
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        displayBluetoothOFF();
                        break;
                    case BluetoothAdapter.STATE_ON:
                        displayBluetoothON();
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        displayBluetoothON();
                        break;
                }
            }
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Unregister broadcast listeners
        getActivity().unregisterReceiver(btStatusReceiver);
    }
}
