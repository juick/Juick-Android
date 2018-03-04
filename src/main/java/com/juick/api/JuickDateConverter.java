package com.juick.api;

import com.bluelinelabs.logansquare.typeconverters.DateTypeConverter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

/**
 * Created by vt on 04/03/2018.
 */

public class JuickDateConverter extends DateTypeConverter {
    private final DateFormat dateFormat;
    public JuickDateConverter() {
        dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }
    @Override
    public DateFormat getDateFormat() {
        return dateFormat;
    }
}
