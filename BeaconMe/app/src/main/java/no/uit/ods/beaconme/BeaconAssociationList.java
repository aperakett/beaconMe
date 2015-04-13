package no.uit.ods.beaconme;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;


/**
 * Author: Espen Maeland Wilhelmsen, espen.wilhelmsen@gmail.com<br>
 *
 * Implements a BTLE Association list which makes it possible to
 * search and find values depending the id and uuid of the beacon.<br>
 *
 * Searching is first done on ID (MAC), if there is no hit, the
 * search continues on UUID. This makes it possible to replacing
 * a beacon while still offering the same information.<br>
 *
 * The associations is stored in an instance of the JSONObject which
 * is inserted into a JSONArray.<br>
 *
 * The associations are saved to a file named "/associations" to make
 * the data persistent.
 */
public class BeaconAssociationList {
    private JSONArray   associations;
    private File        assFile;
    String              fileName = "/associations";

    /**
     *  Constructor method for Beacon Association List.<br>
     *
     *  It needs a context in order to be able to access files.
     */
    public BeaconAssociationList(Context context) {
        assFile = new File(context.getFilesDir() + fileName);

        // If the associations file exist, read data from it and insert it to the list
        if (assFile.exists()) {
            try {
                // read the data from file
                FileReader fr = new FileReader(assFile);
                char[] buf = new char[((int) assFile.length())];
                fr.read(buf);
                fr.close();
                associations = new JSONArray(new String(buf));
            } catch (Exception e) {
                Log.e("BeaconAssociationList", e.getMessage());
                e.printStackTrace();
            }
        }
        else {
            associations = new JSONArray();
        }
    }

    /**
     * Adds a beacon association to the "list". If the beacon is not found
     * by its "id", or the hit is on "uuid" a new association is added.
     * However if the beacon is found in the list, the values are simply updated.
     *
     * @param beacon The Beacon class instance to be added. Contains all possible
     *               information about the beacon.
     * @param name String with the name of the Beacon, this is to make the
     *             beacon easier to identify.
     * @param value String with the association of the beacon, this might be
     *              a URL or simply a label.
     * @param notify Integer with notification level
     *               0 = no notify,
     *               1 = notify when closer than 1m
     *               2 = notify when closer than 15m,
     *               3 = notify reguardless
     *
     * @throws JSONException The input data is inserted to a new JSONObject
     * which might cause this exception. It might also be caused when inserting
     * the JSONObject into the JSONArray.
     */
    public void add (Beacon beacon, String name, String value, int notify) throws JSONException, IOException {
        int i = this.contains(beacon);
        // Beacon is not in list, create a new entry
        if (i == -1) {
            JSONObject json = new JSONObject();
            json.put("id", beacon.getAddress());
            json.put("uuid", beacon.getUuid());
            json.put("major", beacon.getMajor());
            json.put("minor", beacon.getMinor());
            json.put("name", name);
            json.put("value", value);
            json.put("notify", notify);
            associations.put(json);

        }
        // The beacon is in list, the values must be updated,
        else {
            JSONObject json = ((JSONObject) associations.get(i));
            json.remove("value");
            json.put("value", value);
            json.remove("uuid");
            json.put("uuid", beacon.getUuid());
            json.remove("major");
            json.put("major", beacon.getMajor());
            json.remove("minor");
            json.put("minor", beacon.getMinor());
            json.remove("name");
            json.put("name", name);
            json.remove("notify");
        }
        this.commit();
    }

    /**
     * Removes an association from the list by iterating the list
     * while searching for the ID (MAC) of the beacon.
     *
     * @param id String to identify the association to be removed.
     *           It's the ID (MAC) address of the beacon.
     * @throws JSONException Might be thrown since the associations
     * is stored in JSONObject within A JSONArray.
     */
    public void remove (String id) throws JSONException {
        for (int i = 0; i < associations.length(); i++) {
            JSONObject json = ((JSONObject) associations.get(i));
            if (json.get("id").equals(id))
                associations.remove(i);
        }
    }

    /**
     * Returns a String with the association tied to either a ID (MAC) or
     * the UUID of a beacon.
     *
     * @param beacon The Beacon class instance to be added. Contains all possible
     *               information about the beacon.
     * @return Returns null if the id/uuid is not found.
     * @throws JSONException Might be thrown due to data being stored
     * in JSONObjects placed inside a JSONArray.
     */
    public String getAssociation (Beacon beacon) throws JSONException {
        int i = this.contains(beacon);
        // id not found in list
        if (i == -1)
            return null;
        // found the id, returning the value
        else
            return ((JSONObject) associations.get(i)).get("value").toString();
    }

    /**
     * Returns a String with the name tied to either a ID (MAC) or the
     * UUID of a beacon.
     *
     * @param beacon The Beacon class instance to be added. Contains all possible
     *               information about the beacon.
     * @return Returns null if the id/uuid is not found.
     * @throws JSONException Might be thrown due to data being stored
     * in JSONObjects placed inside a JSONArray.
     */
    public String getName (Beacon beacon) throws JSONException {
        int i = this.contains(beacon);
        // id not found in list
        if (i == -1)
            return null;
            // found the id, returning the value
        else
            return ((JSONObject) associations.get(i)).get("name").toString();
    }

    /**
     * Return the notify parameter of the beacon
     *
     * @param beacon The beacon we are looking for
     * @return Returns the notification parameter, if not found -1 is returned.
     * @throws JSONException Data is stored in JSONArray and JSONObject
     * so JSONException might be thrown when accessing these structures.
     */
    public int getNotify (Beacon beacon) throws JSONException {
        int i = this.contains(beacon);
        if (i == -1)
            return -1;
        else
            return ((Integer) ((JSONObject) associations.get(i)).get("notify"));
    }

    /**
     * Returns the association number "i" within the list.
     *
     * @param i Integer representing the list number to get.
     * @return JSONObject with the association at position "i".
     * @throws JSONException Might be thrown due to data being stored
     * in JSONObjects placed inside a JSONArray.
     */
    public JSONObject get (int i) throws JSONException {
        return ((JSONObject) associations.get(i));
    }

    /**
     * Searches for a association in the list, returns the position
     * where the id or uuid is found. If not found -1 is returned.
     *
     * @param beacon The Beacon class instance to be added. Contains all possible
     *               information about the beacon.
     * @return Integer with the position of the association
     * @throws JSONException Might be thrown due to data being stored
     * in JSONObjects placed inside a JSONArray.
     */
    public int contains (Beacon beacon) throws JSONException {
        int beaconNum = -1;
        for (int i = 0; i < associations.length(); i++) {
            JSONObject ass = ((JSONObject) this.associations.get(i));
            if (ass.getString("id").equals(beacon.getAddress())) {
                beaconNum = i;
            }
            // save the position of a matching uuid
            else if (ass.get("uuid").equals(beacon.getUuid()) &&
                     ass.get("major").equals(beacon.getMajor()) &&
                     ass.get("minor").equals(beacon.getMinor()))
                beaconNum = i;
        }
        // return the hit on uuid since no hit on id was found
        return beaconNum;
    }

    public void notify (Beacon beacon, Context context) throws JSONException {
        int idx = contains(beacon);
        if (idx != -1) {
            JSONObject ass = ((JSONObject) this.associations.get(idx));
            //check if notification must be issued.
            Integer notify = ass.getInt("notify");
            double distance = beacon.getDistance();
            if (notify == 1 && distance < 1.0) {
                Log.e("NOTIFICATION", "NEAR distance: " + String.format("%.2f", distance));
            } else if (notify == 2 && distance < 15.0) {
                Log.e("NOTIFICATION", "MID distance: " + String.format("%.2f", distance));
            } else if (notify == 3) {
                Log.e("NOTIFICATION", "FAR distance: " + String.format("%.2f", distance));
            }

            if (notify != 0) {
                Intent intent = new Intent(context.getApplicationContext(), MainActivity.class);
                PendingIntent pIntent = PendingIntent.getActivity(context, 0, intent, 0);

                long[] pattern = {220,220,220,220,220};
                Notification n = new Notification.Builder(context)
                    .setSmallIcon(R.drawable.beacon)
                    .setContentTitle("Beacon proximity")
                    .setContentText("You are approximatly " + String.format("%.2f", distance) + "m from " + ass.getString("name"))
                    .setContentIntent(pIntent)
                    .setAutoCancel(true)
                    .setOnlyAlertOnce(true)
                    .setVibrate(pattern)
                    .build();

                NotificationManager nMan = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                nMan.notify(0, n);
                synchronized (n) {
                    n.notify();
                }
                Log.e("BLABLABLA", "notifying.....");
            }
        }

    }

    /**
     * Translate the notify parameter to something readable
     *
     * @param notify The notify variabnle to be translated
     * @return Returns a String with a readable text describing the
     * notify variable.
     */
    public String getNotificationString (Integer notify) {
        switch (notify) {
            case 0:
                return "Never";
            case 1:
                return "< 1m";
            case 2:
                return "< 15m";
            case 3:
                return "Always";
            default:
                return "Not set";
        }
    }

    /**
     * Returns the number of objects (associations) in the list.
     *
     * @return Integer representing the length of the list.
     */
    public int getCount () {
        return associations.length();
    }

    /**
     * Commits the association list to disk.
     *
     * @throws IOException Might be thrown due to disk- write
     * operations.
     */
    public void commit () throws IOException {
        assFile.delete();
        FileWriter fw = new FileWriter(assFile);
        fw.write(associations.toString());
        fw.flush();
        fw.close();
    }

}
