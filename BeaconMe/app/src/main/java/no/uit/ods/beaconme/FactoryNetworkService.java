package no.uit.ods.beaconme;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * Assumes that whoever calls this service has notified the user to enter username and password
 * such that this service can supply the BeaconFactory with authentication and thus connect to
 * the server.
 * <p>
 * The caller is responsible to verify authentication with the provided method before using any
 * other features of this service.
 *
 * @author 	Vegard Strand (vegard920@gmail.com)
 * @version	1.0
 * @since	2015-02-10
 */
public class FactoryNetworkService extends Service {

    private IBinder         mBinder         = new LocalBinder();
    private BeaconFactory   beaconFactory   = null;
    private boolean         isAuthenticated = false;
    private String          url             = "http://192.168.1.202:3000";

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public IBinder getBinder () {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startID) {
        Toast.makeText(this, "FactoryNetworkService Started", Toast.LENGTH_LONG).show();
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        beaconFactory = new BeaconFactory(url);
        beaconFactory.setUser("admin@server.com", "admin123");

        Log.i("FactoryNetworkService", "onCreate()");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(this, "FactoryNetworkService Stopped", Toast.LENGTH_LONG).show();
    }

    /**
     * Reads the config file, looking for user authentication.
     *
     * @return boolean true if user has authenticated, false otherwise.
     */
    public boolean isAuthenticated() {
        return false;
    }

    public boolean establishConnection() {
        Log.i("FactoryNetworkService", "establishConnection()");
        FactoryEstablish establish = new FactoryEstablish();
        Thread t = new Thread(establish);
        t.start();

        // Sync
        try {
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return establish.getStatus();
    }

    public JSONObject getCategories() {
        Log.i("FactoryNetworkService", "getCategories()");
        FactoryCategories categories = new FactoryCategories();
        Thread t = new Thread(categories);
        t.start();

        // Sync
        try {
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return categories.getCategories();
    }

    public JSONObject getBeacon(String beaconMAC) {
        Log.i("FactoryNetworkService", "getBeacon()");
        FactoryGetBeacon beacon = new FactoryGetBeacon(beaconMAC);
        Thread t = new Thread(beacon);
        t.start();

        // Sync
        try {
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return beacon.getBeacon();
    }

    public boolean setBeacon(String uuid, String bcn_url, int category_id,
                             String mac) {
        Log.i("FactoryNetworkService", "setBeacon()");
        FactorySetBeacon beacon = new FactorySetBeacon(uuid, bcn_url, category_id, mac);
        Thread t = new Thread(beacon);
        t.start();

        // Sync
        try {
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return beacon.getStatus();
    }

    /* INNER FACTORY HELPER CLASSES START */

    private class FactorySetBeacon implements Runnable {
        private volatile boolean status;
        private String  uuid;
        private String  url;
        private int     category_id;
        private String  mac;

        FactorySetBeacon(String uuid, String url, int category_id, String mac) {
            this.uuid = uuid;
            this.url = url;
            this.category_id = category_id;
            this.mac = mac;
        }

        @Override
        public void run() {
            try {
                this.status = beaconFactory.setBeacon(this.uuid, this.url,
                        this.category_id, this.mac);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        public boolean getStatus() {
            return this.status;
        }
    }

    private class FactoryGetBeacon implements Runnable {
        private volatile    JSONObject beacon;
        private String      mac;

        FactoryGetBeacon(String mac) {
            this.mac = mac;
        }

        @Override
        public void run() {
            try {
                this.beacon = beaconFactory.getBeacon(this.mac);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        public JSONObject getBeacon() {
            return this.beacon;
        }
    }

    private class FactoryCategories implements Runnable {
        private volatile JSONObject categories;

        @Override
        public void run() {
            try {
                this.categories = beaconFactory.getCategories();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        public JSONObject getCategories() {
            return this.categories;
        }
    }

    private class FactoryEstablish implements Runnable {
        private volatile boolean status;

        @Override
        public void run() {
            try {
                this.status = beaconFactory.establishConnection();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        public boolean getStatus() {
            return this.status;
        }
    }

    public class LocalBinder extends Binder {
        FactoryNetworkService getService() {
            // Return this instance of LocalService so clients can call public methods
            return FactoryNetworkService.this;
        }
    }

    /* END */
}
