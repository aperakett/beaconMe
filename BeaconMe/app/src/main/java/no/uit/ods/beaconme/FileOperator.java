package no.uit.ods.beaconme;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;

public class FileOperator {
    private String  fName;
    private int     fMode;

    /**
     *
     * @param filename  Full filename. Example: 'settings.json'
     * @param mode      File Context Mode, set Context.MODE_PRIVATE for internal storage
     *                  (recommended). See 'http://developer.android.com/reference/android/
     *                  content/Context.html' for more information on MODEs'
     */
    FileOperator(String filename, int mode) {
        this.fName  = filename;
        this.fMode  = mode;
    }

    /**
     * Truncates file if it exists and writes content bytes to file.
     * The file is set on Class construction.
     */
    public void write(String content, Context context) throws IOException {
        writeClean(content, context, false, "");
    }

    /**
     * Expects JSONArray as String (String content) and a context (Context context) as
     * method arguments. Iterates array and updates objects that already exists, appends
     * non-existing objects. The key argument is used to index the JSON file.
     *
     * Example,
     * writeJSON("settings", "[ {"api_key": "superkey"} ]", ...) will produce
     * the following file,
     *
     * {
     *     settings: [
     *         {
     *             "api_key": "superkey"
     *         }
     *     ]
     * }
     */
    public void writeJSON(String key, String content, Context context) throws IOException,
            JSONException {

        File file = new File(context.getFilesDir(), this.fName);
        if (!file.exists()) {
            writeClean(content, context, true, key);
        } else {
            searchAndReplace(key, content, context);
        }
    }

    /**
     * Reads and returns the file content. If the file does not exist, an
     * empty string is returned.
     */
    public String read(Context context) throws IOException {
        File file = new File(context.getFilesDir(), this.fName);
        if (!file.exists()) {
            return "";
        }

        BufferedReader  inputReader = new BufferedReader(new InputStreamReader(
            context.openFileInput(this.fName)));
        String          inputString;
        StringBuffer    stringBuffer = new StringBuffer();
        while ((inputString = inputReader.readLine()) != null) {
            stringBuffer.append(inputString + "\n");
        }
        inputReader.close();
        return stringBuffer.toString();
    }

    /**
     * Deletes the file if it exists from the context file directory.
     */
    public boolean delete(Context context) {
        File file = new File(context.getFilesDir(), this.fName);
        boolean res = false;
        if (file.exists()) {
            res = file.delete();
        }
        return res;
    }

    private void writeClean(String content, Context context, boolean isJSON, String key)
            throws IOException {

        FileOutputStream outputStream;
        String output;
        if (isJSON) {
            output = "{ " + key + ": " + content + " }";
        } else {
            output = content;
        }

        outputStream = context.openFileOutput(this.fName, this.fMode);
        outputStream.write(output.getBytes());
        outputStream.close();
    }

    private void searchAndReplace(String key, String content, Context context) throws IOException,
            JSONException {

        // File from Internal Storage
        String fContent = this.read(context);
        if (fContent == "") {
            return;
        }
        JSONObject fObj = new JSONObject(fContent);
        JSONArray settings = fObj.getJSONArray(key);

        // Input content
        JSONArray input = new JSONArray(content);

        for (int j = 0; j < input.length(); j++) {
            JSONObject curr = input.getJSONObject(j);

            boolean isFound = false;
            for (int i = 0; i < settings.length() && !isFound; i++) {
                JSONObject currSetting = settings.getJSONObject(i);
                isFound = compareKeys(currSetting, curr);
            }

            if (!isFound) {
                settings.put(curr);
            }
        }
        writeClean(settings.toString(), context, true, key);
    }

    private boolean compareKeys(JSONObject obj1, JSONObject obj2) throws JSONException {
        boolean hasReplaced = false;

        for(Iterator<String> iter_outer = obj1.keys(); iter_outer.hasNext();) {
            String key_outer = iter_outer.next();

            for(Iterator<String> iter_inner = obj2.keys(); iter_inner.hasNext();) {
                String key2 = iter_inner.next();
                if (key_outer.equalsIgnoreCase(key2)) {
                    obj1.put(key_outer, obj2.getString(key2));
                    hasReplaced = true;
                }
            }
        }
        return hasReplaced;
    }
}
