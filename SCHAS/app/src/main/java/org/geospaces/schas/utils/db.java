package org.geospaces.schas.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.format.Formatter;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.geospaces.schas.AsyncTasks.PostToServer;
import org.geospaces.schas.Fragments.GoogleMaps;
import org.geospaces.schas.UtilityObjectClasses.DatabaseLocationObject;
import org.geospaces.schas.utils.GetGoogleLocations.Entry;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class db {
    private static float batLevel;
    public final static String DIRECTORY = "/SCHAS";
    public final static String FILE_NAME = "LOC.txt";
    public final static String SECONDARY_FILE_NAME = "SECONDARY_LOC.txt";
    public final static String FILE_READY = "LOC_ready.txt";
    public final static String GOOGLE_FILE_NAME = "GOOGLE_LOCS.TXT";
    public static String tempFileName = "schasTempFile";
    public final static int FILE_SIZE = 8 * 1024;

    public static int numberOfSplitFiles;
    public static int numberOfFilesUploaded;
    public static int numberOfFailedUploads;

    public static Location lastLocation;
    public final static String FILE_SETTINGS = "Settings.txt";

    public static List<String> otherDataLines = new ArrayList<>();

    public static Context contextForUpload;

    public static String read(String fileName) {
        File file = getFile(fileName);

        if (!file.exists()) {
            return "";
        }
        String str = "ERROR reading File:  " + fileName;
        char[] bytes = new char[(int) file.length()];
        try {
            FileReader in = new FileReader(file);
            in.read(bytes);
            str = new String(bytes);
            in.close();
        } catch (Exception e) {
            Log.e("ERR", "" + e);
        }
        return str;
    }

    public static void Write(String msg) throws IOException {
        File file = db.getFile(db.FILE_NAME);
        BufferedWriter out = new BufferedWriter(new FileWriter(file.getAbsolutePath(), file.exists()));

        if (msg.endsWith("\n")) {
            out.write(msg);
        }
        else {
            out.write(msg + "\n");
        }

        out.close();
    }

    public static void WriteToFile(String fileName, String msg) throws IOException {
        File file = db.getFile(fileName);
        BufferedWriter out = new BufferedWriter(new FileWriter(file.getAbsolutePath(), file.exists()));

        if (msg.endsWith("\n")) {
            out.write(msg);
        }
        else {
            out.write(msg + "\n");
        }

        out.close();
    }

    public static String getUploadableText(Context context, String fileName) throws Exception {
        File file = getFile(FILE_READY);
        if (!file.exists()) {
            boolean b = db.rename(false, fileName);
            if (!b) {
                throw new Exception("File not found: " + FILE_READY);
            }
        }
        String str = db.read(FILE_READY);
        return str;
    }

    public static File getFile(String fileName) {
        File externalMem2 = Environment.getExternalStorageDirectory();
        File directory2 = new File(externalMem2.getAbsolutePath() + DIRECTORY);
        directory2.mkdirs();
        fileName = (fileName == null) ? FILE_READY : fileName;
        File file = new File(directory2, fileName);

        return file;
    }

    public static String getAttack(String severity) {
        if (lastLocation == null) {
            lastLocation = new Location("null_location");
        }
        StringBuffer sb = new StringBuffer(512);
        long sessionNum = System.currentTimeMillis() / 1000000 * 60;

        if (lastLocation != null){
            GoogleMaps.PlotAttackOnMap(severity, lastLocation);
        }

        StringBuffer append = sb.append(
                "measured_at=" + (System.currentTimeMillis() / 1000) + "," +
                        "lat=" + lastLocation.getLatitude() + "," +
                        "lon=" + lastLocation.getLongitude() + "," +
                        "alt=" + lastLocation.getAltitude() + "," +
                        "speed=" + lastLocation.getSpeed() + "," +
                        "bearing=" + lastLocation.getBearing() + "," +
                        "accuracy=" + lastLocation.getAccuracy() + "," +
                        "record_type=" + severity + "," +
                        "session_num=" + sessionNum + "" +
                        ""
        );

        return sb.toString();
    }

    public static String getMedicine(String medicineUsed) {
        if (lastLocation == null) {
            lastLocation = new Location("null_location");
        }
        StringBuffer sb = new StringBuffer(512);
        long sessionNum = System.currentTimeMillis() / 1000000 * 60;

        if (lastLocation != null){
            GoogleMaps.PlotInhalerOnMap(lastLocation);
        }

        StringBuffer append = sb.append(
                "measured_at=" + (System.currentTimeMillis() / 1000) + "," +
                        "lat=" + lastLocation.getLatitude() + "," +
                        "lon=" + lastLocation.getLongitude() + "," +
                        "alt=" + lastLocation.getAltitude() + "," +
                        "speed=" + lastLocation.getSpeed() + "," +
                        "bearing=" + lastLocation.getBearing() + "," +
                        "accuracy=" + lastLocation.getAccuracy() + "," +
                        "medicine_Used=" + medicineUsed + "," +
                        "session_num=" + sessionNum + "" +
                        ""
        );

        return sb.toString();
    }

    public static String getPeakFlow(String pef, String fev) {
        if (lastLocation == null) {
            lastLocation = new Location("null_location");
        }
        else {
            GoogleMaps.PlotInhalerOnMap(lastLocation);
        }
        StringBuffer sb = new StringBuffer(512);
        long sessionNum = System.currentTimeMillis() / 1000000 * 60;

        StringBuffer append = sb.append(
                "measured_at=" + (System.currentTimeMillis() / 1000) + "," +
                        "record_type=" + ("peakflow") + "," +
                        "lat=" + lastLocation.getLatitude() + "," +
                        "lon=" + lastLocation.getLongitude() + "," +
                        "alt=" + lastLocation.getAltitude() + "," +
                        "speed=" + lastLocation.getSpeed() + "," +
                        "bearing=" + lastLocation.getBearing() + "," +
                        "accuracy=" + lastLocation.getAccuracy() + "," +
                        "pef=" + pef + "," +
                        "fev=" + fev + "," +
                        "session_num=" + sessionNum
        );

        String writeString = sb.toString();

        return writeString;
    }

    public static String getHeartBeat(Context cntx) {

        batLevel = getBatteryLevel(cntx);

        StringBuffer sb = new StringBuffer(512);
        long sessionNum = System.currentTimeMillis() / 1000000 * 60;
        long milliseconds = System.currentTimeMillis();
        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(cntx);
        String stringUName = SP.getString("username", "NA");
        PackageInfo pInfo = null;
        String versionStuff = null;

        try {
            pInfo = cntx.getPackageManager().getPackageInfo(cntx.getPackageName(), 0);
            versionStuff = "version=" + pInfo.versionName + "-" + pInfo.versionCode;
        }
        catch(Exception e) {
            Log.i("package", "could not get package info");
        }

        StringBuffer append = sb.append(
                "measured_at=" + (milliseconds / 1000) + "," +
                        "record_type=" + ("active") + "," +
                        "battery_level=" + batLevel + "," +
                        "session_num=" + sessionNum + "," +
                        versionStuff + "," +
                        "user=" + stringUName + "\n"
        );

        return sb.toString();
    }

    public static float getBatteryLevel(Context cntx) {
        Intent batteryIntent = cntx.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        // Error checking that probably isn't needed but I added just in case.
        if (level == -1 || scale == -1) {
            return 50.0f;
        }

        return ((float) level / (float) scale) * 100.0f;
    }

    public static String getInhalerData(int buttonPresses) {

        StringBuffer sb = new StringBuffer(512);
        long sessionNum = System.currentTimeMillis() / 1000000 * 60;

        StringBuffer append = sb.append(
                "inhaler_count=" + buttonPresses
        );

        return sb.toString();
    }

    public static void writeInhalerDataToFile(int pressDuration, Date pressTime){
        if (lastLocation == null) {
            lastLocation = new Location("null_location");
        }
//        else {
//            GoogleMaps.PlotInhalerOnMap(lastLocation);
//        }

        StringBuffer sb = new StringBuffer(512);


        StringBuffer append = sb.append(
                "measured_at=" + pressTime.getTime() / 1000 + "," +
                        "record_type=" + ("INHALER") + "," +
                        "lat=" + lastLocation.getLatitude() + "," +
                        "lon=" + lastLocation.getLongitude() + "," +
                        "is_valid=" + (1) + "," +
                        "accuracy=" + (0) + "," +
                        "session_num=" + (0) + "," +
                        "pef=" + pressDuration
        );

        try {
            Write(append.toString());
        } catch (IOException e) {
            e.printStackTrace();
            Log.i("Text File Write Error", "Error writing inhaler data to text file");
        }
    }

    public static boolean fileReady() {
        File to = getFile(FILE_READY);
        return to.exists();
    }

    public synchronized static boolean rename(boolean force, String fileName) {
        File from = getFile(fileName);
        File to = getFile(FILE_READY);

        if (!from.exists()) {
            return false;
        }
        if (to.exists() && !force) {
            return false;
        }
        from.renameTo(to);
        return true;
    }

    public static void delete() {
        File externalMem2 = Environment.getExternalStorageDirectory();
        File directory2 = new File(externalMem2.getAbsolutePath() + DIRECTORY);
        directory2.mkdirs();
        File to = new File(directory2, FILE_READY);
        to.delete();
        //File from = new File(directory2, FILE_NAME);
        //from.delete();
    }

    public static String canUploadData(Context ctx) {
        ConnectivityManager connManager = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        WifiManager wifiman = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        HashMap m = new HashMap();

        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(ctx);
        boolean useCell = SP.getBoolean("CellularData", false);
        SCHASSettings.useCellular = useCell;

        if (mWifi.isConnected()) {
            int linkSpeed = wifiman.getConnectionInfo().getLinkSpeed();
            int ip = wifiman.getConnectionInfo().getIpAddress();
            String ips = Formatter.formatIpAddress(ip);
            String str = "SPEED: " + linkSpeed + "Mbps, STRENGTH: " + wifiman.getConnectionInfo().getRssi() + "dBm";
            return str;
        }
        else if (useCell){
            ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info = cm.getActiveNetworkInfo();
            String str;
            if (info != null){
                str = "CONNECTION TYPE: " + info.getType();
            }
            else{
                str = "Null Network Connection Info";
            }

            return str;
        }
        return null;
    }

    public static synchronized String Upload(Context ctx, Activity act) throws IOException {
        contextForUpload = ctx;
        String str;
        if (null == (str = db.canUploadData(ctx))) {
            return "NO Wireless Connection and no cellular upload enabled! Please check back";
        }
        else{
            db.PrepareTextFile();
            db.CreateDuplicateFile();
            db.splitFileIntoUploadableChunks();
        }

        numberOfFilesUploaded = 1;
        numberOfFailedUploads = 0;
        String fileName = tempFileName + numberOfFilesUploaded + ".TXT";
        str = db.Post(act, ctx, "/aura/webroot/loc.jsp", fileName);
        if (str != null){
            numberOfFailedUploads++;
        }

        return null;
    }

    private static PostToServer POST_TO_SERVER = null;

    private static synchronized String Post(Activity act, Context context, String service, String fileName) {

        if (POST_TO_SERVER != null && !POST_TO_SERVER.COMPLETED) {   // Avoid race condition
            return "Message: One upload in progress, please wait ...";
        }

        String host = SCHASSettings.host;

        if (host == null || null == canUploadData(context) || host.equals("null")) {
            SCHASSettings.Initialize();
            return "Warning: Cannot find host: ";
        }
        File from = getFile(fileName);
        File ready = getFile(FILE_READY);
        if (ready.exists()){
            ready.delete();
        }
        from.renameTo(ready);

        //db.rename(false, fileName);
        if (!db.fileReady()) {
            return "Message: " + SCHASSettings.host + " No files to upload!!";
        }

        String url = "http://" + host + service;
        List<NameValuePair> nv = new ArrayList<NameValuePair>(2);
        String msg = "";

        try {
            msg = getUploadableText(context, fileName);
        } catch (Exception e) {
            return "ERROR: exception while reading input file " + e;
        }
        String ID = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);

        //String msg1 = msg.substring(0, Math.min(msg.length(), 1024 * 1024 * 1 ));
        nv.add(new BasicNameValuePair("api_key", ID));
        nv.add(new BasicNameValuePair("mobile_id", ID));
        nv.add(new BasicNameValuePair("text", msg));
        nv.add(new BasicNameValuePair("maxPostSize", ""+msg.length()));
        //nv.add(new BasicNameValuePair("maxPostSize", "-1"));

        String ns = "Sending: " + url + "\n" + msg.substring(0, Math.min(msg.length() - 1, 256));
        Log.i("", ns);

        POST_TO_SERVER = null;
        POST_TO_SERVER = new PostToServer(nv, act, true);
        POST_TO_SERVER.execute(url);

        return null;
    }

    public static String ContinueUpload(Activity act){
        numberOfFilesUploaded++;
        String str;

        if(numberOfFilesUploaded > numberOfSplitFiles){
            Log.w("DB:post:", " Post succeeded");
            contextForUpload = null;
            return "upload was completed, number of failed uploads: " + numberOfFailedUploads;
        }

        String fileName = tempFileName + numberOfFilesUploaded + ".TXT";
        str = db.Post(act, contextForUpload, "/aura/webroot/loc.jsp", fileName);
        if (str != null){
            numberOfFailedUploads++;
        }

        return null;
    }

    //retrieves a location from the maps fragment when called there, and pushes in a location
    //to be written to the data file
    public static void getLocationData(Location location, String provider) {
        lastLocation = location;
        StringBuffer sb = new StringBuffer(512);
        float sessionNum = System.currentTimeMillis() / 1000000 * 60;
        boolean isValid = true;
        GoogleMaps.lineCount++;

        sb.append(
                "measured_at=" + (System.currentTimeMillis() / 1000) + "," +
                        "lat=" + location.getLatitude() + "," +
                        "lon=" + location.getLongitude() + "," +
                        "alt=" + location.getAltitude() + "," +
                        "speed=" + location.getSpeed() + "," +
                        "bearing=" + location.getBearing() + "," +
                        "accuracy=" + location.getAccuracy() + "," +
                        "record_type=" + provider + "," +
                        "session_num=" + sessionNum + "," +
                        "is_valid=" + isValid
        );

        String writeString = sb.toString();

        try {
            Write(writeString + "\n");
        } catch (IOException e) {
            Log.e("ERROR", "Exception appending to log file", e);
        }
    }

    public static List ReadFromLocationsFile() throws IOException{
        List locationList = new ArrayList();

        //open the file that has all of the location values stored within it
        File file = db.getFile(db.FILE_NAME);

        if (!file.exists()) {
            return locationList;
        }

        BufferedReader in = new BufferedReader(new FileReader(file));
        String nextLine = in.readLine();

        //while the next line to be read does not return null (empty line, or EOF)
        while (nextLine != null) {
            //check to see if this line is a location or a heartbeat or a peakflow reading
            if (nextLine.contains("lat=") && !nextLine.contains("peakflow") && !nextLine.contains("INHALER")) {
                //split the string by the '=' and ',' delimiters
                String[] currentLine = nextLine.split("=|,");

                DatabaseLocationObject dlo = new DatabaseLocationObject(
                        currentLine[1],
                        Float.valueOf(currentLine[3]),
                        Float.valueOf(currentLine[5]),
                        currentLine[7],
                        currentLine[9],
                        currentLine[11],
                        currentLine[13],
                        currentLine[15],
                        currentLine[17],
                        Boolean.valueOf(currentLine[19])
                );

                locationList.add(dlo);
            }

            else{
                otherDataLines.add(nextLine);
            }
            nextLine = in.readLine();
        }

        in.close();
        file.delete();

        return locationList;
    }

    //take the information from the DLO's and create the info for the text file
    //wipe the file at the start of this method
    public static void PrepareTextFile() {
        File file = db.getFile(db.FILE_NAME);

        List<DatabaseLocationObject> locationList = new ArrayList<>();
        try{
            locationList = GoogleMaps.GetDLOList();
        } catch(Exception e){
            Log.i("upload", "there was an exception getting locations from google map fragment");
        }

        for (DatabaseLocationObject dlo : locationList) {
            String writeString = dlo.ToString();

            try {
                Write(writeString + "\n");
            } catch (IOException e) {
                Log.e("ERROR", "Exception appending to log file", e);
            }
        }

        for (String otherData : otherDataLines){
            try {
                Write(otherData + "\n");
            } catch (IOException e) {
                Log.e("ERROR", "Exception appending to log file", e);
            }
        }

    }

//    //use to compare the location values from a clicked marker with
//    //values in the text file
//    public void compareLocation(double markerLat, double markerLon) throws IOException {
//        //if the marker is clicked, find the location in the text file and mark invalid
//        File file = db.getFile(db.FILE_NAME);
//        BufferedReader in = new BufferedReader(new FileReader(file));
//        String nextLine = in.readLine();
//        int lineCount = 1;
//
//        //while the next line to be read does not return null (empty line, or EOF)
//        while (nextLine != null) {
//            if (nextLine.contains("lat=")) {
//                //get the indices of the known substrings
//                int indexLat = nextLine.indexOf("lat=");
//                int indexLon = nextLine.indexOf("lon=");
//                int indexAlt = nextLine.indexOf("alt=");
//                //create substrings for the values of the lat and lon floats between known indices
//                String latString = nextLine.substring(indexLat + 4, indexLon-1);
//                String lonString = nextLine.substring(indexLon + 4, indexAlt - 1);
//
//                //cast the strings to floats and put into the static array in GoogleMaps for plotting
//                double thisLat = Double.valueOf(latString);
//                double thisLon = Double.valueOf(lonString);
//
//                if ((thisLat == markerLat) && (thisLon == markerLon)) {
//
//                }
//            }
//            nextLine = in.readLine();
//            lineCount++;
//        }
//        in.close();
//    }

    //Old method, new one is above
    //use a readbuffer to read in from the txt and plot the points
//    public static void plotTxtPoints() throws IOException
//    {
//        //open the file that has all of the location values stored within it
//        File file = db.getFile(db.FILE_NAME);
//
//        if (!file.exists()) {
//            return;
//        }
//
//        BufferedReader in = new BufferedReader(new FileReader(file));
//        String nextLine = in.readLine();
//
//        //while the next line to be read does not return null (empty line, or EOF)
//        while (nextLine != null) {
//            GoogleMaps.lineCount++;
//            //check to see if this line is a location or a heartbeat
//            if (nextLine.contains("lat=")) {
//                //get the indices of the known substrings
//                int indexLat = nextLine.indexOf("lat=");
//                int indexLon = nextLine.indexOf("lon=");
//                int indexAlt = nextLine.indexOf("alt=");
//                //create substrings for the values of the lat and lon floats between known indices
//                String latString = nextLine.substring(indexLat + 4, indexLon-1);
//                String lonString = nextLine.substring(indexLon + 4, indexAlt - 1);
//
//                //cast the strings to floats and put into the static array in GoogleMaps for plotting
//                double nextLat = Double.valueOf(latString);
//                double nextLon = Double.valueOf(lonString);
//                LatLng nextlatLng = new LatLng(nextLat, nextLon);
//                GoogleMaps.AddToLocList(nextlatLng);
//            }
//            nextLine = in.readLine();
//        }
//
//        in.close();
//    }

    //use a readbuffer to read in from the secondary txt and plot the points
    public static void plotSecondaryTxtPoints() throws IOException
    {
        //open the file that has all of the location values stored within it
        File file = db.getFile(SECONDARY_FILE_NAME);

        if (!file.exists()) {
            Log.i("plot secondary", "");
            return;
        }

        Log.i("reading secondary text", "");
        BufferedReader in = new BufferedReader(new FileReader(file));
        String nextLine = in.readLine();

        //while the next line to be read does not return null (empty line, or EOF)
        while (nextLine != null) {
            //GoogleMaps.lineCount++;
            //check to see if this line is a location or a heartbeat
            if (nextLine.contains("lat=") && !nextLine.contains("peakflow") && !nextLine.contains("INHALER")) {
                //get the indices of the known substrings
                int indexLat = nextLine.indexOf("lat=");
                int indexLon = nextLine.indexOf("lon=");
                int indexAlt = nextLine.indexOf("alt=");
                //create substrings for the values of the lat and lon floats between known indices
                String latString = nextLine.substring(indexLat + 4, indexLon-1);
                String lonString = nextLine.substring(indexLon + 4, indexAlt - 1);

                //cast the strings to floats and put into the static array in GoogleMaps for plotting
                double nextLat = Double.valueOf(latString);
                double nextLon = Double.valueOf(lonString);
                LatLng nextlatLng = new LatLng(nextLat, nextLon);
                GoogleMaps.AddToSecondaryLocList(nextlatLng);
            }
            nextLine = in.readLine();
        }

        in.close();
    }

    public static void CreateDuplicateFile()throws IOException{
        File externalMem = Environment.getExternalStorageDirectory();
        File directory = new File(externalMem.getAbsolutePath() + DIRECTORY);

        File oldFile = getFile((SECONDARY_FILE_NAME));
        if(oldFile.exists()) oldFile.delete();

        File from = getFile(FILE_NAME);

        File to = new File (directory, SECONDARY_FILE_NAME);

        InputStream in = new FileInputStream(from);
        OutputStream out = new FileOutputStream(to);

        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();

        GoogleMaps.removeMarkers();

        try {
            GoogleMaps.RefreshMapAfterUpload();
        } catch (Exception e) {
            throw new IOException("could not refresh google map" + e);
        }
    }

    public static void CreateGoogleLocationFile(List<Entry> trackList){
        for (Entry entry : trackList){
            String[] coords = entry.location.trim().split("\\s+");
            //location string format from Google:
            //longitude+" "+latitude+" "+alt(?)
            StringBuffer sb = new StringBuffer(512);

            sb.append(
                "lat=" + coords[1] + "," +
                "lon=" + coords[0] + "," +
                "record_type=" + "goog"
            );

            String writeString = sb.toString();

            try {
                Write(writeString + "\n");
            } catch (IOException e) {
                Log.e("ERROR", "Exception appending to log file", e);
            }
        }
    }

    public static int GetNumberOfLocations() throws IOException
    {
        //open the file that has all of the location values stored within it
        File file = db.getFile(db.FILE_NAME);

        int count = 0;

        if (!file.exists()) {
            return count;
        }

        BufferedReader in = new BufferedReader(new FileReader(file));
        String nextLine = in.readLine();

        //while the next line to be read does not return null (empty line, or EOF)
        while (nextLine != null) {
            //check to see if this line is a location or a heartbeat
            if (nextLine.contains("lat=")) {
               count++;
            }
            nextLine = in.readLine();
        }
        in.close();

        return count;
    }

    public static void splitFileIntoUploadableChunks() throws IOException{
        int numberOfLines = 0;
        numberOfSplitFiles = 1;

        File file = db.getFile(db.FILE_NAME);

        BufferedReader in = new BufferedReader(new FileReader(file));
        String nextLine = in.readLine();

        File tempFile = db.getFile(tempFileName + numberOfSplitFiles + ".TXT");
        OutputStream out = new FileOutputStream(tempFile);

        //while the next line to be read does not return null (empty line, or EOF)
        while (nextLine != null) {
            numberOfLines++;
            nextLine += "\n";
            out.write(nextLine.getBytes());

            nextLine = in.readLine();
            if (numberOfLines >= 50){
                out.close();
                numberOfSplitFiles++;
                tempFile = getFile(tempFileName + numberOfSplitFiles + ".TXT");
                out = new FileOutputStream(tempFile);
                numberOfLines = 0;
            }
        }
        in.close();
        out.close();

        file.delete();
    }
}
