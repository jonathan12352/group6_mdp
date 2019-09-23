package com.group6.mdp;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;

public class BluetoothConnectionHandler {

    private static final String TAG = "BluetoothConnectionHandler";
    private final String NAME = "MDP Group 6";
    public final UUID MY_UUID;

    private final BluetoothAdapter bluetoothAdapter;
    private Context context;

    private BluetoothDevice connectToDevice;

    private static AcceptThread serverSocketThread;
    private static ConnectedThread connectedSocketThread;
    private static ConnectThread connectSocketThread;

    public static boolean bluetoothConnectionStatus = false;

    public BluetoothConnectionHandler(Context context){
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.context = context;
        this.MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
        startServerSocket();
    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            // Use a temporary object that is later assigned to mmServerSocket
            // because mmServerSocket is final.
            BluetoothServerSocket tmp = null;
            try {
                // MY_UUID is the app's UUID string, also used by the client code.
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket's listen() method failed", e);
            }
            mmServerSocket = tmp;
            Log.i(TAG, "End of AcceptThread");
        }

        public void run() {
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned.
            while (true) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Socket's accept() method failed", e);
                    break;
                }

                if (socket != null) {
                    Log.i(TAG, "Calling manageMyConnectedSocket from AcceptThread, Device: " + socket.getRemoteDevice());
                    // A connection was accepted. Perform work associated with
                    // the connection in a separate thread.
                    bluetoothConnectionStatus = true;
                    manageMyConnectedSocket(socket.getRemoteDevice(), socket);
                    break;
                }
            }
        }

        // Closes the connect socket and causes the thread to finish.
        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }

    private class ConnectThread extends Thread {
        private BluetoothSocket mmSocket;

        public ConnectThread(BluetoothDevice connectToThisDevice) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            connectToDevice = connectToThisDevice;

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                tmp = connectToThisDevice.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {

                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            bluetoothAdapter.cancelDiscovery();

            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                Log.i(TAG, "Unable to connect to device: " + connectException.getMessage());

                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                    return;
                }

                try {
                    Log.e("","Trying Fallback...");

                    mmSocket =(BluetoothSocket) connectToDevice.getClass().getMethod("createRfcommSocket", new Class[] {int.class}).invoke(connectToDevice,1);
                    mmSocket.connect();

                    Log.e("","Connected");
                }
                catch (Exception e2) {
                    Log.e("", "Couldn't establish Bluetooth connection!");
                }

                return;
            }

            Log.i(TAG, "Calling manageMyConnectedSocket from ConnectThread, Device: " + connectToDevice);
            bluetoothConnectionStatus = true;
            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            manageMyConnectedSocket(connectToDevice, mmSocket);
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private byte[] mmBuffer; // mmBuffer store for the stream

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "ConnectedThread: Starting.");

            Intent connectionStatus = new Intent("ConnectionStatus");
            connectionStatus.putExtra("Status", "connected");
            connectionStatus.putExtra("Device", connectToDevice);
            LocalBroadcastManager.getInstance(context).sendBroadcast(connectionStatus);
            bluetoothConnectionStatus = true;

            this.mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = mmSocket.getInputStream();
                tmpOut = mmSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while(true){
                try {
                    bytes = mmInStream.read(buffer);
                    String incomingmessage = new String(buffer, 0, bytes);
                    Log.d(TAG, "InputStream: "+ incomingmessage);

                    Intent incomingMessageIntent = new Intent("incomingMessage");
                    incomingMessageIntent.putExtra("receivedMessage", incomingmessage);

                    LocalBroadcastManager.getInstance(context).sendBroadcast(incomingMessageIntent);
                } catch (IOException e) {
                    Log.e(TAG, "Error reading input stream. "+e.getMessage());

                    Intent connectionStatus = new Intent("ConnectionStatus");
                    connectionStatus.putExtra("Status", "disconnected");
                    connectionStatus.putExtra("Device", connectToDevice);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(connectionStatus);
                    bluetoothConnectionStatus = false;

                    break;
                }
            }
        }

        // Call this from the main activity to send data to the remote device.
        public void write(byte[] bytes) {
            Log.d(TAG, "write: Writing to output stream: "+ bytes.toString());
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.e(TAG, "Error writing to output stream. "+e.getMessage());
            }
        }

        // Call this method from the main activity to shut down the connection.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }

    public synchronized void startServerSocket(){
        Log.d(TAG, "Starting Server Socket");

        if(connectSocketThread!=null){
            connectSocketThread.cancel();
            connectSocketThread = null;
        }
        if(serverSocketThread == null){
            serverSocketThread = new AcceptThread();
            serverSocketThread.start();
        }
    }

    public void startClientThread(BluetoothDevice device) {
        Log.d(TAG, "startClientThread Called");

        connectSocketThread = new ConnectThread(device);
        connectSocketThread.start();
    }


    public void manageMyConnectedSocket(BluetoothDevice BTDevice, BluetoothSocket BTSocket){
        connectToDevice = BTDevice;

        if (serverSocketThread != null) {
            serverSocketThread.cancel();
            serverSocketThread = null;
        }

        connectedSocketThread = new ConnectedThread(BTSocket);
        connectedSocketThread.start();
    }

    public void shutdownServerSocket(){
        if (serverSocketThread != null) {
            serverSocketThread.cancel();
            serverSocketThread = null;
        }
        else{
            Log.i(TAG, "There is no Server Socket Thread to Shutdown");
        }
    }

    public static void write(byte[] out){
        Log.d(TAG, "write: Write Method of BluetoothConnectionHandler is called." );
        if(connectedSocketThread!=null){
            connectedSocketThread.write(out);
        }
        else{
            Log.e(TAG, "connectedSocketThread is null");
        }
    }
}
