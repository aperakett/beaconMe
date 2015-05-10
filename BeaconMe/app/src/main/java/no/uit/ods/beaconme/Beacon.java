package no.uit.ods.beaconme;

import android.bluetooth.BluetoothDevice;
import java.util.Arrays;

/**
 *
 *  This class implements beacon. It holds the information about the beacon
 *   which is accessed through the bundled methods. <br>
 *
 *  The beacon have a variable threshold which is used to delay the
 *  beacon from being removed the beacon list to prevent beacons from
 *  beeing prematurly removed. <br>
 *
 *  Author: Espen Mæland Wilhelmsen, espen.wilhelmsen@gmail.com
 */
public class Beacon {
    private String      name;
    private String      mac;
    private String      uuid;
    private String      category;
    private String      url;
    private int         major;
    private int         minor;
    private int         signalLevel;
    private int         rssi;
    private int         threshold;
    private boolean     updated;
    final private int   initialThreshold = 3;

    /**
     * Constructor method. <br>
     *
     * Requires a BluetoothDevice class item, a signal strength (RSSI)
     * and a scanRecord which is bundled with the leScan method of the
     * BluetoothAdapter. (It's the beacon's broadcast packet in raw form).
     *
     * @param device  BluetoothDevice from the Android standard library
     * @param signal  Integer that represents the signal level in dBm
     * @param sRecord The raw data from the BTLE scan, contains all info from the beacon
     */
    public Beacon(BluetoothDevice device, int signal, byte[] sRecord) {
        parseBeacon(sRecord);
        this.name      = device.getName();
        this.mac       = device.getAddress();
        this.rssi      = signal;
        this.threshold = initialThreshold;
        this.updated   = false;
        this.category  = null;
        this.url       = null;
    }

    /**
     * Setter for category variable.
     *
     * @param category String representing the category.
     */
    public void setCategory(String category) {
        this.category = category;
    }

    /**
     * Getter for category variable.
     *
     * @return Returns String representing the category.
     */
    public String getCategory() {
        return this.category;
    }

    /**
     * Setter for the url variable.
     *
     * @param url String representing the url.
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Getter for the url variable.
     *
     * @return Returns String representing the url.
     */
    public String getUrl() {
        return this.url;
    }

    /**
     * Set the uuid from beacon extracted from the scanRecord
     * of the BluetoothDevice. So the UUID is not parsed to check
     * services offered by the BluetoothDevice. The format of the
     * UUID is set to be standard. I.e with dashes to make it easy
     * to read. <br>
     *
     * Set the major from beacon extracted from the scanRecord
     * of the BluetoothDevice. It's located in the advertising packet of the
     * beacon in bytes 26-27.
     * If the device is not broadcasting this information
     * 0 is set. <br>
     *
     * Sets the minor extracted from the scanRecord
     * of the BluetoothDevice. It's located in the advertising packet of the
     * beacon in bytes 28-29
     * If the device is not broadcasting this information
     * 0 is set. <br>
     *
     * Sets the txpower the beacon is transmitting with, found at byte
     * 30 in the advertisment packet.
     *
     * @param sRecord Byte array with the advertisment packet from the beacon.
     */
    private void parseBeacon (byte[] sRecord) {
        // copy out the uuid from the scanrecord
        byte[] a = Arrays.copyOfRange(sRecord, 9, 25);

        // Convert the byte array to hex- string
        StringBuilder sb = new StringBuilder(a.length * 2);
        for (byte b : a)
            sb.append(String.format("%02x", b & 0xff));

        // fix the string syntax to be uuid style
        sb.insert(8, "-");
        sb.insert(13, "-");
        sb.insert(18, "-");
        sb.insert(23, "-");
        this.uuid = sb.toString().toUpperCase();

        // set the major
        // Test that the advertisement packet is big enough to contain major
        if (sRecord.length >= 27) {
            this.major = ((sRecord[25] & 0xff) << 8) | (sRecord[26] & 0xff);
        } else
            this.major = 0;

        // set the minor
        // Test that the advertisement packet is big enough to contain minor
        if (sRecord.length >= 29) {
            this.minor = ((sRecord[27] & 0xff) << 8) | (sRecord[28] & 0xff);
        } else
            this.minor = 0;

        // set the signallevel of the beacon
        if (sRecord.length >= 30)
            this.signalLevel = 0xff - (sRecord[29] & 0xff);
        else
            this.signalLevel = 0xff;
    }

    /**
     * Returns the updates variable. <br>
     *
     * Used to prevent updating beacons multiple times per scan
     * interval.
     *
     * @return
     */
    public boolean getUpdated() {
        return this.updated;
    }

    public void setUpdated(boolean b) {
        this.updated = b;
    }

    /**
     * Returns the RSSI, which is the signal strength of the beacon.
     *
     * @return double with the RSSI of the Beacon.
     */
    public int getRssi() {
        return rssi;
    }

    /**
     * Updates the RSSI variable inside the Beacon class with a new
     * signal strength.
     *
     * @param strength The new RSSI of the beacon.
     */
    public void putRssi(int strength) {
        this.rssi = strength;
    }

    /**
     * Returns the ID (MAC address) of the beacon.
     *
     * @return A String in xx:yy:xx:yy:xx:yy format with the mac
     * address of the beacon.
     */
    public String getAddress() {
        return this.mac;
    }

    /**
     * Returns the name of the beacon.
     *
     * @return A String in with the name of the beacon
     */
    public String getName() {
        return this.name;
    }

    /**
     * Returns the UUID of the beacon.
     *
     * @return A string with the UUID of the beacon.
     */
    public String getUuid() {
        return this.uuid;
    }

    /**
     * Returns the Major value of the beacon.
     *
     * @return Integer with the 2 byte major value.
     */
    public Integer getMajor() {
        return this.major;
    }

    /**
     * Returns the Minor value of the beacon.
     *
     * @return Integer with the 2 byte minor value.
     */
    public Integer getMinor() {
        return this.minor;
    }

    /**
     * Get the signal level (txPower) the beacon is transmitting with.
     *
     * @return Integer with the transmission level.
     */
    public Integer getSignalLevel() {
        return this.signalLevel;
    }

    /**
     * Gets the estimated distance from the beacon based on the
     * beacons advertised signal level at 1m and the rssi registered
     * on the device. <br>
     *
     * If it is not possible to range estimate the device, infinity
     * is returned.
     *
     * @return A double with the distance from beacon in meters.
     */
    public double getDistance() {
        if (getRssi() == 0 || getSignalLevel() == 0xff) {
            return Float.POSITIVE_INFINITY; // if we cannot determine accuracy, return infinity
        }
        double ratio = getRssi() * 1.0/getSignalLevel();
        if (ratio < 1.0) {
            return Math.pow(ratio,10);
        }
        else {
            return (0.89976)*Math.pow(ratio,7.7095) + 0.111;
        }
    }

    /**
     * Returns the value of the threshold variable from the beacon class.
     *
     * @return Integer with the threshold of the beacon.
     */
    public int getThreshold() {
        return threshold;
    }

    /**
     * Resets the threshold  to its default value.
     */
    public void resetThreshold() {
        this.threshold = initialThreshold;
    }

    /**
     * Decrease the threshold variable by 1.
     */
    public void decreaseThreshold() {
        threshold--;
    }

}
