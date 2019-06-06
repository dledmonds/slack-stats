package com.dledmonds.slack;

import java.util.Date;

/**
 * @author dledmonds
 */
public class Utils {

    public static Date convertTsToDate(String strTs) {
        double ts = Double.parseDouble(strTs);
        Date date = new Date((long)(Math.floor(ts) * 1000));
        return date;
    }

}
