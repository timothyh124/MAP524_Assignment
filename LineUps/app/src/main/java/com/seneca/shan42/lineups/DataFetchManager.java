package com.seneca.shan42.lineups;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;
import org.w3c.dom.Text;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.Timestamp;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Objects;

/**
 * Created by timothy on 12/08/15.
 */
public class DataFetchManager {
    private SharedPreferences sharedPref;
    private Context m_context;
    private final String TAG = "LINEUPS";
    private final String URL_PATH = "http://www.cbsa-asfc.gc.ca/bwt-taf/bwt-eng.csv";
    private ArrayList<CanadaOffice> officeObjs = null;
    private int targetIdx =0;
    static private ArrayList<String> fetchedRawData = null;
    static private Date lastUpdate;
    static boolean doUpdate = false;
    static private String currentNotification = "";
    private AsyncTask<Void,Void,Void> currentDaemon;

    /* Private inner Class */
    // Do data fetch from the source but No GUI interaction
    private class DataFetchService extends
            AsyncTask<Void, Void, ArrayList<String>> {

        protected ArrayList<String> doInBackground(Void... param) {
            Log.d(TAG,"Data fetch thread Started");
            return fetchDataTask();
        }

        protected void onPostExecute(ArrayList<String> result) {
            Log.d(TAG,"Data fetch thread Ended");
            fetchedRawData = result;
            lastUpdate = new Date();
        }
    }

    /*
     * parse the existing fetched data and
     * updates GUI based on parsed data
     */
    private class DataParseDisplayService extends
            AsyncTask<Void, Void, ArrayList<CanadaOffice>> {
        private String m_currentOffice;
        private Activity m_activity;

        DataParseDisplayService(String selectedOffice, Activity activity) {
            this.m_currentOffice = selectedOffice;
            this.m_activity = activity;
        }
        protected ArrayList<CanadaOffice> doInBackground(Void... param) {
            Log.d(TAG, "Parse thread started");
            return parseCanadaOfficeTask();
        }

        protected void onPostExecute(ArrayList<CanadaOffice> result) {
            // screen setup
            officeObjs = result;
            setCurOffice(m_currentOffice);
            updateGUI(m_activity);

        }
    }
    /*
     * fetches new data from the source and parse it.
     * In addition, updates GUI based on new information
     */
    private class UpdateAndDisplayDataService extends
            AsyncTask<Void,Void,ArrayList<CanadaOffice>> {
        Activity m_activity;

        UpdateAndDisplayDataService(Activity activity) {
            this.m_activity = activity;
        }
        @Override
        protected ArrayList<CanadaOffice> doInBackground(Void... params) {
            return updateAndParseDataTask();

        }protected void onPostExecute(ArrayList<CanadaOffice> result) {
            officeObjs = result;
            String curOffice = ((TextView)m_activity.findViewById(R.id.office_name)).getText().toString();
            setCurOffice(curOffice);
            updateGUI(m_activity);
        }
    }
    /*
     * A daemon service for automated update and notification sender
     */
    private class UpdateAndNotificationDaemonService extends AsyncTask<Void, Void, Void> {
        private Activity m_activity;
        private int m_targetIdx = 0;

        public UpdateAndNotificationDaemonService(Activity activity, String selectedOffice) {
            this.m_activity = activity;
            for(CanadaOffice e: officeObjs) {
                if(e.getName().equals(selectedOffice))
                    m_targetIdx = officeObjs.indexOf(e);
            }
        }
        @Override
        protected Void doInBackground(Void... params) {
            SharedPreferences sharedPref = m_activity.getApplicationContext().
                                                        getSharedPreferences(Constants.PREFERENCE_FILE_KEY, Context.MODE_PRIVATE);
            // it will run while doUpdate is true
            while(doUpdate) {
                try {
                    Thread.sleep(10*60*1000); // sleep for 10 minutes
                } catch (InterruptedException e) {
                    Log.d(TAG,e.getMessage());
                }
                officeObjs = updateAndParseDataTask();
                CanadaOffice office = officeObjs.get(m_targetIdx);
                /*
                 * Source code for notification from RSS service lab source
                 */
                NotificationCompat.Builder mBuilder =
                        new NotificationCompat.Builder(m_activity)
                                .setSmallIcon(R.mipmap.ic_launcher)
                                .setContentTitle("LineUps - " + office.getName());
                if(sharedPref.getString("type", "Travel").equals("Travel")) {
                    mBuilder.setContentText("Current travel wait time: " + office.getTravelWaitTime());
                } else {
                    mBuilder.setContentText("Current commercial wait time: " + office.getCommercialWaitTime());
                }
                NotificationManager mNotificationManager = (NotificationManager) m_activity.getSystemService(Context.NOTIFICATION_SERVICE);

                mNotificationManager.notify(123, mBuilder.build());
            }
            return null;
        }
    }
    /* Updates current view based on conditions
     * and user preferences
     */
    public void updateGUI(Activity activity) {
        SharedPreferences sharedPref = activity.getApplicationContext().
                                                getSharedPreferences(Constants.PREFERENCE_FILE_KEY, Context.MODE_PRIVATE);
        TextView officeName = (TextView) activity.findViewById(R.id.office_name);
        TextView waitTime = (TextView) activity.findViewById(R.id.time_to_wait);
        TextView lastUpdated = (TextView) activity.findViewById(R.id.last_updated);
        TextView method = (TextView) activity.findViewById(R.id.method);
        Switch notiSwitch = (Switch) activity.findViewById(R.id.noti_switch);
        Log.d(TAG,"OfficeName: " +officeName.getText().toString());
        Log.d(TAG,"Current Setting: "+currentNotification);

        officeName.setText(officeObjs.get(targetIdx).getName());

        if(currentNotification.equals(officeName.getText().toString())) {
            notiSwitch.setChecked(true);
        } else {
            notiSwitch.setChecked(false);
        }
        // need to set either travel or commerical wait time
        // depends on user preference
        Log.d(TAG,sharedPref.getString("type","Travel"));
        if(sharedPref.getString("type","Travel").equals("Travel")) {
            method.setText("for Travel Line");
            waitTime.setText(officeObjs.get(targetIdx).getTravelWaitTime());
        } else {
            method.setText("for Commercial Line");
            waitTime.setText(officeObjs.get(targetIdx).getCommercialWaitTime());
        }
        Date curTime = new Date();
        lastUpdated.setText(((curTime.getTime() - lastUpdate.getTime())/60000)+" minutes ago");
    }
    /* Default constructor */
    public DataFetchManager(Context context) {
        this.m_context = context;
    }

    /*
     * Simple http connection Opener
     */
    private InputStream openHttpConnection()
            throws IOException {
        InputStream in = null;
        int response = -1;

        URL url = new URL(URL_PATH);
        URLConnection conn = url.openConnection();

        if (!(conn instanceof HttpURLConnection))
            throw new IOException("Not an HTTP connection");
        try {
            HttpURLConnection httpConn = (HttpURLConnection) conn;
            httpConn.setAllowUserInteraction(false);
            httpConn.setInstanceFollowRedirects(true);
            httpConn.setRequestMethod("GET");
            httpConn.connect();
            response = httpConn.getResponseCode();
            if (response == HttpURLConnection.HTTP_OK) {
                in = httpConn.getInputStream();
            }

        } catch (Exception ex) {
            Log.d(TAG, ex.getLocalizedMessage());
            throw new IOException("Error connecting");
        }
        return in;
    }

    /* fetch data from the online source
     * and returns array of strings
     */
    private ArrayList<String> fetchDataTask() {
        ArrayList<String> fetchedData = new ArrayList<String>();
        BufferedReader reader;
        try {
            InputStream is = openHttpConnection();
            reader = new BufferedReader(new InputStreamReader(is));
            String tmp;
            tmp = reader.readLine();
            if(!tmp.equalsIgnoreCase(Constants.EXPECTED_COLUMNS)) {
                throw new RuntimeException("Source table schema has changed!");
            }
            while ((tmp = reader.readLine()) != null) {
                tmp.equalsIgnoreCase(Constants.EXPECTED_COLUMNS);
                fetchedData.add(tmp);
                Log.d(TAG,tmp);
            }
            is.close();
            reader.close();
        } catch (IOException e) {
            Log.d(TAG, e.getMessage());
        } catch (RuntimeException e) {
            Log.d(TAG, e.getMessage());
        }
        return fetchedData;
    }

    /* Parses fetched Raw Data string array into list of
     * CanadaOffice objects
     * insert them into database as well for future purpose
     */
    private ArrayList<CanadaOffice> parseCanadaOfficeTask() {


        //Log.d("TAG","Fetched DATA: "+fetchedRawData);
        ArrayList<CanadaOffice> offices = new ArrayList<>();
        if(fetchedRawData != null) {

            String tmpSet[] = null;
            if (!fetchedRawData.isEmpty()) {

                for (String e : fetchedRawData) {
                    tmpSet = e.split(";;");
                    /** WHY BELOW CONDITION FAIL?
                     * NEED TO CHECK PARSED COLUMN NUMBER AND WHAT I SET IT FOR
                     */
                    //if (tmpSet.length == Constants.COLUMN_SIZE) {
                        offices.add(new CanadaOffice(tmpSet[0], tmpSet[1].split("/")[0], tmpSet[2], tmpSet[3], tmpSet[5]));
                    //}
                }
                /**
                 * Database Insert
                 */
                String query;
                SQLiteDBHandler db = new SQLiteDBHandler(m_context);
                SQLiteDatabase dbw = db.getWritableDatabase();
                SQLiteDatabase dbr = db.getReadableDatabase();
                Cursor result;


                int qryResult = 0;
                for (CanadaOffice e : offices) {
                    Log.d(TAG,"test: "+e.toString());
                    qryResult = dbw.update(Constants.TABLE_NAME, e.getContentValues(),"NAME = '" +
                            e.getContentValues().get("NAME").toString()+"'", null);
                    if(qryResult == 0) {
                        dbw.insert(Constants.TABLE_NAME,null, e.getContentValues());
                    }
                }
                result = dbr.query(Constants.TABLE_NAME, null, null,null,null,null,null);
                if(result.getCount() == 0) Log.d(TAG, "WTH?");
                for(int idx = 0; idx < result.getCount(); ++idx) {
                    result.moveToNext();
                    Log.d(TAG, result.getString(result.getColumnIndex("NAME")) + ", " +
                            result.getString(result.getColumnIndex("LOCATION")) + ", " +
                            result.getString(result.getColumnIndex("LAST_UPDATED")) + ", " +
                            result.getString(result.getColumnIndex("COMMERCIAL_CA")) + ", " +
                            result.getString(result.getColumnIndex("TRAVELLER_CA")));
                }
                dbr.close();
                dbw.close();
            }
        }

        return offices;
    }

    /*
     * fetch data and parse data into array of canada objects
     */
    public ArrayList<CanadaOffice> updateAndParseDataTask() {
        fetchedRawData = fetchDataTask();
        return parseCanadaOfficeTask();
    }


    //1
    public void fetchData() {
        new DataFetchService().execute();
    }
    //2
    public void parseAndDisplayData(String selectedOffice, Activity activity) {
        new DataParseDisplayService(selectedOffice, activity).execute();
    }

    //3
    public void updateAndDisplayData(Activity activity) {
        new UpdateAndDisplayDataService(activity).execute();
    }

    /*
     * turn on switch to start the automated update cycle
     */
    public void beginUpdateCycle(Activity activity, String selectedOffice) {
        if(doUpdate) currentDaemon.cancel(true);
        currentDaemon = new UpdateAndNotificationDaemonService(activity, selectedOffice).execute();
        doUpdate = true;
    }
    /*
     * turn off switch to stop the automated update cycle
     */
    public void endUpdateCycle() {
        if(doUpdate) currentDaemon.cancel(true);
        doUpdate = false;
    }
    /*
     * set flag for currently displayed office
     */
    public void setCurOffice(String name) {
        for(CanadaOffice e: officeObjs) {
            if(e.getName().equals(name))
                targetIdx = officeObjs.indexOf(e);
                Log.d(TAG,"In Set Cur Office: "+e.toString());
        }

    }
    public boolean isEmpty() {
        if(fetchedRawData != null)
            return false;
        else return true;
    }
    /* Trigger and flag of currently set office for notification

     */
    public void setCurrentNotification(String office) {
        currentNotification = office;
    }
    /*
     * Check if notification is active
     */
    public boolean isCurrentNotification(String office) {
        return currentNotification.equals(office);

    }
}
