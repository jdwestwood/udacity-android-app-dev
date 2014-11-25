package com.example.johnandjai.sunshine.app.data;

import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by John and Jai on 10/20/2014.
 */
public class WeatherContract {
    // The 'Content authority' is a name for the entire content provider, similar to the
    // relationship between a domain name and its website.  A convenient string to use for
    // the content authority is the package name for the app, which is guaranteed to be
    // unique on the device.
    public static final String CONTENT_AUTHORITY = "com.example.johnandjai.sunshine.app";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    // Possible paths (appended to base content URI for possible URI's
    public static final String PATH_WEATHER = "weather";
    public static final String PATH_LOCATION = "location";

    // Inner class that defines the table contents of the weather table.
    public static final class WeatherEntry implements BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_WEATHER).build();
        // special MIME type prefixes that indicate that the URI returns either a directory
        // (list of items) or a single item.
        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/" + CONTENT_AUTHORITY + "/" + PATH_WEATHER;
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/" + CONTENT_AUTHORITY + "/" + PATH_WEATHER;

        // define the name and columns in the weather table.
        public static final String TABLE_NAME = "weather";
        // Column with the foreign key into the location table.
        public static final String COLUMN_LOC_KEY = "location_id";
        // Date, stored as Text with format yyyy-MM-dd
        public static final String COLUMN_DATETEXT = "date";
        // Weather id as returned by the API, to identify the icon to be used
        public static final String COLUMN_WEATHER_ID = "weather_id";

        // Short description and long description of the weather, as provided by API,
        // e.g., "clear" vs "sky is clear".
        public static final String COLUMN_SHORT_DESC = "short_desc";

        // Min and max temperatures for the day (stored as floats)
        public static final String COLUMN_MIN_TEMP = "min";
        public static final String COLUMN_MAX_TEMP = "max";

        // Humidity is stored as a float representing percentage
        public static final String COLUMN_HUMIDITY = "humidity";
        // Pressure is stored as a float
        public static final String COLUMN_PRESSURE = "pressure";
        // Humidity is stored as a float representing windspeed mph
        public static final String COLUMN_WIND_SPEED = "wind";
        // Degrees are meteorological degrees (e.g., 0 is north, 180 is south)
        // stored as a float.
        public static final String COLUMN_DEGREES = "degrees";

        public static Uri buildWeatherUri(long id) {
            // URI for returning an entry whose _ID is id
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

        public static Uri buildWeatherLocation(String locationSetting) {
            // URI for returning a URI for a location
            return CONTENT_URI.buildUpon().appendPath(locationSetting).build();
        }

        public static Uri buildWeatherLocationWithStartDate(String locationSetting, String startDate) {
            // URI for retrieving weather starting on a specified date, which is put into the URI
            // as a URI query parameter.
            return CONTENT_URI.buildUpon().appendPath(locationSetting)
                    .appendQueryParameter(COLUMN_DATETEXT, startDate).build();
        }

        public static Uri buildWeatherLocationWithDate(String locationSetting, String date) {
            // URI for retrieving the weather on a specific date, which is put into the URI as
            // part of the path.
            return CONTENT_URI.buildUpon().appendPath(locationSetting).appendPath(date).build();
        }

        public static String getLocationSettingFromUri(Uri uri) {
            return uri.getPathSegments().get(1);
        }

        public static String getDateFromUri(Uri uri) {
            return uri.getPathSegments().get(2);
        }

        public static String getStartDateFromUri(Uri uri) {
            return uri.getQueryParameter(COLUMN_DATETEXT);
        }
    }

    // Inner class that defines the table contents of the location table.
    public static class LocationEntry implements BaseColumns {
        // inherits the ._ID field from BaseColumns
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_LOCATION).build();
        // special MIME type prefixes that indicate that the URI returns either a directory
        // (list of items) or a single item.
        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/" + CONTENT_AUTHORITY + "/" + PATH_LOCATION;
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/" + CONTENT_AUTHORITY + "/" + PATH_LOCATION;

        // define the name and columns in the location table.
        public static final String TABLE_NAME = "location";
        // no need to specify the _ID column
        // location setting stored as Text; will be sent to openweathermap
        // as the location query
        public static final String COLUMN_LOCATION_SETTING = "location_setting";
        // human readable location name, provided by the API, stored as Text.
        public static final String COLUMN_CITY_NAME = "city_name";
        // location latitude and longitude, provided by the API, stored as floats
        public static final String COLUMN_COORD_LAT = "coord_lat";
        public static final String COLUMN_COORD_LONG = "coord_long";

        public static Uri buildLocationUri(long id) {
            // URI for returning an entry whose _ID is id
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }
    }

    public static final String DATE_FORMAT = "yyyyMMdd";
    /**
     * Converts Date class to a string representation, used for easy comparison and database
     * lookup.
     * @param date the input date
     * @return a DB-friendly representation of the date, using the format defined in DATE_FORMAT
     */
    public static String getDbDateString(Date date) {
        // convert Date date to a string with the format DATE_FORMAT 'yyyyMMdd' for storing
        // in the weather database
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        return sdf.format(date);
    }

    public static Date getDateFromDb(String dateText) {
        // Convert the date string stored in the database to a Date.
        SimpleDateFormat dbDateFormat = new SimpleDateFormat(DATE_FORMAT);
        try {
            return dbDateFormat.parse(dateText);
        } catch(ParseException e) {
            e.printStackTrace();
            return null;
        }
    }
}
