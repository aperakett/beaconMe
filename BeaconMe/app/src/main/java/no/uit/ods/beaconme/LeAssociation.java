package no.uit.ods.beaconme;

/*
 *  Author: Espen Mæland Wilhelmsen, espen.wilhelmsen@gmail.com
 *
 *  Associates a BTLE beacon with a string
 */
public class LeAssociation {
    private String id;
    private String uuid;
    private String value;

    public LeAssociation(String id, String uuid, String value) {
        this.id     = id;
        this.uuid   = uuid;
        this.value  = value;
    }

    public String getId () {
        return this.id;
    }

    public String getUuid () {
        return this.uuid;
    }

    public String getValue () {
        return this.value;
    }

    public void setValue (String value) {
        this.value = value;
    }

    public void setUuid (String uuid) {
        this.uuid = uuid;
    }

    public void setId (String id) {
        this.id = id;
    }
}
