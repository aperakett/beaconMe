package no.uit.ods.beaconme;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

/**
 * Created by Dani on 14.04.2015.
 */
public class BeaconSplash extends Activity {
    private int showTime = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_beacon_splash);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // After the given time load the class we want, the downloads from back-end should be complete
                Intent intent = new Intent(BeaconSplash.this, MainActivity.class);
                startActivity(intent);

                // Now we can end this activity
                finish();
            }
        }, showTime);
    }
}

