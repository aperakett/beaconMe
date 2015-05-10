package no.uit.ods.beaconme;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
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
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 *  Implements the activity that shows the list of associations made on
 *  the local device. <br>
 *
 *  Author: Espen Mæland Wilhelmsen, espen.wilhelmsen@gmail.com
 */

public class BeaconScanListActivity extends Activity implements AbsListView.OnItemClickListener {
    private BeaconScannerService    mService;
    private BeaconScanListAdapter   mAdapter;
    private BeaconList              mList;
    private boolean                 initialized;
    private boolean                 sort = false;
    ListView                        mListView;


    /**
     * Automatically called upon activity creation. <br>
     *
     * Requires the scanner service bundled as intent- extras.
     * The scanner service iBinder must be named "binderScan". <br>
     *
     * The scanner service is set up and the views are created.
     *
     * @param savedInstanceState The bundle the activity is started with, gets
     *                           automatically assigned.
     */
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

            createListView();
            initialized = true;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /**
     * Upon onDestroy all associations are commited to disk
     * to make sure none is lost.
     */
    public void onDestroy () {
        super.onDestroy();
        try {
            mService.getAssociationList().commit();
        } catch (IOException e) {
            Log.e("BEaconScanListActivity", "Failed to save association list: " + e.getMessage());
        }
    }

    /**
     * This function sets up the listview and schedules
     * periodical updates to the listview. Context menu
     * are registered.
     */
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
                if (sort)
                    mList.sort();
                // finally update the view via the UiThread which is the adapter owner
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mAdapter.notifyDataSetChanged();
                    }
                });
            }
        };
        int updateInterval = Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(this).getString("sync_settings", "2500"));
        final ScheduledFuture scannerHandle = scheduler.scheduleAtFixedRate(scan, updateInterval, updateInterval, TimeUnit.MILLISECONDS);
        scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                scannerHandle.cancel(true);
            }
        }, 60 * 60, TimeUnit.SECONDS);
        scan.run();
    }

    /**
     * Used to enable/disable sorting of the list in the view.
     * Beacons are aranged by the distance.
     *
     * @param view The view, not used but from the xml this is "required"
     */
    public void sort (View view) {
        if (this.sort)
            this.sort = false;
        else
            this.sort = true;
    }

    /**
     * Required method for AbsListView.OnItemClickListener.
     * Launches the contextmenu.
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Log.e("BeaconScanListActivity", "Click - pos: " + String.valueOf(position) + ", id: " + String.valueOf(id));
        this.openContextMenu(view);
    }

    /**
     * Launches the item selected on the context menu.
     *
     * @param item The menuitem selected in the context menu.
     * @return boolean indicating method success.
     */
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
                    return false;
                }
                break;
        }
        return true;
    }

    /**
     * Creates the context menu, inflates it with the menuoptions in
     * R.menu.menu_context_device_list.
     *
     * @param menu  The menu to inflate menuitems into.
     * @param v The view
     * @param menuInfo ConextMenuInfo
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = this.getMenuInflater();
        inflater.inflate(R.menu.menu_context_device_list, menu);
    }

    /**
     * Adds a local association to the device. It's stored in the scanner service
     * which again saves it to disk. <br>
     *
     * A AlertDialogue is launched giving the user the possiblility of entering a name
     * for the beacon and an association.
     *
     * @param beaconNumber The number of beacon selected from the listview.
     */
    public void assLocalAdd (final int beaconNumber) {
        View layout = getLayoutInflater().inflate(R.layout.beacon_add_local_association_alert, (ViewGroup)findViewById(R.id.beacon_add_local));
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        //Get user input
        final EditText inputName = (EditText)layout.findViewById(R.id.beacon_add_local_name);
        final EditText inputAss  = (EditText)layout.findViewById(R.id.beacon_add_local_ass);
        final Spinner  spinner   = (Spinner) layout.findViewById(R.id.beacon_add_notificationSpinner);

        final List<String> list = Arrays.asList("Don't notify", "Less than 1m", "Less than 15m", "Allways notify");

        // create adapter with arraylist and populate spinner with list
        ArrayAdapter adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, list);
        spinner.setAdapter(adapter);

        //get beacon association if allready associated
        JSONObject ass = null;
        try {
            Integer assNum = mService.getAssociationList().contains(mList.getItem(beaconNumber));
            if (assNum != -1) {
                ass = mService.getAssociationList().get(assNum);
                inputName.setText(ass.getString("name"));
                inputAss.setText(ass.getString("value"));
                spinner.setSelection(ass.getInt("notify"));
            }
        }
        catch (Exception e) {
            Log.e("BeaconScanListActivity", "Failed to get association with: " + e.getMessage());
        }

        alert.setView(layout);

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int button) {
                Integer notify  = list.indexOf(spinner.getSelectedItem().toString());
                String name     = inputName.getText().toString();
                String ass      = inputAss.getText().toString();

                try {
                    mService.getAssociationList().add(mList.getItem(beaconNumber), name, ass, notify);
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
     * Removes a local association from the device. The scanner service
     * removes the association from it's association list. <br>
     *
     * Launches an alert dialogue for the user to confirm deletion.
     *
     * @param beaconNumber The number of the beacon the be removed from the list.
     */
    private void assLocalRemove (final int beaconNumber) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Remove beacon association");
        alert.setMessage("Are you sure you want to remove the association?");
        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int button) {
                try {
                    mService.getAssociationList().remove(mList.getItem(beaconNumber).getAddress());
                } catch (JSONException e) {
                    Log.e("BeaconScanListActivity", "Failed to remove association from list: " + e.getMessage());
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
     * Adds a association to the backend. This methods opens an alert-
     * dialogue that prompts the user for beacon name, assocaition and
     * category. This inormation is sent to the back via the BeaconClient
     * class.
     *
     * If connection to the backend can't be achieved, the method call aborts.
     *
     * TODO: the user must be "real", not a dummy
     *
     * @param beaconNumber The beacon selected from the listview.
     * @throws JSONException Might be thrown due to category parsing from JSONArray.
     * @throws InterruptedException Might be thrown due to the network operations.
     */
    private void assRemoteAdd (final int beaconNumber) throws JSONException, InterruptedException {
        // final BeaconClient bClient     = new BeaconClient();
        final BeaconClient bClient = BeaconClient.getInstance();
        JSONObject beaconInfoBackend   = null;

        // Fetch categories from back- end, if this fails, notify user and abort
        final JSONArray categories     = bClient.getCategories(this.getBaseContext());
        if (categories == null) {
            Log.i("BeaconScanListActivity", "Failed to fetch categories from server");
            Toast.makeText(getApplicationContext(), "Failed to fetch categories from server", Toast.LENGTH_SHORT).show();
            return;
        }

        final View layout = getLayoutInflater().inflate(R.layout.beacon_add_remote_association_alert,
                                                  (ViewGroup)findViewById(R.id.categories));
        final Spinner spinner           = (Spinner) layout.findViewById(R.id.categorySpinner);
        AlertDialog.Builder alert       = new AlertDialog.Builder(this);
        final ArrayList<String> list    = new ArrayList<>();

        // Add categories to a list used in the spinner
        try {
            for (int i = 0; i < categories.length(); i++) {
                String topic = ((JSONObject)categories.get(i)).getString("topic");
                list.add(topic);
            }
        } catch (Exception e) {
            Log.e("BeaconScanListActivity", "assRemoteAdd() failed parsing categories with: " + e.getMessage());
            e.printStackTrace();
        }

        // create adapter with arraylist and populate spinner with list
        ArrayAdapter adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, list);
        spinner.setAdapter(adapter);

        // create a textview to put information about the beacon to
        TextView textView = ((TextView) layout.findViewById(R.id.beacon_info));
        StringBuilder beaconInfo = new StringBuilder();
        // get the beacon from the local list and insert data from beacon to view
        final Beacon beacon = mList.getItem(beaconNumber);
        if (beacon != null) {
            String info = "Beacon ID:\n" + beacon.getAddress() +
                    "\nUUID:\n" + beacon.getUuid() +
                    "\nMajor: " + beacon.getMajor() +
                    ", Minor: " + beacon.getMinor();
            beaconInfo.append(info);
        }
        else {
            Log.e("BeaconScanListActivity", "Failed to find the pressed beacon");
            return;
        }

        // attempt to get beaconinformation from the backend system
        final JSONArray beaconHits;
        beaconHits = bClient.getBeacons("", beacon.getUuid(), 0, "", "", String.valueOf(beacon.getMajor()), String.valueOf(beacon.getMinor()), getBaseContext());

        // append beaconinfo to the alert dialogue view
        if (beaconHits != null && beaconHits.length() > 0) {
            Log.e("BeaconListActivity", beaconHits.toString());
            beaconInfoBackend = beaconHits.getJSONObject(0);
            String info = "\n\nBeacon Name:\n" +
                    beaconInfoBackend.get("name") +
                    "\nAssociated to:\n" +
                    beaconInfoBackend.get("url").toString();
            beaconInfo.append(info);
        }
        textView.setText(beaconInfo.toString());
        alert.setView(spinner);

        // get the edittext where the new association is expected, and fill in fields with
        // the information fetched from server about beacon if it exist
        final EditText inputAssociation     = (EditText) layout.findViewById(R.id.associationInput);
        final EditText inputAssociationName = (EditText) layout.findViewById(R.id.associationInputName);

        try {
            if (beaconInfoBackend != null) {
                inputAssociationName.setText(beaconInfoBackend.get("name").toString());
                inputAssociation.setText(beaconInfoBackend.get("url").toString());
                // attempt to find the category of the beacon
                for (int i = 0; i < categories.length(); i++) {
                    if (((JSONObject) categories.get(i)).getInt("id") == beaconInfoBackend.getInt("category_id")) {
                        spinner.setSelection(i);
                        break;
                    }
                }
            }
        }
        catch (Exception e) {
            Log.i("BeaconScanListActivity", "Beacon is unknown to backend, error message: " + e.getMessage());
        }

        //set the cancel- button
        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int button) {
            }
        });

        //set the OK- button
        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int button) {
                Integer retval      = 0;
                String inAssStr     = inputAssociation.getText().toString();
                String inAssNameStr = inputAssociationName.getText().toString();
                int cat             = -1;

                try {
                    cat = Integer.valueOf(((JSONObject)categories.get(list.indexOf(spinner.getSelectedItem().toString()))).get("id").toString());
                } catch (JSONException e) {
                    Log.e("BeaconScanListActivity", "Failed getting categorynumber from JSONArray with: " + e.getMessage());
                }

                // if the beacon is not already registered do this:
                try {
                    if (beaconHits == null || beaconHits.length() == 0) {
                        retval = bClient.createBeacon(inAssNameStr,
                                beacon.getUuid(),
                                inAssStr,
                                cat,
                                beacon.getAddress(),
                                String.valueOf(beacon.getMajor()),
                                String.valueOf(beacon.getMinor()),
                                getBaseContext());
                    }
                        // if the beacon is registered, change it to new values
                    else {
                        retval = bClient.setBeacon(inAssNameStr,
                                beacon.getUuid(),
                                inAssStr,
                                cat,
                                beacon.getAddress(),
                                String.valueOf(beacon.getMajor()),
                                String.valueOf(beacon.getMinor()),
                                getBaseContext());
                    }
                }
                catch (InterruptedException e) {
                    Log.e("BeaconScanListActivity", "Failed to send beacon to backend with: " + e.getMessage());
                }
                if (retval != 200)
                    Toast.makeText(getApplicationContext(), "Failed to update beacon! (" + String.valueOf(retval) + ")", Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(getApplicationContext(), "Beacon have been added.", Toast.LENGTH_SHORT).show();
            }
        });
        try {
            alert.setView(layout);
            final AlertDialog al = alert.create();
            assRemoteSetEditTextOptions(layout, al);
            al.show();
        }
        catch (Exception e) {
            Log.e("BeaconScanListActivity", "alertShow() error: " + e.getMessage());
        }
    }

    /**
     * Sets the EditText listener action, when enter is pressed on the first
     * focus is changed to the next and when enter is clicked in the second
     * the Positivebutton is pressed in the AlertDialog.
     *
     * @param layout The AlertDialogue view.
     * @param alert An instance of the AlertDialog object.
     */
    private void assRemoteSetEditTextOptions (final View layout, final AlertDialog alert) {
        EditText ed = ((EditText) layout.findViewById(R.id.associationInputName));
        ed.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View view, int keyCode, KeyEvent keyevent) {
                //If the keyevent is a key-down event on the "enter" button
                if ((keyevent.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    layout.findViewById(R.id.associationInput).requestFocus();
                    return true;
                }
                return false;
            }
        });
        ed = ((EditText) layout.findViewById(R.id.associationInput));
        ed.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View view, int keyCode, KeyEvent keyevent) {
                //If the keyevent is a key-down event on the "enter" button
                if ((keyevent.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    alert.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
                    return true;
                }
                return false;
            }
        });
    }

    /**
     *
     *  BeaconScanListAdapter to used to populate listview, requires a BeaconList
     *  and the BeaconScannerService as input to the constructor method. <br>
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

        /**
         * Get the beacon at position specified by i.
         *
         * @param i Integer with the beacon number to get.
         * @return Instance of the beacon class at position i.
         */
        public Beacon getItem(int i) {
            return btleDevices.getItem(i);
        }

        /**
         * Returns the number of devices in list.
         *
         * @return Integer with the list size.
         */
        public int getCount() {
            if (btleDevices != null)
                return btleDevices.getCount();
            else
                return 0;
        }

        /**
         * Creates a listitem for the listview of beacons "recorded"
         * since the activity was started. <br>
         *
         * This method is automatically called once per beacon in the
         * list.
         *
         * @param i The position in the list to create a view
         * @param view The view to insert the listitem to.
         * @param viewGroup A viewGroup
         * @return
         */
        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            if (view == null) {
                view = inflater.inflate(R.layout.beacon_listview, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceSignalAddress = (TextView) view.findViewById(R.id.le_range);
                viewHolder.deviceUuid = (TextView) view.findViewById(R.id.le_uuid);
                viewHolder.deviceMajorMinor = (TextView) view.findViewById(R.id.le_uuid_major_minor);
                viewHolder.deviceNotifyName = (TextView) view.findViewById(R.id.le_notify_name);
                viewHolder.devicePic = (ImageView) view.findViewById(R.id.le_pic);
                viewHolder.deviceMac = (TextView) view.findViewById(R.id.le_mac);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            Beacon beacon = this.getItem(i);
            viewHolder.deviceSignalAddress.setText("Distance: " + String.format("%.1f", beacon.getDistance()) + "m");
            viewHolder.deviceMac.setText("MAC: " + beacon.getAddress());
            viewHolder.deviceUuid.setText(beacon.getUuid());
            viewHolder.deviceMajorMinor.setText("Major/Minor: " + beacon.getMajor() +
                                                "/" + beacon.getMinor());

            Integer notify = null;
            try {
                notify = mService.getAssociationList().getNotify(beacon);

                if (notify >= 0) {
                    String tmp = "\nName: " + mService.getAssociationList().getName(beacon) +
                                ", Notify: " +
                                 mService.getAssociationList().getNotificationString(notify);

                    viewHolder.deviceNotifyName.setText(tmp);
                }
                else
                    viewHolder.deviceNotifyName.setText("");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (!mService.getList().contains(beacon.getAddress()))
                viewHolder.devicePic.setImageResource(R.drawable.beacon_not_in_range);
            else
                viewHolder.devicePic.setImageResource(R.drawable.beacon);
            return view;
        }
    }

    /**
     * Helper class for creating the listview.
     */
    private class ViewHolder {
        ImageView   devicePic;
        TextView    deviceSignalAddress;
        TextView    deviceUuid;
        TextView    deviceMac;
        TextView    deviceMajorMinor;
        TextView    deviceNotifyName;
    }
}
