package no.uit.ods.beaconme;


import android.content.Context;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;


/*
 *  Author: Espen Mæland Wilhelmsen, espen.wilhelmsen@gmail.com
 *
 * Implements a BTLE Association list which makes it possible to
 * search and find values depending the id of the beacon.
 */
public class LeAssociationList {
    private LinkedList associations;
    private  File assFile;

    public LeAssociationList (Context context) {
        associations = new LinkedList();
        assFile = new File(context.getFilesDir() + "/associations");
        String id, uuid, value;

        // If the associations file exist, read data from it and insert it to the list
        if (assFile.exists()) {
            try {
//            FileWriter fw = new FileWriter(f, true);
//            fw.write(test);
//            fw.close();

                // read the data from file
                FileReader fr = new FileReader(assFile);
                char[] buf = new char[((int) assFile.length())];
                fr.read(buf);
                fr.close();

                // Todo: verify list from disk

                // split up the data and insert it to the list
                String[] data = (new String(buf)).split("\n");
                for (int i = 0; i < data.length; i++) {
                    String[] ass = data[i].split("\t");
                    id      = ass[0];
                    uuid    = ass[1];
                    value   = ass[2];

                    this.add(id, uuid, value);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public boolean add (String id, String uuid, String value) {
        int i = this.contains(id, uuid);
        LeAssociation ass;
        Boolean retval = true;
        // The beacon is not in list and must be added.
        if (i == -1) {
            ass = new LeAssociation(id, uuid, value);
            retval = this.associations.add(ass);
        }
        // The beacon is in list, the value must be updated
        else {
            ass = getAssociation(i);
            ass.setValue(value);
        }
        //Todo save the list to disk.

        try {
            FileWriter fw = new FileWriter(assFile, true);
            fw.write(id + "\t" + uuid + "\t" + value + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return retval;
    }

    public String get (String id, String uuid) {
        int i = this.contains(id, uuid);
        // id not found in list
        if (i == -1)
            return null;
        // found the id, returning the value
        else
            return this.getAssociation(i).getValue();
    }

    private int contains (String id, String uuid) {
        for (int i = 0; i < associations.size(); i++) {
            LeAssociation ass = ((LeAssociation) this.associations.get(i));
            if (ass.getId() == id || ass.getUuid() == uuid)
                // Todo, change id or uuiid if one is not matching?
                return i;
        }
        return -1;
    }

    private LeAssociation getAssociation (int pos) {
        return ((LeAssociation) this.associations.get(pos));
    }

}
