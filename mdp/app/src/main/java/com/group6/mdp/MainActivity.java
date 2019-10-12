package com.group6.mdp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.app.FragmentManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.text.method.ScrollingMovementMethod;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

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

    TextView exploreTime;
    TextView fastestPathTime;

    ToggleButton modeToggleButton;
    ToggleButton setRobotStartPointToggleButton;
    ToggleButton setWaypointToggleButton;
    ToggleButton setObstacleToggleButton;
    ToggleButton startExploreButton;
    ToggleButton startFastestPathButton;

    Button explorationResetButton;
    Button fastestPathResetButton;

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

    boolean autoUpdate = true;

    boolean manuallyUpdateMap = false;

    static BluetoothConnectionHandler BTConnectHandler;

    Handler messageRefreshTimerHandler = new Handler();

    public static long fastestTime = 0;
    public static long explorationTime = 0;

    Runnable refreshMessageSentReceived = new Runnable(){
        @Override
        public void run() {
            refreshMessage();
            messageRefreshTimerHandler.postDelayed(refreshMessageSentReceived, 1000);
        }};

    Runnable fastestPathTimer = new Runnable(){
        @Override
        public void run() {
            long millis = System.currentTimeMillis() - fastestTime;
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;

            fastestPathTime.setText(String.format("%02d:%02d", minutes, seconds));
        }
    };

    Runnable explorationTimer = new Runnable(){
        @Override
        public void run() {
            long millis = System.currentTimeMillis() - explorationTime;
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;

            exploreTime.setText(String.format("%02d:%02d", minutes, seconds));

            messageRefreshTimerHandler.postDelayed(this, 500);
        }
    };

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

        InitializeSharedPreferences();

        editor.putString("sentText", "");
        editor.putString("receivedText", "");
        editor.putString("direction","None");
        editor.putString("connStatus", "Disconnected");
        editor.commit();

        exploreTime = findViewById(R.id.exploretime);
        fastestPathTime = findViewById(R.id.fastestpath);

        turnLeftButton = findViewById(R.id.leftbutton);
        turnRightButton = findViewById(R.id.rightbutton);
        moveForwardButton = findViewById(R.id.upbutton);
        moveBackButton = findViewById(R.id.downbutton);

        messageSentView = findViewById(R.id.messagesent);
        messageReceivedView = findViewById(R.id.messagereceived);
        btConnectStatus = findViewById(R.id.bluetoothstatus);

        messageReceivedView.setMovementMethod(new ScrollingMovementMethod());
        messageSentView.setMovementMethod(new ScrollingMovementMethod());

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
                else
                    map.setWaypointStatus(false);
            }
        });

        setObstacleToggleButton = findViewById(R.id.setobstacle);

        setObstacleToggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "Clicked setObstacleToggleButton");
                if (!map.getSetObstacleStatus()) {
                    Utils.showToast(MainActivity.this,"Please Plot Obstacles.");
                    map.setSetObstacleStatus(true);
                }
                else
                    map.setSetObstacleStatus(false);
                Log.i(TAG, "Exiting setObstacleToggleButton()");
            }
        });

        setRobotStartPointToggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "Clicked setRobotStartPointToggleButton");
                if (!setRobotStartPointToggleButton.isChecked()){
                    map.setStartCoordStatus(false);
                    Utils.showToast(MainActivity.this, "Cancelled selecting starting point");
                }
                else if (setRobotStartPointToggleButton.isChecked() && !map.getAutoUpdate()) {
                    Utils.showToast(MainActivity.this, "Please Select Starting Point");
                    map.setStartCoordStatus(true);
                } else{
                    Utils.showToast(MainActivity.this, "Please Select Manual Mode.");
                    setRobotStartPointToggleButton.setChecked(false);
                }
            }
        });

        (findViewById(R.id.resetgridmap)).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                Utils.showToast(MainActivity.this, "Resetting The GridMap...");
                map.resetMap();
            }
        });

        setRobotModeBehavior();

        (findViewById(R.id.getvoicebutton)).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH);
                if(!map.getAutoUpdate())
                    startActivityForResult(intent, 10);
                else
                    Log.e(TAG, "Voice Error: Please Change To Manual Mode");
            }
        });

        (modeToggleButton).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                setRobotModeBehavior();
            }
        });

        startExploreButton = findViewById(R.id.startexplorebutton);
        startFastestPathButton = findViewById(R.id.startfastestpathbutton);

        startExploreButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                if(startExploreButton.isChecked()){
                    sendMessage("XEXPLORE|");
                    explorationTime = System.currentTimeMillis();
                    messageRefreshTimerHandler.postDelayed(explorationTimer, 0);
                }
                else{
                    messageRefreshTimerHandler.removeCallbacks(explorationTimer);
                }
            }
        });

        startFastestPathButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                if(startFastestPathButton.isChecked()){
                    sendMessage("XFASTEST|");
                    fastestTime = System.currentTimeMillis();
                    messageRefreshTimerHandler.postDelayed(fastestPathTimer, 0);
                }
                else{
                    messageRefreshTimerHandler.removeCallbacks(fastestPathTimer);
                }
            }
        });

        explorationResetButton = findViewById(R.id.explorereset);
        fastestPathResetButton = findViewById(R.id.fastestpathreset);

        explorationResetButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                Utils.showToast(MainActivity.this, "Reseting exploration time...");
                exploreTime.setText("00:00:00");
                if(startExploreButton.isChecked())
                    startExploreButton.toggle();
                messageRefreshTimerHandler.removeCallbacks(explorationTimer);
            }
        });

        //WAYPOINT|X|Y

        fastestPathResetButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                Utils.showToast(MainActivity.this,"Reseting fastest time...");
                fastestPathTime.setText("00:00:00");
                if (startFastestPathButton.isChecked())
                    startFastestPathButton.toggle();
                messageRefreshTimerHandler.removeCallbacks(fastestPathTimer);
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
                Utils.showToast(MainActivity.this,"MessageActivity Button Selected");
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

            Log.i(TAG, "messageReceiver() Message Received: " + message);

            try {
                //if (message.length() > 7 && message.substring(2,6).equals("grid")) {
                if(message.contains("EXPLORE")){
                    String obstacleString = "";
                    String[] getInformationString = message.split(Pattern.quote("|"));

                    for(String s : getInformationString){
                        Log.i(TAG, "getInformationString: " + s);
                    }

                    //String obstacleHexString = message.substring(11,message.length()-2);
                    String obstacleHexString = getInformationString[2].replace(" ", "");
                    Log.i(TAG, "obstacleHexString Received: " + obstacleHexString);

                    /*BigInteger hexBigIntegerExplored = new BigInteger(obstacleHexString, 16);
                    String obstacleBinaryString = hexBigIntegerExplored.toString(2);

                    while (obstacleBinaryString.length() < 300)
                        obstacleBinaryString = "0" + obstacleBinaryString;

                    Log.i(TAG, "ObstacleBinaryString: " + obstacleBinaryString);

                    for (int i = 0; i< obstacleBinaryString.length(); i=i+15) {

                        int j=0;
                        String subString = "";

                        while (j<15) {
                            subString = subString + obstacleBinaryString.charAt((j++)+i);
                        }
                        obstacleString = subString + obstacleString;
                    }

                    hexBigIntegerExplored = new BigInteger(obstacleString, 2);
                    obstacleString = hexBigIntegerExplored.toString(16);*/

                    JSONObject amdObject = new JSONObject();
                    //amdObject.put("explored", "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff");
                    /*amdObject.put("length", obstacleHexString.length()*4);
                    amdObject.put("obstacle", obstacleString);*/
                    amdObject.put("explored", getInformationString[1].replace(" ", ""));
                    amdObject.put("length",  obstacleHexString.length()*4);
                    amdObject.put("obstacle", obstacleHexString);

                    amdObject.put("direction", getInformationString[4]);
                    amdObject.put("coordinate", getInformationString[3]);

                    JSONArray amdArray = new JSONArray();
                    amdArray.put(amdObject);
                    JSONObject amdMessage = new JSONObject();
                    amdMessage.put("map", amdArray);
                    message = String.valueOf(amdMessage);
                }
            } catch (JSONException e) {
                Log.d(TAG, "Error Processing Received Message: " + e.getMessage());
            }
            catch(NumberFormatException e){
                Log.e(TAG, "Big Integer Format Exception: " + e.getMessage());
                return;
            }


                try {
                    Log.d(TAG, "receivedMessage updateMapInformation: " + message);

                    map.setReceivedJsonObject(new JSONObject(message));

                    if (map.getAutoUpdate())
                        map.updateMapInformation();

                    Log.i(TAG, "messageReceiver: message decode successful");
                } catch (JSONException e) {
                    Log.i(TAG, "messageReceiver: message decode unsuccessful: " + e.getMessage());
                }

            InitializeSharedPreferences();
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
                    mBTDevice = data.getExtras().getParcelable("mBTDevice");
                    myUUID = (UUID) data.getSerializableExtra("myUUID");
                }
                break;
            case 10:
                if(resultCode == Activity.RESULT_OK) {
                    if(processVoiceCommand(data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)))
                        Log.i(TAG, "Voice command was sent successfully");
                }
                break;
        }
    }

    private boolean processVoiceCommand(ArrayList<String> results){
        for (String str : results) {

            Log.i(TAG, "processVoiceCommand string: " + str);

            if (str.contains("forward")) {
                moveRobot(moveForwardButton);
                return true;
            }
            else if(str.contains("back")){
                moveRobot(moveBackButton);
                return true;
            }
            else if(str.contains("left")){
                moveRobot(turnLeftButton);
                return true;
            }
            else if(str.contains("right")) {
                moveRobot(turnRightButton);
                return true;
            }
        }

        Log.d(TAG, "processVoiceCommand Error: Error recognising voice command");
        return false;
    }

    public static void sendMessage(String message){

        InitializeSharedPreferences();

        if(BluetoothConnectionHandler.bluetoothConnectionStatus){
            BluetoothConnectionHandler.write(message.getBytes());
            editor.putString("sentText", messageSentView.getText() + "\n " + message);
            editor.commit();
        }
    }

    public static void receiveMessage(String message) {
        Log.i(TAG, "Entering receiveMessage");
        InitializeSharedPreferences();
        editor.putString("receivedText", sharedPreferences.getString("receivedText", "") + "\n " + message);
        editor.commit();
        Log.i(TAG, "Exiting receiveMessage");
    }

    public void toggleAutoMode(View view){
        setRobotModeBehavior();
    }

    public static void sendMessage(String name, int x, int y) throws JSONException {
        InitializeSharedPreferences();

        //JSONObject jsonObject = new JSONObject();
        String message;

        switch(name) {
            case "starting":
            case "waypoint":
                //jsonObject.put(name, name);
                //jsonObject.put("x", x);
                //jsonObject.put("y", y);
                message = String.format("%s (%d, %d)", name, x, y);
                break;
            default:
                message = "Unexpected default for sendMessage: " + name;
                break;
        }

        editor.putString("sentText", messageSentView.getText() + "\n " + message);
        editor.commit();

        if (BTConnectHandler.bluetoothConnectionStatus) {
            sendMessage("X" + String.format("%s|%d|%d",name.toUpperCase(),x,y));
        }
    }

    private void setRobotModeBehavior(){
        if(modeToggleButton.isChecked()){
            //Initiate AUTO Behavior
            try {
                map.setAutoUpdate(true);
                autoUpdate = true;
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
                    message = "AW1";
                    break;
                case R.id.downbutton:
                    map.moveRobot("back");
                    message = "AS1";
                    break;
                case R.id.leftbutton:
                    map.moveRobot("left");
                    message = "AA1";
                    break;
                case R.id.rightbutton:
                    map.moveRobot("right");
                    message = "AD1";
                    break;
                default:
                    Log.d(TAG, "Invalid Input");
                    return;
            }

            if(map.getValidPosition() || Arrays.asList("AD1", "AA1").contains(message)){
                Log.i(TAG, "moveRobot sending message: " + message);
                sendMessage(message);
            }

            refreshCoordinateAndDirectionLabel();
        }
        else
            Utils.showToast(MainActivity.this, "Please Toggle to Manual Mode.");
    }

    private void refreshCoordinateAndDirectionLabel(){
        robot_xpos.setText(String.valueOf(map.getCurCoord()[0]));
        robot_ypos.setText(String.valueOf(map.getCurCoord()[1]));
        robotDirection.setText("DIRECTION: " + sharedPreferences.getString("direction", ""));
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

    public static void InitializeSharedPreferences() {
        sharedPreferences = MainActivity.context.getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    BroadcastReceiver connectionStatusBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            BluetoothDevice mDevice = intent.getParcelableExtra("Device");
            String status = intent.getStringExtra("Status");
            InitializeSharedPreferences();

            if(status.equals("connected")){
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
        } catch(IllegalArgumentException e){
            e.printStackTrace();
        }
    }

    public void refreshDirection(String direction) {
        map.setRobotDirection(direction);
        Log.i(TAG, "Direction is set to " + direction);
    }

    public void manualUpdateMap(View view){
        if(!modeToggleButton.isChecked() && map!=null){
            sendMessage("sendArena");

            try{
                map.updateMapInformation();
            }
            catch(JSONException e){
                Log.e(TAG, "manualUpdateMap Error: " + e.getMessage());
            }
        }
        else
            Utils.showToast(MainActivity.this, "This Button Only Works in Manual Update Mode.");
    }
}

