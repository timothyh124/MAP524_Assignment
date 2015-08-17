package com.seneca.shan42.lineups;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class LoadingActivity extends FragmentActivity implements LocationListener{
    private final String DB_FILE_NAME = "BWT";
    private final String TAG = "LINEUPS";
    private DataFetchManager dfMgr;
    private LocationManager locManager;
    private String provider;
    private Location curLoc = null;
    private LocationListener locListener = this;
    final Context context = this;
    final Activity thisActivity = this;
    private String[] officeList = null;
    private boolean sorted = false;

    // AsyncTask class for calculating the distance from current location to
    // locations of offices stored in database
    // at the end, it sorts the office list by closest distance in the beginning of array
    private class NearestOfficeServiceTask extends
            AsyncTask<LocationManager, Void, String[]> {

        NearestOfficeServiceTask() { }

        @Override
        protected String[] doInBackground(LocationManager... locMgr) {
            String[] offices;
            Location[] officeLocs;
            Cursor result;
            SQLiteDBHandler dbHandler;
            // get current location in advance
            curLoc = locManager.getLastKnownLocation(provider);
            // get Location data from sqlite db
            dbHandler = new SQLiteDBHandler(getApplicationContext(), Constants.DB_NAME);
            SQLiteDatabase dbr = dbHandler.getReadableDatabase();
            result = dbr.query(Constants.LOCATION_TABLE, null, null, null, null, null, null);
            offices = new String[result.getCount()];
            officeLocs = new Location[result.getCount()];

            // get office list and locations of all offices
            for(int idx = 0; idx < result.getCount(); ++idx) {
                result.moveToNext();
                officeLocs[idx] = new Location("");
                officeLocs[idx].setLatitude(result.getDouble(result.getColumnIndex("LATITUDE")));
                officeLocs[idx].setLongitude(result.getDouble(result.getColumnIndex("LONGITUDE")));
                offices[idx] = result.getString(result.getColumnIndex("NAME"));
            }

            // get current location or wait for 5 second
            long startTime = SystemClock.elapsedRealtime();
            long endTime;
            long elapsedMilliSeconds = 0;
            while(curLoc == null && elapsedMilliSeconds < 10000.0){
                curLoc = locManager.getLastKnownLocation(provider);
                endTime = SystemClock.elapsedRealtime();
                elapsedMilliSeconds = endTime - startTime;
            }

            // get distance between current location to each offices
            // and sort office based by distnace ascending
            if(curLoc != null) {
                sorted = true;
                float[] distances = new float[result.getCount()];
                for (int idx = 0; idx < officeLocs.length; ++idx) {
                    distances[idx] = getDistance(curLoc, officeLocs[idx]);
                }
                // simple selection sort
                int idxMin;
                float tmpDist;
                String tmpName;
                for (int i = 0; i < distances.length - 1; ++i) {
                    idxMin = i;
                    for (int j = i+1; j < distances.length; ++j) {
                        if(distances[j] < distances[idxMin]) {
                            idxMin = j;
                        }
                    }
                    if(idxMin != i) {
                        // this can be done with generic swap function
                        // will be updated if I have extra time
                        tmpDist = distances[i];
                        distances[i] = distances[idxMin];
                        distances[idxMin] = tmpDist;
                        tmpName = offices[i];
                        offices[i] = offices[idxMin];
                        offices[idxMin] = tmpName;
                    }
                }
                for(int i = 0; i < offices.length; i++) {
                    Log.d(TAG, "Office: " + offices[i] + ", Distance: " + distances[i]);
                }
            } else {
                for(int i = 0; i < offices.length; i++) {
                    Log.d(TAG, "Office: " + offices[i]);
                }
            }
            return offices;
        }
        // shows the dialog with the prepared office list
        // it can be ordered by distance to current position or
        // default list if current location is not available
        protected void onPostExecute(String[] result) {
            final AlertDialog.Builder locBuilder = new AlertDialog.Builder(context);
            ArrayAdapter adapter;
            LayoutInflater inflater = getLayoutInflater();
            View convertView = (View) inflater.inflate(R.layout.acvitivy_init_listview, null);
            locBuilder.setView(convertView);
            ListView lv = (ListView) convertView.findViewById(R.id.init_lv);

            officeList = result;

            if(sorted) {
                String[] displayList = new String[4];
                for(int i = 0; i < 4; ++i) {
                    displayList[i] = officeList[i];
                }
                locBuilder.setTitle("Found 4 nearest custom offices");
                adapter = new ArrayAdapter(context, R.layout.layout_custom_listview, displayList);
            } else {
                locBuilder.setTitle("All custom offices");
                adapter = new ArrayAdapter(context, R.layout.layout_custom_listview, officeList);
            }

            lv.setAdapter(adapter);
            //must dismiss dialog on item click
            lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    //set preferred location
                    intent.putExtra("prefOffice", officeList[position]);
                    intent.putExtra("officeList", officeList);
                    startActivity(intent);
                    thisActivity.finish();
                }
            });
            final AlertDialog locAlert = locBuilder.create();
            locAlert.setCancelable(false);

            locAlert.show();


        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_loading);
        View screen = findViewById(R.id.loading_screen);

        /**
         * Check if database exists
         */
        try {
            /* create path and file */
            String destPath = "/data/data/" + context.getPackageName() +
                    "/databases/";
            File destPathFile = new File(destPath);
            if(!destPathFile.exists())
                destPathFile.mkdirs();
            File destFile = new File(destPath + DB_FILE_NAME);
            if(!destFile.exists()) {
                Log.d(TAG, "First run, copying default database!");
                copyFile(context.getAssets().open(DB_FILE_NAME),
                        new FileOutputStream(destPath + "/" + DB_FILE_NAME));
            }
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } // end of database check
        /**
         * Request for data file and parse them into object in background
         */
        Log.d(TAG, "requesting thread run for data fetch");
        dfMgr = new DataFetchManager(context);
        dfMgr.fetchData();

        /**
         * Check if location service is on
         */
        // get current location
        locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        provider = locManager.getBestProvider(criteria, false);

        // check if gps is enabled
        // source from http://stackoverflow.com/questions/843675/how-do-i-find-out-if-the-gps-of-an-android-device-is-enabled

        if (!locManager.isProviderEnabled(provider) ) {
            Log.d(TAG, "setting location manager");
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                    .setCancelable(false)
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                            dialog.dismiss();
                            startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));

                            locManager.requestLocationUpdates(provider, 1, 1, locListener);

                            // wait until we get the current location
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                            dialog.dismiss();
                        }
                    });
            final AlertDialog providerAlert = builder.create();
            providerAlert.setCancelable(false);
            providerAlert.show();
        } else {
            Log.d(TAG, "SOFAR SO GOOD");
        }
        locManager.requestLocationUpdates(provider, 1, 1,locListener);
        new NearestOfficeServiceTask().execute(locManager);
    }

    /* Copy file
     * source : http://stackoverflow.com/questions/9292954/how-to-make-a-copy-of-a-file-in-android
     */
    private void copyFile(InputStream src, FileOutputStream dst) throws IOException {
        byte[] buf = new byte[1024];
        OutputStream os = dst;
        int len;
        try {
            while((len = src.read(buf))> 0) {
                os.write(buf, 0, len);
            }
            os.close();
            os.flush();
        } catch(IOException e) {
            Log.d(TAG,e.getMessage());
        }
    }

    private float getDistance(Location a, Location b) {
        return a.distanceTo(b);
    }
    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            Log.d(TAG, location.getLatitude() + " and " + location.getLongitude());
            curLoc = location;
            locManager.removeUpdates(this); // I only need location once
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}
