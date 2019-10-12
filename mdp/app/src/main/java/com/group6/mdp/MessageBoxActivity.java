package com.group6.mdp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import java.nio.charset.Charset;

public class MessageBoxActivity extends AppCompatActivity {

    public static final String TAG = "MessageBoxActivity";
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    private String receivedText;
    private String sentText;
    private String connStatus;

    TextView messageReceivedTextView;
    TextView messageSentTextView;

    EditText typeTextEditView;

    Button sendTextBtn;
    Button clearTextBtn;

    BluetoothConnectionHandler BTHandler;

    Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        connStatus = "Disconnected";
        setContentView(R.layout.activity_message_box);
        intent = getIntent();

        sharedPreferences = getApplicationContext().getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);

        if (sharedPreferences.contains("receivedText"))
            receivedText = sharedPreferences.getString("receivedText", "");
        if (sharedPreferences.contains("sentText"))
            sentText = sharedPreferences.getString("sentText", "");

        messageReceivedTextView = findViewById(R.id.messageReceivedTextView);
        messageSentTextView = findViewById(R.id.messageSentTextView);
        typeTextEditView = findViewById(R.id.typeBoxEditText);
        sendTextBtn = findViewById(R.id.sendTextBtn);
        clearTextBtn = findViewById(R.id.clearTextBtn);

        messageReceivedTextView.setMovementMethod(new ScrollingMovementMethod());
        messageSentTextView.setMovementMethod(new ScrollingMovementMethod());

        messageReceivedTextView.setText(receivedText);
        messageSentTextView.setText(sentText);

        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, new IntentFilter("incomingMessage"));
        IntentFilter filter2 = new IntentFilter("ConnectionStatus");
        LocalBroadcastManager.getInstance(this).registerReceiver(connectionStatusBroadcastReceiver, filter2);

        sendTextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sentText = " " + typeTextEditView.getText().toString();

                sharedPreferences = getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("sentText", String.format("%s\n%s", sharedPreferences.getString("sentText", ""), sentText));
                editor.commit();

                messageSentTextView.setText(sharedPreferences.getString("sentText", ""));
                typeTextEditView.setText(" ");

                if (BluetoothConnectionHandler.bluetoothConnectionStatus == true) {
                    byte[] bytes = sentText.getBytes(Charset.defaultCharset());
                    BluetoothConnectionHandler.write(bytes);
                }
            }
        });

        clearTextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                messageSentTextView.setText("");
            }
        });

        if (sharedPreferences.contains("connStatus"))
            connStatus = sharedPreferences.getString("connStatus", "");
    }

    BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            sharedPreferences = getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);
            messageReceivedTextView.setText(sharedPreferences.getString("receivedText", ""));
        }
    };

    private BroadcastReceiver connectionStatusBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            BluetoothDevice mDevice = intent.getParcelableExtra("Device");
            String status = intent.getStringExtra("Status");
            sharedPreferences = getApplicationContext().getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);
            editor = sharedPreferences.edit();

            if(status.equals("connected")){

                Log.d(TAG, "mBroadcastReceiver5: Device now connected to "+mDevice.getName());
                Utils.showToast(MessageBoxActivity.this, "Device now connected to "+mDevice.getName());
                editor.putString("connStatus", "Connected to " + mDevice.getName());
            }
            else if(status.equals("disconnected")){
                Log.d(TAG, "mBroadcastReceiver5: Disconnected from "+mDevice.getName());
                Utils.showToast(MessageBoxActivity.this, "Disconnected from "+mDevice.getName());
                BTHandler = new BluetoothConnectionHandler(MessageBoxActivity.this);
                BTHandler.startServerSocket();

                editor.putString("connStatus", "Disconnected");

                closeKeyboard(MessageBoxActivity.this);
                //myDialog.show();
            }
            editor.commit();
        }
    };

    public static void closeKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        View view = activity.getCurrentFocus();
        if (view == null)
            view = new View(activity);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

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
}
