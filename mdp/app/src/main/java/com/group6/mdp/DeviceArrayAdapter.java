package com.group6.mdp;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class DeviceArrayAdapter extends ArrayAdapter<BluetoothDevice> {

    private LayoutInflater mLayoutInflater;
    private ArrayList<BluetoothDevice> myDevices;
    private int viewResourceId;

    public DeviceArrayAdapter(Context context, int resourceId, ArrayList<BluetoothDevice> devices){
        super(context, resourceId, devices);
        this.myDevices = devices;
        mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        viewResourceId = resourceId;
    }

    public View getView(int position, View view, ViewGroup parent) {

        view = mLayoutInflater.inflate(viewResourceId, null);

        BluetoothDevice device = myDevices.get(position);

        if (device != null) {
            TextView deviceName = (TextView) view.findViewById(R.id.deviceName);
            TextView deviceAdress = (TextView) view.findViewById(R.id.deviceAddress);

            if (deviceName != null) {
                deviceName.setText(device.getName());
            }
            if (deviceAdress != null) {
                deviceAdress.setText(device.getAddress());
            }
        }

        return view;
    }
}
