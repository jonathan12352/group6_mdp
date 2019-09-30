package com.group6.mdp;


import android.app.DialogFragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

public class ConfigureFragment extends DialogFragment {

    private static final String TAG = "ConfigureFragment";
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    View fragment;

    Button cancelButton;
    Button saveButton;
    EditText f1ValueEditText, f2ValueEditText;
    String f1Value, f2Value;

    public ConfigureFragment() {}


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        fragment = inflater.inflate(R.layout.fragment_configure, container, false);

        f1ValueEditText = fragment.findViewById(R.id.f1ValueEditText);
        f2ValueEditText = fragment.findViewById(R.id.f2ValueEditText);

        sharedPreferences = getActivity().getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);

        if (sharedPreferences.contains("F1")) {
            f1ValueEditText.setText(sharedPreferences.getString("F1", ""));
            f1Value = sharedPreferences.getString("F1", "");

        }
        if (sharedPreferences.contains("F2")) {
            f2ValueEditText.setText(sharedPreferences.getString("F2", ""));
            f2Value = sharedPreferences.getString("F2", "");
        }

        if (savedInstanceState != null) {
            f1Value = savedInstanceState.getStringArray("F1F2 value")[0];
            f2Value = savedInstanceState.getStringArray("F1F2 value")[1];
        }

        saveButton = fragment.findViewById(R.id.saveBtn);
        cancelButton = fragment.findViewById(R.id.cancelBtn);

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editor = sharedPreferences.edit();
                editor.putString("F1", f1ValueEditText.getText().toString());
                editor.putString("F2", f2ValueEditText.getText().toString());
                editor.commit();
                if (!sharedPreferences.getString("F1", "").equals(""))
                    f1Value = f1ValueEditText.getText().toString();
                if (!sharedPreferences.getString("F2", "").equals(""))
                    f2Value = f2ValueEditText.getText().toString();
                Utils.showToast(getActivity(), "Saving values...");
                getDialog().dismiss();
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (sharedPreferences.contains("F1"))
                    f1ValueEditText.setText(sharedPreferences.getString("F1", ""));
                if (sharedPreferences.contains("F2"))
                    f2ValueEditText.setText(sharedPreferences.getString("F2", ""));
                getDialog().dismiss();
            }
        });

        return fragment;
    }
}
