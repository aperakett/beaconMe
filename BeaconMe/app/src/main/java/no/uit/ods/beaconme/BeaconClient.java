package no.uit.ods.beaconme;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

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
 * BeaconClient requires that users authenticates to the cloud before use.
 * The authentication process is done through the authenticate method. On
 * authentication the method stores the user's API key internally in the
 * class as well as on disk in a file named settings.json. It is preferred
 * that this class is used as a singleton to prevent disk I/O.
 *
 * Since the class performs network operations, there has to be a timeout
 * variable to prevent hiccups. Default timeout value is 20 seconds or
 * 20 000 milliseconds. This value can be set by calling setConnTimeOut()
 * and get by getConnTimeOut().
 *
 * @author 	Vegard Strand (vst030@post.uit.no)
 * @version	1.3
 * @since	2015-04-10
 */
public class BeaconClient {
    private String  serverUrl    = "http://beaconme.ddns.net:3000";
    private String  apiKey       = "";
    private int     connTimeOut  = 20000;   // In milliseconds
    private static  BeaconClient instance = null;

    protected BeaconClient() {
        // Exists only to defeat instantiation.
    }

    public static BeaconClient getInstance() {
        if (instance == null) {
            instance = new BeaconClient();
        }
        return instance;
    }

    public boolean authenticate(String email, String password, Context c)
            throws InterruptedException, IOException, JSONException {
        if (!isOnline(c)) {
            return false;
        }

        AuthToServer    authToServer;
        Thread          t;
        int             status;

        authToServer = new AuthToServer();
        authToServer.setUser(email, password);
        t = new Thread(authToServer);
        t.start();

        // Sync
        t.join();

        status = authToServer.getHttpStatus();
        if (status != 200) {
            return false;
        }

        this.apiKey = authToServer.getApiKey();

        // Write key to settings file
        String          fContent;
        FileOperator    fOperator;

        fContent    = "[{ \"api_key\": " +  this.apiKey + "}]";
        fOperator   = new FileOperator("settings.json", Context.MODE_PRIVATE);
        fOperator.writeJSON("settings", fContent, c);

        return true;
    }

    /**
     * Set network connection timeout in milliseconds.
     */
    public void setConnTimeOut(int timeOut) {
        this.connTimeOut = timeOut;
    }

    /**
     * Returns the network connection timeout in milliseconds.
     */
    public int getConnTimeOut() {
        return this.connTimeOut;
    }

    /**
     * Returns found beacons in the cloud service. On any error such as no internet,
     * no beacons found, etc. the method returns null.
     */
    public JSONArray getBeacons(String mac, String uuid, int categoryId, String beaconUrl,
            String name, String major, String minor, Context c) throws InterruptedException {
        if (!isOnline(c)) {
            return null;
        }

        GetBeacons      getBeacons;
        Thread          t;

        getBeacons = new GetBeacons(this.apiKey, mac, beaconUrl, uuid,
                                    name, categoryId, major, minor);
        t = new Thread(getBeacons);
        t.start();

        // Sync
        t.join();
        return getBeacons.getBeacons();
    }

    /**
     * Inserts a fresh instance of a beacon into the cloud service.
     * The method returns the HTTP status of the request, note: on
     * no internet, the method returns the value of 0.
     */
    public int createBeacon(String name, String uuid, String beaconUrl,
            int categoryId, String mac, String major, String minor, Context c)
            throws InterruptedException {

        if (!isOnline(c)) {
            return 0;
        }

        CreateBeacon    createBeacon;
        Thread          t;

        createBeacon = new CreateBeacon(this.apiKey, mac, beaconUrl, uuid, name,
                                        categoryId, major, minor);
        t = new Thread(createBeacon);
        t.start();

        // Sync
        t.join();
        return createBeacon.getHttpStatus();
    }

    /**
     * Updates a beacon in the cloud service. All arguments are optional.
     * However, the more arguments, the better chance of finding and
     * updating the beacon. The method returns the HTTP status of the
     * request, note: on no internet, the method returns the value of 0.
     */
    public int setBeacon(String name, String uuid, String beaconUrl,
            int categoryId, String mac, String major, String minor, Context c)
            throws InterruptedException {
        if (!isOnline(c)) {
            return 0;
        }

        SetBeacon       setBeacon;
        Thread          t;

        setBeacon   = new SetBeacon(this.apiKey, mac, beaconUrl, uuid, name,
                                    categoryId, major, minor);
        t = new Thread(setBeacon);
        t.start();

        // Sync
        t.join();
        return setBeacon.getHttpStatus();
    }

    /**
     * Returns categories from the cloud service. On any error such as no internet,
     * no beacons found, etc. the method returns null.
     */
    public JSONArray getCategories(Context c) throws InterruptedException {
        if (!isOnline(c)) {
            return null;
        }

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
     * Returns a category from the cloud service if found. On any error such as no internet,
     * no beacons found, etc. the method returns null.
     */
    public JSONObject getCategory(int categoryId, String topic, Context c)
            throws InterruptedException {
        if (!isOnline(c)) {
            return null;
        }

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
            String params, boolean useCache, boolean doInput, boolean doOutput)
            throws IOException {
        URL 				mUrl;
        HttpURLConnection 	conn;
        BeaconClient        b;

        b    = getInstance();
        mUrl = new URL(url);
        conn = (HttpURLConnection) mUrl.openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("Content-Length", "" +
                Integer.toString(params.getBytes().length));
        conn.setUseCaches(useCache);
        conn.setDoInput(doInput);
        conn.setDoOutput(doOutput);
        conn.setConnectTimeout(b.getConnTimeOut());

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
        private String              proximity;
        private int                 category_id;
        private String              name;
        private String              major;
        private String              minor;
        private String              key;

        GetBeacons(String key, String mac, String bcn_url, String proximity, String name,
                   int category_id, String major, String minor) {
            this.mac            = mac;
            this.bcn_url        = bcn_url;
            this.proximity      = proximity;
            this.name           = name;
            this.major          = major;
            this.minor          = minor;
            this.key            = key;
            this.category_id    = category_id;
        }

        @Override
        public void run() {
            try {
                url 	= serverUrl                 + "/api_get_beacon";
                params	= "key="                    + this.key
                        + "&beacon[mac]="           + this.mac
                        + "&beacon[url]="           + this.bcn_url
                        + "&beacon[proximity]="     + this.proximity
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
        private String              proximity;
        private int                 categoryId;
        private String              name;
        private String              major;
        private String              minor;
        private String              key;

        SetBeacon(String key, String mac, String beaconUrl, String proximity, String name,
                  int categoryId, String major, String minor) {
            this.mac        = mac;
            this.beaconUrl  = beaconUrl;
            this.proximity  = proximity;
            this.name       = name;
            this.categoryId = categoryId;
            this.major      = major;
            this.minor      = minor;
            this.key        = key;
        }

        public void run() {
            try {
                params = "key="                     + this.key
                       + "&beacon[name]="           + this.name
                       + "&beacon[proximity]="      + this.proximity
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
        private String              proximity;
        private int                 categoryId;
        private String              name;
        private String              major;
        private String              minor;
        private String              key;

        CreateBeacon(String key, String mac, String beaconUrl, String proximity, String name,
                     int categoryId, String major, String minor) {
            this.mac        = mac;
            this.beaconUrl  = beaconUrl;
            this.proximity  = proximity;
            this.name       = name;
            this.categoryId = categoryId;
            this.major      = major;
            this.minor      = minor;
            this.key        = key;
        }

        public void run() {
            try {
                params = "key="                     + this.key
                       + "&beacon[name]="           + this.name
                       + "&beacon[proximity]="      + this.proximity
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
        private String              key;
        private String              email;
        private String              password;

        public void run() {

            try {
                url 	= serverUrl + "/api_authenticate";
                params	= "user[email]="        + this.email
                        + "&user[password]="    + this.password;
                conn 	= createConnection(url, "POST", params, false, true, true);

                sendPostRequest(params, conn);
                this.rcode = conn.getResponseCode();
                if (this.rcode == 200) {
                    String res  = readResponse(conn.getInputStream());
                    obj         = new JSONObject(res);
                    this.key    = obj.getString("key");
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

        public void setUser(String email, String password) {
            this.email      = email;
            this.password   = password;
        }

        public int      getHttpStatus() {
            return this.rcode;
        }
        public String   getApiKey()     { return this.key;   }
    }

    private boolean isOnline(Context c) {
        ConnectivityManager cm =
                (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }
}
