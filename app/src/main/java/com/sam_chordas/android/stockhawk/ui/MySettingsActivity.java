package com.sam_chordas.android.stockhawk.ui;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.sam_chordas.android.stockhawk.R;

public class MySettingsActivity extends PreferenceActivity {

    protected static final String HISTORIC_DATA_KEY = "pref_number_of_days";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }

}
