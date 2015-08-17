package com.seneca.shan42.lineups;

import android.content.ContentValues;

/**
 * Created by timothy on 12/08/15.
 */
/**
 * Poorly built Canada Office class inherited from Office
 * for data management
 */
public class CanadaOffice extends Office {
    private String m_commercial = "N/A";
    private String m_travel = "N/A";

    public CanadaOffice() {
        super();
    }

    public CanadaOffice(String name, String location, String lastUpdated, String commercial, String travel) {
        super(name, location, lastUpdated);
        if(!commercial.isEmpty() && !travel.isEmpty()) {
            this.m_commercial = commercial;
            this.m_travel = travel;
        }
    }
    @Override
    public String toString() {
        if(m_commercial.equals("N/A") || m_travel.equals("N/A"))
            return "Object: CaadaOffice - This object is empty";
        else
            return super.toString() + ", Commercial: " + m_commercial +
                        ", Travel: " + m_travel;
    }

    /*
     * returns map(ContentValues) for data insertion or update
     */
    public ContentValues getContentValues() {
        ContentValues cv = super.getContentValues();
        cv.put("COMMERCIAL_CA", m_commercial);
        cv.put("TRAVELLER_CA", m_travel);
        return cv;
    }
    public String getName() {
        return super.getName();
    }
    public String getLastUpdated () {
        return super.getLastUpdated();
    }
    public String getCommercialWaitTime() {
        return m_commercial;
    }
    public String getTravelWaitTime() {
        return m_travel;
    }

}
