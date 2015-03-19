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
import android.widget.Toast;
import org.json.JSONException;


/**
 *  Author: Espen Mæland Wilhelmsen, espen.wilhelmsen@gmail.com
 *
 *  This class implements a scanner service that maintains a list of
 *  nearby beacons in a LeDeviceList class list.
 *
 *  The class checks for both BT and BTLE support
 */
public class BeaconScannerService extends Service {
    private Handler handler;
    private BluetoothAdapter btAdapter;
    private BeaconList btleDeviceList;
    private BeaconAssociationList associationList;
    private int scanPeriod;
    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        BeaconScannerService getService() {
            // Return this instance of LocalService so clients can call public methods
            return BeaconScannerService.this;
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
                    final Beacon beacon = new Beacon(device, rssi, scanRecord);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            btleDeviceList.addDevice(beacon);
                            // TODO, clean up the beacon notification
                            try {
                                associationList.notify(beacon, getBaseContext());
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    });

                }

            };

    // Constructor method.
    public BeaconScannerService() {
        super();
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
        handler = new Handler(thread.getLooper());


        // Set the default scan interval in ms.
        scanPeriod = 2500;

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

        // initialize the beacon device list
        btleDeviceList = new BeaconList();

        // set up the association list
        associationList = new BeaconAssociationList(getApplicationContext());

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("BeaconScanService", "Starting service");
        // TODO Evalue sticky vs not sticky....
        return START_NOT_STICKY;
    }
    @Override
    public void onDestroy () {
        Log.i("BeaconScanService", "onDestroy()");
    }

    public int getNumberOfDevices () {
        return btleDeviceList.getCount();
    }

    // Returns the beacon list from the service.
    public BeaconList getList () {
        return btleDeviceList;
    }

    // Toggles a scan, if a scan is running, the running scan is stopped
    // to prevent errors.
    public void scan() {

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                btAdapter.stopLeScan(btleScanCallback);
            }
        }, scanPeriod);

        // clear beacons out of range before starting scan
        btleDeviceList.clear();
        btAdapter.startLeScan(btleScanCallback);
    }

    public void addAssociation(Beacon beacon, String name, String association, Integer notify) {
        try {
            associationList.add(beacon, name, association, notify);
        }
        catch (Exception e) {
            Log.e("BeaconScannerService", e.getMessage());
        }
    }


    public String getAssociation(Beacon beacon) {
        try {
            return associationList.getAssociation(beacon);
        }
        catch (Exception e) {
            Log.e("BeaconScannerService", e.getMessage());
        }
        return null;
    }

    public String getAssociationName(Beacon beacon) {
        try {
            return associationList.getName(beacon);
        }
        catch (Exception e) {
            Log.e("BeaconScannerService", e.getMessage());
        }
        return null;
    }

    public Integer getAssociationNotify(Beacon beacon) {
        try {
            return associationList.getNotify(beacon);
        }
        catch (Exception e) {
            Log.e("BeaconScannerService", e.getMessage());
        }
        return -1;
    }

    /**
     * TODO fix uuid association removal???
     * @param id
     * @param uuid
     */
    public void removeAssociation(String id, String uuid) {
        try {
            associationList.remove(id);
        } catch (JSONException e) {
            Log.e("BeaconScannerService", "failed to remove association: " + e.getMessage());
        }
    }

    public void commitAssociation() {
        try {
            associationList.commit();
        }
        catch (Exception e) {
            Log.e("BeaconScannerService", "failed to commit associations: " + e.getMessage());
        }
    }

    public BeaconAssociationList getAssociations() {
        return associationList;
    }
}

