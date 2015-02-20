package no.uit.ods.beaconme;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 *  BeaconListActivity class
 *
 */

public class BeaconListActivity extends Activity implements AbsListView.OnItemClickListener {
    private BeaconScannerService mService;
    private FactoryNetworkService mFNetwork;
    private BeaconListAdapter mAdapter;
    private BeaconList mList;
    private ListView mListView;
    private boolean initialized;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (!initialized) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_beacon_device_list);
            Log.i("BeaconListActivity", "onCreate()");

            // fetch the scan service from arguments
            Bundle bundle = getIntent().getExtras();

            // set up the scan service
            IBinder iBinder = bundle.getBinder("binderScan");
            BeaconScannerService.LocalBinder binderScan = (BeaconScannerService.LocalBinder) iBinder;
            mService = binderScan.getService();

            // set up the network service
            iBinder = bundle.getBinder("binderNetwork");
            FactoryNetworkService.LocalBinder binderNetwork = (FactoryNetworkService.LocalBinder) iBinder;
            mFNetwork = binderNetwork.getService();

            createListView();
            initialized = true;
        }
    }

    public void onDestroy () {
        super.onDestroy();
        mService.commitAssociation();
    }

    public void createListView () {

        // create list and adapter for listview
        mList = new BeaconList();
        mAdapter = new BeaconListAdapter(this, mList, mService);
mAdapter.notifyDataSetChanged();
        // create listview and attach adapter
        mListView = (ListView) findViewById(R.id.beaconListView);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);
        registerForContextMenu(mListView);

        // schedule periodical updating of the list based on the service- list
        // by creating a runnable which is scheduled at fixed rate
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        final Runnable scan = new Runnable() {
            @Override
            public void run() {
                BeaconList sList = mService.getList();
                // iterate all beacons in service list and add them to the
                // recordable list
                for (int i = 0; i < sList.getCount(); i++) {
                    Beacon beacon = sList.getItem(i);
                    if (!mList.contains(beacon)) {
                        mList.addDevice(beacon);
                    }
                }
                // finally update the view via the UiThread which is the adapter owner
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mAdapter.notifyDataSetChanged();
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
//        Log.i("BeaconListActivity", "onItemClick() " + String.valueOf(position));
//
//        Toast.makeText(parent.getContext(), "Clicked item: " + String.valueOf(position), Toast.LENGTH_SHORT).show();
//        try {
//            lastBeaconClicked = mList.getItem(position);
//        }
//        catch (Exception e) {
//            Log.i("BeaconListActivity", "Failed getting beacon from list, out of bounds:" + e.getMessage());
//        }
//        Log.i("BeaconListActivity ", "Last clicked: " + lastBeaconClicked.getId());
    }

    public boolean onContextItemSelected(MenuItem item) {
        // Get the beacon number clicked on
        final int beaconNumber = ((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).position;
        AlertDialog.Builder alert;

        switch (item.getItemId()) {
            // add beacon association by getting user input which
            // associated with the last clicked beacon
            case R.id.menu_context_device_list_add:
                assLocalAdd(beaconNumber);
                break;
            case R.id.menu_context_device_list_remove:
                assLocalRemove(beaconNumber);
                break;

            case R.id.menu_context_device_remote_add:
                try {
                    assRemoteAdd(beaconNumber);
                }
                catch (Exception e) {
                    Log.e("BeaconListActivity",  "Failed to create association with: " + e.getMessage());
                }
                break;
        }
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = this.getMenuInflater();
        inflater.inflate(R.menu.menu_context_device_list, menu);
    }

    private void assLocalAdd (final int beaconNumber) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Add beacon association");
        alert.setMessage("Type in something to associate with");

        //Get user input
        final EditText input = new EditText(this);
        alert.setView(input);
        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int button) {
                String ass = input.getText().toString();
                mService.addAssociation(mList.getItem(beaconNumber), ass);
            }
        });
        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int button) {

            }
        });
        alert.show();
    }


    private void assLocalRemove (final int beaconNumber) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Remove beacon association");
        alert.setMessage("Are you sure you want to remove the association?");
        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int button) {
                mService.removeAssociation(mList.getItem(beaconNumber).getId(), mList.getItem(beaconNumber).getUuid());
            }
        });
        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int button) {

            }
        });
        alert.show();
    }

    private void assRemoteAdd (final int beaconNumber) throws JSONException {

//        Spinner spinner = new Spinner(this);
        View layout = getLayoutInflater().inflate(R.layout.beacon_category, (ViewGroup)findViewById(R.id.categories));
        final Spinner spinner = (Spinner) layout.findViewById(R.id.categorySpinner);
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        final JSONArray categories = new JSONArray(new String("[{\"id\":1,\"subject\":\"Automobile\"},{\"id\":2,\"subject\":\"Sport\"},{\"id\":3,\"subject\":\"Clothes\"},{\"id\":4,\"subject\":\"Games\"},{\"id\":10,\"subject\":\"Cars\"},{\"id\":11,\"subject\":\"Animals\"},{\"id\":12,\"subject\":\"Test1\"}]"));
//        final JSONArray categories = mFNetwork.getCategories().get("categories"));
        final ArrayList<String> list = new ArrayList<String>();

        // Fetch categories from back- end
        try {
            for (int i = 0; i < categories.length(); i++) {
                    String subject = ((JSONObject)categories.get(i)).getString("subject").toString();
                    list.add(subject);
            }
        } catch (Exception e) {
            Log.e("BeaconListActivity", "assRemoteAdd() failed parsing categories with: " + e.getMessage());
            e.printStackTrace();
        }

        // create adapter with arraylist and populate spinner with list
        ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, list);
        spinner.setAdapter(adapter);


        // create a textview to put information about the beacon to
        TextView textView = ((TextView) layout.findViewById(R.id.beacon_info));
        StringBuilder beaconInfo = new StringBuilder();
        // get the beacon from the local list and insert data from beacon to view
        final Beacon beacon = mList.getItem(beaconNumber);
        if (beacon != null) {
            beaconInfo.append("ID:\n" + beacon.getId() + "\nUUID:\n" + beacon.getUuid());
        }

        try {
            // get information about beacon from backend
            JSONObject beaconInfoBackend = mFNetwork.getBeacon(beacon.getId());
            beaconInfo.append("\nAssociated to:\n" + beaconInfoBackend.get("url").toString());
        }
        catch (Exception e) {
            Log.e("BeaconListActivity", "Failed getting beaconinfo from backend: " + e.getMessage());
        }
        textView.setText(beaconInfo.toString());
        alert.setView(spinner);

        // get the edittext where the new association is expected
        final EditText inputAssociation = (EditText) layout.findViewById(R.id.associationInput);

        //set the cancel- button
        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int button) {
            }
        });
        //set the OK- button
        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int button) {
                String inAssStr = inputAssociation.getText().toString();
                int t = -1;
                try {
                    t = Integer.valueOf(((JSONObject)categories.get(list.indexOf(spinner.getSelectedItem().toString()))).get("id").toString());
                } catch (JSONException e) {
                    Log.e("BeaconListActivity", "Failed getting categorynumber from JSONArray with: " + e.getMessage());
                }
                Log.e("BeaconListActivity", "category id: " + String.valueOf(t));
                mFNetwork.setBeacon(beacon.getUuid(), inAssStr, t, beacon.getId());

            }
        });
        try {
            alert.setView(layout);
            alert.show();

        }
        catch (Exception e) {
            Log.e("BeaconListActivity", "alertShow() error: " + e.getMessage());
        }
    }

}
