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
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    // Bluetooth Connection Status
    public final static int SOCKET_CONNECTED = 1;
    public final static int DATA_RECEIVED = 2;
    public final static int STATUS_RECEIVED = 3;
    public final static int ROBOT_MOVEMENT = 4;
    public final static int OBSTACLE_DATA = 5;
    public final static int ERROR_OCCURRED = 6;

    private BluetoothAdapter bluetoothAdapter;
    private ArrayAdapter<String> bluetoothArrayAdapter;
    private ArrayList<BluetoothDevice> bluetoothDeviceList;
    private ConnectionThread connectionThread;
    private HostThread hostThread;
    private boolean listenForConnection = false;
    private boolean autoMode = false;
    private boolean updateMaze = false;

    // UI Components
    private TextView txtViewRobotStatus;
    private EditText editTxtTextCmd;
    private EditText editTxtStartPosX;
    private EditText editTxtStartPosY;
    private Button btnUpdate;
    private ImageButton btnUp;
    private ImageButton btnDown;
    private ImageButton btnLeft;
    private ImageButton btnRight;
    private FrameLayout frameMaze;
    private PixelGridView pixelGridView;

    // Continuous Button Touch Handling
    private Handler repeatUpdateHandler = new Handler();
    private boolean forwardIncrement = false;
    private boolean reverseIncrement = false;
    private final static int REP_DELAY = 750;

    private Bundle savedInstanceState;
    private int startPosX = 0;
    private int startPosY = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.savedInstanceState = savedInstanceState;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
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
        else if (id == R.id.action_listen) {
            createSocket(null);
        }
        else if (id == R.id.action_setcommand) {
            openCommandActivity();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void initComponents() {
        txtViewRobotStatus = (TextView) findViewById(R.id.txtViewRobotStatus);
        editTxtStartPosX = (EditText) findViewById(R.id.editTxtStartPosX);
        editTxtStartPosY = (EditText) findViewById(R.id.editTxtStartPosY);
        editTxtTextCmd = (EditText) findViewById(R.id.editTxtTextCmd);
        btnUpdate = (Button) findViewById(R.id.btnUpdate);
        btnUp = (ImageButton) findViewById(R.id.btnUp);
        btnDown = (ImageButton) findViewById(R.id.btnDown);
        btnLeft = (ImageButton) findViewById(R.id.btnLeft);
        btnRight = (ImageButton) findViewById(R.id.btnRight);
        frameMaze = (FrameLayout) findViewById(R.id.frameMaze);

        pixelGridView = new PixelGridView(this);
        frameMaze.addView(pixelGridView);

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
                    Toast.makeText(getApplicationContext(),
                            "Bluetooth connection established.",
                            Toast.LENGTH_SHORT).show();
                    break;
                }
                case DATA_RECEIVED: {
                    // Handle Map Descriptor here
                    if (autoMode == true || updateMaze == true) {
                        String data = (String)msg.obj;
                        String grid = hex_to_binary(data);
                        pixelGridView.drawObstacles(grid);
                        updateMaze = false;
                    }
                    break;
                }
                case STATUS_RECEIVED: {
                    String data = (String)msg.obj;
                    txtViewRobotStatus.setText(data);
                    break;
                }
                case ROBOT_MOVEMENT: {
                    String movement = (String)msg.obj;

                    int charIndex = 0;
                    int count = 0;
                    char command = '\0';

                    if (movement.length() ==  1) {
                        if (movement.charAt(0) == 'f') {
                            pixelGridView.moveRobot("f");
                        }
                        else {
                            pixelGridView.rotateRobot(movement);
                        }
                    }
                    else {
                        while (charIndex < movement.length()) {

                            // Not digit
                            if (!(movement.charAt(charIndex) >= '0' && movement.charAt(charIndex) <= '9')) {
                                command = movement.charAt(charIndex);
                            }
                            else { // Digit

                                int startIndex = charIndex;
                                int endIndex = charIndex;
                                while (endIndex < movement.length()
                                        && movement.charAt(endIndex) >= '0'
                                        && movement.charAt(endIndex) <= '9') {

                                    if (!(movement.charAt(endIndex) >= '0' &&
                                            movement.charAt(endIndex) <= '9')) {
                                        break;
                                    }
                                    endIndex++;
                                }

                                String subString = movement.substring(startIndex, endIndex);
                                int number = Integer.parseInt(subString);

                                Log.w("Command", command + "");
                                Log.w("Command", number +"");

                                for (int i = 0; i < number; i++) {
                                    final Handler handler = new Handler();
                                    count++;
                                    handler.postDelayed(new RobotMovementRunnable(pixelGridView, command), 200 * count);
                                }
                            }
                            charIndex++;
                        }
                    }
                    break;
                }
                case OBSTACLE_DATA: {
                    String obstacle = (String)msg.obj;
                    pixelGridView.addObstacle(obstacle);
                    break;
                }
                case ERROR_OCCURRED: {
                    if (listenForConnection) {
                        Toast.makeText(getApplicationContext(),
                                "Connection lost, will re-listen for connection now.",
                                Toast.LENGTH_SHORT).show();

                        hostThread.cancel();
                        hostThread = new HostThread(handler, bluetoothAdapter);
                        hostThread.run();
                    }
                    else {
                        Toast.makeText(getApplicationContext(),
                                "Connection lost. Please re-establish Bluetooth connection by SEARCH DEVICE.",
                                Toast.LENGTH_SHORT).show();
                    }
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

                listenForConnection = false;
                dialog.dismiss();

                bluetoothAdapter.cancelDiscovery();
                BluetoothDevice device = bluetoothDeviceList.get(which);
                ConnectThread clientThread = new ConnectThread(device, handler);
                clientThread.start();
            }
        });
        return builder.create();
    }

    public void createSocket(View view) {
        Toast.makeText(getApplicationContext(),
                "Accepting Bluetooth connections now..",
                Toast.LENGTH_SHORT).show();

        listenForConnection = true;
        hostThread = new HostThread(handler, bluetoothAdapter);
        hostThread.start();
    }

    public void moveLeft(View view) {
        if (connectionThread != null) {
            sendBluetoothCommand("l");
            pixelGridView.rotateRobot("l");
        }
    }

    public void moveRight(View view) {
        if (connectionThread != null) {
            sendBluetoothCommand("r");
            pixelGridView.rotateRobot("r");
        }
    }

    public void moveUp(View view) {
        if (connectionThread != null) {
            sendBluetoothCommand("f");
            pixelGridView.moveRobot("f");
        }
    }

    public void moveDown(View view) {
        if (connectionThread != null) {
            sendBluetoothCommand("r");
            pixelGridView.moveRobot("r");
        }
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
            autoMode = true;
            btnUpdate.setEnabled(false);
        }
        else {
            autoMode = false;
            btnUpdate.setEnabled(true);
        }
    }

    public void updateGrid(View view) {

        sendBluetoothCommand("GRID");
        updateMaze = true;
    }

    public void setCoordinates(View view) {
        startPosX = Integer.parseInt(editTxtStartPosX.getText().toString());
        startPosY = Integer.parseInt(editTxtStartPosY.getText().toString());

        pixelGridView.setRobotStartPos(startPosX, startPosY, 0.0);
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

    private String hex_to_binary(String hex) {
        String hex_char, bin_char, binary;
        binary = "";
        int len = hex.length() / 2;
        for (int i = 0; i < len; i++) {
            hex_char = hex.substring(2 * i, 2 * i + 2);
            int conv_int = Integer.parseInt(hex_char, 16);
            bin_char = Integer.toBinaryString(conv_int);
            bin_char = zero_pad_bin_char(bin_char);
            if (i == 0) binary = bin_char;
            else binary = binary + bin_char;
        }
        return binary;
    }

    private String zero_pad_bin_char(String bin_char){
        int len = bin_char.length();
        if(len == 8) return bin_char;
        String zero_pad = "0";
        for(int i=1;i<8-len;i++) zero_pad = zero_pad + "0";
        return zero_pad + bin_char;
    }
}
