package no.uit.ods.beaconme;


import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;


/**
 *  Author: Espen Mæland Wilhelmsen, espen.wilhelmsen@gmail.com
 *
 * Implements a BTLE Association list which makes it possible to
 * search and find values depending the id of the beacon.
 */
public class BeaconAssociationList {
    private JSONArray   associations;
    private File        assFile;

    /**
     *  Constructor method for Beacon Association List.
     *
     *  It needs a context in order to be able to access files.
     */
    public BeaconAssociationList(Context context) {
        assFile = new File(context.getFilesDir() + "/associations");

        // If the associations file exist, read data from it and insert it to the list
        if (assFile.exists()) {
            try {
                // read the data from file
                FileReader fr = new FileReader(assFile);
                char[] buf = new char[((int) assFile.length())];
                fr.read(buf);
                fr.close();

                // Todo: verify list from disk
                Log.i("BeaconAssociationList", "Reading list from storage(" + String.valueOf(assFile.length()) + "):"  + new String(buf));

                associations = new JSONArray(new String(buf));
                Log.i("BeaconAssociationList", "json: " + associations.toString());
            } catch (Exception e) {
                Log.e("BeaconAssociationList", e.getMessage());
                e.printStackTrace();
            }
        }
        else {
            associations = new JSONArray();
        }
    }

    public void add (String id, String uuid, String name, String value) throws JSONException {
        int i = this.contains(id, uuid);
        // The beacon is not in list and must be added.
        if (i == -1) {
            JSONObject json = new JSONObject();
            json.put("id", id);
            json.put("uuid", uuid);
            json.put("name", name);
            json.put("value", value);
            associations.put(json);
        }
        // The beacon is in list, the value must be updated,
        // update both value and uuid.
        else {
            JSONObject json = ((JSONObject) associations.get(i));
            json.remove("value");
            json.put("value", value);
            json.remove("uuid");
            json.put("uuid", uuid);
            json.remove("name");
            json.put("name", name);
        }
    }

    public void remove (String id, String uuid) throws JSONException {
        for (int i = 0; i < associations.length(); i++) {
            JSONObject json = ((JSONObject) associations.get(i));
            if (json.get("id").equals(id))// || json.get("uuid").equals(uuid))
                associations.remove(i);
        }
    }

    public String getAssociation (String id, String uuid) throws JSONException {
        int i = this.contains(id, uuid);
        // id not found in list
        if (i == -1)
            return null;
        // found the id, returning the value
        else
            return ((JSONObject) associations.get(i)).get("value").toString();
    }

    public String getName (String id, String uuid) throws JSONException {
        int i = this.contains(id, uuid);
        // id not found in list
        if (i == -1)
            return null;
            // found the id, returning the value
        else
            return ((JSONObject) associations.get(i)).get("name").toString();
    }

    public JSONObject get (int i) throws JSONException {
        return ((JSONObject) associations.get(i));
    }

    private int contains (String id, String uuid) throws JSONException {
        int uuidNum = -1;
        for (int i = 0; i < associations.length(); i++) {
            JSONObject ass = ((JSONObject) this.associations.get(i));
            if (ass.get("id").toString().equals(id))
                // Todo, change id or uuid if one is not matching?
                return i;
            // save the position of a matching uuid
//            else if (ass.get("uuid").equals(uuid))
//                uuidNum = i;
        }
        // return the hit on uuid since no hit on id was found
        return uuidNum;
    }

    public int getCount () {
        return associations.length();
    }

    public void commit () throws IOException {
        assFile.delete();
        FileWriter fw = new FileWriter(assFile);
        fw.write(associations.toString());
        fw.flush();
        fw.close();
    }

}
