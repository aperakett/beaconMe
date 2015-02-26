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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import org.json.JSONException;
import org.json.JSONObject;


public class MyBeacons extends ActionBarActivity implements AbsListView.OnItemClickListener {
    private BeaconScannerService mService;
    private FactoryNetworkService mFNetwork;
    private ListView mListView;
    private MyBeaconListAdapter mAdapter;


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

        // set up the network service
        iBinder = bundle.getBinder("binderNetwork");
        FactoryNetworkService.LocalBinder binderNetwork = (FactoryNetworkService.LocalBinder) iBinder;
        mFNetwork = binderNetwork.getService();


        // TODO: generate the listview
        createListView();

    }

    private void createListView () {
        mAdapter = new MyBeaconListAdapter(this, mService.getAssociations());
        mListView = (ListView) findViewById(R.id.my_beacons_list);
        mListView.setOnItemClickListener(this);
        mListView.setAdapter(mAdapter);
        registerForContextMenu(mListView);

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    }

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
                            mService.removeAssociation(mService.getAssociations().get(menuNumber).get("id").toString(), mService.getAssociations().get(menuNumber).get("uuid").toString());
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
        }
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        // TODO: fix content of menu
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = this.getMenuInflater();
        inflater.inflate(R.menu.menu_context_my_beacons, menu);
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        return true;
//    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//
//        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }
//        return super.onOptionsItemSelected(item);
//    }

    private class MyBeaconListAdapter extends BaseAdapter {
        private LayoutInflater          inflater;
        private BeaconAssociationList   btleDevices;
        private BeaconScannerService    mService;

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
                viewHolder.deviceValue = (TextView) view.findViewById(R.id.my_beacon_value);
                viewHolder.deviceId = (TextView) view.findViewById(R.id.my_beacon_addr);
                viewHolder.deviceUuid = (TextView) view.findViewById(R.id.my_beacon_uuid);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolderBeacon) view.getTag();
            }

            try {
                JSONObject ass = btleDevices.get(i);
                viewHolder.deviceValue.setText(ass.get("value").toString());
                viewHolder.deviceId.setText(ass.get("id").toString());
                viewHolder.deviceUuid.setText(ass.get("uuid").toString());

            }
            catch (Exception e) {
                Log.e("MyBeacons", "Failed to create view for list- item, with error: " + e.getMessage());
            }

            return view;
        }
    }

    private class ViewHolderBeacon {
        ImageView   devicePic;
        TextView    deviceValue;
        TextView    deviceUuid;
        TextView    deviceId;
    }
}
