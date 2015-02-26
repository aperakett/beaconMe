package no.uit.ods.beaconme;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;

/**
 *  Author: Espen Mæland Wilhelmsen, espen.wilhelmsen@gmail.com
 *
 *  Storage class for the bluetooth low energy devices.
 *  The class is specialised to hold the Beacon class objects.
 *
 *  The add method adds a beacon only after checking if the address (MAC)
 *  of the beacon is allready in the list.
 *
 *  The clear method of the class is based on the threshold
 *  variable in the Beacon class, if a beacon has a threshold of 0
 *  it's removed, if not it left in the list and the threshold is
 *  decreased.
 *
 */
public class BeaconList {
    private ArrayList<Beacon> list;

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
        }

    }

    // Clear the list (purge all devices)
    public void clear() {

        // iterate beacons in list
        for (int i = 0; i < list.size(); i++) {
            Beacon beacon = list.get(i);

            //check if the threshold is 0, if so remove the beacon
            if (beacon.getThreshold() <= 0) {
                list.remove(i);
                i--;
            }
            else {
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

    // Check if beacon is contained in list based on MAC
    public boolean contains (String mac) {
        for (int i = 0; i < list.size(); i++) {
            Beacon b = list.get(i);
            if (b.getBtDevice().getAddress().equals(mac))
                return true;
        }
        return false;
    }

    // fetch a beacon from the list
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

    public Beacon getItem(int i) throws IndexOutOfBoundsException {
        return list.get(i);
    }

}
