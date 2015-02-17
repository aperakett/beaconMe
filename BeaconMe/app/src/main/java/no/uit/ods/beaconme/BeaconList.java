package no.uit.ods.beaconme;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;

/**
 *  Author: Espen Mæland Wilhelmsen, espen.wilhelmsen@gmail.com
 *
 *  Storage class for the bluetooth low energy devices
 */
public class BeaconList {
    private ArrayList<Beacon> list;
    private BeaconListAdapter mAdapter;

    // Constructor
    public BeaconList() {
        super();
        list = new ArrayList<Beacon>();
    }

    // Add device to list, if the Beacon is in the list
    // update the RSSI variable
    public void addDevice(Beacon beacon) {
        Beacon b = get(beacon);
        // If the beacon is not found in the list, add it.
        if (b == null) {
            list.add(beacon);
        }
        else {
            // Update the RSSI variable while here..
            b.putRssi(beacon.getRssi());
            b.resetThreshold();
//            Log.i("BeaconList", "Updating RSSI");
        }

    }

    public BeaconListAdapter getAdapter () {
        return mAdapter;
    }

    public void setAdapter (BeaconListAdapter adapter) {
//        Log.i("BeaconList", "Setting adapter: " + adapter.toString());
        mAdapter = adapter;
    }

    // Clear the list (purge all devices)
    public void clear() {

        // iterate beacons in list
        for (int i = 0; i < list.size(); i++) {
            Beacon beacon = list.get(i);

            //check if the threshold is 0, if so remove the beacon
            if (beacon.getThreshold() <= 0) {
//                Log.i("BeaconList", "clear(), Removing beacon number: " + String.valueOf(i));
                list.remove(i);
                i--;
            }
            else {
//                Log.i("Beacon.clear()", "Decreasing counter on beacon: " + String.valueOf(i) + " to " + String.valueOf(beacon.getThreshold()));
                beacon.decreaseThreshold();
            }
        }
    }

    // Returns the number of devices in list
    public int getCount() {
        if (list != null)
            return list.size();
        else
            return 0;
    }

    // Check if beacon is contained in list
    public boolean contains (Beacon beacon) {
        for (int i = 0; i < list.size(); i++) {
            Beacon b = ((Beacon) list.get(i));
            // if this is true, the beacon is found in the list
            if (b.getBtDevice().getAddress().equals( beacon.getBtDevice().getAddress() )) {
                return true;
            }
        }
        return false;
    }

    // Check if beacon is contained in list
    public Beacon get (Beacon beacon) {
        for (int i = 0; i < list.size(); i++) {
            Beacon b = ((Beacon) list.get(i));
            // if this is true, the beacon is found in the list
            if (b.getBtDevice().getAddress().equals( beacon.getBtDevice().getAddress() )) {
                return b;
            }
        }
        return null;
    }

    public Beacon getItem(int i) {
        return list.get(i);
    }

}
