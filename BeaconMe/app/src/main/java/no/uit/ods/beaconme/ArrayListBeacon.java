package no.uit.ods.beaconme;

import java.util.ArrayList;

/**
* Extension of the ArrayList class adapted to fit the LeBeacon class
*/
public class ArrayListBeacon {
    private ArrayList<LeBeacon> list;

    public ArrayListBeacon() {
        list = new ArrayList<LeBeacon>();
    }

    public void clear () {
        list.clear();
    }

    public LeBeacon get (int i) {
        return ((LeBeacon) list.get(i));
    }

    public void add (LeBeacon beacon) {
        list.add(beacon);
    }

    public void remove (int i) {
        list.remove(i);
    }

    public int size () {
        return list.size();
    }

    public ArrayListBeacon clone () {
        ArrayListBeacon new_list = new ArrayListBeacon();
        new_list.list = ((ArrayList) list.clone());

        return new_list;
    }

    // Check if beacon is contained in list
    public boolean contains (LeBeacon beacon) {

        for (int i = 0; i < this.size(); i++) {
            LeBeacon b = ((LeBeacon) this.get(i));

            // if this is true, the beacon is found in the list
            if (b.getBtDevice().getAddress().equals( beacon.getBtDevice().getAddress() )) {

                // Update the RSSI variable while here..
                b.putRssi(beacon.getRssi());
                b.resetThreshold();
                return true;
            }
        }
        return false;
    }
}
