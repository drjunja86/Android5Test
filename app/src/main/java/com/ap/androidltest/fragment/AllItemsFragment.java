package com.ap.androidltest.fragment;


import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.ToggleButton;

import com.ap.androidltest.R;

//import android.widget.RatingBar;
//import android.widget.SeekBar;

/**
 * A simple {@link Fragment} subclass.
 */
public class AllItemsFragment extends Fragment {


    public AllItemsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_all_items, container, false);
        RadioButton rb = (RadioButton) view.findViewById(R.id.radioButton);
        Switch aSwitch = (Switch) view.findViewById(R.id.a_switch);
        ToggleButton tb = (ToggleButton) view.findViewById(R.id.toggleButton);
        CheckBox cb = (CheckBox) view.findViewById(R.id.checkBox);
//        SeekBar sb = (SeekBar) view.findViewById(R.id.seekBar);
//        RatingBar rtb = (RatingBar) view.findViewById(R.id.ratingBar);
        EditText et = (EditText) view.findViewById(R.id.editText);
        Spinner spinner = (Spinner) view.findViewById(R.id.spinner);

        tb.setText("Toggle button example");
        rb.setText("Radio button example");
        aSwitch.setText("Switch example");
        cb.setText("Checkbox example");
        et.setHint("Type text here");

        String colors[] = {"Red", "Blue", "White", "Yellow", "Black", "Green", "Purple", "Orange", "Grey"};
        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(
                getActivity(), android.R.layout.simple_spinner_item, colors);
        spinner.setAdapter(spinnerArrayAdapter);
        return view;
    }


}
