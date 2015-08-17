package com.seneca.shan42.lineups;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by timothy on 13/08/15.
 */

/**
 * Simple Database Handler as done through lab
 */
public class SQLiteDBHandler extends SQLiteOpenHelper {
    private static final int VERSION = 1;

    /* For content provider */
    public SQLiteDBHandler(Context context) {
        super(context, Constants.DB_NAME, null, VERSION);
    }
    /* For internal App use */
    public SQLiteDBHandler(Context context, String dbName) {
        super(context, dbName.toUpperCase(), null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) { }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVer, int newVer) { }
}