package com.example.johnandjai.sunshine.app;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.example.johnandjai.sunshine.app.data.WeatherContract;

import java.util.Date;

public class ForecastFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    // LoaderManager.LoaderCallbacks interface allows ForecastFragment to be notified when
    // the weather data changes and update its content accordingly.

    public ForecastFragment() {
    }

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface OnItemSelectedListener {
        /**
         * Specifies the callback for when an item has been selected.  The containing activity
         * (MainActivity in this case) must implement the onItemSelected method.
         */
        public void onItemSelected(String date);
    }

    // the callback interface of the containing activity (MainActivity)
    private OnItemSelectedListener mItemSelectedListenerInterface;

    private final String LOG_TAG = getClass().getSimpleName();

    private String mLocation;                    // instance variable to save our location
    private boolean mUseTodayLayout;

    // Each loader in a Fragment has an ID, which allows multiple Loaders to be active at the
    // same time.
    private static final int FORECAST_LOADER = 0;
    private static String SELECTED_ITEM_KEY = "cursor_position";
    private static String SELECTED_DATE_KEY = "forecast_date";

    // For the forecast view, we are showing only a small subset of the stored data.
    // Specify the columns we need.
    private static final String[] FORECAST_COLUMNS = {
            // In this case the _id needs to be fully qualified with a table name, since
            // the content provider joins the location & weather tables in the background
            // and both have an _id column.
            WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
            WeatherContract.WeatherEntry.COLUMN_DATETEXT,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING
    };

    // These columns indices are tied to FORECAST_COLUMNS.  If FORECAST_COLUMNS changes,
    // these must change as well.
    public static final int COL_WEATHER_ID = 0;
    public static final int COL_WEATHER_DATE = 1;
    public static final int COL_WEATHER_WEATHER_ID = 2;
    public static final int COL_WEATHER_DESC = 3;
    public static final int COL_WEATHER_MAX_TEMP = 4;
    public static final int COL_WEATHER_MIN_TEMP = 5;
    public static final int COL_LOCATION_SETTING = 6;

    // First method of populating forecast data is to put data into a String[] and use an
    // ArrayAdapter.
    // Create a global ArrayAdapter, which will take data from a source of forecast
    // data and use it to populate a ListView it's attached to.
    // private ArrayAdapter<String> mForecastAdapter;

    // Final method of populating forecast data is to use a CursorLoader which updates every
    // time data in the database changes, and also avoids having to query the database for the
    // same data when the user changes the orientation of the phone.  We also use ForecastAdapter,
    // which is a custom CursorAdapter, to load the data from the Cursor to the ListView items.
    private ForecastAdapter mForecastAdapter;
    private ListView mListView;
    private static int mPosition = 0;           // current selected position in the forecast list
    private static String mDate;                // forecast date for the currently selected position

    // implement the LoaderManager.LoaderCallbacks<Cursor> interface
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        // CursorLoader initialized in onActivityCreated
//        Log.d(LOG_TAG, "Hello from onCreateLoader");
        // Only return data after today.
        String startDate = WeatherContract.getDbDateString(new Date());

        // Sort order: ascending, by date.
        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATETEXT + " ASC";

        mLocation = Utility.getPreferredLocation(getActivity());
        Uri weatherForLocationUri =
                WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(mLocation, startDate);
        Log.d("ForecastFragment", "Uri: " + weatherForLocationUri.toString());

        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return new CursorLoader(getActivity(),
                weatherForLocationUri,                    // query for the Loader
                FORECAST_COLUMNS,                         // projection
                null,                                     // selection
                null,                                     // selectionArgs
                sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor data) {
//        Log.d(LOG_TAG, "Hello from onLoadFinished");
        mForecastAdapter.swapCursor(data);
        if (mPosition != ListView.INVALID_POSITION) {
            // If we do not need to restart the loader, and there is a desired position to
            // restore to, do so now.
            mListView.setSelection(mPosition);
            mListView.setItemChecked(mPosition, true);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
//        Log.d(LOG_TAG, "Hello from onLoadReset");
        mForecastAdapter.swapCursor(null);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        // life cycle of a Loader is bound to the Activity, not the Fragment.
        super.onActivityCreated(savedInstanceState);
        // last argument 'this' is the interface the LoaderManager will call to report about changes
        // in the state of the Loader.  ForecastFragment implements the LoaderCallbacks<Cursor>
        // interface.
        getLoaderManager().initLoader(FORECAST_LOADER, null, this);
    }

    // called before onCreate
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mItemSelectedListenerInterface = (OnItemSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement the OnItemSelectedListener interface");
        }
    }

    @Override
    // called before onCreateView
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Add this line in order for this fragment to handle menu events
        this.setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection. The action bar will automatically handle clicks
        // on the Home/Up button, as long as you specify a parent activity in
        // AndroidManifest.xml
        switch (item.getItemId()) {
            case R.id.action_refresh:
                updateWeather();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             final Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        if (savedInstanceState != null && savedInstanceState.containsKey(SELECTED_ITEM_KEY)) {
            // The ListView probably has not been populated yet.  Perform the actual swapout
            // in onLoadFinished.
            mPosition = savedInstanceState.getInt(SELECTED_ITEM_KEY);
            mDate = savedInstanceState.getString(SELECTED_DATE_KEY);
        }
        // Original code connected the daily forecast string parsed from the Json from the
        // OpenWeather API to a single TextView using an ArrayAdapter
        // Needed in original method of populating data based on using an ArrayAdapter
        // mForecastAdapter.setNotifyOnChange(true);

        // Next version connects the database columns with TextViews for each piece of weather
        // information using a SimpleCursorAdapter; use an Adapter to match up list items with
        // rows in the weather database.
        /*
        mForecastAdapter = new SimpleCursorAdapter(
                getActivity(),        // the current context (this fragment's parent activity)
                R.layout.list_item_forecast,         // ID of list item layout (XML filename)
                null,                                // Cursor, will be specified later
                // put the data in these column names in the database as fetched by CursorLoader...
                new String[]{WeatherContract.WeatherEntry.COLUMN_DATETEXT,
                        WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
                        WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
                        WeatherContract.WeatherEntry.COLUMN_MIN_TEMP
                },
                // into the following TextView id's.
                new int[]{R.id.list_item_date_textview,
                        R.id.list_item_forecast_textview,
                        R.id.list_item_high_textview,
                        R.id.list_item_low_textview
                },
                0
        );

        mForecastAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            // ViewBinder is used by external clients of SimpleCursorAdapter to bind values fom
            // the Cursor to a view; it is a callback for SimpleCursorAdapter to change the
            // default view binding behavior built into SimpleCursorAdapter.
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                // view is the View that binds the data in column columnIndex of the cursor;
                // we use setViewValue to perform extra formatting and conversions beyond the
                // defaults in SimpleCursorAdapter.
                switch (columnIndex) {
                    case COL_WEATHER_MAX_TEMP:
                    case COL_WEATHER_MIN_TEMP: {
                        // for max and min temperature, we have to do some formatting and possibly
                        // a conversion.
                        boolean isMetric = Utility.isMetric(getActivity());
                        ((TextView) view).setText(Utility.formatTemperature(
                                        cursor.getDouble(columnIndex), isMetric)
                        );
                        return true;
                    }
                    case COL_WEATHER_DATE: {
                        String dateString = cursor.getString(columnIndex);
                        TextView dateView = (TextView) view;
                        dateView.setText(Utility.formatDate(dateString));
                        return true;
                    }
                }
                return false;
            }
        });
        */

        // Final version uses a custom ForecastAdapter extended from CursorAdapter, which
        // supports multiple list item views: today's forecast has a different layout than
        // all the other days.
        mForecastAdapter = new ForecastAdapter(getActivity(), null, 0);
        mForecastAdapter.setUseTodayLayout(mUseTodayLayout);

        mListView = (ListView) rootView.findViewById(R.id.listview_forecast);
        mListView.setAdapter(mForecastAdapter);
        // set the click listener for a forecast item in the listView.
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            // anonymous class syntax for implementing the AdapterView.OnItemClickListener
            // interface.
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                mPosition = position;
                ForecastAdapter adapter = (ForecastAdapter) adapterView.getAdapter();
                Cursor cursor = adapter.getCursor();
                if (cursor != null && cursor.moveToPosition(mPosition)) {
                    // Original method used an ArrayAdapter, and the forecast string was already a
                    // single formatted string; pass the forecast string using Intent.EXTRA_TEXT:
                    // String forecast = mForecastAdapter.getItem(position);

                    // Second method composed the forecast string for a particular day from data
                    // extracted from the cursor holding the weather data from the database;
                    // the forecast string was then passed to the intent that launches DetailActivity
                    // using Intent.EXTRA_TEXT.
                    // boolean isMetric = Utility.isMetric(getActivity());
                    // String forecast = String.format("%s - %s - %s/%s",
                    //        Utility.formatDate(cursor.getString(COL_WEATHER_DATE)),
                    //        cursor.getString(COL_WEATHER_DESC),
                    //        Utility.formatTemperature(cursor.getDouble(COL_WEATHER_MAX_TEMP), isMetric),
                    //        Utility.formatTemperature(cursor.getDouble(COL_WEATHER_MIN_TEMP), isMetric));

                    // Third method passes just the db date string in COL_WEATHER_DATE to the
                    // DetailActivity; the ContentLoader in DetailActivity will get all the forecast
                    // data for that date via the appropriate URI.
                    // Intent intent = new Intent(getActivity(), DetailActivity.class)
                    //        .putExtra(DetailActivity.DATE_KEY, cursor.getString(COL_WEATHER_DATE));
                    // startActivity(intent);

                    // Final method invokes the onItemSelected callback in the containing
                    // activity (the MainActivity), which will determine whether to open the
                    // DetailActivity (phone) or update the DetailFragment in the two-pane layout
                    // in the MainActivity (tablet).
                    // mItemSelectedListenerInterface is assigned in the onAttach event handler;
                    // it is the callback interface for the containing activity.
                    mDate = cursor.getString(COL_WEATHER_DATE);
                    mItemSelectedListenerInterface.onItemSelected(mDate);
//                Toast.makeText(getActivity(), forecast, Toast.LENGTH_SHORT).show();
                }
            }
        });
        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // When tablets rotate, the currently selection list item needs to be saved.
        // When no item is selected, mPosition will be set to ListView.INVALID_POSITION,
        // so check for that before storing.
        if (mPosition != ListView.INVALID_POSITION) {
            outState.putInt(SELECTED_ITEM_KEY, mPosition);
            if (mDate != null) {
                outState.putString(SELECTED_DATE_KEY, mDate);
            }
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    public void onResume() {
        // event triggered when activity resumes after user navigates away (to the Settings menu,
        // for example.
        super.onResume();
        if (mLocation != null && !Utility.getPreferredLocation(getActivity()).equals(mLocation)) {
            // load weather data from database; (do NOT run a FetchWeatherTask).
            getLoaderManager().restartLoader(FORECAST_LOADER, null, this);
        }
    }

    private void updateWeather() {
        // Get the location value in the Settings and get the weather forecast data.
        String location = Utility.getPreferredLocation(getActivity());
        new FetchWeatherTask(getActivity()).execute(location);
    }

    public void setUseTodayLayout(boolean useTodayLayout) {
        mUseTodayLayout = useTodayLayout;
        if (mForecastAdapter != null) {
            mForecastAdapter.setUseTodayLayout(mUseTodayLayout);
        }
    }

    public String getSelectedForecastDate() {
        return mDate;
    }
}
