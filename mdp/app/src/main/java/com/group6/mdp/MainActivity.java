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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.text.method.ScrollingMovementMethod;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    final static String TAG = "MainActivity";

    Intent intent;

    private static SharedPreferences sharedPreferences;
    private static SharedPreferences.Editor editor;
    private static Context context;

    TextView messageReceivedView;
    TextView btConnectStatus;
    static TextView messageSentView;

    TextView robotDirection;
    TextView robotstatus;

    TextView robot_xpos;
    TextView robot_ypos;

    ToggleButton modeToggleButton;
    ToggleButton setRobotStartPointToggleButton;
    ToggleButton setWaypointToggleButton;
    ToggleButton setObstacleToggleButton;

    Button f1Button;
    Button f2Button;

    ImageButton turnLeftButton;
    ImageButton turnRightButton;
    ImageButton moveForwardButton;
    ImageButton moveBackButton;

    Button setRobotDirectionButton;
    Button setConfigButton;

    BluetoothDevice mBTDevice;
    UUID myUUID;

    GridMap map;

    ProgressDialog displayStatus;

    boolean autoUpdate = true;

    static BluetoothConnectionHandler BTConnectHandler;

    Handler messageRefreshTimerHandler = new Handler();

    Runnable refreshMessageSentReceived = new Runnable(){
        @Override
        public void run() {
            refreshMessage();
            messageRefreshTimerHandler.postDelayed(refreshMessageSentReceived, 1000);
        }};

    public void refreshMessage() {
        messageReceivedView.setText(sharedPreferences.getString("receivedText", ""));
        messageSentView.setText(sharedPreferences.getString("sentText", ""));
        robotDirection.setText("DIRECTION: " + sharedPreferences.getString("direction",""));
        btConnectStatus.setText(sharedPreferences.getString("connStatus", ""));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.mainmenutoolbar);
        setSupportActionBar(toolbar);

        context = MainActivity.this;

        sharedPreferences();

        editor.putString("sentText", "");
        editor.putString("receivedText", "");
        editor.putString("direction","None");
        editor.putString("connStatus", "Disconnected");
        editor.commit();

        turnLeftButton = findViewById(R.id.leftbutton);
        turnRightButton = findViewById(R.id.rightbutton);
        moveForwardButton = findViewById(R.id.upbutton);
        moveBackButton = findViewById(R.id.downbutton);

        messageSentView = findViewById(R.id.messagesent);
        messageReceivedView = findViewById(R.id.messagereceived);
        btConnectStatus = findViewById(R.id.bluetoothstatus);

        messageReceivedView.setMovementMethod(new ScrollingMovementMethod());

        robotDirection = findViewById(R.id.direction);
        robotstatus = findViewById(R.id.robotstatus);

        modeToggleButton = findViewById(R.id.modeToggleButton);

        setRobotDirectionButton = findViewById(R.id.setrobotdirection);
        setRobotStartPointToggleButton = findViewById(R.id.setrobotstartpoint);
        setConfigButton = findViewById(R.id.configbutton);

        f1Button = findViewById(R.id.setting1button);
        f2Button = findViewById(R.id.setting2button);

        f1Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (sharedPreferences.contains("F1") && !sharedPreferences.getString("F1", "").equals(""))
                    sendMessage(sharedPreferences.getString("F1", ""));
            }
        });

        f2Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (sharedPreferences.contains("F2")  && !sharedPreferences.getString("F2", "").equals(""))
                    sendMessage(sharedPreferences.getString("F2", ""));
            }
        });

        final FragmentManager fm = getFragmentManager();
        final ChangeDirectionFragment directionFragment = new ChangeDirectionFragment();
        final ConfigureFragment configurationsFragment = new ConfigureFragment();

        setRobotDirectionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                directionFragment.show(fm, "Showing Direction Fragment");
            }
        });

        setConfigButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                configurationsFragment.show(fm, "Showing Configurations Fragment");
            }
        });

        robot_xpos = findViewById(R.id.x_pos);
        robot_ypos = findViewById(R.id.y_pos);

        map = new GridMap(this);
        map = findViewById(R.id.mapView);

        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, new IntentFilter("incomingMessage"));

        setWaypointToggleButton = findViewById(R.id.setwaypoint);

        setWaypointToggleButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                if(setWaypointToggleButton.isChecked()){
                    map.setWaypointStatus(true);
                    Log.i(TAG, "Setting waypoints is allowed now");
                }
            }
        });

        setObstacleToggleButton = findViewById(R.id.setobstacle);

        setObstacleToggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "Clicked setObstacleToggleButton");
                if (!map.getSetObstacleStatus()) {
                    Utils.showToast(MainActivity.this,"Please plot obstacles");
                    map.setSetObstacleStatus(true);
                    map.toggleCheckedBtn("obstacleImageBtn");
                }
                else if (map.getSetObstacleStatus())
                    map.setSetObstacleStatus(false);
                Log.i(TAG, "Exiting setObstacleToggleButton");
            }
        });

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
                } else{
                    Utils.showToast(MainActivity.this, "Please select manual mode");
                    setRobotStartPointToggleButton.setChecked(false);
                }
            }
        });

        ((Button)findViewById(R.id.resetgridmap)).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                Utils.showToast(MainActivity.this, "Reseting map...");
                map.resetMap();
            }
        });

        setRobotModeBehavior();

        ((Button)findViewById(R.id.getvoicebutton)).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH);
                startActivityForResult(intent, 10);
            }
        });

        messageRefreshTimerHandler.post(refreshMessageSentReceived);
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
                intent = new Intent(MainActivity.this, BluetoothHandler.class);
                startActivityForResult(intent, 1);
                break;
            case R.id.messageMenuItem:
                Utils.showToast(MainActivity.this,"Message Box selected");
                intent = new Intent(MainActivity.this, MessageBoxActivity.class);
                editor.putString("receivedText", messageReceivedView.getText().toString());
                editor.putString("sentText",  messageSentView.getText().toString());
                startActivity(intent);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("receivedMessage");

            Log.i(TAG, "messageReceiver message received: " + message);

            try {
                if (message.length() > 7 && message.substring(2,6).equals("grid")) {
                    String resultString = "";
                    String amdString = message.substring(11,message.length()-2);
                    Log.i(TAG, "amdString: " + amdString);
                    BigInteger hexBigIntegerExplored = new BigInteger(amdString, 16);
                    String exploredString = hexBigIntegerExplored.toString(2);

                    Log.i(TAG, "exploredString: " + exploredString);

                    while (exploredString.length() < 300)
                        exploredString = "0" + exploredString;

                    for (int i=0; i<exploredString.length(); i=i+15) {
                        int j=0;
                        String subString = "";
                        while (j<15) {
                            Log.i(TAG, "substring index: " + (j+i));
                            subString = subString + exploredString.charAt(j+i);
                            j++;
                        }
                        resultString = subString + resultString;
                    }
                    hexBigIntegerExplored = new BigInteger(resultString, 2);
                    resultString = hexBigIntegerExplored.toString(16);

                    JSONObject amdObject = new JSONObject();
                    amdObject.put("explored", "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff");
                    amdObject.put("length", amdString.length()*4);
                    amdObject.put("obstacle", resultString);
                    JSONArray amdArray = new JSONArray();
                    amdArray.put(amdObject);
                    JSONObject amdMessage = new JSONObject();
                    amdMessage.put("map", amdArray);
                    message = String.valueOf(amdMessage);
                    Log.i(TAG, "Executed for AMD message, message: " + message);
                }
            } catch (JSONException e) {
                Log.d(TAG, "Error processing received message: " + e.getMessage());
                //e.printStackTrace();
            }
            catch(NumberFormatException e){
                e.printStackTrace();
                Log.e(TAG, "Big Integer Format Exception: " + e.getMessage());
                return;
            }

            if (map.getAutoUpdate()) {
                try {
                    Log.d(TAG, "receivedMessage updateMapInformation: " + message);
                    map.setReceivedJsonObject(new JSONObject(message));
                    map.updateMapInformation();
                    Log.i(TAG, "messageReceiver: message decode successful");
                } catch (JSONException e) {
                    Log.i(TAG, "messageReceiver: message decode unsuccessful: " + e.getMessage());
                }
            }
            sharedPreferences();
            String receivedText = String.format("%s\n%s", sharedPreferences.getString("receivedText", ""), message);
            editor.putString("receivedText", receivedText);
            editor.commit();
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode){
            case 1:
                if(resultCode == Activity.RESULT_OK) {
                    mBTDevice = (BluetoothDevice) data.getExtras().getParcelable("mBTDevice");
                    myUUID = (UUID) data.getSerializableExtra("myUUID");
                }
                break;
            case 10:
                if(resultCode == Activity.RESULT_OK) {
                    if(processVoiceCommand(data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS))){
                        Log.i(TAG, "Voice command was sent successfully");
                    }
                }
                break;
        }
    }

    private boolean processVoiceCommand(ArrayList<String> results){
        for (String str : results) {

            Log.i(TAG, "processVoiceCommand string: " + str);

            if (str.equals("move forward")) {
                moveRobot(moveForwardButton);
                return true;
            }
            else if(str.equals("move back")){
                moveRobot(moveBackButton);
                return true;
            }
            else if(str.equals("turn left")){
                moveRobot(turnLeftButton);
                return true;
            }
            else if(str.equals("turn right")) {
                moveRobot(turnRightButton);
                return true;
            }
        }

        Log.d(TAG, "processVoiceCommand Error: Error recognising voice command");
        return false;
    }

    public static void sendMessage(String message){

        sharedPreferences();

        BluetoothConnectionHandler.write(message.getBytes());

        editor.putString("sentText", messageSentView.getText() + "\n " + message);
        editor.commit();
    }

    public static void receiveMessage(String message) {
        Log.i(TAG, "Entering receiveMessage");
        sharedPreferences();
        editor.putString("receivedText", sharedPreferences.getString("receivedText", "") + "\n " + message);
        editor.commit();
        Log.i(TAG, "Exiting receiveMessage");
    }

    public void toggleAutoMode(View view){
        setRobotModeBehavior();
    }

    public static void sendMessage(String name, int x, int y) throws JSONException {
        sharedPreferences();

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
                message = "Unexpected default for sendMessage: " + name;
                break;
        }
        editor.putString("sentText", messageSentView.getText() + "\n " + message);
        editor.commit();
        sendMessage("X" + String.valueOf(jsonObject));
        if (BTConnectHandler.bluetoothConnectionStatus == true) {
            byte[] bytes = message.getBytes(Charset.defaultCharset());
            BTConnectHandler.write(bytes);
        }
    }

    private void setRobotModeBehavior(){
        if(modeToggleButton.isChecked()){
            //Initiate AUTO Behavior
            try {
                map.setAutoUpdate(true);
                autoUpdate = true;
                //map.toggleCheckedBtn("None");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Utils.showToast(MainActivity.this, "AUTO MODE");
        }
        else{
            //Initiate Manual Behavior
            try {
                map.setAutoUpdate(false);
                autoUpdate = false;
                //map.toggleCheckedBtn("None");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            Utils.showToast(MainActivity.this, "MANUAL MODE");

        }
    }

    public void moveRobot(View view){

        if(!map.getAutoUpdate()){

            if(!map.getCanDrawRobot()){
                Log.d(TAG, "Cannot Draw Robot");
                return;
            }

            String message = "";

            switch(view.getId()){
                case R.id.upbutton:
                    map.moveRobot("forward");
                    message = "f";
                    break;
                case R.id.downbutton:
                    map.moveRobot("back");
                    message = "r";
                    break;
                case R.id.leftbutton:
                    map.moveRobot("left");
                    message = "tl";
                    break;
                case R.id.rightbutton:
                    map.moveRobot("right");
                    message = "tr";
                    break;
                default:
                    Log.d(TAG, "Invalid Input");
                    return;
            }

            if(map.getValidPosition() || Arrays.asList("tr", "tl").contains(message)){
                Log.i(TAG, "moveRobot sending message: " + message);
                sendMessage(message);
            }

            refreshCoordinateAndDirectionLabel();
        }
        else{
            Utils.showToast(MainActivity.this, "Please Toggle to Manual Mode.");
        }
    }

    private void refreshCoordinateAndDirectionLabel(){
        robot_xpos.setText(String.valueOf(map.getCurCoord()[0]));
        robot_ypos.setText(String.valueOf(map.getCurCoord()[1]));
        robotDirection.setText("DIRECTION: " + sharedPreferences.getString("direction", ""));
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

    public static void sharedPreferences() {
        sharedPreferences = MainActivity.context.getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    BroadcastReceiver connectionStatusBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            BluetoothDevice mDevice = intent.getParcelableExtra("Device");
            String status = intent.getStringExtra("Status");
            sharedPreferences();

            if(status.equals("connected")){
                try {
                    displayStatus.dismiss();
                } catch(NullPointerException e){
                    e.printStackTrace();
                }

                Log.d(TAG, "connectionStatusBroadcastReceiver: Device now connected to " + mDevice.getName());
                Utils.showToast(MainActivity.this, "Device now connected to " + mDevice.getName());
                editor.putString("connStatus", "Connected to " + mDevice.getName());
                btConnectStatus.setText("Connected to " + mDevice.getName());
            }
            else if(status.equals("disconnected")){
                Log.d(TAG, "connectionStatusBroadcastReceiver: Disconnected from "+mDevice.getName());
                Utils.showToast(MainActivity.this, "Disconnected from " + mDevice.getName());
                BTConnectHandler = new BluetoothConnectionHandler(MainActivity.this);
                BTConnectHandler.startServerSocket();

                editor.putString("connStatus", "Disconnected");
                btConnectStatus = findViewById(R.id.bluetoothstatus);
                btConnectStatus.setText("Disconnected");

                //displayStatus.show();
            }
            editor.commit();
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

    public void manualUpdateMap(View view){
        if(!map.getAutoUpdate() && map!=null){
            map.invalidate();
        }
        else{
            Utils.showToast(MainActivity.this, "This button only works in manual update mode");
        }
    }
}

