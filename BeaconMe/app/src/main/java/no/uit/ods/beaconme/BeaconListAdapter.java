package no.uit.ods.beaconme;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 *  Author: Espen Mæland Wilhelmsen, espen.wilhelmsen@gmail.com
 *
 *  Adapterclass to used to populate listview, requires a BeaconList
 *  and the BeaconScannerService as input to the constructor method.
 *
 *  If the Beaconlist in the BeaconScanner service is specified
 */


class BeaconListAdapter extends BaseAdapter {
    private LayoutInflater          inflater;
    private BeaconList              btleDevices;
    private BeaconScannerService    mService;

    // Constructor
    public BeaconListAdapter(Context context, BeaconList list, BeaconScannerService service) {
        super();
        inflater    = LayoutInflater.from(context);
        btleDevices = list;
        mService = service;
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    public Beacon getItem(int i) {
        return btleDevices.getItem(i);
    }

    // Returns the number of devices in list
    public int getCount() {
        if (btleDevices != null)
            return btleDevices.getCount();
        else
            return 0;
    }

    public void setList (BeaconList list) {
        Log.i("BeaconListAdapter", "setList(): " + list.toString());
        btleDevices = list;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolder viewHolder;
        if (view == null) {
            view = inflater.inflate(R.layout.beacon_listview, null);
            viewHolder = new ViewHolder();
            viewHolder.deviceAddress = (TextView) view.findViewById(R.id.le_addr);
            viewHolder.deviceSignal = (TextView) view.findViewById(R.id.le_rssi);
            viewHolder.deviceName = (TextView) view.findViewById(R.id.le_name);
            viewHolder.deviceUuid = (TextView) view.findViewById(R.id.le_uuid);
            viewHolder.deviceAssociation = (TextView) view.findViewById(R.id.le_ass);
            viewHolder.devicePic = (ImageView) view.findViewById(R.id.le_pic);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }

        Beacon beacon = this.getItem(i);
        BluetoothDevice device = beacon.getBtDevice();
        final String deviceName = device.getName();

        if (deviceName != null && deviceName.length() > 0) {
            viewHolder.deviceName.setText(deviceName);
        } else {
            viewHolder.deviceName.setText(R.string.unknown_device);
        }
        viewHolder.deviceAddress.setText(device.getAddress());
        viewHolder.deviceSignal.setText(String.valueOf(beacon.getRssi()));
        viewHolder.deviceUuid.setText("\n" + beacon.getUuid());
        String ass = mService.getAssociation(beacon.getId(), beacon.getUuid());
        if (ass == null)
            ass = "Not Associated";
        viewHolder.deviceAssociation.setText("\n\n" + ass);
        return view;
    }
}

class ViewHolder {
    ImageView   devicePic;
    TextView    deviceName;
    TextView    deviceSignal;
    TextView    deviceAddress;
    TextView    deviceUuid;
    TextView    deviceAssociation;
}

