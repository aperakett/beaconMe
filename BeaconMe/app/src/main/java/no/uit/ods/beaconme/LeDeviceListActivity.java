package no.uit.ods.beaconme;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ListActivity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


public class LeDeviceListActivity extends Activity {
    private LeScannerService mService;
    private LeDeviceListAdapter mAdapter;
    private ArrayListBeacon mServiceList;
    private ArrayListBeacon recordedDeviceList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_le_device_list);

        // fetch the scan service from arguments
        Bundle bundle = getIntent().getExtras();

        if (bundle == null)
            Log.i("Device List Activity", "bundle == null :(");

        IBinder iBinder = bundle.getBinder("binder");
        if (iBinder == null)
            Log.i("Device List Activity", "binder == null :(");

        LeScannerService.LocalBinder binder = (LeScannerService.LocalBinder) iBinder;
        mService = binder.getService();

        // todo get the association list....

        // TODO FIX make the list "recordable"
        // Create Adapter and set the list
        mAdapter = new LeDeviceListAdapter();
        mServiceList = mService.getList();
        mAdapter.setList(mServiceList);
        mService.setAdapter(mAdapter);

        // create a listview and attach the adapter
        ListView mList = (ListView) findViewById(android.R.id.list);
        mList.setAdapter(mAdapter);

    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//
//
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_le_device_list, menu);
//        Log.i("CREATING LE DEVICE ACTIVITY", "onCreateOptionsMenu");
//
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//
//        Log.i("CREATING LE DEVICE ACTIVITY", "onOptionsItemSelected");
//
//        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }


    //    class LeDeviceListAdapter extends com.example.espen.btlescan.LeDeviceList {
    class LeDeviceListAdapter extends BaseAdapter {
        private LayoutInflater inflater;
        private ArrayListBeacon btleDevices;

        // Constructor
        public LeDeviceListAdapter() {
            super();
            inflater = getLayoutInflater();
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        public LeBeacon getItem(int i) {
            return btleDevices.get(i);
        }

        // Returns the number of devices in list
        public int getCount() {
            if (btleDevices != null)
                return btleDevices.size();
            else
                return 0;
        }

        public void setList (ArrayListBeacon list) {
            btleDevices = list;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            if (view == null) {
                view = inflater.inflate(R.layout.fragment_ledevicelist_list, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.le_addr);
                viewHolder.deviceSignal = (TextView) view.findViewById(R.id.le_rssi);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.le_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            LeBeacon beacon = this.getItem(i);
            BluetoothDevice device = beacon.getBtDevice();
            final String deviceName = device.getName();

            if (deviceName != null && deviceName.length() > 0) {
                viewHolder.deviceName.setText(deviceName);
            } else {
                viewHolder.deviceName.setText(R.string.unknown_device);
            }
            viewHolder.deviceAddress.setText(device.getAddress());
            viewHolder.deviceSignal.setText(String.valueOf(beacon.getRssi()));
            return view;
        }
    }

    static class ViewHolder {
        TextView deviceName;
        TextView deviceSignal;
        TextView deviceAddress;
    }

}
