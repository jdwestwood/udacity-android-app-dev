package com.example.johnandjai.sunshine.app;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.test.AndroidTestCase;
import android.util.Log;

import com.example.johnandjai.sunshine.app.data.WeatherDbHelper;

import static com.example.johnandjai.sunshine.app.data.WeatherContract.LocationEntry;
import static com.example.johnandjai.sunshine.app.data.WeatherContract.WeatherEntry;

/**
 * Created by John and Jai on 10/21/2014.
 */
public class TestDb extends AndroidTestCase {

    public static final String LOG_TAG = TestDb.class.getSimpleName();

    // the test runner will run every method that begins with 'test' in the order
    // they are declared in the class; each test should have a failure path that
    // includes an assert.
    public void testCreateDb() throws Throwable {
        // delete the current database
        mContext.deleteDatabase(WeatherDbHelper.DATABASE_NAME);
        // create a new one
        SQLiteDatabase db = new WeatherDbHelper(
                this.mContext).getWritableDatabase();
        // test if the new one was successfully created
        assertEquals(true, db.isOpen());
        db.close();
    }

    public void testInsertReadDb() {
        // test inserting and reading an entry

        // If there is an error in the SQL table creation Strings, errors will
        // be thrown here when we try to get a writable database.
        WeatherDbHelper dbHelper = new WeatherDbHelper(mContext);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Create a new map of values, where column names are the keys.
        // dummy location data

        ContentValues locationValues = populateLocationData();

        long locationRowId = db.insert(LocationEntry.TABLE_NAME, null, locationValues);

        // Verify we got a row back.
        assertTrue(locationRowId != -1);
        Log.d(LOG_TAG, "New location table row id: " + locationRowId);

        // Specify which columns we want.
        String[] locationColumns = {
                LocationEntry._ID,
                LocationEntry.COLUMN_LOCATION_SETTING,
                LocationEntry.COLUMN_CITY_NAME,
                LocationEntry.COLUMN_COORD_LAT,
                LocationEntry.COLUMN_COORD_LONG
        };

        // A cursor is the primary interface to the query results; use a custom projection
        // (the columns array).
        Cursor locationCursor = db.query(LocationEntry.TABLE_NAME,    // table to query
                locationColumns,
                null,                                          // columns for the 'where' clause
                null,                                          // values for the 'where' clause
                null,                                          // columns to group by
                null,                                          // columns to filter by row groups
                null                                           // sort order
        );

        verifyTableData(locationValues, locationCursor);
        locationCursor.close();

        // create dummy data for the weather table.
        ContentValues weatherValues = populateWeatherData(locationRowId);
        long weatherRowId = db.insert(WeatherEntry.TABLE_NAME, null, weatherValues);

        // Verify we got a row back.
        assertTrue(weatherRowId != -1);
        Log.d(LOG_TAG, "New weather table row id: " + weatherRowId);

        // Specify which columns we want.
        String[] weatherColumns = {
                WeatherEntry._ID,
                WeatherEntry.COLUMN_LOC_KEY,
                WeatherEntry.COLUMN_DATETEXT,
                WeatherEntry.COLUMN_WEATHER_ID,
                WeatherEntry.COLUMN_SHORT_DESC,
                WeatherEntry.COLUMN_MIN_TEMP,
                WeatherEntry.COLUMN_MAX_TEMP,
                WeatherEntry.COLUMN_HUMIDITY,
                WeatherEntry.COLUMN_PRESSURE,
                WeatherEntry.COLUMN_WIND_SPEED,
                WeatherEntry.COLUMN_DEGREES};

        // Query for all rows in the WeatherEntry table
        Cursor weatherCursor = db.query(WeatherEntry.TABLE_NAME,             // table to query
                weatherColumns,
                null,                                          // columns for the 'where' clause
                null,                                          // values for the 'where' clause
                null,                                          // columns to group by
                null,                                          // columns to filter by row groups
                null);                                         // sort order

        verifyTableData(weatherValues, weatherCursor);
        weatherCursor.close();
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
        ContentValues locationValues = new ContentValues();

        String testName = "North Pole";
        String testLocationSetting = "99705";
        double testLatitude = 64.772;
        double testLongitude = -147.35;

        locationValues.put(LocationEntry.COLUMN_CITY_NAME, testName);
        locationValues.put(LocationEntry.COLUMN_LOCATION_SETTING, testLocationSetting);
        locationValues.put(LocationEntry.COLUMN_COORD_LAT, testLatitude);
        locationValues.put(LocationEntry.COLUMN_COORD_LONG, testLongitude);
        return locationValues;
    }

    private ContentValues populateWeatherData(long locationRowId) {
        ContentValues weatherValues = new ContentValues();

        String dateText = "20141205";
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
