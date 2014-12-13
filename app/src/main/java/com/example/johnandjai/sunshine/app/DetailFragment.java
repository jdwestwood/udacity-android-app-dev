package com.example.johnandjai.sunshine.app;

/**
 * Created by John and Jai on 11/7/2014.
 */

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.johnandjai.sunshine.app.data.WeatherContract;

/**
 * A placeholder fragment containing a simple view.
 */
public class DetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    // LoaderManager.LoaderCallbacks interface allows DetailFragment to be notified when
    // the weather data changes and update its content accordingly.

    // make the forecast global, so can share it easily
    private String mDate;
    private static final String LOG_TAG = DetailFragment.class.getSimpleName();

    public static final int DETAIL_LOADER = 0;
    public static final String DATE_KEY = "forecast_date";
    public static final String LOCATION_KEY = "location";


    String[] FORECAST_COLUMNS = {
            // In this case the _id needs to be fully qualified with a table name, since
            // the content provider joins the location & weather tables in the background
            // and both have an _id column.
            WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
            WeatherContract.WeatherEntry.COLUMN_DATETEXT,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.WeatherEntry.COLUMN_HUMIDITY,
            WeatherContract.WeatherEntry.COLUMN_PRESSURE,
            WeatherContract.WeatherEntry.COLUMN_WIND_SPEED,
            WeatherContract.WeatherEntry.COLUMN_DEGREES
    };

    // These columns indices are tied to FORECAST_COLUMNS.  If FORECAST_COLUMNS changes,
    // these must change as well.
    public static final int COL_WEATHER_ID = 0;
    public static final int COL_WEATHER_DATE = 1;
    public static final int COL_WEATHER_WEATHER_ID = 2;
    public static final int COL_WEATHER_DESC = 3;
    public static final int COL_WEATHER_MAX_TEMP = 4;
    public static final int COL_WEATHER_MIN_TEMP = 5;
    public static final int COL_WEATHER_HUMIDITY = 6;
    public static final int COL_WEATHER_PRESSURE = 7;
    public static final int COL_WEATHER_WIND_SPEED = 8;
    public static final int COL_WEATHER_DEGREES = 9;

    // Member fields for the various Views in the fragment (assigned in onCreateView and
    // used in onLoaderFinished).
    private TextView mDayView;
    private TextView mDateView;
    private TextView mDescView;
    private ImageView mIconView;
    private TextView mMaxTempView;
    private TextView mMinTempView;
    private TextView mHumidityView;
    private TextView mWindView;
    private TextView mPressureView;
    private DirectionSpeedView mDirectionSpeedView;

    private static final String FORECAST_SHARE_HASHTAG = " #SunshineApp";
    private String mForecastStr;                         // forecast string for sharing
    private String mLocation;

    private ShareActionProvider mShareActionProvider;

    // constructor
    public DetailFragment() {
        // need to tell the fragment that it has an options menu
        setHasOptionsMenu(true);
    }

    /**
     * Create a new instance of DetailFragment, initialized with weather details
     * @param dateString  the date for the detailed weather information 'yyyyMMdd'
     * @return
     */
    public static DetailFragment newInstance(String dateString) {
        DetailFragment detailFragment = new DetailFragment();
        // bundle the date with the fragment
        Bundle args = new Bundle();
        args.putString(DetailFragment.DATE_KEY, dateString);
        detailFragment.setArguments(args);
        return detailFragment;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        // This is called when a new Loader is created, which occurs in onActivityCreated below.
        // This fragment uses only one loader, so we do not need to check the id of the loader.

        // Original: the DetailFragment was always part of DetailActivity, which was started
        // using an intent from MainActivity.
        // Intent intent = getActivity().getIntent();
        // if (intent != null && intent.hasExtra(DetailFragment.DATE_KEY)) {

        // Final: the DetailFragment can either be part of DetailActivity (phone) or the right-hand
        // pane in the MainActivity (tablet).  In either case, when the DetailFragment is
        // instantiated, it is assigned a Bundle containing the key DetailFragment.DATE_KEY and
        // the dateString in the form 'yyyyMMdd' as the value.
        String dateString = getArguments().getString(DetailFragment.DATE_KEY);
        // Note that mLocation is assigned when the Loader is created or restarted, If the user
        // rotates the device, the parent activity is stopped and restarted, which causes mLocation
        // to be lost.  The Loader persists, is not automatically re-run.  We use logic below in
        // onActivityCreated, onSaveInstanceState and onResume to store and recall mLocation
        // in a Bundle, and restart the Loader when the user's location has changed.
        mLocation = Utility.getPreferredLocation(getActivity());
        Uri weatherUri = WeatherContract.WeatherEntry
                    .buildWeatherLocationWithDate(mLocation, dateString);
        return new CursorLoader(getActivity(),
                       weatherUri,                      // query
                       FORECAST_COLUMNS,                // projection
                       null,                            // selection
                       null,                            // selectionArgs
                       null);                           // sort order
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (cursor.moveToFirst()) {
            String dateString = cursor.getString(COL_WEATHER_DATE);
            String weatherDesc = cursor.getString(COL_WEATHER_DESC);
            // Set day: 'Wednesday'
            mDayView.setText(Utility.getDayName(getActivity(), dateString));
            // Set date: 'June 24'
            mDateView.setText(Utility.getFormattedMonthDay(getActivity(), dateString));
            // Set weather description
            mDescView.setText(weatherDesc);
            // Set weather icon
            int weatherID = cursor.getInt(COL_WEATHER_WEATHER_ID);
            // For the Detail view, use the 'art_' resources, which are the large color images
            mIconView.setImageResource(Utility.getArtResourceForWeatherCondition(weatherID));
            mIconView.setContentDescription(weatherDesc);

            boolean isMetric = Utility.isMetric(getActivity());

            // Set high and low temperatures
            String high = Utility.formatTemperature(
                    getActivity(), cursor.getDouble(COL_WEATHER_MAX_TEMP), isMetric);
            String low = Utility.formatTemperature(
                    getActivity(), cursor.getDouble(COL_WEATHER_MIN_TEMP), isMetric);
            mMaxTempView.setText(high);
            mMinTempView.setText(low);

            // Set humidity: 'Humidity: 84%'
            String humidity = Utility.formatHumidity(getActivity(), cursor.getDouble(COL_WEATHER_HUMIDITY));
            mHumidityView.setText(humidity);
            // Set wind speed and direction: 'Wind: 6 km/h NW'
            String wind = Utility.formatWind(getActivity(),
                    cursor.getDouble(COL_WEATHER_WIND_SPEED),
                    cursor.getDouble(COL_WEATHER_DEGREES),
                    isMetric);
            mWindView.setText(wind);
            // Set pressure: 'Pressure: 1014 kPa'
            String pressure = Utility.formatPressure(getActivity(),
                    cursor.getDouble(COL_WEATHER_PRESSURE),
                    isMetric);
            mPressureView.setText(pressure);

            // Make the wind speed and direction available to
            mDirectionSpeedView.setSpeed(cursor.getDouble(COL_WEATHER_WIND_SPEED));
            mDirectionSpeedView.setDirection(cursor.getDouble(COL_WEATHER_DEGREES));

            // forecast string for sharing
            mForecastStr = String.format("%s - %s - %s/%s", dateString, weatherDesc, high, low);
        } else {
            Log.v(LOG_TAG, "In onLoadFinished, cursor returned no data.");
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        getLoaderManager().restartLoader(DetailFragment.DETAIL_LOADER, null, this);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        // life cycle of a Loader is bound to the Activity, not the Fragment.
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            // the cursor Loader persists across stopping/started the activity, so need to
            // assign the saved value of mLocation here; will check if mLocation has changed
            // in onResume, which is called after onActivityCreated.
            mLocation = savedInstanceState.getString(DetailFragment.LOCATION_KEY);
        }
        // DetailFragment can be loaded in the DetailActivity (phone) or MainActivity (tablet).
        // If it is part of the DetailActivity, then it is started via an intent from the
        // MainActivity.

        // Original: DetailFragment always created in DetailActivity; create the Loader when
        // DetailActivity starts.
        /* Intent intent = getActivity().getIntent();
         if (intent != null && intent.hasExtra(DetailFragment.DATE_KEY)) {
            // phone: DetailFragment in DetailActivity
            // last argument 'this' is the interface the LoaderManager will call to report about changes
            // in the state of the Loader.  DetailFragment implements the LoaderCallbacks<Cursor>
            // interface.
            getLoaderManager().initLoader(DetailFragment.DETAIL_LOADER, null, this);
         } else {
            // tablet: DetailFragment and ForecastFragment are side by side
            // Can use to display dummy data hardcoded in fragment_detail.xml for initial debug.
         } */

        // Final: create the forecast data Loader when the containing activity starts.
        // For phones, the containing activity is DetailActivity; for tablets it is MainActivity.
        // last argument 'this' is the interface the LoaderManager will call to report about changes
        // in the state of the Loader.  DetailFragment implements the LoaderCallbacks<Cursor>
        // interface.
        Bundle args = getArguments();
        // make sure this DetailFragment was created using the newInstance method.
        if (args != null && args.containsKey(DetailFragment.DATE_KEY)) {
            getLoaderManager().initLoader(DetailFragment.DETAIL_LOADER, null, this);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Since mLocation is assigned in onCreateLoader, we need to save it when the activity
        // ends (due to, e.g., user rotating the device); the Loader persists when the activity
        // ends.
        if (mLocation != null) {
            outState.putString(DetailFragment.LOCATION_KEY, mLocation);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        /* Original: DetailFragment is loaded in the DetailActivity via an intent from MainActivity
        Intent intentA = getActivity().getIntent();
        if (intentA != null && intentA.hasExtra(DetailFragment.DATE_KEY)) {
            // phone: DetailFragment in DetailActivity
            // check if location has changed; if so, need to restart the Loader so it listens for
            // for changes on a new URI.
            if (mLocation != null && !mLocation.equals(Utility.getPreferredLocation(getActivity()))) {
                getLoaderManager().restartLoader(DetailFragment.DETAIL_LOADER, null, this);
            }
        } else {
            // tablet: DetailFragment and ForecastFragment are side by side
        } */

        // Final: DetailFragment can be loaded in the DetailActivity (phone) or MainActivity (tablet).
        // Either way, we need to check if the location has changed and if so rerun the Loader to
        // obtain the forecast for the new location.
        Bundle args = getArguments();           // get arguments assigned in the newInstance method
        // make sure this DetailFragment was created using the newInstance method.
        if (args != null && args.containsKey(DetailFragment.DATE_KEY) &&
            mLocation != null && !mLocation.equals(Utility.getPreferredLocation(getActivity()))) {
            getLoaderManager().restartLoader(DetailFragment.DETAIL_LOADER, null, this);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_detail_layout, container, false);
        // Original version: the TextView is populated with the forecast or date string
        // passed in Intent.EXTRA_TEXT; in the final version, the forecast data is loaded
        // via a Loader<Cursor> in the onLoadFinished method above.
        // the detail Activity called via intent; inspect the intent.
        // Intent intent = getActivity().getIntent();
        // if (intent != null && intent.hasExtra(Intent.EXTRA_TEXT)) {
        //    mDate = intent.getStringExtra(Intent.EXTRA_TEXT);
        //    TextView textView = (TextView) rootView.findViewById(R.id.detail_day);
        //    textView.setText(mDate);
        //}
        // Cache the Views within the fragment_detail.xml layout to member fields for later
        // use in onLoadFinished.
        mDayView = (TextView) rootView.findViewById(R.id.detail_day_textview);
        mDateView = (TextView) rootView.findViewById(R.id.detail_date_textview);
        mDescView = (TextView) rootView.findViewById(R.id.detail_desc_textview);
        mIconView = (ImageView) rootView.findViewById(R.id.detail_icon);
        mMaxTempView = (TextView) rootView.findViewById(R.id.detail_max_temp_textview);
        mMinTempView = (TextView) rootView.findViewById(R.id.detail_min_temp_textview);
        mHumidityView = (TextView) rootView.findViewById(R.id.detail_humidity_textview);
        mWindView = (TextView) rootView.findViewById(R.id.detail_wind_textview);
        mPressureView = (TextView) rootView.findViewById(R.id.detail_pressure_textview);
        mDirectionSpeedView = (DirectionSpeedView) rootView.findViewById(R.id.detail_direction_speed_view);

        return rootView;
    }
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.detailfragment, menu);
        // Get the Share menu item
        MenuItem menuItem = menu.findItem(R.id.action_share);
        // Get the ShareActionProvider and hold onto it to set/change the share intent.
        // Use the ShareActionProvider in the support.v7 library for compatibility back to
        // API Level 7 (ShareActionProvider was added to Android in API Level 14).
        // MenuItemCompat is a helper method in the support.v4 library for accessing features
        // in MenuItem introduced after API Level 4 in a backwards compatible fashion.
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);
        // no need to create an OptionItemSelected handler; mShareActionProvider takes care
        // of executing the intent when the Share button is clicked.
        if (mShareActionProvider != null) {
            mShareActionProvider.setShareIntent(createShareForecast());
        } else {
            Log.d(LOG_TAG, "ShareActionProvider is null?");
        }
    }

    private Intent createShareForecast() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        // the share activity is not added to the activity stack, so the user will not return
        // to the share activity if he leaves the app during the share activity and comes
        // back to the app later.
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, mForecastStr + FORECAST_SHARE_HASHTAG);
        return intent;
    }
}