package com.seneca.shan42.lineups;

import android.content.ContentValues;

/**
 * Created by timothy on 12/08/15.
 */

/**
 * Poorly built Office class which will be the parent
 * of Canadian office and U.S. offcie
 * Currently, only Canadian offices are implemented
 */
public class Office {
    private String m_name = "N/A";
    private String m_location = "N/A";
    private String m_lastUpdated = "N/A";

    protected Office() { }

    protected Office(String name, String location, String lastUpdated) {
        this();
        if(!name.isEmpty() && !location.isEmpty()) {
            this.m_name = name;
            this.m_location = location;
            this.m_lastUpdated = lastUpdated;

        }
    }

    @Override
    public String toString() {
        if(m_name.equals("N/A") || m_location.equals("N/A"))
            return "Object: Office - This object is empty";
        else
            return "Name: " + m_name +", Location: " + m_location + ", Updated: " + m_lastUpdated;
    }

    /**
     * returns map(ContentValues) for db insert
     * @return
     */
    protected ContentValues getContentValues() {
        ContentValues cv = new ContentValues();
        cv.put("NAME", m_name);
        cv.put("LOCATION", m_location);
        cv.put("LAST_UPDATED", m_lastUpdated);
        return cv;
    }
    protected String getName() {
        return this.m_name;
    }
    protected String getLastUpdated () {
        return this.m_lastUpdated;
    }
}
