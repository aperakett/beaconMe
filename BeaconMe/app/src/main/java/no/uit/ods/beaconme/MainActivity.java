package no.uit.ods.beaconme;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


public class MainActivity extends ActionBarActivity implements View.OnClickListener {
    public BeaconScannerService mService;
    public boolean mBound = false;
    Button button;
    ListView lv;
    ArrayAdapter<String> catAdapter;
    ArrayList<notificationItem> notList = new ArrayList<>();
    int notifyID = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_beacon_categories);
        findViewsById();

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


        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        // With the help of a scheduler we get updated beacons from the service for the local notification system
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        final Runnable scan = new Runnable() {
            @Override
            public void run() {
                if (mService != null ) {
                    for (int i = 0; i < mService.getList().getCount(); i++) {
                        if (preferences.getBoolean("personal_notifications", true) &&
                                                    preferences.getBoolean("all_notifications", true)) {
                            try {
                                if (mService.getAssociationList().notify(mService.getList().getItem(i))) {
                                    notification(mService.getList().getItem(i));
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        };
        final ScheduledFuture scannerHandle = scheduler.scheduleAtFixedRate(scan, 5000, 5000, TimeUnit.MILLISECONDS);
        scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                scannerHandle.cancel(true);
            }
        }, 60 * 60, TimeUnit.SECONDS);
        scan.run();


        // Gets the categories from the back-end system
        ArrayList<String> catList = getCategories();

        // Set the category list to multiple choice for checkboxes
        catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_multiple_choice, catList);
        lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        lv.setAdapter(catAdapter);

    }

    /**
     * This method handles the notifications of nearby beacons in the selected categories, it checks if the notification already is pushed
     * and updates the notification if it is, it passes the beacon on with the correct ID to the sendNotification method.
     *
     * @param beacon A beacon object.
     */
    public void notification(Beacon beacon) {
        if (notList.isEmpty()) {

            notificationItem newItem = new notificationItem(beacon.getAddress(), notifyID);
            notList.add(newItem);
            notifyID++;
        }

        notificationItem tmpItem = null;
        for (notificationItem n : notList) {
            if (n.getName().equals(beacon.getAddress())) {
                tmpItem = n;
                break;
            }
        }

        if (tmpItem == null) {
            notificationItem newItem = new notificationItem(beacon.getAddress(), notifyID);
            notList.add(newItem);
            try {
                sendNotification(beacon, notifyID, newItem);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            notifyID++;
        }
        else {
            try {
                sendNotification(beacon, tmpItem.ID, tmpItem);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * This method makes the notifications and pushes it out, it also checks the association is an URL and makes a button
     * in the notification that launches the browser if it is.
     *
     * @param beacon A beacon object.
     * @param ID A notification ID that helps tell if the notification is already pushed out or not.
     * @param item  A notification object.
     */
    public void sendNotification(Beacon beacon, int ID, notificationItem item) throws JSONException {
        final String notifyContent;

            notifyContent = "Name: " + mService.getAssociationList().getName(beacon) + "\n" + "Distance " + String.format("%.0f", beacon.getDistance()) + "m"
                    + "\n" + "Association: " + mService.getAssociationList().getAssociation(beacon);


        // Modify the association string if it's a url but don't include http://
        String association;

            association = mService.getAssociationList().getAssociation(beacon);

        if (association.startsWith("www.")) {
            association = "http://" + association;
        }

        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(association));
        PendingIntent pi = PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);

        if (item.builder == null) {
            NotificationCompat.Builder b = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.drawable.ic_settings_input_antenna_white_18dp)
                    .setContentTitle("A new Beacon is nearby!")
                    .setContentText(mService.getAssociationList().getName(beacon) + " " + "Is in range!")
                    .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                    .setOnlyAlertOnce(true)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(notifyContent));

            if (isURL(association)) {
                b.addAction(R.drawable.ic_language_white_18dp, "Launch association", pi);
            }
            item.builder = b;
        }

        // Start the NotificationManager service
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        item.builder.setStyle(new NotificationCompat.BigTextStyle().bigText(notifyContent));
        // Send the notification
        nm.notify(ID, item.builder.build());
    }

    /**
     * Check if it's an URL, returns true or false
     *
     * @param url The string to be checked.
     *
     * @return true/false
     */
    public boolean isURL(String url){
        if (url.startsWith("www.")) {
            url = "http://" + url;
        }

        if (url.startsWith("http://")) {
            return true;
        }

        return false;

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

    /**
     *
     * This method gets a JSONArray of categories from the back-end system and converts it to
     * a string array, it also saves the array on disk for backup or uses the backup if fail
     *
     * @return array with categories in String format
     */
    private ArrayList getCategories() {
        String tempJSON;
        String filename = "SavedCat.sav";

        // Set up the "connection"
        BeaconClient bc = BeaconClient.getInstance();

        // Get a JSONArray from the server with the categories
        JSONArray catArray = null;
        try {
            catArray = bc.getCategories(getBaseContext());
        } catch (InterruptedException e) {
            Log.e("Category", "getCategories from back-end system failed with: " + e.getMessage());
        }

        // Try to save the Category array to disk, so we have some categories locally
        // in case of no network or other issues
        if (catArray != null) {
            tempJSON = catArray.toString();
            saveToDisk(tempJSON, filename);
        } else {
            char[] buffer = readFromDisk(filename);

            // Convert the String read from disk back to a JSONArray
            if (buffer != null) {
                try {
                    catArray = new JSONArray(new String(buffer));
                } catch (JSONException e) {
                    Log.e("Category", "Convert to JSONArray failed with: " + e.getMessage());
                }
            }
        }

        // Convert the received JSONArray to a String array with the categories, if something went wrong with the transfer
        // we get the locally saved file, so we can use the last successfully downloaded version
        ArrayList<String> catList = new ArrayList<>();
        if (catArray != null) {
            for (int i = 0; i < catArray.length(); i++) {
                try {
                    catList.add(((JSONObject) catArray.get(i)).get("topic").toString());
                } catch (JSONException e) {
                    Log.e("Category", "JSON object retrieval failed with: " + e.getMessage());
                }
            }
        }
        return catList;
    }

    /**
     * Saves a string in a file on disk
     *
     * @param array The array to read from.
     * @param filename The filname to save to.
     */
    public void saveToDisk(String array, String filename) {
        File f = new File(getApplicationContext().getFilesDir() + filename);
        try {
            // Make sure we write to a "clean" file
            f.delete();

            // Make a file to save to, then transfer the array to the file
            FileWriter writer = new FileWriter(f);
            writer.write(array);

            // Closes the transfer AND the file
            writer.close();
        } catch (Exception e) {
            Log.e("Category", "Save Categories to disk failed with: " + e.getMessage());
        }
    }

    /**
     * Reads from disk and returns a char[] buffer
     *
     * @param filename The filename to read from
     *
     * @return A string read from the file on disk
     */
    public char[] readFromDisk(String filename) {
        char[] buffer = null;
        // Read the local file to get the "backup" array
        try {
            File f = new File(getApplicationContext().getFilesDir() + filename);
            if(f.exists()) {
                FileReader reader = new FileReader(f);
                buffer = new char[((int) f.length())];
                reader.read(buffer);
                reader.close();
            }
            else {
                // The file doesn't exist, so we show a dialog with a error message and returns to "home" screen.
                AlertDialog alertDialog = new AlertDialog.Builder(this).create();
                alertDialog.setTitle("ERROR!");
                alertDialog.setMessage("Ups, something went terribly wrong, are you connected to the internet?");
                alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });
                alertDialog.show();
            }
        } catch (Exception e) {
            Log.e("Category", "Read Categories from disk failed with: " + e.getMessage());
        }
        return buffer;
    }

    private void findViewsById() {
        lv = (ListView) findViewById(R.id.catlist);
        button = (Button) findViewById(R.id.applyButton);
    }

    public void onClick(View view) {
        SparseBooleanArray checked = lv.getCheckedItemPositions();
        ArrayList<String> checkedItems = new ArrayList<>();

        // Loop through the category list and add the checked category to the list with the checked items
        for (int i = 0; i < checked.size(); i++) {
            int pos = checked.keyAt(i);

            if(checked.valueAt(i)) {
                checkedItems.add(catAdapter.getItem(pos));
            }
        }

        // Make a new array containing the results
        String[] resultArray = new String[checkedItems.size()];

        // Copy the items over to the array
        for (int i = 0; i < checkedItems.size(); i++) {
            resultArray[i] = checkedItems.get(i);
        }

        Intent intent = new Intent(this, BeaconFilter.class);

        // Create a bundle, used to pass data between activities
        Bundle b = new Bundle();

        // Insert the results in the bundle
        b.putStringArray("checkedItems", resultArray);
        b.putBinder("binderScan", mService.getBinder());
        intent.putExtras(b);

        // Use the intent to start the Filter class
        startActivity(intent, b);
    }


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
    public void categories () {
        Intent intent = new Intent(this, BeaconCategory.class);
        Bundle bundle = new Bundle();
        bundle.putBinder("binderScan", mService.getBinder());
        intent.putExtras(bundle);
        startActivity(intent, bundle);
    }

    /**
     * Makes a notification object with a name and an ID
     */
    private class notificationItem {
        public String name;
        public Integer ID;
        public NotificationCompat.Builder builder;

        notificationItem(String name, int ID) {
            this.name = name;
            this.ID = ID;
            this.builder = null;
        }

        public String getName() {
            return name;
        }
    }
}
