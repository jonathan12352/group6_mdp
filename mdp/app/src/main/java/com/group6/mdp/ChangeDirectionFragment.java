package com.group6.mdp;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import android.app.DialogFragment;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ChangeDirectionFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ChangeDirectionFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ChangeDirectionFragment extends DialogFragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private SharedPreferences.Editor editor;
    private SharedPreferences sharedPreferences;

    private OnFragmentInteractionListener mListener;

    private static final String TAG = "DirectionFragment";
    View rootView;

    Button saveBtn, cancelDirectionBtn;
    Spinner directionSpinner;
    String direction = "";

    public ChangeDirectionFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ChangeDirectionFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ChangeDirectionFragment newInstance(String param1, String param2) {
        ChangeDirectionFragment fragment = new ChangeDirectionFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_change_direction, container, false);

        sharedPreferences = getActivity().getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();

        directionSpinner = (Spinner) rootView.findViewById(R.id.directionValueSpinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.directions_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        directionSpinner.setAdapter(adapter);

        if(sharedPreferences.contains("direction")){
            String direction = sharedPreferences.getString("direction", "");
            int index = adapter.getPosition(direction);
            Log.i(TAG, String.format("Direction Index: %d, Direction: %s", 0, direction));
            directionSpinner.setSelection(index);
        }

        saveBtn = rootView.findViewById(R.id.saveBtn);
        cancelDirectionBtn = rootView.findViewById(R.id.cancelDirectionBtn);

        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                direction = String.valueOf(directionSpinner.getSelectedItem().toString());
                editor.putString("direction",direction);
                ((MainActivity)getActivity()).refreshDirection(direction);
                Utils.showToast(rootView.getContext(), String.format("Saving Value %s", direction));
                editor.commit();
                getDialog().dismiss();
            }
        });

        cancelDirectionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getDialog().dismiss();
            }
        });

        return rootView;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    /*@Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }*/

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    public void Save(View view){

        ((TextView)rootView.findViewById(R.id.direction)).setText(String.format("Direction: %s", direction));
        ((MainActivity)getActivity()).refreshDirection(direction);
        getDialog().dismiss();
    }

    public void Cancel(View view){
        getDialog().dismiss();
    }
}
