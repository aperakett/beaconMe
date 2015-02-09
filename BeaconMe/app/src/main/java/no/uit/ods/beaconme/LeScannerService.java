package no.uit.ods.beaconme;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;
import android.widget.BaseAdapter;
import android.widget.Toast;

/*
 *  Author: Espen Mæland Wilhelmsen, espen.wilhelmsen@gmail.com
 *
 *  This class implements a scanner service that maintains a list of
 *  nearby beacons in a LeDeviceList class list.
 *
 *  The class checks for both BT and BTLE support
 */
public class LeScannerService extends Service {
    private Handler handler;
    private BluetoothAdapter btAdapter;
    private LeBeaconList btleDeviceList;
    public LeAssociationList associationList;

    //TODO implement as list (mAdapter)
    private BaseAdapter mAdapter;
    private boolean scanning;
    private int sleepPeriod;

    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        LeScannerService getService() {
            // Return this instance of LocalService so clients can call public methods
            return LeScannerService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public IBinder getBinder () {
        return mBinder;
    }

    // Scan callback- interface, stores the beacons in the list
    private BluetoothAdapter.LeScanCallback btleScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    final LeBeacon beacon = new LeBeacon(device, rssi);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {

                            btleDeviceList.addDevice(beacon);
                            if (mAdapter != null)
                                mAdapter.notifyDataSetChanged();
                        }
                    });

                }

            };

    // Constructor method.
    public LeScannerService() {
        super();

    }

    public void setAdapter (BaseAdapter adapter) {
        mAdapter = adapter;
    }

    @Override
    public void onCreate () {
        HandlerThread thread = new HandlerThread("ServiceStartArguments", 10);
        thread.start();

        //Check if BTLE is supported by device
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.btle_not_supported, Toast.LENGTH_SHORT).show();
        }

        // create the handler used for timer aborted scanning...
        handler = new Handler();

        // Set the default scan interval in ms.
        sleepPeriod = 2500;

        // Set up the bluetooth adapter through manager
        BluetoothManager btMan = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btMan.getAdapter();

        // check if BT is supported
        if (btAdapter == null) {
            Toast.makeText(this, R.string.bt_not_supported, Toast.LENGTH_SHORT).show();
            return;
        }

        // BT is supported, but disabled, try enabling it
        else if (!btAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableBtIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(enableBtIntent);
        }

        // Initiate the scanning variable
        scanning = false;

        btleDeviceList = new LeBeaconList();

        // set up the association list
        LeAssociationList associationList = new LeAssociationList(getApplicationContext());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
//        Log.i("ON_START_COMMAND", "Starting?");
        // TODO Evalue sticky vs not sticky....
        return START_NOT_STICKY;
    }

    public int getNumberOfDevices () {
        return btleDeviceList.getCount();
    }

    // Returns the beacon list from the service.
    public ArrayListBeacon getList () {
        return btleDeviceList.getList();
    }

    // Toggles a scan, if a scan is running, the running scan is stopped
    // to prevent errors.
    public void scan() {
        if (!scanning) {
            // Stops scanning after a pre-defined scan period.
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    scanning = false;
                    btAdapter.stopLeScan(btleScanCallback);
                }
            }, sleepPeriod);

            scanning = true;

            // clear beacons out of range before starting scan
            btleDeviceList.clear();
            btAdapter.startLeScan(btleScanCallback);
        }
        else {
            scanning = false;
            btAdapter.stopLeScan(btleScanCallback);
        }

    }
}

