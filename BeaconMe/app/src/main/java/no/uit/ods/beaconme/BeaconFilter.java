package no.uit.ods.beaconme;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/*
*
* Created by Dani on 02.03.2015.
*
* This class does the filtration and notification
* It includes a beacon class and a custom adapter
*/
public class BeaconFilter extends ActionBarActivity implements AbsListView.OnItemClickListener {
    private BeaconScannerService cService;
    final ArrayList<testBeacon> finalResultList = new ArrayList<>();
    ArrayList<notificationItem> notList = new ArrayList<>();
    int notifyID = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        String filename = "SavedCat.sav";
        JSONArray catArray;
        ArrayList<testBeacon> beaconList;
        ArrayList<testCategory> categoryList;
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_beacon_results);

        // Set up the ListView
        ListView view1 = (ListView) findViewById(R.id.resultList);
        ListView view2 = (ListView) findViewById(R.id.resultView);

        Bundle b = getIntent().getExtras();

        // Need the scan service
        IBinder iBinder = b.getBinder("binderScan");
        BeaconScannerService.LocalBinder binderScan = (BeaconScannerService.LocalBinder) iBinder;
        cService = binderScan.getService();

        // Get the checked items from the BeaconCategory class
        String[] checkedArray = b.getStringArray("checkedItems");

        // Get the JSON category array from disk
        catArray = readFromDisk(filename);

        // Get a list of all beacons from the back-end system, if it fails a local version will be used
        // It also converts it to a String array so it's easier to work with
        JSONArray beaconArray = getBeacons();

        // Convert the received JSONArray to a String array with the beacons relevant information
        beaconList = convertBeacon(beaconArray, "category_id","name", "mac", "uuid", "major", "minor", "url");

        // Convert the received JSONArray to a String array
        categoryList = convertCat(catArray, "id", "topic");

        // Get the filtered beacons back in a list (this way we still got access to relevant information)
        final ArrayList<testBeacon> resultList = filterResults(checkedArray, categoryList, beaconList);

        ArrayAdapter<String> outputAdapter = new ArrayAdapter<>(this, R.layout.beacon_selected_results, R.id.selectedName, checkedArray);
        final ResultAdapter resultAdapter = new ResultAdapter(this, finalResultList);

        view1.setAdapter(outputAdapter);
        view2.setAdapter(resultAdapter);
        view2.setOnItemClickListener(this);
        registerForContextMenu(view2);

        // With the help of a scheduler we get updated beacons from the service
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        final Runnable scan = new Runnable() {
            @Override
            public void run() {
                // Need to clear the results to remove beacons that are not in the service list (not in range)
                finalResultList.clear();

                for (testBeacon t : resultList) {
                    for (int i = 0; i < cService.getList().getCount(); i++) {
                        if (t.getMac().equals(cService.getList().getItem(i).getAddress())) {
                            if(!finalResultList.contains(t)) {
                                t.distance = cService.getList().getItem(i).getDistance();
                                try {
                                    if (cService.getAssociationList().getAssociation(cService.getList().getItem(i)) != null) {
                                        t.association = cService.getAssociationList().getAssociation(cService.getList().getItem(i));
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                if (preferences.getBoolean("result_notifications", true) &&
                                                            preferences.getBoolean("all_notifications", true)) {
                                    notification(t);
                                }

                                finalResultList.add(t);
                            }
                        }
                    }
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        resultAdapter.notifyDataSetChanged();
                    }
                });
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
    }

    /**
     * This method handles the notifications of nearby beacons in the selected categories, it checks if the notification already is pushed
     * and updates the notification if it is, it passes the beacon on with the correct ID to the sendNotification method.
     *
     * @param beacon A beacon object.
     */
    public void notification(testBeacon beacon) {
        if (notList.isEmpty()) {
            notificationItem newItem = new notificationItem(beacon.getName(), notifyID);
            notList.add(newItem);
            notifyID++;
        }

        notificationItem tmpItem = null;
        for (notificationItem n : notList) {
            if (n.getName().equals(beacon.getName())) {
                tmpItem = n;
                break;
            }
        }

        if (tmpItem == null) {
            notificationItem newItem = new notificationItem(beacon.getName(), notifyID);
            notList.add(newItem);
            sendNotification(beacon, notifyID, newItem);
            notifyID++;
        }
        else {
            sendNotification(beacon, tmpItem.ID, tmpItem);
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
    public void sendNotification(testBeacon beacon, int ID, notificationItem item) {
        // This is the notification text, relevant information is collected from the beacon
        final String notifyContent = "Name: " + beacon.getName() + "\n" + "Distance " + String.format("%.0f", beacon.getDistance()) + "m"
                + "\n" + "Association: " + beacon.getAssociation() + "\n" + "Category: " + beacon.getCatName();

        // Modify the association string if it's a url but don't include http://
        String association = beacon.getAssociation();
        if (association.startsWith("www.")) {
            association = "http://" + association;
        }

        // Set up the action for button push
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(association));
        PendingIntent pi = PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);

        if (item.builder == null) {
            NotificationCompat.Builder b = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_settings_input_antenna_white_18dp)
                .setContentTitle("A new Beacon is nearby!")
                .setContentText(beacon.getName() + " " + "(" + beacon.getCatName() + ")" + " " + "is in range!")
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setOnlyAlertOnce(true)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(notifyContent));

            // Check if the association is an URL, only show the button if it is.
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

    // This method handles the click on the result list, if it is an URL, the browser is launched, else do nothing.

    /**
     * This method handles the click on the result list, if it is an URL, the browser is launched, else do nothing.
     *
     * @param parent
     * @param view
     * @param position
     * @param id
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String ass = finalResultList.get(position).getAssociation();
        if (ass == null) {
            return;
        }

        // If it starts with www we add http:// as the Uri.parse needs http://
        if (ass.startsWith("www.")) {
            ass = "http://" + ass;
        }

        if (ass.startsWith("http://")) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(ass));
            startActivity(browserIntent);
        }
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


    /**
     * Creates the onclick menu
     *
     * @param menu
     * @param view
     * @param menuInfo
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);
        MenuInflater inflater = this.getMenuInflater();
        inflater.inflate(R.menu.menu_context_filter_list, menu);
    }


    /**
     * If menu item is selected
     *
     * @param item
     * @return
     */
    public boolean onContextItemSelected(MenuItem item) {
        // Get the beacon number clicked on
        final int beaconNumber = ((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).position;

        switch (item.getItemId()) {
            // add beacon association by getting user input which
            // associated with the last clicked beacon
            case R.id.menu_context_device_list_save:
                assLocalAdd(beaconNumber);
                break;
        }
        return true;
    }


    /**
     * This method is borrowed from the BeaconScanListActivity written by Espen, to add local association
     *
     * @param beaconNumber
     */
    public void assLocalAdd (final int beaconNumber) {
        View layout = getLayoutInflater().inflate(R.layout.beacon_add_local_association_alert, (ViewGroup)findViewById(R.id.beacon_add_local));
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        //Get user input
        final EditText inputName = (EditText)layout.findViewById(R.id.beacon_add_local_name);
        final EditText inputAss  = (EditText)layout.findViewById(R.id.beacon_add_local_ass);
        final Spinner spinner   = (Spinner) layout.findViewById(R.id.beacon_add_notificationSpinner);

        final List<String> list = Arrays.asList("Don't notify", "Less than 1m", "Less than 15m", "Always notify");

        // create adapter with arraylist and populate spinner with list
        ArrayAdapter adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, list);
        spinner.setAdapter(adapter);

        alert.setView(layout);
        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int button) {
                Integer notify  = list.indexOf(spinner.getSelectedItem().toString());
                String name     = inputName.getText().toString();
                String ass      = inputAss.getText().toString();

                try {
                    for (int i = 0; i < cService.getList().getCount(); i++) {
                        if (finalResultList.get(beaconNumber).getMac().equals(cService.getList().getItem(i).getAddress())) {
                            try {
                                cService.getAssociationList().add(cService.getList().getItem(i), name, ass, notify);
                            } catch (Exception e) {
                                Log.e("BEaconScanListActivity", "Failed to add assocaition: " + e.getMessage());
                            }
                            /*cService.getAssociationList().add(cService.getList().getItem(i), name, ass, notify);
                            mService.getAssociationList().add(mList.getItem(beaconNumber), name, ass, notify);
                            cService.getAssociationList().add(cService.getList().getItem(i), name, ass, notify);*/
                        }
                    }

                } catch (Exception e) {
                    Log.e("BEaconScanListActivity", "Failed to add assocaition: " + e.getMessage());
                }
            }
        });
        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int button) {

            }
        });
        alert.show();
    }

    /**
     * This method gets a JSONArray of beacons from the back-end system and converts it to
     * a string array, it also saves the array on disk for backup or uses the backup if fail
     *
     * @return An array with all beacons from the back-end system
     */
    private JSONArray getBeacons() {
        String tempJSON;
        String filename = "SavedBeacons.sav";

        BeaconClient bc = BeaconClient.getInstance();

        // Get a JSONArray from the server with the categories
        JSONArray beaconArray = null;
        try {
            beaconArray = bc.getBeacons("", "", 0, "", "", "", "", getBaseContext());
        } catch (InterruptedException e) {
            Log.e("BeaconFilter", "getBeacons from back-end system failed with: " + e.getMessage());
        }

        // Try to save the beacon array to disk, so we have some categories locally
        // in case of no network or other issues
        if (beaconArray != null) {
            tempJSON = beaconArray.toString();
            saveToDisk(tempJSON, filename);
        } else {
            beaconArray = readFromDisk(filename);
        }

        return beaconArray;
    }


    /**
     * This method converts a JSONArray of beacons to an ArrayList with the relevant information.
     *
     * @param array The JSON array to read from
     * @param ID The beacon ID
     * @param name The beacon name
     * @param mac The beacon mac address
     * @param uuid The beacon uuid
     * @param major The beacon major
     * @param minor The beacon minor
     * @param url The Beacon association
     *
     * @return List of beacon objects
     */
    private ArrayList<testBeacon> convertBeacon(JSONArray array, String ID, String name, String mac, String uuid, String major, String minor, String url) {
        ArrayList<testBeacon> List = new ArrayList<>();
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                try {
                    testBeacon tmpBeacon = new testBeacon(((JSONObject) array.get(i)).getInt(ID),((JSONObject) array.get(i)).get(name).toString(), "",
                            ((JSONObject) array.get(i)).get(mac).toString(), ((JSONObject) array.get(i)).get(uuid).toString(),
                            ((JSONObject) array.get(i)).get(major).toString(), ((JSONObject) array.get(i)).get(minor).toString(), 0, ((JSONObject) array.get(i)).get(url).toString());

                    List.add(tmpBeacon);
                } catch (JSONException e) {
                    Log.e("BeaconFilter", "JSON object retrieval failed with: " + e.getMessage());
                }
            }
        }
        return List;
    }


    /**
     * This method converts a JSONArray of categories to an ArrayList with the relevant information.
     *
     * @param array The array to convert
     * @param ID The category ID
     * @param name The category name
     *
     * @return An arraylist with category objects
     */
    private ArrayList<testCategory> convertCat(JSONArray array, String ID, String name) {
        ArrayList<testCategory> List = new ArrayList<>();
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                try {
                    testCategory tmp = new testCategory(((JSONObject) array.get(i)).getInt(ID),((JSONObject) array.get(i)).get(name).toString());
                    List.add(tmp);
                } catch (JSONException e) {
                    Log.e("Category", "JSON object retrieval failed with: " + e.getMessage());
                }
            }
        }

        return List;
    }


    /**
     * Saves a string in a file on disk
     *
     * @param array The array to read from.
     * @param filename The filname to save to.
     */
    private void saveToDisk(String array, String filename) {
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
            Log.e("BeaconFilter", "Save beacons to disk failed with: " + e.getMessage());
        }
    }


    /**
     * Reads from disk and returns a char[] buffer
     *
     * @param filename The filename to read from
     *
     * @return A string read from the file on disk
     */
    private JSONArray readFromDisk(String filename) {
        char[] buffer = null;
        JSONArray tmpArray = null;

        // Read the local file to get the "backup" array
        try {
            File f = new File(getApplicationContext().getFilesDir() + filename);

            FileReader reader = new FileReader(f);
            buffer = new char[((int) f.length())];
            reader.read(buffer);
            reader.close();
        } catch (Exception e) {
            Log.e("BeaconFilter", "Get beacons from disk failed with: " + e.getMessage());
        }

        // Convert the String read from disk back to a JSONArray
        try {
            tmpArray = new JSONArray(new String(buffer));
        } catch (JSONException e) {
            Log.e("Category", "Convert to JSONArray failed with: " + e.getMessage());
        }

        return tmpArray;
    }


    /**
     * This method does the actual filtering.
     *
     * @param checkedArray The array with selected categories received from the category activity.
     * @param categoryList The list with all the categories from the back-end system.
     * @param beaconList The list with all the beacons from the back-end system.
     *
     * @return A list with beacon objects.
     */
    private ArrayList<testBeacon> filterResults(String[] checkedArray, ArrayList<testCategory> categoryList, ArrayList<testBeacon> beaconList) {
        // Filtrate the checked items from the category list
        ArrayList<testBeacon> resultList = new ArrayList<>();

        for (int i = 0; i < checkedArray.length; i++) {
            for (testCategory c : categoryList) {
                if ((c.getTopic()).equals(checkedArray[i])) {
                    for (testBeacon t : beaconList) {
                        if ((c.getID()).equals(t.getCatID())) {
                            t.catName = c.getTopic();
                            resultList.add(t);
                        }
                    }
                }
            }
        }
        return resultList;
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


    /**
     * This is a beacon object, with relevant information for the result.
     */
    private class testBeacon {
        public Integer catID;
        public String name;
        public String catName;
        public String mac;
        public String uuid;
        public String major;
        public String minor;
        public String association;
        double distance;

        testBeacon(Integer catID, String name, String catName, String mac,
                   String uuid, String major, String minor, double distance, String association) {
            this.catID = catID;
            this.name = name;
            this.catName = catName;
            this.mac = mac;
            this.uuid = uuid;
            this.major = major;
            this.minor = minor;
            this.distance = distance;
            this.association = association;
        }

        public Integer getCatID() {
            return catID;
        }

        public String getCatName() {
            return catName;
        }

        public String getMac() {
            return mac;
        }

        public String getName() {
            return name;
        }

        public double getDistance() { return distance; }

        public String getAssociation() { return association; }
    }


    /**
     * This is a category object, with relevant information for the result.
     */
    private class testCategory {
        public Integer ID;
        public String topic;

        testCategory(Integer ID, String topic) {
            this.ID = ID;
            this.topic = topic;
        }

        public Integer getID() {
            return ID;
        }

        public String getTopic() {
            return topic;
        }
    }


    /**
     * Custom adapter to show what we want in the result list.
     */
    private class ResultAdapter extends ArrayAdapter<testBeacon> {
        private ArrayList<testBeacon>   beacons;
        private Context                 context;
        private LayoutInflater          inflater;

        public ResultAdapter(Context context, ArrayList<testBeacon> beacons) {
            super(context, R.layout.beacon_filter_result);
            inflater = LayoutInflater.from(context);
            this.beacons = beacons;
        }

        @Override
        public long getItemId(int pos) {
            return 0;
        }

        public testBeacon getItem(int pos) {
            return beacons.get(pos);
        }

        // How many objects in the list?
        public int getCount() {
            if (beacons == null) {
                return 0;
            }
            else {
                return beacons.size();
            }
        }

        /**
         *
         * @param pos
         * @param view
         * @param viewGroup
         *
         * @return view
         */
        @Override
        public View getView(int pos, View view, ViewGroup viewGroup) {
            listContent content;
            if (view == null) {
                view = inflater.inflate(R.layout.beacon_filter_result, null);
                content = new listContent();
                content.name = (TextView) view.findViewById(R.id.resultName);
                content.category = (TextView) view.findViewById(R.id.resultCategory);
                content.distance = (TextView) view.findViewById(R.id.resultDistance);
                content.association = (TextView) view.findViewById(R.id.resultAssociation);
                view.setTag(content);
            }
            else {
                content = (listContent) view.getTag();
            }

            testBeacon beacon = this.getItem(pos);

            content.name.setText(beacon.getName());
            content.category.setText(beacon.getCatName());
            content.distance.setText("-" + " " + "Approx." + " " + String.format("%.0f", beacon.getDistance()) + "m" + " " + "away");
            content.association.setText("Linked to: " + beacon.getAssociation());

            return view;
        }
    }


    /**
     * Just a simple object for the contents of the list
     */
    private class listContent {
        TextView name;
        TextView category;
        TextView distance;
        TextView association;
    }
}