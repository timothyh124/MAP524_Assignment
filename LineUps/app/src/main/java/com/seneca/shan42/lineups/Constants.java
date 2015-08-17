package com.seneca.shan42.lineups;

/**
 * Created by timothy on 10/08/15.
 */
public class Constants {
    // Shared preference
    public static final String PREFERENCE_FILE_KEY=
            "com.seneca.shan42.lineups.PREFERENCE_FILE_KEY";
    // data feed source
    public static final String DATA_URL=
            "http://www.cbsa-asfc.gc.ca/bwt-taf/bwt-eng.csv";
    // Defines the key for the status "extra" in an Intent
    public static final int COLUMN_SIZE = 7;
    public static final String EXPECTED_COLUMNS =
            "Customs Office;; Location;; Last updated;; Commercial Flow - Canada bound;; Commercial Flow - U.S. bound;; Travellers Flow - Canada bound;; Travellers Flow - U.S. bound;;";
    public static final String DB_NAME = "BWT";
    public static final String TABLE_NAME = "WAIT_TIME_LIST";
    public static final String LOCATION_TABLE = "locations";
    public static final int INIT_LOC_NUMB = 3;
    public static final String SERVICE_INTENT=
            "com.seneca.shan42.BROADCAST.COMPLETE";
    public static final String BROADCAST_ACTION =
            "com.seneca.shan42.STATUS";
    // Defines the key for the status "extra" in an Intent
    public static final String EXTENDED_DATA_STATUS =
            "com.seneca.shan42.ACTION.ASK";
}
