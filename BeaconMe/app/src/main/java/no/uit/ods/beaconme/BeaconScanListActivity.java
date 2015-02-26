package no.uit.ods.beaconme;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.IBinder;
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
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 *  Author: Espen Mæland Wilhelmsen, espen.wilhelmsen@gmail.com
 *
 *  BeaconScanListActivity class
 *
 */

public class BeaconScanListActivity extends Activity implements AbsListView.OnItemClickListener {
    private BeaconScannerService    mService;
    private FactoryNetworkService   mFNetwork;
    private BeaconScanListAdapter   mAdapter;
    private BeaconList              mList;
    private ListView                mListView;
    private boolean                 initialized;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (!initialized) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_beacon_device_list);
            Log.i("BeaconScanListActivity", "onCreate()");

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
        mAdapter = new BeaconScanListAdapter(this, mList, mService);
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
    }

    public boolean onContextItemSelected(MenuItem item) {
        // Get the beacon number clicked on
        final int beaconNumber = ((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).position;

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
                    Log.e("BeaconScanListActivity",  "Failed to create association with: " + e.getMessage());
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
        View layout = getLayoutInflater().inflate(R.layout.beacon_add_local_association_alert, (ViewGroup)findViewById(R.id.beacon_add_local));
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        //alert.setTitle("Add beacon");
        //alert.setMessage("Type in something to associate with");

        //Get user input
        final EditText inputName = (EditText)layout.findViewById(R.id.beacon_add_local_name);
        final EditText inputAss  = (EditText)layout.findViewById(R.id.beacon_add_local_ass);

        alert.setView(layout);
        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int button) {
                String name = inputName.getText().toString();
                String ass  = inputAss.getText().toString();
                mService.addAssociation(mList.getItem(beaconNumber), name, ass);
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
        View layout = getLayoutInflater().inflate(R.layout.beacon_add_remote_association_alert, (ViewGroup)findViewById(R.id.categories));
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
            Log.e("BeaconScanListActivity", "assRemoteAdd() failed parsing categories with: " + e.getMessage());
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
            Log.e("BeaconScanListActivity", "Failed getting beaconinfo from backend: " + e.getMessage());
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
                    Log.e("BeaconScanListActivity", "Failed getting categorynumber from JSONArray with: " + e.getMessage());
                }
                Log.e("BeaconScanListActivity", "category id: " + String.valueOf(t));
                mFNetwork.setBeacon(beacon.getUuid(), inAssStr, t, beacon.getId());

            }
        });
        try {
            alert.setView(layout);
            alert.show();

        }
        catch (Exception e) {
            Log.e("BeaconScanListActivity", "alertShow() error: " + e.getMessage());
        }
    }


    /**
     *  Author: Espen Mæland Wilhelmsen, espen.wilhelmsen@gmail.com
     *
     *  BeaconScanListAdapter to used to populate listview, requires a BeaconList
     *  and the BeaconScannerService as input to the constructor method.
     *
     *  If the Beaconlist in the BeaconScanner service is specified
     */
    private class BeaconScanListAdapter extends BaseAdapter {
        private LayoutInflater          inflater;
        private BeaconList              btleDevices;
        private BeaconScannerService    mService;

        // Constructor
        public BeaconScanListAdapter(Context context, BeaconList list, BeaconScannerService service) {
            super();
            inflater    = LayoutInflater.from(context);
            btleDevices = list;
            mService = service;
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        public Beacon getItem(int i) {
            return btleDevices.getItem(i);
        }

        // Returns the number of devices in list
        public int getCount() {
            if (btleDevices != null)
                return btleDevices.getCount();
            else
                return 0;
        }

        public void setList (BeaconList list) {
            Log.i("BeaconScanListAdapter", "setList(): " + list.toString());
            btleDevices = list;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            if (view == null) {
                view = inflater.inflate(R.layout.beacon_listview, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.le_addr);
                viewHolder.deviceSignal = (TextView) view.findViewById(R.id.le_rssi);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.le_name);
                viewHolder.deviceUuid = (TextView) view.findViewById(R.id.le_uuid);
                viewHolder.deviceAssociation = (TextView) view.findViewById(R.id.le_ass);
                viewHolder.devicePic = (ImageView) view.findViewById(R.id.le_pic);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            Beacon beacon = this.getItem(i);
            BluetoothDevice device = beacon.getBtDevice();

            // Set the name of the beacon, first check if the name is set in a local
            // association, if not, set the name broadcasted by the beacon if any
            // else set the name to unknown device.
            final String deviceName = device.getName();
            final String localDeviceName = mService.getAssociationName(device.getAddress(), null);
            if (localDeviceName != null) {
                viewHolder.deviceName.setText(localDeviceName);
            }
            else if (deviceName != null && deviceName.length() > 0) {
                viewHolder.deviceName.setText(deviceName);
            }
            else {
                viewHolder.deviceName.setText(R.string.unknown_device);
            }

            viewHolder.deviceAddress.setText(device.getAddress());
            viewHolder.deviceSignal.setText(String.valueOf(beacon.getRssi()));
            viewHolder.deviceUuid.setText("\n" + beacon.getUuid());
            String ass = mService.getAssociation(beacon.getId(), beacon.getUuid());
            if (ass == null)
                ass = "Not Associated";
            viewHolder.deviceAssociation.setText("\n\n" + ass);

            // TODO: fix color scheme for beacons in range (green) and out of range (gray?)
            if (!mService.getList().contains(beacon.getId()))
                viewHolder.devicePic.setImageResource(R.drawable.beacon_not_in_range);
            else
                viewHolder.devicePic.setImageResource(R.drawable.beacon);
            return view;
        }
    }

    private class ViewHolder {
        ImageView   devicePic;
        TextView    deviceName;
        TextView    deviceSignal;
        TextView    deviceAddress;
        TextView    deviceUuid;
        TextView    deviceAssociation;
    }

}
