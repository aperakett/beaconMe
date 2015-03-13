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
 * The serverUrl attribute for the BeaconClient class is set
 * accordingly to the server url. Be aware, the url is hardcoded!
 * <p>
 * Latest Changes
 * - Attribute 'topic' is added to each beacon as in result of get request.
 * - Method getCategory(categoryId, topic) is introduced. Now supports search
 *   on categories from id and/or topic attribute.
 *
 * @author 	Vegard Strand (vst030@post.uit.no)
 * @version	1.2
 * @since	2015-02-26
 */
public class BeaconClient {
    private String  username 	    = "";
    private String  password 	    = "";
    private String  serverUrl       = "http://beaconme.ddns.net:3000";

    /**
     *
     * @param username as email
     * @param password
     * @throws InterruptedException
     */
    BeaconClient(String username, String password) throws InterruptedException {

        this.username = username;
        this.password = password;

    }

    /**
     * Returns a JSONArray with a collection of JSONObjects representing beacons.
     * If no beacons were found on search parameters, the method returns an empty
     * array. If no connection or error, it returns null.
     *
     * @param mac           String
     * @param uuid          String
     * @param categoryId    Integer
     * @param beaconUrl     String
     * @param name          String
     * @param major         String
     * @param minor         String
     * @return JSONArray with JSONObjects representing beacons
     * @throws InterruptedException
     */
    public JSONArray getBeacons(String mac, String uuid, int categoryId,
                                String beaconUrl, String name, String major, String minor)
    throws InterruptedException {

        AuthToServer    authToServer;
        GetBeacons      getBeacons;
        Thread          t;
        int             status;
        String          token;

        authToServer = new AuthToServer();
        t = new Thread(authToServer);
        t.start();

        // Sync
        t.join();

        status = authToServer.getHttpStatus();
        if (status != 200) {

            // An error has occurred, either 401 unauthorized, 500 internal server
            // error, 404 not found, etc.
            return null;

        }

        token       = authToServer.getToken();
        getBeacons  = new GetBeacons(token, mac, beaconUrl, uuid,
                                     name, categoryId, major, minor);
        t = new Thread(getBeacons);
        t.start();

        // Sync
        t.join();
        return getBeacons.getBeacons();

    }

    /**
     * Creates a new beacon on back-end system. All arguments are required.
     *
     * @param name          String
     * @param uuid          String
     * @param beaconUrl     String
     * @param categoryId    Integer
     * @param mac           String
     * @param major         String
     * @param minor         String
     * @return an integer representing the status code of the request
     */
    public int createBeacon(String name, String uuid, String beaconUrl,
                            int categoryId, String mac, String major, String minor)
    throws InterruptedException {

        AuthToServer    authToServer;
        CreateBeacon    createBeacon;
        Thread          t;
        int             status;
        String          token;

        authToServer = new AuthToServer();
        t = new Thread(authToServer);
        t.start();

        // Sync
        t.join();

        status = authToServer.getHttpStatus();
        if (status != 200) {

            // An error has occurred, either 401 unauthorized, 500 internal server
            // error, 404 not found, etc.
            return status;

        }

        token           = authToServer.getToken();
        createBeacon    = new CreateBeacon(token, mac, beaconUrl, uuid, name,
                                           categoryId, major, minor);
        t = new Thread(createBeacon);
        t.start();

        // Sync
        t.join();
        return createBeacon.getHttpStatus();

    }

    /**
     * Update a beacon on back-end system. All arguments are optional. However,
     * the more arguments, the better chance of finding and updating the beacon.
     *
     * @param name          String
     * @param uuid          String
     * @param beaconUrl     String
     * @param categoryId    Integer
     * @param mac           String
     * @param major         String
     * @param minor         String
     * @return an integer representing the status code of the request
     */
    public int setBeacon(String name, String uuid, String beaconUrl,
                         int categoryId, String mac, String major, String minor)
    throws InterruptedException {

        AuthToServer    authToServer;
        SetBeacon       setBeacon;
        Thread          t;
        int             status;
        String          token;

        authToServer = new AuthToServer();
        t = new Thread(authToServer);
        t.start();

        // Sync
        t.join();

        status = authToServer.getHttpStatus();
        if (status != 200) {

            // An error has occurred, either 401 unauthorized, 500 internal server
            // error, 404 not found, etc.
            return status;

        }

        token       = authToServer.getToken();
        setBeacon   = new SetBeacon(token, mac, beaconUrl, uuid, name,
                                    categoryId, major, minor);
        t = new Thread(setBeacon);
        t.start();

        // Sync
        t.join();
        return setBeacon.getHttpStatus();

    }

    /**
     * Returns categories found on back-end system.
     *
     * @return JSONArray representing the categories, null on error
     */
    public JSONArray getCategories() throws InterruptedException {

        GetCategories   getCategories;
        Thread          t;

        getCategories = new GetCategories();
        t = new Thread(getCategories);
        t.start();

        // Sync
        t.join();

        return getCategories.getCategories();

    }

    /**
     * Returns a category based on input search arguments
     */
    public JSONObject getCategory(int categoryId, String topic) throws InterruptedException {

        GetCategory   getCategory;
        Thread        t;

        getCategory = new GetCategory(categoryId, topic);
        t = new Thread(getCategory);
        t.start();

        // Sync
        t.join();

        return getCategory.getCategory();

    }

    private static HttpURLConnection createConnection(String url, String method,
                                                      String params, boolean useCache,
                                                      boolean doInput, boolean doOutput)
    throws IOException {

        URL 				mUrl;
        HttpURLConnection 	conn;

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

    private static String readResponse(InputStream inputStream) throws IOException {

        BufferedReader  reader;
        String          line;
        StringBuffer    response;

        reader      = new BufferedReader(new InputStreamReader(inputStream));
        response    = new StringBuffer();

        while((line = reader.readLine()) != null) {
            response.append(line);
        }

        reader.close();
        return response.toString();

    }

    private static void sendPostRequest(String params, HttpURLConnection conn) throws IOException {

        DataOutputStream writer;

        writer = new DataOutputStream(conn.getOutputStream());
        writer.writeBytes(params);
        writer.flush();
        writer.close();

    }

    private String sendGetRequest(HttpURLConnection conn) throws IOException {

        BufferedReader  inStream;
        String          inputLine;
        StringBuffer    response;

        conn.setRequestMethod("GET");
        conn.setDoOutput(false);

        inStream    = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        response    = new StringBuffer();

        while ((inputLine = inStream.readLine()) != null) {
            response.append(inputLine);
        }

        inStream.close();
        return response.toString();

    }

    private class GetBeacons implements Runnable {

        private String              url;
        private HttpURLConnection   conn;
        private String              params;
        private JSONArray           beacons;
        private String              mac;
        private String              bcn_url;
        private String              uuid;
        private int                 category_id;
        private String              name;
        private String              major;
        private String              minor;
        private String              token;

        GetBeacons(String token, String mac, String bcn_url, String uuid, String name,
                   int category_id, String major, String minor) {

            this.mac            = mac;
            this.bcn_url        = bcn_url;
            this.uuid           = uuid;
            this.name           = name;
            this.major          = major;
            this.minor          = minor;
            this.token          = token;
            this.category_id    = category_id;

        }

        @Override
        public void run() {
            try {
                url 	= serverUrl                 + "/api_get_beacon";
                params	= "token="                  + this.token
                        + "&beacon[mac]="           + this.mac
                        + "&beacon[url]="           + this.bcn_url
                        + "&beacon[uuid]="          + this.uuid
                        + "&beacon[category_id]="   + this.category_id
                        + "&beacon[name]="          + this.name
                        + "&beacon[major]="         + this.major
                        + "&beacon[minor]="         + this.minor;
                conn    = createConnection(url, "POST", params, false, true, true);

                sendPostRequest(params, conn);
                if (conn.getResponseCode() == 200) {

                    String response = readResponse(conn.getInputStream());
                    this.beacons    = new JSONArray(response);

                }
            } catch (JSONException | IOException e) {

                e.printStackTrace();
                this.beacons = null;

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

        private int                 rcode;
        private String              params;
        private String              url;
        private HttpURLConnection   conn;
        private String              mac;
        private String              beaconUrl;
        private String              uuid;
        private int                 categoryId;
        private String              name;
        private String              major;
        private String              minor;
        private String              token;

        SetBeacon(String token, String mac, String beaconUrl, String uuid, String name,
                  int categoryId, String major, String minor) {

            this.mac        = mac;
            this.beaconUrl  = beaconUrl;
            this.uuid       = uuid;
            this.name       = name;
            this.categoryId = categoryId;
            this.major      = major;
            this.minor      = minor;
            this.token      = token;

        }

        public void run() {
            try {
                params = "token="                   + this.token
                       + "&beacon[name]="           + this.name
                       + "&beacon[uuid]="           + this.uuid
                       + "&beacon[mac]="            + this.mac
                       + "&beacon[url]="            + this.beaconUrl
                       + "&beacon[category_id]="    + this.categoryId
                       + "&beacon[major]="          + this.major
                       + "&beacon[minor]="          + this.minor;
                url    = serverUrl                  + "/api_set_beacon";
                conn   = createConnection(url, "POST", params, false, true, true);

                sendPostRequest(params, conn);
                this.rcode = conn.getResponseCode();

            } catch (IOException e) {

                e.printStackTrace();
                this.rcode = 500; // Internal Server Error

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

    private class CreateBeacon implements Runnable {
        private int                 rcode;
        private String              params;
        private String              url;
        private HttpURLConnection   conn;
        private String              mac;
        private String              beaconUrl;
        private String              uuid;
        private int                 categoryId;
        private String              name;
        private String              major;
        private String              minor;
        private String              token;

        CreateBeacon(String token, String mac, String beaconUrl, String uuid, String name,
                     int categoryId, String major, String minor) {

            this.mac        = mac;
            this.beaconUrl  = beaconUrl;
            this.uuid       = uuid;
            this.name       = name;
            this.categoryId = categoryId;
            this.major      = major;
            this.minor      = minor;
            this.token      = token;

        }

        public void run() {
            try {
                params = "token="                   + this.token
                       + "&beacon[name]="           + this.name
                       + "&beacon[uuid]="           + this.uuid
                       + "&beacon[mac]="            + this.mac
                       + "&beacon[url]="            + this.beaconUrl
                       + "&beacon[category_id]="    + this.categoryId
                       + "&beacon[major]="          + this.major
                       + "&beacon[minor]="          + this.minor;
                url    = serverUrl                  + "/api_create_beacon";
                conn   = createConnection(url, "POST", params, false, true, true);

                sendPostRequest(params, conn);
                this.rcode = conn.getResponseCode();

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

    private class GetCategories implements Runnable {

        private JSONArray           categories;
        private String              url;
        private HttpURLConnection   conn;
        private String              response;

        @Override
        public void run() {
            try {
                url         = serverUrl + "/api_get_categories";
                conn        = createConnection(url, "GET", "", false, true, true);
                response    = sendGetRequest(conn);

                if (conn.getResponseCode() == 200) {

                    this.categories = new JSONArray(response);

                }
            } catch (IOException | JSONException e) {

                e.printStackTrace();
                this.categories = null;

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

    private class GetCategory implements Runnable {

        private JSONObject          category;
        private String              url;
        private HttpURLConnection   conn;
        private String              response;
        private String              params;
        private int                 categoryId;
        private String              topic;

        GetCategory(int categoryId, String topic) {

            this.categoryId = categoryId;
            this.topic = topic;

        }

        @Override
        public void run() {
            try {
                url 	= serverUrl             + "/api_get_category";
                params	= "&category[id]="      + this.categoryId
                        + "&category[topic]="   + this.topic;
                conn    = createConnection(url, "POST", params, false, true, true);

                sendPostRequest(params, conn);
                if (conn.getResponseCode() == 200) {

                    String response = readResponse(conn.getInputStream());
                    this.category    = new JSONObject(response);

                }
            } catch (JSONException | IOException e) {

                e.printStackTrace();
                this.category = null;

            } finally {

                if(conn != null) {
                    conn.disconnect();
                }

            }
        }

        public JSONObject getCategory() {
            return this.category;
        }
    }

    private class AuthToServer implements Runnable {

        private int                 rcode;
        private String              url;
        private HttpURLConnection   conn;
        private String              params;
        private JSONObject          obj;
        private String              token;

        public void run() {
            if (username == "" || password == "") {
                this.rcode = 401; // Unauthorized
                return;
            }

            try {
                url 	= serverUrl + "/api_authenticate";
                params	= "user[username]=" + username
                        + "&user[password]=" + password;
                conn 	= createConnection(url, "POST", params, false, true, true);

                sendPostRequest(params, conn);
                this.rcode = conn.getResponseCode();
                if (this.rcode == 200) {

                    String res  = readResponse(conn.getInputStream());
                    obj         = new JSONObject(res);
                    this.token  = obj.getString("token");

                }
            } catch (JSONException | IOException e) {

                e.printStackTrace();
                this.rcode = 500; // Internal Server Error

            } finally {

                if (conn != null) {
                    conn.disconnect();
                }

            }
        }

        public String   getToken()      { return this.token; }
        public int      getHttpStatus() {
            return this.rcode;
        }
    }
}
