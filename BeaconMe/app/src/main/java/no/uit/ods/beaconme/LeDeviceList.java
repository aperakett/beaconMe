package no.uit.ods.beaconme;

import android.util.Log;
import android.view.LayoutInflater;


/**
 * Storage class for the bluetooth low energy devices
 *
 */
public class LeDeviceList {
    private ArrayListBeacon btleDevices;
    private LayoutInflater inflater;

    // Constructor
    public LeDeviceList() {
        super();

        btleDevices = new ArrayListBeacon();
    }
    public LeDeviceList(ArrayListBeacon list) {
        super();
        btleDevices = list.clone();

    }
    // Add device to list
    public void addDevice(LeBeacon beacon) {
        if (!btleDevices.contains(beacon)) {
            btleDevices.add(beacon);
        }
    }

    // Clear the list (purge all devices)
    public void clear() {
        // iterate beacons in list
        for (int i = 0; i < btleDevices.size(); i++) {
            LeBeacon beacon = btleDevices.get(i);
            //check if the threshold is 0, if so remove the beacon
            if (beacon.getThreshold() <= 0) {
                Log.i("Beacon.clear()", "Removing beacon number: " + String.valueOf(i));
                btleDevices.remove(i);
                i--;
            }
            else {
                Log.i("Beacon.clear()", "Decreasing counter on beacon: " + String.valueOf(i) + " to " + String.valueOf(beacon.getThreshold()));
                beacon.decreaseThreshold();
            }
        }
    }

    // Returns the number of devices in list
    public int getCount() {
        if (btleDevices != null)
            return btleDevices.size();
        else
            return 0;
    }

    public LeBeacon getItem(int i) {
        return btleDevices.get(i);
    }

    public void setList (ArrayListBeacon list) {
        btleDevices = list;
    }

    public ArrayListBeacon getList () {
        return btleDevices;
    }
}







