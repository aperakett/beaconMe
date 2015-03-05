package no.uit.ods.beaconme;

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

    /**
     *  Constructor method for the class.
     *
     *  It creates a new instance of the ArrayList internally.
     */
    public BeaconList() {
        super();
        list = new ArrayList<>();
    }

    /**
     * Adds an instance of the Beacon class to the list.
     *
     * If the beacon is already in the list by first checking
     * with the get method if the beacon is previously inserted
     * in the list. If it is, the RSSI (signal level) of the beacon is
     * updated, and the threshold of the beacon is reset (the beacon is
     * visible, therefore the threshold is reset). If the beacon is not
     * previously added, it's simply added.
     *
     * The point of the threshold is to add delay to the removal of
     * a beacon in case for some reason a beacon is not seen in a scan-
     * period thus preventing beacons from being inserted and removed
     * due to this.
     *
     * @param beacon A instance of the beacon class that is to be added
     *               to the BeaconList.
     */
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

    /**
     * Iterate the BeaconList and remove all beacons that have a
     * threshold of 0 or less.
     *
     * This method should be used between all scan intervals to remove
     * beacons that are out of range. The threshold provides a certain
     * delay before the beacon is removed.
     */
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

    /**
     * Returns the number of Beacon class objects in the list
     * to the caller.
     *
     * @return Integer with beacons in the list.
     */
    public int getCount() {
        if (list != null)
            return list.size();
        else
            return 0;
    }

    /**
     * Check if the beacon given as argument is contained
     * in the BeaconList. Since the usage of this is intended
     * for a device. The beacon is just checked for presence
     * on the MAC address, not the UUID + major + minor.
     *
     * @param beacon A instance of the Beacon class to check for presence.
     * @return Returns a boolean indicating the presence.
     */
    public boolean contains (Beacon beacon) {
        for (int i = 0; i < list.size(); i++) {
            Beacon b = list.get(i);
            // if this is true, the beacon is found in the list
            if (b.getAddress().equals( beacon.getAddress() )) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if the beacon given as argument is contained
     * in the BeaconList. Since the usage of this is intended
     * for a device. The beacon is just checked for presence
     * on the MAC address, not the UUID + major + minor.
     *
     * @param mac A String with the mac address of the beacon, the mac must
     *            be in xx:yy:xx:yy:xx:yy format.
     * @return Returns a boolean indicating the presence.
     */
    public boolean contains (String mac) {
        for (int i = 0; i < list.size(); i++) {
            Beacon b = list.get(i);
            if (b.getAddress().equals(mac))
                return true;
        }
        return false;
    }

    // fetch a beacon from the list

    /**
     * Gets a beacon from the BeaconList class, this method is very
     * similar to contains, expect that it returns a beacon.
     *
     * @param beacon An instance of the Beacon class to retrieve.
     * @return Returns the beacon, or null if it's not found.
     */
    public Beacon get (Beacon beacon) {
        for (int i = 0; i < list.size(); i++) {
            Beacon b = list.get(i);
            // if this is true, the beacon is found in the list
            if (b.getAddress().equals( beacon.getAddress() )) {
                return b;
            }
        }
        return null;
    }

    /**
     * Gets the Beacon at a certain position in the list.
     *
     * @param i Integer with the beacon number to get.
     * @return A intance of the Beacon class that corresponds to the
     * argument i.
     * @throws IndexOutOfBoundsException Can be thrown if access outside
     * the bounds of the list is attempted.
     */
    public Beacon getItem(int i) throws IndexOutOfBoundsException {
        return list.get(i);
    }

}
