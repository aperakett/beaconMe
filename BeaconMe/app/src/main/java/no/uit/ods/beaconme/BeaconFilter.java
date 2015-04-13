package no.uit.ods.beaconme;

import android.content.Context;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/*
*
* Created by Dani on 02.03.2015.
*
*/
public class BeaconFilter extends ActionBarActivity implements AbsListView.OnItemClickListener {
    private BeaconScannerService cService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        String filename = "SavedCat.sav";
        JSONArray catArray;
        ArrayList<testBeacon> beaconList;
        ArrayList<testCategory> categoryList;

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
        beaconList = convertBeacon(beaconArray, "category_id","name", "mac", "uuid", "major", "minor");

        // Convert the received JSONArray to a String array
        categoryList = convertCat(catArray, "id", "topic");

        // Get the filtered beacons back in a list (this way we still got access to relevant information)
        final ArrayList<testBeacon> resultList = filterResults(checkedArray, categoryList, beaconList);

        final ArrayList<testBeacon> finalResultList = new ArrayList<>();

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
                                Beacon tmpBeacon = cService.getList().getItem(i);
                                //t.association = cService.getAssociation(tmpBeacon);

                                finalResultList.add(t);
                            }
                            // System.out.println(t.getName());
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

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    }

    public boolean onContextItemSelected(MenuItem item) {
        // Get the beacon number clicked on
        final int beaconNumber = ((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).position;

        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = this.getMenuInflater();
        inflater.inflate(R.menu.menu_context_device_list, menu);
    }

    // This method gets a JSONArray of categories from the back-end system and converts it to
    // a string array, it also saves the array on disk for backup or uses the backup if fail
    private JSONArray getBeacons() {
        String tempJSON;
        String filename = "SavedBeacons.sav";

        //  set a user in the BeaconClient class
        /*
        BeaconClient bc = null;
        try {
            bc = new BeaconClient("admin@server.com", "admin123");
        } catch (InterruptedException e) {
            Log.e("BeaconFilter", "Connect to back-end system failed with: " + e.getMessage());
        }
        */
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

    // This method converts a JSONArray of beacons to an ArrayList with the relevant information
    private ArrayList<testBeacon> convertBeacon(JSONArray array, String ID, String name, String mac, String uuid, String major, String minor) {
        ArrayList<testBeacon> List = new ArrayList<>();
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                try {
                    testBeacon tmpBeacon = new testBeacon(((JSONObject) array.get(i)).getInt(ID),((JSONObject) array.get(i)).get(name).toString(), "",
                            ((JSONObject) array.get(i)).get(mac).toString(), ((JSONObject) array.get(i)).get(uuid).toString(),
                            ((JSONObject) array.get(i)).get(major).toString(), ((JSONObject) array.get(i)).get(minor).toString(), 0, "");
                    List.add(tmpBeacon);
                } catch (JSONException e) {
                    Log.e("BeaconFilter", "JSON object retrieval failed with: " + e.getMessage());
                }
            }
        }
        return List;
    }

    // This method converts a JSONArray of categories to an ArrayList with the relevant information
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

    // Saves a string in a file on disk
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

    // Reads from disk and returns a char[] buffer
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

    // This method does the actual filtering
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

    // This is a beacon object, with relevant information for the result.
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
    // This is a category object, with relevant information for the result.
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
            content.distance.setText("( Approximately" + " " + String.format("%.0f", beacon.getDistance()) + " " + "meters )");
            content.association.setText("Linked to: " + beacon.getAssociation());
            return view;
        }
    }

    // Just a simple holder for the contents of the list
    private class listContent {
        TextView name;
        TextView category;
        TextView distance;
        TextView association;
    }
}