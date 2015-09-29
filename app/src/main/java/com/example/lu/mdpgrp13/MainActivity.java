package com.example.lu.mdpgrp13;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    public final static int SOCKET_CONNECTED = 1;
    public final static int DATA_RECEIVED = 2;
    public final static int ERROR_OCCURED = 3;

    private BluetoothAdapter bluetoothAdapter;
    private ArrayAdapter<String> bluetoothArrayAdapter;
    private ArrayList<BluetoothDevice> bluetoothDeviceList;
    private ConnectionThread connectionThread;

    // UI Components
    private ImageView imgViewBluetooth;
    private TextView txtViewBluetooth;
    private TextView txtViewRobotStatus;
    private EditText editTxtTextCmd;
    private EditText editTxtStartPosX;
    private EditText editTxtStartPosY;
    private Button btnBluetooth;
    private Button btnUpdate;
    private ImageButton btnUp;
    private ImageButton btnDown;
    private ImageButton btnLeft;
    private ImageButton btnRight;
    private FrameLayout frameMaze;
    private PixelGridView pixelGridView;

    private Bundle savedInstanceState;
    private int startPosX = 0;
    private int startPosY = 0;
    private double robotDirection = 0.0f;

    private Handler repeatUpdateHandler = new Handler();
    private boolean forwardIncrement = false;
    private boolean reverseIncrement = false;
    private int incrementValue = 0;
    private final static int REP_DELAY = 750;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.savedInstanceState = savedInstanceState;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        initComponents();

        bluetoothAdapter  = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter  == null) {
            new AlertDialog.Builder(this)
                    .setTitle("Error")
                    .setMessage("This device does not support Bluetooth")
                    .setPositiveButton("Exit", new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }
        else {

            initComponents();

            IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(btStatusReceiver, filter);

            if (bluetoothAdapter.isEnabled()) {
                displayBluetoothON();
            }
            else {
                displayBluetoothOFF();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_search) {
            openBluetoothSearch();
            return true;
        }
        else if (id == R.id.action_setcommand) {
            openCommandActivity();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void initComponents() {
        imgViewBluetooth = (ImageView)findViewById(R.id.imgViewBluetooth);
        txtViewBluetooth = (TextView) findViewById(R.id.txtViewBluetooth);
        txtViewRobotStatus = (TextView) findViewById(R.id.txtViewRobotStatus);
        editTxtStartPosX = (EditText) findViewById(R.id.editTxtStartPosX);
        editTxtStartPosY = (EditText) findViewById(R.id.editTxtStartPosY);
        editTxtTextCmd = (EditText) findViewById(R.id.editTxtTextCmd);
        btnBluetooth = (Button) findViewById(R.id.btnBluetooth);
        btnUpdate = (Button) findViewById(R.id.btnUpdate);
        btnUp = (ImageButton) findViewById(R.id.btnUp);
        btnDown = (ImageButton) findViewById(R.id.btnDown);
        btnLeft = (ImageButton) findViewById(R.id.btnLeft);
        btnRight = (ImageButton) findViewById(R.id.btnRight);
        frameMaze = (FrameLayout) findViewById(R.id.frameMaze);

        pixelGridView = new PixelGridView(this);
        pixelGridView.setNumColumns(20);
        pixelGridView.setNumRows(15);
        frameMaze.addView(pixelGridView);

        btnBluetooth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bluetoothAdapter.isEnabled()) {
                    bluetoothAdapter.disable();
                } else {
                    bluetoothAdapter.enable();
                }
            }
        });

        btnUp.setOnLongClickListener(
                new View.OnLongClickListener() {
                    public boolean onLongClick(View arg0) {
                        forwardIncrement = true;
                        repeatUpdateHandler.post(new RptUpdater());
                        return false;
                    }
                }
        );
        btnUp.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if ((event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL)
                        && forwardIncrement) {
                    forwardIncrement = false;
                }
                return false;
            }
        });

        btnDown.setOnLongClickListener(
                new View.OnLongClickListener() {
                    public boolean onLongClick(View arg0) {
                        reverseIncrement = true;
                        repeatUpdateHandler.post(new RptUpdater());
                        return false;
                    }
                }
        );
        btnDown.setOnTouchListener( new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if ((event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL)
                        && reverseIncrement) {
                    reverseIncrement = false;
                }
                return false;
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
                    case BluetoothAdapter.STATE_ON:
                        displayBluetoothON();
                        break;
                    case BluetoothAdapter.STATE_DISCONNECTED:
                        Toast.makeText(getApplicationContext(),
                                "Bluetooth connection disconnected.",
                                Toast.LENGTH_SHORT).show();
                        connectionThread = null;
                        break;
                }
            }
        }
    };

    private void openCommandActivity() {
        Intent intent = new Intent(this, CommandActivity.class);
        startActivity(intent);
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

    Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case SOCKET_CONNECTED: {
                    connectionThread = (ConnectionThread)msg.obj;
                    Toast.makeText(getApplicationContext(), "Bluetooth connection established.", Toast.LENGTH_SHORT).show();
                    break;
                }
                case DATA_RECEIVED: {
                    String data = (String)msg.obj;
                    txtViewRobotStatus.setText(data);
                    break;
                }
                case ERROR_OCCURED: {
                    String data = (String)msg.obj;
                    Toast.makeText(getApplicationContext(), data, Toast.LENGTH_SHORT).show();
                    break;
                }
            }
        }
    };

    class RptUpdater implements Runnable {
        public void run() {
            if (forwardIncrement) {
                moveUp(null);
            }
            else if (reverseIncrement){
                moveDown(null);
            }
            repeatUpdateHandler.postDelayed( new RptUpdater(), REP_DELAY);
        }
    }

    public AlertDialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Device");
        builder.setAdapter(bluetoothArrayAdapter, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {

                dialog.dismiss();
                bluetoothAdapter.cancelDiscovery();

                BluetoothDevice device = bluetoothDeviceList.get(which);
                ConnectThread clientThread = new ConnectThread(device, handler);
                clientThread.run();
            }
        });
        return builder.create();
    }

    public void moveLeft(View view) {
        sendBluetoothCommand("tl");
        pixelGridView.rotateRobot("tl");
    }

    public void moveRight(View view) {
        sendBluetoothCommand("tr");
        pixelGridView.rotateRobot("tr");
    }

    public void moveUp(View view) {
        sendBluetoothCommand("f");
        pixelGridView.moveRobot("f");
    }

    public void moveDown(View view) {
        sendBluetoothCommand("r");
        pixelGridView.moveRobot("r");
    }

    public void sendText(View view) {
        String text = editTxtTextCmd.getText().toString();
        sendBluetoothCommand(text);
    }

    public void beginExploration(View view) {
        if ((startPosX < 1) || (startPosY < 1)) {
            Toast.makeText(getApplicationContext(),
                    "Please specify start position of robot.",
                    Toast.LENGTH_SHORT).show();
        }
        else {
            String cmd = "be";

            if (startPosX < 10) {
                cmd += "0" + startPosX;
            }
            else {
                cmd += startPosX;
            }

            if (startPosY < 10) {
                cmd += "0" + startPosY;
            }
            else {
                cmd += startPosY;
            }

            sendBluetoothCommand(cmd);
        }
    }

    public void beginShortestPath(View view) {
        startPosX = 1;
        startPosY = 1;

        // UPDATE ROBOT COORDINATES IN MAZE
        sendBluetoothCommand("beginFastest");
    }

    public void cmdOne(View view) {
        SharedPreferences sharedpreferences = getSharedPreferences(
                CommandActivity.MDP_PREFERENCES, Context.MODE_PRIVATE);

        String cmd1 = sharedpreferences.getString(
                CommandActivity.COMMAND_1, "");

        sendBluetoothCommand(cmd1);
    }

    public void cmdTwo (View view) {
        SharedPreferences sharedpreferences = getSharedPreferences(
                CommandActivity.MDP_PREFERENCES, Context.MODE_PRIVATE);

        String cmd2 = sharedpreferences.getString(
                CommandActivity.COMMAND_2, "");

        sendBluetoothCommand(cmd2);
    }

    public void onToggleClicked(View view) {

        boolean auto = ((ToggleButton)view).isChecked();

        if (auto) {
            btnUpdate.setEnabled(false);
            // SEND ARENA INFO
        }
        else {
            btnUpdate.setEnabled(true);
        }
    }

    public void updateGrid(View view) {
        // SEND "GRID" command to get data
    }

    public void setCoordinates(View view) {
        startPosX = Integer.parseInt(editTxtStartPosX.getText().toString());
        startPosY = Integer.parseInt(editTxtStartPosY.getText().toString());

        pixelGridView.setRobotStartPos(startPosX, startPosY, 10.0);
    }

    private void sendBluetoothCommand(String cmd) {

        if (connectionThread != null) {
            connectionThread.write(cmd.getBytes());
        }
        else {
            Toast.makeText(getApplicationContext(),
                    "Bluetooth connection not established. Please connect to remote device",
                    Toast.LENGTH_SHORT).show();
        }
    }
}
