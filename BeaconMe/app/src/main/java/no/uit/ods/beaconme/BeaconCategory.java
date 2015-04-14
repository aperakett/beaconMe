package no.uit.ods.beaconme;

/**
 * Created by Dani on 02.03.2015.
 *
 * This class sets up the category list, it gets the category from the back-end system if it can, if not
 * it uses a local backup from disk
 */

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;


public class BeaconCategory extends ActionBarActivity implements View.OnClickListener {
    Button button;
    ListView lv;
    ArrayAdapter<String> catAdapter;
    private BeaconScannerService cService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_beacon_categories);
        findViewsById();

        // Gets the categories from the back-end system
        ArrayList<String> catList = getCategories();

        // Set the category list to multiple choice for checkboxes
        catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_multiple_choice, catList);
        lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        lv.setAdapter(catAdapter);

        Bundle b = getIntent().getExtras();

        // Need the scan service for the BeaconFilter class
        IBinder iBinder = b.getBinder("binderScan");
        BeaconScannerService.LocalBinder binderScan = (BeaconScannerService.LocalBinder) iBinder;
        cService = binderScan.getService();

        button.setOnClickListener(this);
    }

    // This method gets a JSONArray of categories from the back-end system and converts it to
    // a string array, it also saves the array on disk for backup or uses the backup if fail
    private ArrayList getCategories() {
        String tempJSON;
        String filename = "SavedCat.sav";

        //  set a user in the BeaconClient class
        BeaconClient bc = BeaconClient.getInstance();
        // try {
            // bc = new BeaconClient("admin@server.com", "admin123");
        // } catch (InterruptedException e) {
        //     Log.e("Category", "Connect to back-end system failed with: " + e.getMessage());
        // }

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

    // Saves a string in a file on disk
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

    // Reads from disk and returns a char[] buffer
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
                alertDialog.setMessage("Ups, something went terribly wrong, are you connected to the interweb?");
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

        Intent intent = new Intent(getApplicationContext(), BeaconFilter.class);

        // Create a bundle, used to pass data between activities
        Bundle b = new Bundle();

        // Insert the results in the bundle
        b.putStringArray("checkedItems", resultArray);
        b.putBinder("binderScan", cService.getBinder());
        intent.putExtras(b);

        // Use the intent to start the OutputActivity class
        startActivity(intent, b);
    }
}