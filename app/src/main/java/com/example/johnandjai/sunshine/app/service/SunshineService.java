package com.example.johnandjai.sunshine.app.service;

import android.app.IntentService;
import android.content.Intent;

/**
 * Created by John and Jai on 12/18/2014.
 */
public class SunshineService extends IntentService {
    /* Added in first half of Lesson 6, replaces the AsyncTask FetchWeatherTask.  Abandoned in the
       second half of Lesson 6 and SunshineSyncAdapter used instead. */
    // SunshineService fetches the weather data using a service.
    // Note that a Service is a context, so no need for mContext here.

    private final String LOG_TAG = SunshineService.class.getSimpleName();
    public static String LOCATION_QUERY_KEY = "location";

    public SunshineService() {
        super("SunshineService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        /*
        // This code was originally the doInBackground method of FetchWeatherTask

        String locationQuery;
        if (intent.hasExtra(LOCATION_QUERY_KEY)) {
            locationQuery = intent.getExtras().getString(LOCATION_QUERY_KEY);
        } else {
            return;
        }

        // These two need to be declared outside the try/catch
        // so that they can be closed in the finally block.
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        String format = "json";
        String units = "metric";
        int numDays = 14;
        String forecastJsonStr;

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
                    weatherValues.put(WeatherContract.WeatherEntry.COLUMN_LOC_KEY, locationId);
                    weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DATETEXT, WeatherContract.getDbDateString(date));
                    weatherValues.put(WeatherContract.WeatherEntry.COLUMN_HUMIDITY, humidity);
                    weatherValues.put(WeatherContract.WeatherEntry.COLUMN_PRESSURE, pressure);
                    weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WIND_SPEED, windSpeed);
                    weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DEGREES, windDirection);
                    weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP, high);
                    weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP, low);
                    weatherValues.put(WeatherContract.WeatherEntry.COLUMN_SHORT_DESC, description);
                    weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID, weatherId);

                    cVVector.add(weatherValues);
                }
                addWeather(cVVector);
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
        } */
    }

    /*
    private void addWeather(Vector<ContentValues> cVVector) {
        if (cVVector.size() > 0) {
            Uri weatherUri = WeatherContract.WeatherEntry.CONTENT_URI;
            // convert the Vector into an array.
            ContentValues[] cvArray = new ContentValues[cVVector.size()];
            cVVector.toArray(cvArray);
            // A Service is a context, so can call getContentResolver directly with the data.
            // Use .bulkInsert here to insert multiple entries efficiently.
            getContentResolver().bulkInsert(weatherUri, cvArray);
            Log.v(LOG_TAG, "Inserted weather for " + cVVector.size() + " days");
        } else {
            Log.v(LOG_TAG, "In addWeather, did not insert any rows.");
        }
    }

    private long addLocation(String locationSetting, String cityName, double lat, double lon) {
        // checks if the locationSetting exists in the location table and if not adds it.
        // returns the id of the location.
        Log.v(LOG_TAG, "Inserting " + cityName + " with coordinates: " + lat + ", " + lon);
        Uri locationUri = WeatherContract.LocationEntry.CONTENT_URI;
        String[] projection = {WeatherContract.LocationEntry._ID};
        String selection = WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = " + locationSetting;

        // A Service is a context, so can call getContentResolver directly with the data.
        Cursor cursor = getContentResolver()
                .query(WeatherContract.LocationEntry.CONTENT_URI, projection, selection, null, null);
        if (cursor.moveToFirst()) {
            // found a row; need to get and return the id
            Log.v(LOG_TAG, "Found it in the database.");
            return cursor.getLong(cursor.getColumnIndex(WeatherContract.LocationEntry._ID));
        } else {
            // insert a row with the new locationSetting
            Log.v(LOG_TAG, "Did not find it in the database, so will insert it now.");
            ContentValues values = new ContentValues();
            values.put(WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING, locationSetting);
            values.put(WeatherContract.LocationEntry.COLUMN_CITY_NAME, cityName);
            values.put(WeatherContract.LocationEntry.COLUMN_COORD_LAT, lat);
            values.put(WeatherContract.LocationEntry.COLUMN_COORD_LONG, lon);
            Uri insertUri = getContentResolver()
                    .insert(WeatherContract.LocationEntry.CONTENT_URI, values);
            return ContentUris.parseId(insertUri);
        }
    }

    static public class AlarmReceiver extends BroadcastReceiver {

        private final String LOG_TAG = AlarmReceiver.class.getSimpleName();

        public void onReceive(Context context, Intent intent) {
            // The 'intent' is the Intent that was sent to this broadcast receiver.
            // Log.e(LOG_TAG, "Received an alarm!");
            String location = intent.getStringExtra(SunshineService.LOCATION_QUERY_KEY);
            Intent serviceIntent = new Intent(context, SunshineService.class)
                                       .putExtra(SunshineService.LOCATION_QUERY_KEY, location);
            context.startService(serviceIntent);
        }
    } */
}
