package no.uit.ods.beaconme;

import android.bluetooth.BluetoothDevice;
import java.util.Arrays;

/**
 *  Author: Espen Mæland Wilhelmsen, espen.wilhelmsen@gmail.com
 *
 *  This class implements beacon. It holds a bluetooth device which
 *  is accessed through the bundled methods.
 *
 *  The beacon have a variable threshold which is used to delay the
 *  beacon from being removed the beacon list to prevent beacons from
 *  beeing prematurly removed.
 *
 *  TODO remove parcable, and public variables
 */
public class Beacon {
    private BluetoothDevice btDevice;
    private int             rssi;
    private int             threshold;
    public byte[]          scanRecord;
    final private int       initialThreshold = 2;

    /**
     *  Constructor method.
     *
     *  Requires a BluetoothDevice class item, a signal strength (RSSI)
     *  and a scanRecord which is bundled with the leScan method of the
     *  BluetoothAdapter. (It's the beacon's broadcast packet in raw form).
     *
     * @param device BluetoothDevice from the Android standard library
     * @param signal Integer that represents the signal level in dBm
     * @param sRecord The raw data from the BTLE scan, contains all info from the beacon
     */
    public Beacon(BluetoothDevice device, int signal, byte[] sRecord) {
        this.btDevice = device;
        this.rssi = signal;
        this.scanRecord = sRecord;
        this.threshold = initialThreshold;
    }

    /**
     * Returns the BluetoothDevice class object from the Beacon class.
     *
     * @return The BluetoothDevice inside the Beacon.
     */
    public BluetoothDevice getBtDevice () {
        return btDevice;
    }

    /**
     * Returns the RSSI, which is the signal strength of the beacon.
     *
     * @return Integer with the RSSI of the Beacon.
     */
    public int getRssi () {
        return rssi;
    }

    /**
     * Updates the RSSI variable inside the Beacon class with a new
     * signal strength.
     *
     * @param strength The new RSSI of the beacon.
     */
    public void putRssi (int strength) {
        this.rssi = strength;
    }

    /**
     * Returns the ID (MAC address) of the beacon.
     *
     * @return a String in xx:yy:xx:yy:xx:yy format with the mac
     * address of the beacon.
     */
    public String getId () {
        return this.btDevice.getAddress();
    }

    /**
     * Returns the UUID of the beacon, extracted from the scanRecord
     * of the BluetoothDevice. So the UUID is not parsed to check
     * services offered by the BluetoothDevice. The format of the
     * UUID is set to be standard. I.e with dashes to make it easy
     * to read.
     *
     * @return A string with the UUID of the beacon.
     */
    public String getUuid () {
        // copy out the uuid from the scanrecord
        byte[] a = Arrays.copyOfRange(this.scanRecord, 9, 25);

        // Convert the byte array to hex- string
        StringBuilder sb = new StringBuilder(a.length * 2);
        for(byte b: a)
            sb.append(String.format("%02x", b & 0xff));

        // fix the string syntax to be uuid style
        sb.insert(8, "-");
        sb.insert(13, "-");
        sb.insert(18, "-");
        sb.insert(23, "-");
        String uuid = sb.toString();

        return uuid.toUpperCase();
    }

    /**
     * Returns the Major value of the beacon, extracted from the scanRecord
     * of the BluetoothDevice. It's located in the advertising packet of the
     * beacon in bytes 26-27.
     *
     * If the device is not broadcasting this information
     * 0 is returned.
     *
     * @return Integer with the 2 byte major value.
     */
    public Integer getMajor () {
        if (scanRecord.length >= 27) {
            Integer major = ((scanRecord[25] & 0xff) << 8) | (scanRecord[26] & 0xff);
            return major;
        }
        else
            return 0;
    }

    /**
     * Returns the Minor value of the beacon, extracted from the scanRecord
     * of the BluetoothDevice. It's located in the advertising packet of the
     * beacon in bytes 28-29
     *
     * If the device is not broadcasting this information
     * 0 is returned.
     *
     * @return Integer with the 2 byte minor value.
     */
    public Integer getMinor () {
        // Test that the advertisement packet is big enough to contain minor
        if (scanRecord.length >= 29) {
            Integer major = ((scanRecord[27] & 0xff) << 8) | (scanRecord[28] & 0xff);
            return major;
        }
        else
            return 0;
    }

    /**
     * Returns the value of the threshold variable from the beacon class.
     *
     * @return
     */
    public int getThreshold () {
        return threshold;
    }

    /**
     * Resets the threshold to its default value.
     */
    public void resetThreshold () {
        threshold = initialThreshold;
    }

    /**
     * Decrease the threshold variable by 1.
     */
    public void decreaseThreshold () {
        threshold--;
    }

    /**
     * Updates the device variable in the Beacon class with a new
     * BluetoothDevice.
     *
     * @param device
     */
    public void putDevice (BluetoothDevice device) {
        this.btDevice = device;
    }
}