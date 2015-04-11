package no.uit.ods.beaconme;

import android.os.Bundle;
import android.preference.PreferenceActivity;


/**
 * Created by Dani on 09.04.2015.
 */
public class BeaconSettings extends PreferenceActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.settings);
    }
}
