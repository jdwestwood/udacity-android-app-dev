package com.example.johnandjai.sunshine.app;

import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.test.AndroidTestCase;
import android.util.Log;

import static com.example.johnandjai.sunshine.app.data.WeatherContract.LocationEntry;
import static com.example.johnandjai.sunshine.app.data.WeatherContract.WeatherEntry;

/**
 * Created by John and Jai on 10/21/2014.
 */
public class TestProvider extends AndroidTestCase {

    public static final String LOG_TAG = TestProvider.class.getSimpleName();
    static public String TEST_CITY = "North Pole";
    static public String TEST_LOCATION = "99705";
    static public String TEST_DATE = "20141205";

    // the test runner will run setUp first, then every method that begins with 'test'
    // in the order they are declared in the class; each test should have a failure path that
    // includes an assert.

    // Since we want each test to start with a clean slate, run deleteAllRecords
    // in setUp (called by the test runner before each test).
    public void setUp() {
        deleteAllRecords();
    }

    // brings our database to an empty state
    public void deleteAllRecords() {
        mContext.getContentResolver().delete(
                WeatherEntry.CONTENT_URI,
                null,                                  // null for selection deletes all rows
                null
        );
        mContext.getContentResolver().delete(
                LocationEntry.CONTENT_URI,
                null,                                  // null for selection deletes all rows
                null
        );

        Cursor cursor = mContext.getContentResolver().query(
                WeatherEntry.CONTENT_URI,
                null,
                null,
                null,
                null
        );
        assertEquals(0, cursor.getCount());
        cursor.close();

        cursor = mContext.getContentResolver().query(
                LocationEntry.CONTENT_URI,
                null,
                null,
                null,
                null
        );
        assertEquals(0, cursor.getCount());
        cursor.close();
    }

    public void testGetType() {
        // Test the getType method in WeatherProvider
        // mContext is a field of AndroidTestCase; getContentResolver returns a ContentResolver;
        // getType returns the MIME type of the uri through a call to WeatherProvider.getType
        // content://com.example.android.sunshine.app/weather/
        String type = mContext.getContentResolver().getType(WeatherEntry.CONTENT_URI);
        // vnd.android.cursor.dir/com.example.android.sunshine.app/weather (WeatherEntry.CONTENT_TYPE)
        assertEquals(WeatherEntry.CONTENT_TYPE, type);

        String testLocation = "94074";
        // content://com.example.android.sunshine.app/weather/94074
        type = mContext.getContentResolver().getType(WeatherEntry.buildWeatherLocation(testLocation));
        // vnd.android.cursor.dir/com.example.android.sunshine.app/weather (WeatherEntry.CONTENT_TYPE)
        assertEquals(WeatherEntry.CONTENT_TYPE, type);

        String testDate = "20140612";
        // content://com.example.android.sunshine.app/weather/94074/20140612
        type = mContext.getContentResolver()
                .getType(WeatherEntry.buildWeatherLocationWithDate(testLocation, testDate));
        // vnd.android.cursor.item/com.example.android.sunshine.app/weather (WeatherEntry.CONTENT_ITEM_TYPE)
        assertEquals(WeatherEntry.CONTENT_ITEM_TYPE, type);

        // content://com.example.android.sunshine.app/location/
        type = mContext.getContentResolver().getType(LocationEntry.CONTENT_URI);
        // vnd.android.cursor.dir/com.example.android.sunshine.app/location (WeatherEntry.CONTENT_TYPE)
        assertEquals(LocationEntry.CONTENT_TYPE, type);

        // content://com.example.android.sunshine.app/location/1
        type = mContext.getContentResolver().getType(LocationEntry.buildLocationUri(1));
        // vnd.android.cursor.item/com.example.android.sunshine.app/location (WeatherEntry.CONTENT_TYPE)
        assertEquals(LocationEntry.CONTENT_ITEM_TYPE, type);
    }

    public void testInsertReadProvider() {
        // Test the WeatherProvider ContentProvider class.

        ContentValues locationValues = populateLocationData();

        Uri insertUri = mContext.getContentResolver()
                .insert(LocationEntry.CONTENT_URI, locationValues);
        long locationRowId = ContentUris.parseId(insertUri);

        Log.d(LOG_TAG, "New location table row id: " + locationRowId);

        // Query for the row we just added.
        Cursor locationCursor = mContext.getContentResolver().query(
                LocationEntry.buildLocationUri(locationRowId),   // uri to query
                null,                                          // null means return all columns
                null,                                          // columns for the 'where' clause
                null,                                          // values for the 'where' clause
                null);                                         // sort order

        verifyTableData(locationValues, locationCursor);

        // create dummy data for the weather table.
        ContentValues weatherValues = populateWeatherData(locationRowId);
        insertUri = mContext.getContentResolver().insert(WeatherEntry.CONTENT_URI, weatherValues);
        long weatherRowId = ContentUris.parseId(insertUri);

        Log.d(LOG_TAG, "New weather table row id: " + weatherRowId);

        // Query for all rows in the WeatherEntry table
        Cursor weatherCursor = mContext.getContentResolver().query(
                WeatherEntry.CONTENT_URI,                      // uri to query
                null,                                          // null means return all columns
                null,                                          // columns for the 'where' clause
                null,                                          // values for the 'where' clause
                null);                                         // sort order

        verifyTableData(weatherValues, weatherCursor);
        weatherCursor.close();

        // Query for rows in the joined WeatherEntry and LocationEntry tables having
        // LocationEntry.COLUMN_LOCATION_SETTING = TEST_LOCATION.
        weatherCursor = mContext.getContentResolver().query(
                WeatherEntry.buildWeatherLocation(TEST_LOCATION), // uri to query
                null,                                          // null means return all columns
                null,                                          // columns for the 'where' clause
                null,                                          // values for the 'where' clause
                null);                                         // sort order

        verifyTableData(weatherValues, weatherCursor);
        weatherCursor.close();

        // Query for rows in the joined WeatherEntry and LocationEntry tables having
        // LocationEntry.COLUMN_LOCATION_SETTING = TEST_LOCATION and
        // WeatherEntry.COLUMN_DATETEXT >= TEST_DATE.  TEST_DATE is a query parameter in
        // the URI.
        weatherCursor = mContext.getContentResolver().query(
                WeatherEntry.buildWeatherLocationWithStartDate(TEST_LOCATION, TEST_DATE),
                null,                                          // null means return all columns
                null,                                          // columns for the 'where' clause
                null,                                          // values for the 'where' clause
                null);                                         // sort order

        verifyTableData(weatherValues, weatherCursor);
        weatherCursor.close();

        // Query for rows in the joined WeatherEntry and LocationEntry tables having
        // LocationEntry.COLUMN_LOCATION_SETTING = TEST_LOCATION and
        // WeatherEntry.COLUMN_DATETEXT = TEST_DATE.  TEST_DATE is part of the URI path.
        weatherCursor = mContext.getContentResolver().query(
                WeatherEntry.buildWeatherLocationWithDate(TEST_LOCATION, TEST_DATE),
                null,                                          // null means return all columns
                null,                                          // columns for the 'where' clause
                null,                                          // values for the 'where' clause
                null);                                         // sort order

        verifyTableData(weatherValues, weatherCursor);
        weatherCursor.close();

        // ** Test the update method
        // assign the selection string for deleting from the location table; the '?'
        // indicates that a value will be supplied in the selectionArgs array.
        String selection =
                LocationEntry.TABLE_NAME + "." + LocationEntry.COLUMN_LOCATION_SETTING + " = ? ";
        String[] selectionArgs = {TEST_LOCATION};
        TEST_LOCATION = "18069";                           // a new location
        TEST_CITY = "Orefield";                            // a new city
        locationValues = populateLocationData();
        int updatedCount = mContext.getContentResolver()
                .update(LocationEntry.CONTENT_URI, locationValues, selection, selectionArgs);
        assertTrue(updatedCount > 0);

        // assign the selection string for deleting from the weather table; the '?'
        // indicates that a value will be supplied in the selectionArgs array.
        selection = WeatherEntry.TABLE_NAME + "." + WeatherEntry.COLUMN_DATETEXT + " = ? ";
        selectionArgs[0] = TEST_DATE;
        TEST_DATE = "20130303";
        weatherValues = populateWeatherData(1);
        updatedCount = mContext.getContentResolver()
                .update(WeatherEntry.CONTENT_URI, weatherValues, selection, selectionArgs);
        assertTrue(updatedCount > 0);

        // ** Test the delete method. Delete from the weather table first since weather table
        // has a foreign key from the locaton table.

        // assign the selection string for deleting from the weather table; the '?'
        // indicates that a value will be supplied in the selectionArgs array.
        selection = WeatherEntry.TABLE_NAME + "." + WeatherEntry.COLUMN_DATETEXT + " = ? ";
        selectionArgs[0] = TEST_DATE;
        int deletedCount = mContext.getContentResolver()
                .delete(WeatherEntry.CONTENT_URI, selection, selectionArgs);
        assertTrue(deletedCount > 0);

        // assign the selection string for deleting from the location table; the '?'
        // indicates that a value will be supplied in the selectionArgs array.
        selection =
                LocationEntry.TABLE_NAME + "." + LocationEntry.COLUMN_LOCATION_SETTING + " = ? ";
        selectionArgs[0] = TEST_LOCATION;
        deletedCount = mContext.getContentResolver()
                .delete(LocationEntry.CONTENT_URI, selection, selectionArgs);
        assertTrue(deletedCount > 0);
    }

    private void verifyTableData(ContentValues values, Cursor cursor) {
        if (cursor.moveToFirst()) {
            int columnIndex;
            for (String columnName : values.keySet()) {
                columnIndex = cursor.getColumnIndex(columnName);
                switch (cursor.getType(columnIndex)) {
                    case Cursor.FIELD_TYPE_FLOAT:
                        assertEquals(cursor.getDouble(columnIndex), values.getAsDouble(columnName));
                        break;
                    case Cursor.FIELD_TYPE_INTEGER:
                        assertEquals(cursor.getLong(columnIndex), (long) values.getAsLong(columnName));
                        break;
                    case Cursor.FIELD_TYPE_STRING:
                        assertEquals(cursor.getString(columnIndex), values.getAsString(columnName));
                        break;
                    default:
                        Log.d(LOG_TAG, "Unhandled case in verifyTableData");
                }
            }
        } else {
            fail("No values returned from the table.");
        }
    }

    private ContentValues populateLocationData() {
        // Create a new map of values, where column names are the keys.
        // dummy location data.
        ContentValues locationValues = new ContentValues();

        String testName = TEST_CITY;
        String testLocationSetting = TEST_LOCATION;
        double testLatitude = 64.772;
        double testLongitude = -147.35;

        locationValues.put(LocationEntry.COLUMN_CITY_NAME, testName);
        locationValues.put(LocationEntry.COLUMN_LOCATION_SETTING, testLocationSetting);
        locationValues.put(LocationEntry.COLUMN_COORD_LAT, testLatitude);
        locationValues.put(LocationEntry.COLUMN_COORD_LONG, testLongitude);
        return locationValues;
    }

    private ContentValues populateWeatherData(long locationRowId) {
        // Create a new map of values, where column names are the keys.
        // dummy location data.
        ContentValues weatherValues = new ContentValues();

        String dateText = TEST_DATE;
        double degrees = 1.1;
        double humidity = 1.2;
        double pressure = 1.3;
        double max_temp = 75;
        double min_temp = 65;
        String short_desc = "Asteroids";
        double wind_speed = 5.5;
        long weather_id = 321;

        weatherValues.put(WeatherEntry.COLUMN_LOC_KEY, locationRowId);   // the foreign key
        weatherValues.put(WeatherEntry.COLUMN_DATETEXT, dateText);
        weatherValues.put(WeatherEntry.COLUMN_DEGREES, degrees);
        weatherValues.put(WeatherEntry.COLUMN_HUMIDITY, humidity);
        weatherValues.put(WeatherEntry.COLUMN_PRESSURE, pressure);
        weatherValues.put(WeatherEntry.COLUMN_MAX_TEMP, max_temp);
        weatherValues.put(WeatherEntry.COLUMN_MIN_TEMP, min_temp);
        weatherValues.put(WeatherEntry.COLUMN_SHORT_DESC, short_desc);
        weatherValues.put(WeatherEntry.COLUMN_WIND_SPEED, wind_speed);
        weatherValues.put(WeatherEntry.COLUMN_WEATHER_ID, weather_id);
        return weatherValues;
    }
}
