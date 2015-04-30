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
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import org.json.JSONException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


/**
 *  This class implements a scanner service that maintains a list of
 *  nearby beacons in a LeDeviceList class list. <br>
 *
 *  The class checks for both BT and BTLE support. <br>
 *
 *  Author: Espen Mæland Wilhelmsen, espen.wilhelmsen@gmail.com
 */
public class BeaconScannerService extends Service {
    private Handler handler;
    private BluetoothAdapter btAdapter;
    private BeaconList btleDeviceList;
    private BeaconAssociationList associationList;
    private int scanPeriod;
    private ScheduledExecutorService scheduler;
    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();

    // Constructor method.
    public BeaconScannerService() {
        super();
    }

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

    /**
     * Return the communication channel to the service.
     * This is what the client use to communicate with the service.
     *
     * @param intent The intent the service is started with.
     * @return Returns the service IBinder.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /**
     * Help function to get the IBinder from the service.
     *
     * @return Returns the IBinder from the service.
     */
    public IBinder getBinder () {
        return mBinder;
    }

    /**
     * The onCreate method is automatically called when starting the
     * service. <br>
     *
     * Sets up the service by: <br>
     *  - Checking for BLuetooth LE support. <br>
     *  - Sets up a thread for running the Bluetooth LE scans in. <br>
     *  - Sets up a BluetoothAdapter. <br>
     *  - Checks if Bluetooth is supported. <br>
     *  - Checks if Bluetooth is enabled on the device, if not a request to
     *  enable it is launced. <br>
     *  - Initialize the BeaconList. <br>
     *  - Initialize the Association list.
     */
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
        scanPeriod = Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(this).getString("sync_settings", "2500"));
Log.e("AAAAAAA", String.valueOf(scanPeriod));
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

        schedulePeriodicalScan();
    }

    /**
     * Automatically run when the service is started.
     *
     * @param intent The intent the service is started with.
     * @param flags x
     * @param startId x
     * @return Returns START_NOT_STICKY
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("BeaconScanService", "Starting service");
        return START_NOT_STICKY;
    }

    /**
     * Automatically run when service is destroyed.
     *
     * Stops the scan schedule.
     */
    @Override
    public void onDestroy () {
        scheduler.shutdown();
        Log.i("BeaconScanService", "onDestroy()");
    }

    /**
     * Returns the beacon list from the service.
     *
     * @return Returns a list with all beacons in proximity.
     */
    public BeaconList getList () {
        return btleDeviceList;
    }

    public BeaconAssociationList getAssociationList() {
        return associationList;
    }

    /**
     * Run a single scan, and add the beacons to the beacon list
     * in the service.
     */
    private void scan() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                btAdapter.stopLeScan(btleScanCallback);
            }
        }, scanPeriod - 200);

        // clear beacons out of range before starting scan
        btleDeviceList.clear();
        btAdapter.startLeScan(btleScanCallback);
    }

    /**
     * Schedules periodical BTLE scan to run in a thread.
     *
     */
    private void schedulePeriodicalScan () {
        scheduler = Executors.newScheduledThreadPool(1);
        final Runnable scan = new Runnable() {
            @Override
            public void run() {
                scan();
            }
        };

        final Thread t = new Thread () {
            public void run () {
                try {
                    scan.run();
                } catch (Exception e) {

                }
            }
        };

        final ScheduledFuture scannerHandle = scheduler.scheduleAtFixedRate(t, scanPeriod, scanPeriod, TimeUnit.MILLISECONDS);
    }

    /**
     * Scan callback- interface, stores the discovered beacons
     * in the list.
     */
    private BluetoothAdapter.LeScanCallback btleScanCallback =
        new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                final Beacon beacon = new Beacon(device, rssi, scanRecord);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        btleDeviceList.addDevice(beacon);
                    }
                });

            }

        };
}

