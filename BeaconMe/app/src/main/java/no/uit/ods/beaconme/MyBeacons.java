package no.uit.ods.beaconme;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import org.json.JSONException;
import org.json.JSONObject;


public class MyBeacons extends ActionBarActivity implements AbsListView.OnItemClickListener {
    private BeaconScannerService mService;
    private MyBeaconListAdapter mAdapter;
    ListView mListView;

    /**
     * onCreate initiates the activity by getting the scanner service from
     * the intent and generating the listview.
     *
     * @param savedInstanceState The bundle that's inserted to the intent.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_beacons);
        Log.i("MyBeacons", "onCreate()");

        // fetch the scan service from arguments
        Bundle bundle = getIntent().getExtras();

        // set up the scan service
        IBinder iBinder = bundle.getBinder("binderScan");
        BeaconScannerService.LocalBinder binderScan = (BeaconScannerService.LocalBinder) iBinder;
        mService = binderScan.getService();

        createListView();
    }

    /**
     * Creates a adapter for populating the listview, sets the adpter to
     * the listview and enables the onClickListener.
     */
    private void createListView () {
        mAdapter = new MyBeaconListAdapter(this, mService.getAssociationList());
        mListView = (ListView) findViewById(R.id.my_beacons_list);
        mListView.setOnItemClickListener(this);
        mListView.setAdapter(mAdapter);
        registerForContextMenu(mListView);

    }

    /**
     * Method is requires for since the MyBeacon class implements
     * AbsListView.OnItemClickListener. This method is not used in the activity.
     *
     * @param parent Not used.
     * @param view Not used.
     * @param position Not used.
     * @param id Not used.
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    }

    /**
     * The method is launched when a long click is done on a listitem in the view. <br>
     *
     * An action is performed depending on the choice in the contextmenu.
     *
     * @param item The MenuItem clicked on.
     * @return If the menuitem is found true is returned.
     */
    public boolean onContextItemSelected(MenuItem item) {
        Log.i("MyBeacons", "onItemClick()");
        // Get the beacon number clicked on
        final int menuNumber = ((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).position;

        switch (item.getItemId()) {
            //remove the beacon
            case R.id.my_beacon_menu_remove:
                AlertDialog.Builder alert = new AlertDialog.Builder(this);
                alert.setTitle("Delete this item from list?");
                alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int button) {
                        try {
                            mService.getAssociationList().remove((mService.getAssociationList().get(menuNumber).get("id").toString()));
                            mAdapter.notifyDataSetChanged();
                        }
                        catch (JSONException e) {
                            Log.e("MyBeacons", "Failed to remove association with: " +  e.getMessage());
                        }
                    }
                });
                alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int button) {

                    }
                });
                alert.show();
                break;
            default:
                return false;
        }
        return true;
    }

    /**
     * Creates the context menu by inflating the menu in
     * R.menu.menu_context_my_beacons
     *
     * @param menu ContextMenu
     * @param v View
     * @param menuInfo ContextMenu.ContextMenuInfo
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        // TODO: fix content of menu
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = this.getMenuInflater();
        inflater.inflate(R.menu.menu_context_my_beacons, menu);
    }

    /**
     * Adapter- class instance used to populate the listview.
     */
    private class MyBeaconListAdapter extends BaseAdapter {
        private LayoutInflater          inflater;
        private BeaconAssociationList   btleDevices;

        // Constructor
        public MyBeaconListAdapter(Context context, BeaconAssociationList list) {
            super();
            inflater    = LayoutInflater.from(context);
            btleDevices = list;
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        public JSONObject getItem(int i) {
            try {
                return btleDevices.get(i);
            }
            catch (JSONException e) {
                Log.e("MyBeacons", "Failed to getItem, with error message: " + e.getMessage());
            }
            return null;
        }

        public int getCount () {
            return btleDevices.getCount();
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolderBeacon viewHolder;
            if (view == null) {
                view = inflater.inflate(R.layout.my_beacon_listview, null);
                viewHolder = new ViewHolderBeacon();
                viewHolder.deviceName = (TextView) view.findViewById(R.id.my_beacon_name);
                viewHolder.deviceValue = (TextView) view.findViewById(R.id.my_beacon_value);
                viewHolder.deviceId = (TextView) view.findViewById(R.id.my_beacon_addr);
                viewHolder.deviceUuid = (TextView) view.findViewById(R.id.my_beacon_uuid);
                viewHolder.deviceUuidMajorMinor = (TextView) view.findViewById(R.id.my_beacon_uuid_major_minor);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolderBeacon) view.getTag();
            }

            try {
                JSONObject ass = btleDevices.get(i);
                viewHolder.deviceName.setText(ass.get("name").toString());
                viewHolder.deviceValue.setText(ass.get("value").toString());
                viewHolder.deviceId.setText(ass.get("id").toString());
                viewHolder.deviceUuid.setText(ass.get("uuid").toString());
                viewHolder.deviceUuidMajorMinor.setText("Major: " + ass.get("major").toString() +
                                                        ", Minor: " + ass.get("minor").toString());

            }
            catch (Exception e) {
                Log.e("MyBeacons", "Failed to create view for list- item, with error: " + e.getMessage());
            }

            return view;
        }
    }

    /**
     *  Helper class to populate a listitem in the listview.
     */
    private class ViewHolderBeacon {
        TextView    deviceName;
        TextView    deviceValue;
        TextView    deviceUuid;
        TextView    deviceUuidMajorMinor;
        TextView    deviceId;
    }
}
