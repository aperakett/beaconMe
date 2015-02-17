package no.uit.ods.beaconme;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


public class BeaconListActivity extends Activity implements AbsListView.OnItemClickListener {
    private BeaconScannerService mService;
    private BeaconListAdapter mAdapter;
    private BeaconList mList;
    private ListView mListView;
    private Beacon lastBeaconClicked;
    private boolean initialized;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (!initialized) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_beacon_device_list);

            // fetch the scan service from arguments
            Bundle bundle = getIntent().getExtras();

            //        if (bundle == null)
            //            Log.i("Device List Activity", "bundle == null :(");

            IBinder iBinder = bundle.getBinder("binder");
            //        if (iBinder == null)
            //            Log.i("Device List Activity", "binder == null :(");

            BeaconScannerService.LocalBinder binder = (BeaconScannerService.LocalBinder) iBinder;
            mService = binder.getService();

            // todo get the association list....

            Log.i("BeaconListActivity", "onCreate()");

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
        Log.i("BeaconListActivity", "onItemClick() " + String.valueOf(position));

        Toast.makeText(parent.getContext(), "Clicked item: " + String.valueOf(position), Toast.LENGTH_SHORT).show();
        try {
            lastBeaconClicked = mList.getItem(position);
        }
        catch (Exception e) {
            Log.i("BeaconListActivity", "Failed getting beacon from list, out of bounds:" + e.getMessage());
        }
        Log.i("BeaconListActivity ", "Last clicked: " + lastBeaconClicked.getId());
    }

    public boolean onContextItemSelected(MenuItem item) {
        // Get the beacon number clicked on
        final int beaconNumber = ((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).position;
        AlertDialog.Builder alert;

        switch (item.getItemId()) {
            // add beacon association by getting user input which
            // associated with the last clicked beacon
            case R.id.menu_context_device_list_add:
                alert = new AlertDialog.Builder(this);
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
                break;

            case R.id.menu_context_device_list_remove:
                alert = new AlertDialog.Builder(this);
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

    public void refreshAdapter (View view) {
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
//            Log.i("BeaconListActivity", "refreshAdapter() - mAdaper: " + mAdapter.toString());
        }
    }

}
