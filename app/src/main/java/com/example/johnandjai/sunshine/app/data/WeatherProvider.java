package com.example.johnandjai.sunshine.app.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

import static com.example.johnandjai.sunshine.app.data.WeatherContract.CONTENT_AUTHORITY;
import static com.example.johnandjai.sunshine.app.data.WeatherContract.LocationEntry;
import static com.example.johnandjai.sunshine.app.data.WeatherContract.PATH_LOCATION;
import static com.example.johnandjai.sunshine.app.data.WeatherContract.PATH_WEATHER;
import static com.example.johnandjai.sunshine.app.data.WeatherContract.WeatherEntry;

/**
 * Created by John and Jai on 10/23/2014.
 */
public class WeatherProvider extends ContentProvider {

    // ContentProvider provides data based on URI's; each URI is tied to an integer constant.
    // Each URI will be used for a different type of query. The <provider> tag in the
    // AndroidManifest tells Android to create a WeatherProvider ContentProvider for the app.
    private static final int WEATHER = 100;
    private static final int WEATHER_WITH_LOCATION = 101;
    private static final int WEATHER_WITH_LOCATION_AND_DATE = 102;
    private static final int LOCATION = 300;
    private static final int LOCATION_ID = 301;

    // the URI matcher used by this content provider
    private static final UriMatcher sUriMatcher = buildUriMatcher();

    private WeatherDbHelper mOpenHelper;
    private static final SQLiteQueryBuilder sWeatherByLocationSettingQueryBuilder;

    // ** for the WEATHER_WITH_LOCATION and WEATHER_WITH_LOCATION_AND_DATE queries. **
    static {
        sWeatherByLocationSettingQueryBuilder = new SQLiteQueryBuilder();
        sWeatherByLocationSettingQueryBuilder.setTables(
                WeatherEntry.TABLE_NAME + " INNER JOIN " +
                LocationEntry.TABLE_NAME + " ON " +
                WeatherEntry.TABLE_NAME + "." + WeatherEntry.COLUMN_LOC_KEY + " = " +
                LocationEntry.TABLE_NAME + "." + LocationEntry._ID);
    }
    // assign the selection string for the WEATHER_WITH_LOCATION query; the '?' indicates that
    // a value will be supplied in the selectionArgs array.
    private static final String sLocationSettingSelection =
            LocationEntry.TABLE_NAME + "." + LocationEntry.COLUMN_LOCATION_SETTING + " = ? ";

    // assign the selection string for the WEATHER_WITH_LOCATION query when a startDate URI
    // query parameter has been passed; the '?'s indicate that values will be supplied in
    // the selectionArgs array.
    private static final String sLocationSettingWithStartDateSelection =
            LocationEntry.TABLE_NAME + "." + LocationEntry.COLUMN_LOCATION_SETTING + " = ? AND " +
            WeatherEntry.TABLE_NAME + "." + WeatherEntry.COLUMN_DATETEXT + " >= ? ";

    // assign the selection string for the WEATHER_WITH_LOCATION_AND_DATE query; the '?'s
    // indicate that values will be supplied in the selectionArgs array.
    private static final String sLocationSettingWithDateSelection =
            LocationEntry.TABLE_NAME + "." + LocationEntry.COLUMN_LOCATION_SETTING + " = ? AND " +
                    WeatherEntry.TABLE_NAME + "." + WeatherEntry.COLUMN_DATETEXT + " = ? ";

    private Cursor getWeatherByLocationSetting(Uri uri, String[] projection, String sortOrder) {
        // called from the query method for the WEATHER_WITH_LOCATION and WEATHER_WITH_LOCATION_AND_DATE
        // cases.  Parse the uri for the locationSetting and startDate; assign one of the selection
        // strings created above, and the selectionArgs.
        // Corresponds to WeatherEntry.buildWeatherLocation and .buildWeatherLocationWithStartDate
        String locationSetting = WeatherEntry.getLocationSettingFromUri(uri);
        String startDate = WeatherEntry.getStartDateFromUri(uri);

        String selection;
        String[] selectionArgs;

        if (startDate == null) {
            selection = sLocationSettingSelection;
            selectionArgs = new String[] {locationSetting};
        } else {
            selection = sLocationSettingWithStartDateSelection;
            selectionArgs = new String[] {locationSetting, startDate};
        }
        return sWeatherByLocationSettingQueryBuilder.query(
                mOpenHelper.getReadableDatabase(), projection, selection, selectionArgs, null, null, sortOrder);
    }

    private Cursor getWeatherByLocationSettingAndDate(Uri uri, String[] projection, String sortOrder) {
        // Corresponds to WeatherEntry.buildWeatherLocationWithDate
        String locationSetting = WeatherEntry.getLocationSettingFromUri(uri);
        String date = WeatherEntry.getDateFromUri(uri);

        String selection = sLocationSettingWithDateSelection;
        String[] selectionArgs = {locationSetting, date};

        return sWeatherByLocationSettingQueryBuilder.query(
                mOpenHelper.getReadableDatabase(), projection, selection, selectionArgs, null, null, sortOrder);
    }

    private static UriMatcher buildUriMatcher() {
        // root of the URI tree will not match anything.
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        // tie each URI to its constant; '*' matches a string and '#' matches a number
        matcher.addURI(CONTENT_AUTHORITY, PATH_WEATHER, WEATHER);
        matcher.addURI(CONTENT_AUTHORITY, PATH_WEATHER + "/*", WEATHER_WITH_LOCATION);
        matcher.addURI(CONTENT_AUTHORITY, PATH_WEATHER + "/*/*", WEATHER_WITH_LOCATION_AND_DATE);
        matcher.addURI(CONTENT_AUTHORITY, PATH_LOCATION, LOCATION);
        matcher.addURI(CONTENT_AUTHORITY, PATH_LOCATION + "/#", LOCATION_ID);
        return matcher;
    }

    @Override
    public boolean onCreate() {
        // Create the underlying database; the <provider> tag in AndroidManifest tells Android
        // to create a WeatherProvider ContentProvider for the app.
        mOpenHelper = new WeatherDbHelper(getContext());
        return true;                               // content provider created successfully
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        // Takes incoming query parameters and returns a Cursor into the data.
        Cursor retCursor;
        // get the MIME type of the data associated with the uri
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case WEATHER_WITH_LOCATION_AND_DATE:
                // weather/*/*   Corresponds to WeatherEntry.buildWeatherLocationWithDate

                retCursor = getWeatherByLocationSettingAndDate(uri, projection, sortOrder);
                break;
            case WEATHER_WITH_LOCATION:
                // weather/* , with or without a startDate as a URI query parameter
                // Corresponds to WeatherEntry.buildWeatherLocation and .buildWeatherLocationWithStartDate
                retCursor = getWeatherByLocationSetting(uri, projection, sortOrder);
                break;
            case WEATHER:
                // weather
                retCursor = mOpenHelper.getReadableDatabase().query(
                        WeatherEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
                break;
            case LOCATION:
                retCursor = mOpenHelper.getReadableDatabase().query(
                        LocationEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
                // location
                break;
            case LOCATION_ID:
                // location/#
                String[] sArgs = {String.valueOf(ContentUris.parseId(uri))};
                retCursor = mOpenHelper.getReadableDatabase().query(
                        LocationEntry.TABLE_NAME,
                        projection,
                        LocationEntry._ID + " = '" + ContentUris.parseId(uri) + "'",
                        null,
                        null,
                        null,
                        sortOrder);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri:" + uri);
        }
        // register a ContentObserver for this uri; if the content of this uri or any of its
        // descendants changes, the ContentObserver is notified.
        retCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return retCursor;
    }

    @Override
    public String getType(Uri uri) {
        // get the MIME type of the data associated with the uri
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case WEATHER_WITH_LOCATION_AND_DATE:
                return WeatherEntry.CONTENT_ITEM_TYPE;
            case WEATHER_WITH_LOCATION:
                return WeatherEntry.CONTENT_TYPE;
            case WEATHER:
                return WeatherEntry.CONTENT_TYPE;
            case LOCATION:
                return LocationEntry.CONTENT_TYPE;
            case LOCATION_ID:
                return LocationEntry.CONTENT_ITEM_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri:" + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        // insert only at the root URI's of each table.  In that way, all ContentObservers
        // at and below the root will be notified whenever anything in the table is inserted.
        // Note that inserts will replace previous entries with the same date and location,
        // per the WeatherEntry table definition in WeatherDbHelper.onCreate
        Uri returnUri;
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        // get the MIME type of the data associated with the uri
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case WEATHER:
                long _id = db.insert(WeatherEntry.TABLE_NAME, null, contentValues);
                if (_id > 0) {
                    returnUri = WeatherEntry.buildWeatherUri(_id);
                } else {
                    throw new SQLException("Failed to insert row into " + uri);
                }
                break;
            case LOCATION:
                _id = db.insert(LocationEntry.TABLE_NAME, null, contentValues);
                if (_id > 0) {
                    returnUri = LocationEntry.buildLocationUri(_id);
                } else {
                    throw new SQLException("Failed to insert row into " + uri);
                }
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri:" + uri);
        }
        // notify ContentObservers that a change has occurred in the table.
        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        // Insert multiple rows together, which is much more efficient that inserting one row at
        // a time.
        // Insert only at the root URI's of each table.  In that way, all ContentObservers
        // at and below the root will be notified whenever anything in the table is inserted.
        // Note that inserts will replace previous entries with the same date and location,
        // per the WeatherEntry table definition in WeatherDbHelper.onCreate

        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int insertedCount = 0;
        switch (match) {
            case WEATHER:
            // only need to bulk insert into the weather table.
                db.beginTransaction();
                try {
                    for (ContentValues value: values) {
                        long _id = db.insert(WeatherEntry.TABLE_NAME, null, value);
                        if (-1 != _id) {
                            insertedCount++;
                        }
                    }
                    db.setTransactionSuccessful();               // commit the new rows
                }
                finally {
                    db.endTransaction();
                }
            default:
                // the method in super inserts the rows one at a time
                insertedCount = super.bulkInsert(uri, values);
        }
        // notify ContentObservers that a change has occurred in the table.
        if (insertedCount > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return insertedCount;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // delete only at the root URI's of each table.  In that way, all ContentObservers
        // at and below the root will be notified whenever anything in the table is deleted.
        // a null selection deletes all the rows.
        int deletedCount = 0;
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        // get the MIME type of the data associated with the uri
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case WEATHER:
                deletedCount = db.delete(WeatherEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case LOCATION:
                deletedCount = db.delete(LocationEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri:" + uri);
        }
        // notify ContentObservers that a change has occurred in the table.
        if (deletedCount > 0) getContext().getContentResolver().notifyChange(uri, null);
        return deletedCount;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String selection, String[] selectionArgs) {
        int updatedCount = 0;
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        // get the MIME type of the data associated with the uri
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case WEATHER:
                updatedCount = db.update(WeatherEntry.TABLE_NAME, contentValues, selection, selectionArgs);
                break;
            case LOCATION:
                updatedCount = db.update(LocationEntry.TABLE_NAME, contentValues, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri:" + uri);
        }
        // notify ContentObservers that a change has occurred in the table.
        if (updatedCount > 0) getContext().getContentResolver().notifyChange(uri, null);
        return updatedCount;
    }
}
