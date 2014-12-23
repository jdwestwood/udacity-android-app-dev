package com.example.johnandjai.sunshine.app.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncRequest;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.example.johnandjai.sunshine.app.DetailActivity;
import com.example.johnandjai.sunshine.app.DetailFragment;
import com.example.johnandjai.sunshine.app.MainActivity;
import com.example.johnandjai.sunshine.app.R;
import com.example.johnandjai.sunshine.app.Utility;
import com.example.johnandjai.sunshine.app.data.WeatherContract;
import com.example.johnandjai.sunshine.app.data.WeatherContract.WeatherEntry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.Vector;

/**
 * Created by John and Jai on 12/18/2014.
 */
/* Additional settings for SunshineSyncAdapter defined in syncadapter.xml using the <sync-adapter> tag.
   See comment in SunshineSyncService for further info about how SunshineSyncAdapter is implemented */
public class SunshineSyncAdapter extends AbstractThreadedSyncAdapter {

    private static final String LOG_TAG = SunshineSyncAdapter.class.getSimpleName();

    public static final int SYNC_INTERVAL = 60*180;                   // every 180 minutes (< API 19)
    public static final int SYNC_FLEXTIME = SYNC_INTERVAL/3;          // every 60 minutes (>= API 19)
    private static final int DELETE_DAYS = 1;                     // delete weather data >= 1 day old

    private static final String[] NOTIFY_WEATHER_PROJECTION = {                            // as we did in DetailFragment
            // In this case the _id needs to be fully qualified with a table name, since
            // the content provider joins the location & weather tables in the background
            // and both have an _id column.
            WeatherEntry.COLUMN_WEATHER_ID,
            WeatherEntry.COLUMN_MAX_TEMP,
            WeatherEntry.COLUMN_MIN_TEMP,
            WeatherEntry.COLUMN_SHORT_DESC,
    };

    // These columns indices are tied to NOTIFY_WEATHER_PROJECTION.  If it changes,
    // these must change as well.
    private static final int INDEX_WEATHER_WEATHER_ID = 0;
    private static final int INDEX_WEATHER_MAX_TEMP = 1;
    private static final int INDEX_WEATHER_MIN_TEMP = 2;
    private static final int INDEX_WEATHER_DESC = 3;

    private static final long DAY_IN_MILLISEC = 1000 * 60 * 60 * 24;
    // use the same ID each time, so only see one notification; the value is arbitrary.
    private static final int WEATHER_NOTIFICATION_ID = 3004;

    private ContentResolver mContentResolver;

    public SunshineSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    public static void initializeSyncAdapter(Context context) {
        // Interface to the outside world.  Called from MainActivity to make sure the account is
        // created that will allow us to get data from the SyncAdapter.
//        Log.d(LOG_TAG, "Hello from initializeSyncAdapter");
        getSyncAccount(context);
    }

    @Override
    public void onPerformSync(Account account, Bundle bundle, String s, ContentProviderClient contentProviderClient, SyncResult syncResult) {
        // This code was originally the doInBackground method of FetchWeatherTask (Lessons 1-5)
        // and then in onHandleIntent in SunshineService for the first part of Lesson 6.

        mContentResolver = getContext().getContentResolver();
        // Get the location value in the Settings and get the weather forecast data.
        String locationQuery = Utility.getPreferredLocation(getContext());

        // These two need to be declared outside the try/catch
        // so that they can be closed in the finally block.
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        String format = "json";
        String units = "metric";
        int numDays = 14;
        String forecastJsonStr;

//        Log.d(LOG_TAG, "Hello from onPerformSync");

        try {
            // Construct the URL for the OpenWeatherMap query
            // Possible parameters are available at OWM's forecast API page, at
            // http://openweathermap.org/API#forecast
            final String FORECAST_BASE_URL = "http://api.openweathermap.org/data/2.5/forecast/daily";
            final String QUERY_PARAM = "q";
            final String FORMAT_PARAM = "mode";
            final String UNITS_PARAM = "units";
            final String DAYS_PARAM = "cnt";
            Uri builtURI = Uri.parse(FORECAST_BASE_URL).buildUpon()
                    .appendQueryParameter(QUERY_PARAM, locationQuery).appendQueryParameter(FORMAT_PARAM, format)
                    .appendQueryParameter(UNITS_PARAM, units).appendQueryParameter(DAYS_PARAM, String.valueOf(numDays))
                    .build();
            String url_string = builtURI.toString();
            Log.i("Forecast API URL: ", url_string);
            URL url = new URL(url_string);

            // Create the request to OpenWeatherMap, and open the connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                // Nothing to do.
                return;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // buffer for debugging.
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                return;
            }
            forecastJsonStr = buffer.toString();
//                Log.i("JSON from api.openweathermap.org: ", forecastJsonStr);

            // These are the names of the JSON objects that need to be extracted.
            // Location information
            final String OWM_CITY = "city";
            final String OWM_CITY_NAME = "name";
            final String OWM_COORD = "coord";

            // Location coordinate
            final String OWM_LATITUDE = "lat";
            final String OWM_LONGITUDE = "lon";

            // Weather information.  Each day's forecast info is an element of the 'list' array.
            final String OWM_LIST = "list";
            final String OWM_DATETIME = "dt";
            final String OWM_TEMPERATURE = "temp";
            final String OWM_MAX = "max";
            final String OWM_MIN = "min";
            final String OWM_PRESSURE = "pressure";
            final String OWM_HUMIDITY = "humidity";
            final String OWM_WEATHER = "weather";
            final String OWM_WEATHER_ID = "id";
            final String OWM_DESCRIPTION = "main";
            final String OWM_SPEED = "speed";
            final String OWM_DEGREES = "deg";

            // need to enclose the following in try ... catch because need to handle
            // JSON exceptions that will be thrown if JSON parsing fails.
            try {
                JSONObject forecastJson = new JSONObject(forecastJsonStr);
                JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

                // Get and insert the location information into the database.
                JSONObject cityJson = forecastJson.getJSONObject(OWM_CITY);
                String cityName = cityJson.getString(OWM_CITY_NAME);

                JSONObject cityCoord = cityJson.getJSONObject(OWM_COORD);
                double cityLatitude = cityCoord.getDouble(OWM_LATITUDE);
                double cityLongitude = cityCoord.getDouble(OWM_LONGITUDE);
                // add the location data to the database if data for the location does not
                // already exist.
                long locationId = addLocation(locationQuery, cityName, cityLatitude, cityLongitude);

                // Get and insert the new weather information into the database.
                // Each day's weather information populates a ContentValues object; the
                // ContentValues objects are stored in a Vector; use Vector because it is designed
                // to expand automatically as items are added; can be accessed by index or
                // iterated.
                Vector<ContentValues> cVVector = new Vector<ContentValues>(weatherArray.length());
                String[] resultStrs = new String[numDays];

                for(int i = 0; i < weatherArray.length(); i++) {
                    // Get the JSON object representing the forecast for one day.
                    JSONObject dayForecast = weatherArray.getJSONObject(i);

                    // API returns a Unix timestamp (measured in seconds) (a long).  We need to
                    // convert that into something human-readable.
                    long dateTime = dayForecast.getLong(OWM_DATETIME);
                    Date date = new Date(1000L*dateTime);

                    // Temperatures are in a child object called "temp".  Try not to name variables
                    // "temp" when working with temperature.  It confuses everybody.
                    JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                    double high = temperatureObject.getDouble(OWM_MAX);
                    double low = temperatureObject.getDouble(OWM_MIN);

                    double pressure = dayForecast.getDouble(OWM_PRESSURE);
                    double humidity = dayForecast.getDouble(OWM_HUMIDITY);
                    // description is in a child array called "weather", which is 1 element long.
                    JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                    String description = weatherObject.getString(OWM_DESCRIPTION);
                    int weatherId = weatherObject.getInt(OWM_WEATHER_ID);

                    double windSpeed = dayForecast.getDouble(OWM_SPEED);
                    double windDirection = dayForecast.getDouble(OWM_DEGREES);

                    ContentValues weatherValues = new ContentValues();
                    weatherValues.put(WeatherEntry.COLUMN_LOC_KEY, locationId);
                    weatherValues.put(WeatherEntry.COLUMN_DATETEXT, WeatherContract.getDbDateString(date));
                    weatherValues.put(WeatherEntry.COLUMN_HUMIDITY, humidity);
                    weatherValues.put(WeatherEntry.COLUMN_PRESSURE, pressure);
                    weatherValues.put(WeatherEntry.COLUMN_WIND_SPEED, windSpeed);
                    weatherValues.put(WeatherEntry.COLUMN_DEGREES, windDirection);
                    weatherValues.put(WeatherEntry.COLUMN_MAX_TEMP, high);
                    weatherValues.put(WeatherEntry.COLUMN_MIN_TEMP, low);
                    weatherValues.put(WeatherEntry.COLUMN_SHORT_DESC, description);
                    weatherValues.put(WeatherEntry.COLUMN_WEATHER_ID, weatherId);

                    cVVector.add(weatherValues);
                }
                addWeather(cVVector);
                deleteWeather(DELETE_DAYS);
                // check if we should send a Notification with the latest weather.
                if (notificationsOn(getContext())) {
//                    Log.e(LOG_TAG, "Notifications on!");
                    notifyWeather();
                }
                else {
//                    Log.e(LOG_TAG, "Notifications off!");
                }
                Log.d(LOG_TAG, "Fetch weather data complete!");
                return;
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
                return;
            }

        } catch (IOException e) {
            Log.e(LOG_TAG, "Error getting JSON: " + e.getMessage(), e);
//                e.printStackTrace();
            // If the code didn't successfully get the weather data, there's no point in attempting
            // to parse it.
            return;
        } finally{
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }
    }

    private void addWeather(Vector<ContentValues> cVVector) {
        if (cVVector.size() > 0) {
            Uri weatherUri = WeatherEntry.CONTENT_URI;      // the root of the WeatherEntry table
            // convert the Vector into an array.
            ContentValues[] cvArray = new ContentValues[cVVector.size()];
            cVVector.toArray(cvArray);
            // A Service is a context, so can call getContentResolver directly with the data.
            // Use .bulkInsert here to insert multiple entries efficiently.
            mContentResolver.bulkInsert(weatherUri, cvArray);
            Log.v(LOG_TAG, "Inserted weather for " + cVVector.size() + " days");
        } else {
            Log.v(LOG_TAG, "In addWeather, did not insert any rows.");
        }
    }

    private void deleteWeather(int deleteDays) {
        // deletes weather that is deleteDays old or older from the database
        // Calendar time fields initialized with current local date and time.
        Calendar calendar = Calendar.getInstance();
        // Set calendar to yesterday.
        calendar.add(Calendar.DATE, -deleteDays);
        // Get yesterday's date from the calendar and convert to a string formatted for the
        // WeatherEntry table.
        String dateString = WeatherContract.getDbDateString(calendar.getTime());
        Uri weatherUri = WeatherEntry.CONTENT_URI;          // the root of the WeatherEntry table
        String selection = WeatherEntry.COLUMN_DATETEXT + " <= ?";
        String selectionArg[] = {dateString};
        int deleted = mContentResolver.delete(weatherUri, selection, selectionArg);
//        Log.e(LOG_TAG, "Deleted " + deleted + " records from WeatherEntry.");
    }

    private long addLocation(String locationSetting, String cityName, double lat, double lon) {
        // checks if the locationSetting exists in the location table and if not adds it.
        // returns the id of the location.
        Log.v(LOG_TAG, "Inserting " + cityName + " with coordinates: " + lat + ", " + lon);
        Uri locationUri = WeatherContract.LocationEntry.CONTENT_URI;
        String[] projection = {WeatherContract.LocationEntry._ID};
        String selection = WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = " + locationSetting;

        // A Service is a context, so can call getContentResolver directly with the data.
        Cursor cursor = mContentResolver
                .query(WeatherContract.LocationEntry.CONTENT_URI, projection, selection, null, null);
        if (cursor.moveToFirst()) {
            // found a row; need to get and return the id
            Log.v(LOG_TAG, "Found it in the database.");
            long locationEntryID = cursor.getLong(cursor.getColumnIndex(WeatherContract.LocationEntry._ID));
            cursor.close();
            return locationEntryID;
        } else {
            // insert a row with the new locationSetting
            Log.v(LOG_TAG, "Did not find it in the database, so will insert it now.");
            cursor.close();
            ContentValues values = new ContentValues();
            values.put(WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING, locationSetting);
            values.put(WeatherContract.LocationEntry.COLUMN_CITY_NAME, cityName);
            values.put(WeatherContract.LocationEntry.COLUMN_COORD_LAT, lat);
            values.put(WeatherContract.LocationEntry.COLUMN_COORD_LONG, lon);
            Uri insertUri = mContentResolver
                    .insert(WeatherContract.LocationEntry.CONTENT_URI, values);
            return ContentUris.parseId(insertUri);
        }
    }

    /**
     * Helper method to have the sync adapter sync immediately - useful for testing.
     * See http://developer.android.com/training/sync-adapters/running-sync-adapter.html
     * @param context An app context
     */
    public static void syncImmediately(Context context) {
//        Log.d(LOG_TAG, "Hello from syncImmediately");

        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(getSyncAccount(context),
                                    context.getString(R.string.content_authority),
                                    bundle);
    }

    /** Helper method to schedule periodic syncs
     *
     */
    public static void configurePeriodicSync(Context context, int syncInterval, int flexTime) {
//        Log.d(LOG_TAG, "Hello from configurePeriodicSync");

        Account account = getSyncAccount(context);
        String authority = context.getString(R.string.content_authority);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {             // API Level 19
            // Use inexact repeating alarms available starting with API Level 19
            SyncRequest request = new SyncRequest.Builder()
                                                 .syncPeriodic(syncInterval, flexTime)
                                                 .setSyncAdapter(account, authority).build();
            ContentResolver.requestSync(request);
        } else {
            ContentResolver.addPeriodicSync(account, authority, new Bundle(), syncInterval);
        }
    }

    /**
     * Helper method to get the fake account to be used with SyncAdapter
     * if the fake account does not exist yet
     * See http://developer.android.com/training/sync-adapters/running-sync-adapter.html
     * @param context The context used to access the account service.
     * @return a fake account.
     */
    public static Account getSyncAccount(Context context) {
        // Returns the Account to pass to the various ContentResolver methods such as .requestSync
        // and .addPeriodicSync that generate requests to SunshineSyncAdapter to get more weather
        // data.
//        Log.d(LOG_TAG, "Hello from getSyncAccount");
        AccountManager accountManager =
                (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);

        // Create an instance of the Account with the needed account information
        Account newAccount = new Account(
                context.getString(R.string.app_name), context.getString(R.string.sync_account_type));

        // If the password does not exist, the account does not exist on the device, in which
        // case, we create it.
        if (accountManager.getPassword(newAccount) == null) {
            // Add the account and account type, no password ("") or username
            // If successful, return the Account object; otherwise return null.
            if (!accountManager.addAccountExplicitly(newAccount, "", null)) {
                return null;
            }
            /* If we do not set android:syncable="true" in the <provider> element
               in the manifest, then we would call
               ContentResolver.setIsSyncable(account, context.getString(R.string.content_authority), 1)
               here.
             */
            // Set up periodic sync and run an immediate sync.
            onAccountCreated(newAccount, context);
        }
        return newAccount;
    }

    private static void onAccountCreated(Account newAccount, Context context) {
        // Do some one-time setup after newAccount is created.  Called from getSyncAccount
//        Log.d(LOG_TAG, "Hello from onAccountCreated");
        SunshineSyncAdapter.configurePeriodicSync(context, SYNC_INTERVAL, SYNC_FLEXTIME);
        // Need to call setSyncAutomatically to enable periodic sync.
        ContentResolver.setSyncAutomatically(newAccount,
                context.getString(R.string.content_authority), true);
        // Finally, let's do a sync to get things started.
        syncImmediately(context);
    }

    private void notifyWeather() {
        Context context = getContext();
        // Check when the last update was and notify if it is the first one today.
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String lastNotificationKey = context.getString(R.string.pref_last_notification);
        long lastSync = prefs.getLong(lastNotificationKey, 0);

        if (System.currentTimeMillis() - lastSync >= 0.5*DAY_IN_MILLISEC) {
            // Last sync was more than 1/2 day ago.  We will send a notification with today's weather.
            // Similar to onCreateLoader in DetailFragment.
            String locationQuery = Utility.getPreferredLocation(context);
            String dateString = WeatherContract.getDbDateString(new Date());
            Uri weatherUri = WeatherEntry.buildWeatherLocationWithDate(locationQuery, dateString);

            // Query the content provider
            Cursor cursor = context.getContentResolver()
                                   .query(weatherUri, NOTIFY_WEATHER_PROJECTION, null, null, null);
            if (cursor.moveToFirst()) {
                int weatherID = cursor.getInt(INDEX_WEATHER_WEATHER_ID);
                double high = cursor.getDouble(INDEX_WEATHER_MAX_TEMP);
                double low = cursor.getDouble(INDEX_WEATHER_MIN_TEMP);
                String desc = cursor.getString(INDEX_WEATHER_DESC);

                int iconID = Utility.getIconResourceForWeatherCondition(weatherID);
                String title = context.getString(R.string.app_name);

                // Build the notification
                boolean isMetric = Utility.isMetric(context);
                String contentText = context.getString(R.string.format_notification, desc,
                                           Utility.formatTemperature(context, high, isMetric, ""),
                                           Utility.formatTemperature(context, low, isMetric, ""));

                // Send the notification
                // See http://developer.android.com/guide/topics/ui/notifiers/notifications.html
                // All of the following .set methods are required.
                NotificationCompat.Builder noteBuilder = new NotificationCompat.Builder(context)
                        .setSmallIcon(iconID)
                        .setContentTitle(title)
                        .setContentText(contentText);
                // Create an explicit intent for an Activity the user will open if they click on
                // the Notification.  (As in the onItemSelected handler in MainActivity.)
                Intent resultIntent = new Intent(context, DetailActivity.class)
                                      .putExtra(DetailFragment.DATE_KEY, dateString);
                // The stack builder object will contain an artificial back stack for the started
                // activity DetailActivity.  This ensures that navigating backward from the
                // activity leads out of Sunshine and back to the Home screen.

                TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
                // Adds the back stack for the Intent (but not the Intent itself).  The Back
                // button at the bottom of the screen will navigate back to the parent of
                // the activity passed to addParentStack, as defined by the value of
                // android:parentActivityName for the activity in the AndroidManifest.  In
                // the present case, we pass MainActivity, so the Back button navigates to the
                // Home screen.  Note the Navigate Up button at the top of the screen still
                // navigates to the MainActivity (parent of DetailActivity).
                stackBuilder.addParentStack(MainActivity.class);
                // Adds the Intent that starts the Activity to the top of the stack.
                stackBuilder.addNextIntent(resultIntent);
                PendingIntent resultPendingIntent =
                                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

                noteBuilder.setContentIntent(resultPendingIntent);
                NotificationManager notificationManager =
                       (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                // Send the notification.  WEATHER_NOTIFICATION_ID allows the notification to be
                // updated later.
                notificationManager.notify(WEATHER_NOTIFICATION_ID, noteBuilder.build());

                // Refresh the last notification in prefs
                SharedPreferences.Editor editor = prefs.edit();
                editor.putLong(lastNotificationKey, System.currentTimeMillis());
                editor.commit();
            }
            cursor.close();
        }
    }

    public static boolean notificationsOn(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(context.getString(R.string.pref_notifications_key),
                context.getResources().getBoolean(R.bool.pref_notifications_default));
    }
}
