package no.uit.ods.beaconme;

import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONArray;

/**
 * The BeaconClient is an API which handles all communication
 * between a HTTP server and any user of this class. All POST
 * requests are verified at the server by session tokens.
 * Session tokens are automatically handled by this very class.
 * <p>
 * Before calling any methods, the caller MUST set a username
 * and password by {@link #setUser(String, String) setUser}.
 * <p>
 * The serverUrl attribute for the BeaconClient class is set
 * accordingly to the server url. Be aware, the url is hardcoded!
 *
 * @author 	Vegard Strand
 * @version	1.0
 * @since	2015-02-26
 */
public class BeaconClient {
    private String  token 		= "";
    private String  username 	= "";
    private String  password 	= "";
    private String  serverUrl   = "http://192.168.1.202:3000";

    /**
     * Sets the username and password in order to authenticate
     * towards the server.
     *
     * @param username A String representing the user's username (email format)
     * @param password A String representing the user's password
     */
    public void setUser(String username, String password) {
        this.username = username;
        this.password = password;
    }

    /**
     * Establishes a first time connection to the server. The
     * server expects to authenticate the user with a password
     * and a username.
     * <p>
     * On successful authentication the server returns a session
     * token which is stored internally in this very class. The
     * token is used to validate all commands that changes data.
     * (Note: the caller of these methods does not have to worry
     * about token expiration etc. This is handled automatically)
     *
     * @return An integer representing the HTTP status of the
     * request. See
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html">
     * http://w3.org/Protocols
     * </a> for a list of available HTTP statuses. This method only
     * returns 401 Unauthorized or 200 OK.
     * <p>
     * Unauthorized means that either the username or password is
     * blank or incorrect.
     */
    public int connectToServer() {
        ConnectToServer cServer = new ConnectToServer();
        Thread t = new Thread(cServer);
        t.start();

        // Sync
        try {
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return cServer.getHttpStatus();
    }

    /**
     * Returns a JSONArray of beacons. The caller can search on
     * any of the given parameters. If you don't wan't to search
     * on an attribute, leave Strings as empty ("") and integers
     * to 0.
     *
     * @param name A String representing the beacon name
     * @param uuid A String representing the beacon uuid
     * @param bcn_url A String representing the beacon association url
     * @param category_id An Integer representing the associated category
     * @param mac A String representing the beacon mac address
     *
     * @return null on error, JSONArray with beacons otherwise
     */
    public JSONArray getBeacons(String mac, String uuid, int category_id,
                                String bcn_url, String name) {
        GetBeacons gBeacons = new GetBeacons(mac, bcn_url, uuid, name, category_id);
        Thread t = new Thread(gBeacons);
        t.start();

        // Sync
        try {
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return gBeacons.getBeacons();
    }

    /**
     * Requests to create a new beacon at the back-end server. All paramteres
     * must be present! Otherwise, it will fail.
     *
     * @param name A String representing the beacon name
     * @param uuid A String representing the beacon uuid
     * @param bcn_url A String representing the beacon association url
     * @param category_id An Integer representing the associated category
     * @param mac A String representing the beacon mac address
     * @return
     */
    public int createBeacon(String name, String uuid, String bcn_url,
                            int category_id, String mac) {
        CreateBeacon cBeacon = new CreateBeacon(mac, bcn_url, uuid, name, category_id);
        Thread t = new Thread(cBeacon);
        t.start();

        // Sync
        try {
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return cBeacon.getHttpStatus();
    }

    /**
     * Updates the beacon on mac address. Any other attributes
     * are optional and should be left empty ("" for Strings and
     * 0 for integers).
     *
     * @param name A String representing the beacon name
     * @param uuid A String representing the beacon uuid
     * @param bcn_url A String representing the beacon association url
     * @param category_id An Integer representing the associated category
     * @param mac A String representing the beacon mac address
     *
     * @return An integer representing the HTTP status of the
     * request. See
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html">
     * http://w3.org/Protocols
     * </a> for a list of available HTTP statuses. This method only
     * returns 401 Unauthorized, 500 Internal Server Error, or 200 OK.
     */
    public int setBeacon(String name, String uuid, String bcn_url,
                         int category_id, String mac) {
        SetBeacon sBeacon = new SetBeacon(mac, bcn_url, uuid, name, category_id);
        Thread t = new Thread(sBeacon);
        t.start();

        // Sync
        try {
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return sBeacon.getHttpStatus();
    }

    /**
     * Fetches all categories (id, subject) from server.
     * Returns data as JSONArray.
     *
     * @return JSONArray with all categories, null on error.
     */
    public JSONArray getCategories() {
        GetCategories cats = new GetCategories();
        Thread t = new Thread(cats);
        t.start();

        // Sync
        try {
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return cats.getCategories();
    }

    // Private Methods ...
    private static HttpURLConnection
    createConnection(String url,
                     String method,
                     String params,
                     boolean useCache,
                     boolean doInput,
                     boolean doOutput)
            throws IOException
    {
        URL 				mUrl = null;
        HttpURLConnection 	conn = null;

        mUrl = new URL(url);
        conn = (HttpURLConnection) mUrl.openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("Content-Length", "" +
                Integer.toString(params.getBytes().length));
        conn.setUseCaches(useCache);
        conn.setDoInput(doInput);
        conn.setDoOutput(doOutput);

        return conn;
    }

    private static String readResponse(InputStream inputStream)
            throws IOException {
        BufferedReader rd = new BufferedReader(new
                InputStreamReader(inputStream));
        String line;
        StringBuffer response = new StringBuffer();
        while((line = rd.readLine()) != null) {
            response.append(line);
        }
        rd.close();
        return response.toString();
    }

    private static void sendPost(String params, HttpURLConnection conn)
            throws IOException {
        DataOutputStream wr = new DataOutputStream(
                conn.getOutputStream());
        wr.writeBytes(params);
        wr.flush();
        wr.close();
    }

    /**
     *
     * @param conn
     * @return
     * @throws IOException
     */
    private String sendGet(HttpURLConnection conn)
            throws IOException {
        conn.setRequestMethod("GET");
        conn.setDoOutput(false);
        BufferedReader in = new BufferedReader(
                new InputStreamReader(conn.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        return response.toString();
    }

    private class GetBeacons implements Runnable {
        private String url;
        private HttpURLConnection conn;
        private String params;
        private volatile JSONArray beacons;

        private String mac;
        private String bcn_url;
        private String uuid;
        private int category_id;
        private String name;

        GetBeacons(String mac, String bcn_url, String uuid, String name, int category_id) {
            this.mac = mac;
            this.bcn_url = bcn_url;
            this.uuid = uuid;
            this.name = name;
            this.category_id = category_id;
        }

        @Override
        public void run() {
            try {
                url 	= serverUrl + "/api_get_beacon";
                params	= "token=" + token
                        + "&beacon[mac]=" + this.mac
                        + "&beacon[url]=" + this.bcn_url
                        + "&beacon[uuid]=" + this.uuid
                        + "&beacon[category_id]=" + this.category_id
                        + "&beacon[name]=" + this.name;
                conn    = createConnection(url, "POST", params,
                        false, true, true);

                sendPost(params, conn);
                int rcode = conn.getResponseCode();
                if (rcode == 200) {
                    String res = readResponse(conn.getInputStream());
                    this.beacons = new JSONArray(res);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if(conn != null) {
                    conn.disconnect();
                }
            }
        }

        public JSONArray getBeacons() {
            return this.beacons;
        }
    }

    private class SetBeacon implements Runnable {
        private volatile int rcode = 0;
        private String params;
        private String url;
        private HttpURLConnection conn;

        private String name;
        private String uuid;
        private String mac;
        private String bcn_url;
        private int category_id;

        SetBeacon(String mac, String bcn_url, String uuid, String name, int category_id) {
            this.mac = mac;
            this.bcn_url = bcn_url;
            this.uuid = uuid;
            this.name = name;
            this.category_id = category_id;
        }

        public void run() {
            params 	= "token=" + token
                    + "&beacon[name]=" + this.name
                    + "&beacon[uuid]=" + this.uuid
                    + "&beacon[mac]=" + this.mac
                    + "&beacon[url]=" + this.bcn_url
                    + "&beacon[category_id]=" + category_id;
            url 	= serverUrl + "/api_set_beacon";
            try {
                conn = createConnection(url, "POST", params, false, true, true);
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                sendPost(params, conn);
                this.rcode = conn.getResponseCode();
                if (rcode == 401) {
                    // Re-authenticate
                    connectToServer();
                    sendPost(params, conn);
                    this.rcode = conn.getResponseCode();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if(conn != null) {
                    conn.disconnect();
                }
            }
        }

        public int getHttpStatus() {
            return this.rcode;
        }
    }

    private class CreateBeacon implements Runnable {
        private volatile int rcode = 0;
        private String params;
        private String url;
        private HttpURLConnection conn;

        private String name;
        private String uuid;
        private String mac;
        private String bcn_url;
        private int category_id;

        CreateBeacon(String mac, String bcn_url, String uuid, String name, int category_id) {
            this.mac = mac;
            this.bcn_url = bcn_url;
            this.uuid = uuid;
            this.name = name;
            this.category_id = category_id;
        }

        public void run() {
            params 	= "token=" + token
                    + "&beacon[name]=" + this.name
                    + "&beacon[uuid]=" + this.uuid
                    + "&beacon[mac]=" + this.mac
                    + "&beacon[url]=" + this.bcn_url
                    + "&beacon[category_id]=" + category_id;
            url 	= serverUrl + "/api_create_beacon";
            try {
                conn = createConnection(url, "POST", params, false, true, true);
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                sendPost(params, conn);
                this.rcode = conn.getResponseCode();
                if (rcode == 401) {
                    // Re-authenticate
                    connectToServer();
                    sendPost(params, conn);
                    this.rcode = conn.getResponseCode();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if(conn != null) {
                    conn.disconnect();
                }
            }
        }

        public int getHttpStatus() {
            return this.rcode;
        }
    }

    private class GetCategories implements Runnable {
        private volatile JSONArray categories;
        private String url;
        private HttpURLConnection conn;
        private String response;
        private int rcode;

        @Override
        public void run() {
            url = serverUrl + "/api_get_categories";
            try {
                conn = createConnection(url, "GET", "", false, true, true);
            } catch (IOException e) {
                e.printStackTrace();
                this.categories = null;
                if(conn != null) {
                    conn.disconnect();
                }
                return;
            }

            try {
                response = sendGet(conn);
                rcode = conn.getResponseCode();
                if (rcode == 200) {
                    this.categories = new JSONArray(response);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            } finally {
                if(conn != null) {
                    conn.disconnect();
                }
            }
        }

        public JSONArray getCategories() {
            return this.categories;
        }
    }

    private class ConnectToServer implements Runnable {
        private volatile int rcode = 0;
        private String url;
        private HttpURLConnection conn;
        private String params;
        private JSONObject obj;

        public void run() {

            if (username == "" || password == "") {
                this.rcode = 401;
                return;
            }

            try {
                url 	= serverUrl + "/api_authenticate";
                params	= "user[username]=" + username
                        + "&user[password]=" + password;
                conn 	= createConnection(url, "POST", params, false, true, true);

                sendPost(params, conn);
                this.rcode = conn.getResponseCode();
                if (this.rcode == 200) {
                    String res = readResponse(conn.getInputStream());

                    // Parse response
                    obj = new JSONObject(res);
                    token = obj.getString("token");
                } else if (conn.getResponseCode() == 401) {
                    // TODO: Handle unauthorized connection
                    this.rcode = 401;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }

        public int getHttpStatus() {
            return this.rcode;
        }
    }
}
