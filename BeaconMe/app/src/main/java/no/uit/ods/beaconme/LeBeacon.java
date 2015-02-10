package no.uit.ods.beaconme;

import android.bluetooth.BluetoothDevice;
import android.os.Parcel;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.widget.Toast;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.UUID;

/*
 *  Author: Espen Mæland Wilhelmsen, espen.wilhelmsen@gmail.com
 *
 *  This class implements beacon. It holds a bluetooth device which
 *  is accessed through the bundled methods.
 *
 *  The threshold is used to delay the beacon from being removed the
 *  beacon list.
 *
 */
public class LeBeacon implements Parcelable {
    private BluetoothDevice btDevice;
    private int             rssi;
    private int             threshold;
    public byte[]          scanRecord;
    final private int       initialThreshold = 2;

    public LeBeacon (BluetoothDevice device, int signal, byte[] sRecord) {
        this.btDevice = device;
        this.rssi = signal;
        this.scanRecord = sRecord;
        this.threshold = initialThreshold;
    }

    public BluetoothDevice getBtDevice () {
        return btDevice;
    }

    public int getRssi () {
        return rssi;
    }

    public void putRssi (int strength) {
        this.rssi = strength;
    }

    public String getId () {
        return this.btDevice.getAddress();
    }

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

    public int getThreshold () {
        return threshold;
    }

    public void resetThreshold () {
        threshold = initialThreshold;
    }

    public void decreaseThreshold () {
        threshold--;
    }

    public void putDevice (BluetoothDevice device) {
        this.btDevice = device;
    }

    protected LeBeacon(Parcel in) {
        btDevice = (BluetoothDevice) in.readValue(BluetoothDevice.class.getClassLoader());
        rssi = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(btDevice);
        dest.writeInt(rssi);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<LeBeacon> CREATOR = new Parcelable.Creator<LeBeacon>() {
        @Override
        public LeBeacon createFromParcel(Parcel in) {
            return new LeBeacon(in);
        }

        @Override
        public LeBeacon[] newArray(int size) {
            return new LeBeacon[size];
        }
    };
}