package com.group6.mdp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.lang.reflect.*;

import java.util.Set;

public class BluetoothHandler extends AppCompatActivity {

    private static final String className = "BluetoothHandler";

    BluetoothAdapter bluetoothAdapter;

    public ArrayList<BluetoothDevice> foundBTDevices;
    public ArrayList<BluetoothDevice> pairedBTDevices;

    ListView foundDevicesListView;
    ListView pairedDevicesListView;

    public DeviceArrayAdapter foundDeviceArrayAdapter;
    public DeviceArrayAdapter pairedDeviceArrayAdapter;

    private BluetoothDevice BTDevice;

    Button connectToDeviceButton;

    BluetoothConnectionHandler BTConnectHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_handler);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        Switch bluetoothSwitch = (Switch) findViewById(R.id.bluetoothSwitch);
        connectToDeviceButton = (Button) findViewById(R.id.connectButton);

        if(bluetoothAdapter.isEnabled()){
            bluetoothSwitch.setChecked(true);
            bluetoothSwitch.setText("ON");
        }

        foundBTDevices = new ArrayList<>();
        pairedBTDevices = new ArrayList<>();

        foundDevicesListView = findViewById(R.id.foundDevicesListView);
        pairedDevicesListView = findViewById(R.id.pairedDevicesListView);

        connectToDeviceButton = (Button) findViewById(R.id.connectButton);

        registerReceiver(BTDeviceBondStatusbroadcastReceiver, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));

        LocalBroadcastManager.getInstance(this).registerReceiver(deviceConnectionBroadcastReceiver, new IntentFilter("ConnectionStatus"));

        foundDevicesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                bluetoothAdapter.cancelDiscovery();
                pairedDevicesListView.setAdapter(pairedDeviceArrayAdapter);

                String deviceName = foundBTDevices.get(i).getName();
                String deviceAddress = foundBTDevices.get(i).getAddress();
                Log.d(className, "A device has been selected.");
                Log.d(className, "Name: " + deviceName);
                Log.d(className, "Address: " + deviceAddress);

                    Log.d(className, "ItemClick: Beginning pairing with " + deviceName);
                    foundBTDevices.get(i).createBond();

                    if(BTConnectHandler==null){
                        BTConnectHandler = new BluetoothConnectionHandler(BluetoothHandler.this);
                    }

                    BTDevice = foundBTDevices.get(i);
            }
        });

        pairedDevicesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                bluetoothAdapter.cancelDiscovery();
                foundDevicesListView.setAdapter(foundDeviceArrayAdapter);

                String deviceName = pairedBTDevices.get(i).getName();
                String deviceAddress = pairedBTDevices.get(i).getAddress();
                Log.d(className, "onItemClick: A device is selected.");
                Log.d(className, "onItemClick: DEVICE NAME: " + deviceName);
                Log.d(className, "onItemClick: DEVICE ADDRESS: " + deviceAddress);

                if(BTConnectHandler==null){
                    BTConnectHandler = new BluetoothConnectionHandler(BluetoothHandler.this);
                }

                BTDevice = pairedBTDevices.get(i);
            }
        });

        bluetoothSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {

                    compoundButton.setText((isChecked) ? "ON" : "OFF");

                if(bluetoothAdapter == null){
                    Log.d(className, "enableDisableBT: Device does not support Bluetooth capabilities!");
                    Utils.showToast(BluetoothHandler.this, "Device Does Not Support Bluetooth");
                    compoundButton.setChecked(false);
                }
                else {
                    if (!bluetoothAdapter.isEnabled()) {
                        Log.d(className, "enableDisableBT: enabling Bluetooth");
                        Log.d(className, "enableDisableBT: Making device discoverable for 600 seconds.");

                        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 600);
                        startActivity(discoverableIntent);

                        compoundButton.setChecked(true);

                        IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
                        registerReceiver(BTStateBroadcastReceiver, BTIntent);

                        IntentFilter discoverIntent = new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
                        registerReceiver(BTScanModeBroadcastReceiver, discoverIntent);
                    }
                    if (bluetoothAdapter.isEnabled()) {
                        Log.d(className, "Disabling Bluetooth");
                        bluetoothAdapter.disable();

                        IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
                        registerReceiver(BTStateBroadcastReceiver, BTIntent);
                    }
                }
            }
        });

        Toolbar toolbar = findViewById(R.id.bluetoothtoolbar);
        setSupportActionBar(toolbar);

        connectToDeviceButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                if(BTDevice ==null)
                {
                    Utils.showToast(BluetoothHandler.this, "Please Select a Device before connecting.");
                }
                else {
                    if(BTConnectHandler==null){
                        BTConnectHandler = new BluetoothConnectionHandler(BluetoothHandler.this);
                    }

                    BTConnectHandler.startClientThread(BTDevice);
                }
            }
        });
    }

    private final BroadcastReceiver BTScanModeBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(bluetoothAdapter.ACTION_SCAN_MODE_CHANGED)) {
                final int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);

                switch (mode) {
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                        Log.d(className, "BTScanModeBroadcastReceiver: Discoverability Enabled.");
                        break;
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                        Log.d(className, "BTScanModeBroadcastReceiver: Able to receive connections.");
                        break;
                    case BluetoothAdapter.SCAN_MODE_NONE:
                        Log.d(className, "BTScanModeBroadcastReceiver: Unable to receive connections.");
                        break;
                    case BluetoothAdapter.STATE_CONNECTING:
                        Log.d(className, "BTScanModeBroadcastReceiver: Connecting");
                        break;
                    case BluetoothAdapter.STATE_CONNECTED:
                        Log.d(className, "BTScanModeBroadcastReceiver: Connected");
                        break;
                }
            }
        }
    };

    private final BroadcastReceiver BTStateBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(bluetoothAdapter.ACTION_STATE_CHANGED)) {

                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(className, "BTStateBroadcastReceiver: STATE OFF");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(className, "BTStateBroadcastReceiver: STATE TURNING OFF");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(className, "BTStateBroadcastReceiver: STATE ON");

                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(className, "BTStateBroadcastReceiver: STATE TURNING ON");
                        break;
                }
            }
        }
    };

    private BroadcastReceiver BTDeviceBondStatusbroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if(action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)){
                BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(mDevice.getBondState() == BluetoothDevice.BOND_BONDED){
                    Log.d(className, "BOND_BONDED.");
                    Utils.showToast(BluetoothHandler.this, "Successfully paired with " + mDevice.getName());
                    BTDevice = mDevice;
                }
                if(mDevice.getBondState() == BluetoothDevice.BOND_BONDING){
                    Log.d(className, "BOND_BONDING.");
                }
                if(mDevice.getBondState() == BluetoothDevice.BOND_NONE){
                    Log.d(className, "BOND_NONE.");
                }
            }
        }
    };

    private BroadcastReceiver foundDevicesBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if(action.equals(BluetoothDevice.ACTION_FOUND)) {
                TextView foundDevicesView = findViewById(R.id.foundDevicesTitleTextView);

                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                foundBTDevices.add(device);

                foundDevicesView.setText("Devices Found: " + foundBTDevices.size());
                foundDeviceArrayAdapter = new DeviceArrayAdapter(context, R.layout.device_arrayadapter_item_view, foundBTDevices);
                foundDevicesListView.setAdapter(foundDeviceArrayAdapter);
            }
        }
    };

    private BroadcastReceiver deviceConnectionBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            BluetoothDevice mDevice = intent.getParcelableExtra("Device");
            String status = intent.getStringExtra("Status");

            Log.i(className, status);

            if(status.equals("connected")){
                Log.d(className, "deviceConnectionBroadcastReceiver: Device now connected to "+mDevice.getName());
                Utils.showToast(BluetoothHandler.this, "Connected to " + mDevice.getName());
            }
            else if(status.equals("disconnected")){
                Log.d(className, "deviceConnectionBroadcastReceiver: Disconnected from " + mDevice.getName());
                Utils.showToast(BluetoothHandler.this, "Disconnected from " + mDevice.getName());
                if(BTConnectHandler==null){
                    BTConnectHandler = new BluetoothConnectionHandler(BluetoothHandler.this);
                }

                BTConnectHandler.startServerSocket();
            }
        }
    };

    public void toggleDeviceScan(View view) {

        foundBTDevices.clear();

        TextView pairedDevicesView = findViewById(R.id.pairedDevicesTitleTextView);

        if(bluetoothAdapter != null) {
            if (!bluetoothAdapter.isEnabled()) {
                Utils.showToast(BluetoothHandler.this, "Please turn on Bluetooth");
            }
            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
                Log.d(className, "toggleDeviceScan: Stopping Discovery Of Bluetooth Devices");

                checkBTPermissions();

                bluetoothAdapter.startDiscovery();
                IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                registerReceiver(foundDevicesBroadcastReceiver, discoverDevicesIntent);
            } else if (!bluetoothAdapter.isDiscovering()) {
                checkBTPermissions();

                bluetoothAdapter.startDiscovery();
                IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                registerReceiver(foundDevicesBroadcastReceiver, discoverDevicesIntent);
            }

            pairedBTDevices.clear();
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

            Log.d(className, "toggleDeviceScan: No. of paired devices found: "+ pairedDevices.size());

            pairedDevicesView.setText("Paired Devices Found: " + pairedDevices.size());

            for(BluetoothDevice d : pairedDevices){
                Log.d(className, "Paired Devices: "+ d.getName() +" : " + d.getAddress());
                pairedBTDevices.add(d);
                pairedDeviceArrayAdapter = new DeviceArrayAdapter(this, R.layout.device_arrayadapter_item_view, pairedBTDevices);
                pairedDevicesListView.setAdapter(pairedDeviceArrayAdapter);
            }
        }
    }

    private void checkBTPermissions() {

        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP)
        {
            int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION") +
                                  this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");

            if (permissionCheck != 0) {
                this.requestPermissions(
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001
                );
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterBroadcastReceiver(foundDevicesBroadcastReceiver);
        unregisterBroadcastReceiver(BTDeviceBondStatusbroadcastReceiver);
        unregisterBroadcastReceiver(BTStateBroadcastReceiver);
        unregisterBroadcastReceiver(BTScanModeBroadcastReceiver);

        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(deviceConnectionBroadcastReceiver);
        } catch(IllegalArgumentException e){
            e.printStackTrace();
        }
    }

    private void unregisterBroadcastReceiver(BroadcastReceiver broadcastReceiver){
        try{
            unregisterReceiver(broadcastReceiver);
        }
        catch(IllegalArgumentException e){
            e.printStackTrace();
        }
    }

    @Override
    public void finish() {
        Intent data = new Intent();
        data.putExtra("BTDevice", (BTDevice!=null) ? BTDevice : null);
        data.putExtra("myUUID", BTConnectHandler != null ? BTConnectHandler.MY_UUID : null);
        setResult(RESULT_OK, data);
        super.finish();
    }


}
