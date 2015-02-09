package no.uit.ods.beaconme;

import android.bluetooth.BluetoothDevice;
import android.os.Parcel;
import android.os.Parcelable;

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
    final private int       initialThreshold = 2;

    public LeBeacon (BluetoothDevice device, int signal) {
        this.btDevice = device;
        this.rssi = signal;
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
        return this.btDevice.getUuids().toString();
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