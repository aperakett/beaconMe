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

/**
 * The BeaconFactory is an API which handles all communication
 * between a HTTP server and any user of this class. All POST
 * requests are verified at the server with session tokens.
 * <p>
 * Session tokens are automatically handled within this class
 * and at the server. Hence, the caller of this class must only
 * set a username and password for which the server can authenticate
 * with.
 * <p>
 * The BeaconFactory must be run in a separate thread due to Android
 * restrictions, see example usage below.
 * <pre>
 * {@code
 *     final BeaconFactory beaconFactory;
 *     String url = "http://url:port";
 *     beaconFactory = new BeaconFactory(url);
 *     beaconFactory.setUser("example@user.com", "my_password");
 *
 *     new Thread() {
 *         public void run() {
 *             beaconFactory.establishConnection();
 *             ...
 *         }
 *     }.start();
 * </pre>
 *
 * @author 	Vegard Strand (vegard920@gmail.com)
 * @version	1.0
 * @since	2015-02-09
 */
public class BeaconFactory {
    private String token 		= "";
    private String username 	= "";
    private String password 	= "";
    private String serverUrl	= "";

    BeaconFactory(String url) {
        this.serverUrl = url;
    }

    /**
     * Sets the username and password in order to authenticate
     * to a server.
     * <p>
     * This method MUST be called prior to 'establishConnection',
     * otherwise, all other commands etc. will fail!
     *
     * @param username
     * @param password
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
     * @return a boolean value in which returns false if either
     * 		   the username or password was empty. Returns true
     * 		   otherwise.
     * @throws IOException
     * @throws JSONException
     */
    public boolean establishConnection()
    throws IOException, JSONException {
        String 				url		= "";
        HttpURLConnection   conn	= null;
        String				params	= "";
        JSONObject          obj		= null;

        if (this.username == "" || this.password == "") {
            return false;
        }

        try {
            url 	= this.serverUrl + "/api_login";
            params	= "session[email]=" + this.username
                    + "&session[password]=" + this.password;
            conn 	= createConnection(url, "POST", params,
                                       false, true, true);

            sendRequest(params, conn);
            if (conn.getResponseCode() == 200) {
                String res = readResponse(conn.getInputStream());

                // Parse response
                obj = new JSONObject(res);
                this.token = obj.getString("token");
            }
        } finally {
            if(conn != null) {
                conn.disconnect();
            }
        }

        return true;
    }

    /**
     * Returns null if beacon was not found by MAC-address
     * on server. Otherwise, it returns a JSON object holding
     * information about the beacon
     *
     * @param 	mac MAC-address
     * @return 	null if not found, JSON otherwise
     * @throws 	IOException
     * @throws 	JSONException
     */
    public JSONObject getBeacon(String mac)
    throws IOException, JSONException {
        String 				url 		= "";
        HttpURLConnection 	conn		= null;
        String				params		= "";
        JSONObject 			obj			= null;

        try {
            url 	= this.serverUrl + "/api_get_beacon";
            params	= "token=" + this.token
                    + "&beacon[mac]=" + mac;
            conn = createConnection(url, "POST", params,
                    false, true, true);

            sendRequest(params, conn);
            int rcode = conn.getResponseCode();
            if (rcode == 200) {
                String res = readResponse(conn.getInputStream());

                // Parse response
                obj = new JSONObject(res);
            }
        } finally {
            if(conn != null) {
                conn.disconnect();
            }
        }

        return obj;
    }

    /**
     * Updates the given beacon if found on server and the
     * user is authenticated as the owner of this beacon.
     * <p>
     * Returns true on success, false on any error such as
     * internal server error, denied access, etc.
     *
     * @param 	uuid
     * @param 	bcn_url
     * @param 	category_id
     * @param 	mac
     * @return	boolean
     * @throws 	IOException
     * @throws	JSONException
     */
    public boolean setBeacon(String uuid, String bcn_url, int category_id,
                             String mac)
    throws IOException, JSONException {
        String 				params 	= "";
        String 				url		= "";
        HttpURLConnection 	conn 	= null;
        String 				res		= "";	// HTTP Response
        JSONObject			obj		= null;
        int 				rcode	= 200;	// HTTP Response Code

        params 	= "token=" + this.token
                + "&beacon[uuid]=" + uuid
                + "&beacon[mac]=" + mac
                + "&beacon[url]=" + bcn_url
                + "&beacon[category_id]=" + category_id;
        url 	= this.serverUrl + "/api_set_beacon";
        conn 	= createConnection(url, "POST", params,
                false, true, true);

        try {
            sendRequest(params, conn);
            rcode = conn.getResponseCode();
            if (rcode == 200) {
                res = readResponse(conn.getInputStream());
                obj = new JSONObject(res);
                this.token = obj.getString("token");
            } else if (rcode == 403) {
                return false;
            }
        } finally {
            if(conn != null) {
                conn.disconnect();
            }
        }

        return true;
    }

    /**
     * Fetches all categories (id, subject) from server.
     * Returns data as JSON.
     *
     * @return JSON with all categories
     * @throws IOException
     * @throws JSONException
     */
    public JSONObject getCategories()
    throws IOException, JSONException {
        String 				params 	= "";
        String 				url		= "";
        HttpURLConnection 	conn 	= null;
        String 				res		= "";	// HTTP Response
        JSONObject			obj		= null;
        int 				rcode	= 200;	// HTTP Response Code

        params 	= "";
        url 	= this.serverUrl + "/api_get_categories";
        conn 	= createConnection(url, "POST", params,
                false, true, true);

        try {
            sendRequest(params, conn);
            rcode = conn.getResponseCode();
            if (rcode == 200) {
                res = readResponse(conn.getInputStream());
                obj = new JSONObject(res);
            } else if (rcode == 403) {
                System.out.println("Access denied!");
            }
        } finally {
            if(conn != null) {
                conn.disconnect();
            }
        }

        return obj;
    }

    private static HttpURLConnection
    createConnection(String url,
                     String method,
                     String params,
                     boolean useCache,
                     boolean doInput,
                     boolean doOutput)
            throws IOException
    {
        URL                 mUrl = null;
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
            response.append('\r');
        }
        rd.close();
        return response.toString();
    }

    private static void sendRequest(String params, HttpURLConnection conn)
            throws IOException {
        DataOutputStream wr = new DataOutputStream(
                conn.getOutputStream());
        wr.writeBytes(params);
        wr.flush();
        wr.close();
    }
}
