package no.uit.ods.beaconme;

import android.content.Intent;
import android.os.Bundle;
import android.test.ActivityUnitTestCase;

/**
 * Created by espen on 3/4/15.
 */
public class MyBeaconsTest extends ActivityUnitTestCase<MyBeacons> {
    Intent mLaunchIntent;
    public MyBeaconsTest(Class<MyBeacons> activityClass) {
        super(activityClass);
    }


    protected void setUp() throws Exception {
        super.setUp();

        mLaunchIntent = new Intent(getInstrumentation().getTargetContext(), MyBeacons.class);

//        Bundle bundle = new Bundle

//        startActivity(mLaunchIntent)

    }
}
