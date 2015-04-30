package no.uit.ods.beaconme;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import org.json.JSONException;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


public class MainActivity extends ActionBarActivity {
    public BeaconScannerService mService;
    public boolean mBound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.i("Main", "onCreate()");

        // Bind service, binds the service so it's reachable from different classes
        Intent intent = new Intent(this, BeaconScannerService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        startService(intent);

        // start the scan schedule
        //schedulePeriodicalScan();

        // Instantiate singleton BeaconClient class
        BeaconClient b = BeaconClient.getInstance();
        b.setConnTimeOut(3000);
        try {
            b.authenticate("admin@server.com", "admin123", getBaseContext());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i("MainActivity", "onDestroy()");
        mService.stopSelf();
        unbindService(mConnection);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            //return true;
            Intent i = new Intent(this, BeaconSettings.class);
            startActivity(i);
        }
        else if (id == R.id.action_scan) {
            scanBtleDevices(item.getActionView());
        }
        else if (id == R.id.action_mybeacons) {
            myBeacons(item.getActionView());
        }
        else if (id == R.id.action_categories) {
            categories(item.getActionView());
        }

        return super.onOptionsItemSelected(item);
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            BeaconScannerService.LocalBinder binder = (BeaconScannerService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

/*
    // Schedules periodical BTLE scan
    public void schedulePeriodicalScan () {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        final Runnable scan = new Runnable() {
            @Override
            public void run() {
                mService.scan();
            }
        };

        final Thread t = new Thread () {
            public void run () {
                try {
                  scan.run();
                } catch (Exception e) {

                }
            }
        };
        final ScheduledFuture scannerHandle = scheduler.scheduleAtFixedRate(t, 5000, 5000, TimeUnit.MILLISECONDS);
        scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                scannerHandle.cancel(false);
            }
        }, 4, TimeUnit.HOURS);

    }
*/
    // Starts the scan activity, which shows a list of ALL nearby beacons
    public void scanBtleDevices (View view) {
        Intent intent = new Intent(this, BeaconScanListActivity.class);
        Bundle bundle = new Bundle();
        bundle.putBinder("binderScan", mService.getBinder());
        intent.putExtras(bundle);
        startActivity(intent, bundle);
    }

    // Starts the MyBeacon activity, which shows a list of ALL nearby beacons
    public void myBeacons (View view) {
        Intent intent = new Intent(this, MyBeacons.class);
        Bundle bundle = new Bundle();
        bundle.putBinder("binderScan", mService.getBinder());
        intent.putExtras(bundle);
        startActivity(intent, bundle);
    }

    // Starts the Category activity, where all categories are listed with checkboxes so you can filter
    public void categories (View view) {
        Intent intent = new Intent(this, BeaconCategory.class);
        Bundle bundle = new Bundle();
        bundle.putBinder("binderScan", mService.getBinder());
        intent.putExtras(bundle);
        startActivity(intent, bundle);
    }
}
