package com.group6.mdp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.app.FragmentManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.text.method.ScrollingMovementMethod;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.Charset;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    final String TAG = "MainActivity";

    TextView messageReceivedView;
    TextView btConnectStatus;
    EditText messagesenttextfield;

    TextView robotDirection;
    TextView robotstatus;

    TextView robot_xpos;
    TextView robot_ypos;

    ToggleButton modeToggleButton;
    ToggleButton setRobotStartPointToggleButton;

    Button setRobotDirectionButton;

    BluetoothDevice mBTDevice;
    UUID myUUID;

    GridMap map;

    ProgressDialog displayStatus;

    boolean autoUpdate = true;

    static BluetoothConnectionHandler BTConnectHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.mainmenutoolbar);
        setSupportActionBar(toolbar);

        messagesenttextfield = findViewById(R.id.messagesent);
        messageReceivedView = findViewById(R.id.messagereceived);
        btConnectStatus = findViewById(R.id.bluetoothstatus);

        messageReceivedView.setMovementMethod(new ScrollingMovementMethod());

        robotDirection = findViewById(R.id.direction);
        robotstatus = findViewById(R.id.robotstatus);

        modeToggleButton = findViewById(R.id.modeToggleButton);

        setRobotDirectionButton = findViewById(R.id.setrobotdirection);
        setRobotStartPointToggleButton = findViewById(R.id.setrobotstartpoint);

        final FragmentManager fm = getFragmentManager();
        final ChangeDirectionFragment directionFragment = new ChangeDirectionFragment();

        setRobotDirectionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                directionFragment.show(fm, "Showing Direction Fragment");
            }
        });

        robot_xpos = findViewById(R.id.x_pos);
        robot_ypos = findViewById(R.id.y_pos);

        map = new GridMap(this);
        map = findViewById(R.id.mapView);

        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, new IntentFilter("incomingMessage"));

        setRobotStartPointToggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "Clicked setRobotStartPointToggleButton");
                if (!setRobotStartPointToggleButton.isChecked())
                    Utils.showToast(MainActivity.this, "Cancelled selecting starting point");
                else if (setRobotStartPointToggleButton.isChecked() && !map.getAutoUpdate()) {
                    Utils.showToast(MainActivity.this, "Please select starting point");
                    map.setStartCoordStatus(true);
                    map.toggleCheckedBtn("setStartPointToggleBtn");
                } else
                    Utils.showToast(MainActivity.this, "Please select manual mode");
            }
        });

        setRobotModeBehavior();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mainmenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.bluetoothMenuItem:
                Utils.showToast(getApplicationContext(),"Showing Bluetooth Configuration Activity");
                Intent intent = new Intent(MainActivity.this, BluetoothHandler.class);
                startActivityForResult(intent, 1);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("receivedMessage");
            String textToOutput = String.format(messageReceivedView.getText().toString() + "\n%s", message);
            messageReceivedView.setText(textToOutput);
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode){
            case 1:
                if(resultCode == Activity.RESULT_OK){
                    mBTDevice = (BluetoothDevice) data.getExtras().getParcelable("mBTDevice");
                    myUUID = (UUID) data.getSerializableExtra("myUUID");
                }
        }
    }

    public void sendMessageFromView(View view){
        sendMessage(messagesenttextfield.getText().toString());
    }

    public static void sendMessage(String message){
        BluetoothConnectionHandler.write(message.getBytes());
    }

    public void toggleAutoMode(View view){
        setRobotModeBehavior();
    }

    public static void sendInitSettings(String name, int x, int y) throws JSONException {
        //sharedPreferences();

        JSONObject jsonObject = new JSONObject();
        String message;

        switch(name) {
            case "starting":
            case "waypoint":
                jsonObject.put(name, name);
                jsonObject.put("x", x);
                jsonObject.put("y", y);
                message = String.format("{0} ({1},{2})", name, x,y);
                break;
            default:
                message = "Unexpected default for printMessage: " + name;
                break;
        }
        //editor.putString("sentText", messageSentTextView.getText() + "\n " + message);
        //editor.commit();
        sendMessage("X" + String.valueOf(jsonObject));
        if (BTConnectHandler.bluetoothConnectionStatus == true) {
            byte[] bytes = message.getBytes(Charset.defaultCharset());
            BTConnectHandler.write(bytes);
        }
    }

    private void SetRobotStatus(String status){
        robotstatus.setText(status);
    }

    private void setRobotModeBehavior(){
        if(modeToggleButton.isChecked()){
            //Initiate AUTO Behavior
            /*try {
                map.setAutoUpdate(false);
                autoUpdate = false;
                //map.toggleCheckedBtn("None");
            } catch (JSONException e) {
                e.printStackTrace();
            }*/
            Utils.showToast(MainActivity.this, "AUTO MODE");
        }
        else{
            //Initiate Manual Behavior
            /*try {
                map.setAutoUpdate(true);
                autoUpdate = true;
                //map.toggleCheckedBtn("None");
            } catch (JSONException e) {
                e.printStackTrace();
            }*/
            Utils.showToast(MainActivity.this, "MANUAL MODE");

        }
    }

    public void moveRobot(View view){

        switch(view.getId()){
            case R.id.upbutton:
                sendMessage("f");
                break;
            case R.id.downbutton:
                sendMessage("r");
                break;
            case R.id.leftbutton:
                sendMessage("tl");
                break;
            case R.id.rightbutton:
                sendMessage("tr");
                break;
        }
    }

    private void SetRobotDirection(String direction){

        robotDirection.setText(direction);
    }

    private void SetRobotCoord(int x, int y){

        if(x!=-1){
            robot_xpos.setText(x);
        }

        if(y!=-1){
            robot_ypos.setText(y);
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
        try{
            IntentFilter filter2 = new IntentFilter("ConnectionStatus");
            LocalBroadcastManager.getInstance(this).registerReceiver(connectionStatusBroadcastReceiver, filter2);
        } catch(IllegalArgumentException e){
            e.printStackTrace();
        }
    }

    BroadcastReceiver connectionStatusBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            BluetoothDevice mDevice = intent.getParcelableExtra("Device");
            String status = intent.getStringExtra("Status");
            //sharedPreferences();

            if(status.equals("connected")){
                try {
                    displayStatus.dismiss();
                } catch(NullPointerException e){
                    e.printStackTrace();
                }

                Log.d(TAG, "connectionStatusBroadcastReceiver: Device now connected to " + mDevice.getName());
                Utils.showToast(MainActivity.this, "Device now connected to " + mDevice.getName());
                //editor.putString("connStatus", "Connected to " + mDevice.getName());
                btConnectStatus.setText("Connected to " + mDevice.getName());
            }
            else if(status.equals("disconnected")){
                Log.d(TAG, "connectionStatusBroadcastReceiver: Disconnected from "+mDevice.getName());
                Utils.showToast(MainActivity.this, "Disconnected from " + mDevice.getName());
                BTConnectHandler = new BluetoothConnectionHandler(MainActivity.this);
                BTConnectHandler.startServerSocket();

                //editor.putString("connStatus", "Disconnected");
                btConnectStatus = findViewById(R.id.bluetoothstatus);
                btConnectStatus.setText("Disconnected");

                //displayStatus.show();
            }
            //editor.commit();
        }
    };

    @Override
    protected void onDestroy(){
        super.onDestroy();
        try{
            LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
            LocalBroadcastManager.getInstance(this).unregisterReceiver(connectionStatusBroadcastReceiver);
            //mSensorManager.unregisterListener(this);
        } catch(IllegalArgumentException e){
            e.printStackTrace();
        }
    }

    public void refreshDirection(String direction) {
        map.setRobotDirection(direction);
        Log.i(TAG, "Direction is set to " + direction);
    }

}

