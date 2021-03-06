// Kindle HD 7 2nd gen runs Android 4.0.3 (API 15) = Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1 = 15
package com.example.johnandjai.sunshine.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.example.johnandjai.sunshine.app.data.WeatherContract;
import com.example.johnandjai.sunshine.app.sync.SunshineSyncAdapter;
import com.johnjai.romainguy.viewserver.ViewServer;

import java.util.Date;

public class MainActivity extends ActionBarActivity implements ForecastFragment.OnItemSelectedListener {

    private final String LOG_TAG = MainActivity.class.getSimpleName();
    private SharedPreferences mSharedPrefs;
    private boolean mTwoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(LOG_TAG, "Created");
        setContentView(R.layout.activity_main);
        /* Original code added ForecastFragment dynamically; in the final code, ForecastFragment
           is a static fragment which is included in the activity_main.xml layout file.
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new ForecastFragment())
                    .commit();
        }
        */
         // Dynamically add the DetailFragment if the FrameLayout with id 'weather_detail_container'
         // exists in the activity_main.xml layout, as it will if the device is large enough
         // to use the two-pane layout.
         // findViewById returns the FrameView
        mTwoPane = (findViewById(R.id.weather_detail_container) != null);

        ForecastFragment forecastFragment = ((ForecastFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.fragment_forecast));
        forecastFragment.setUseTodayLayout(!mTwoPane);

        if (mTwoPane) {
            String selectedDate = forecastFragment.getSelectedForecastDate();
            String dateString = (selectedDate == null) ?
                                 WeatherContract.getDbDateString(new Date()) : selectedDate;
            // Set the DetailFragment to display details for the currently selected forecast date.
            DetailFragment detailFragment = DetailFragment.newInstance(dateString);
            getSupportFragmentManager().beginTransaction()
                        .replace(R.id.weather_detail_container, detailFragment)
                        .addToBackStack(null)
                        .commit();
        }

        // Make sure we have gotten an account.  The function also does an immediate sync to get
        // the current weather data.
        SunshineSyncAdapter.initializeSyncAdapter(this);

        // Enable HierarchyViewer functionality; see notes for the ViewServer class in the
        // ViewServer module.
        ViewServer.get(this).addWindow(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(LOG_TAG, "Starting (becomes visible)");
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Gains focus
        Log.d(LOG_TAG, "Resuming (gains focus)");
       ViewServer.get(this).setFocusedWindow(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Loses focus; pre-Honeycomb, this is the last event guaranteed to fire before Android
        // kills the app.
        Log.d(LOG_TAG, "Pausing (loses focus)");
    }

    @Override
    protected void onStop() {
        super.onStop();
        // No longer visible; post-Honeycomb, this is the last event guaranteed to fire before
        // Android kills the app.
        Log.d(LOG_TAG, "Stopping (no longer visible)");
    }

    @Override
    protected void onDestroy() {
        // Destroyed
        super.onDestroy();
        Log.d(LOG_TAG, "Destroyed");
        ViewServer.get(this).removeWindow(this);
    }

    @Override
    public void onItemSelected(String dateString) {
    // implement the callback required by ForecastFragment.OnItemSelectedListener
    // dateString is 'yyyyMMdd'.
        // Pass the db date string dateString to the
        // DetailActivity; the ContentLoader in DetailActivity will get all the forecast
        // data for that date via the appropriate URI.
        if (mTwoPane) {                                       // two-pane view on tablet
            // Replace the current DetailFragment with a new instance of DetailFragment containing
            // the weather details for the date selected by the user.
            DetailFragment detailFragment = DetailFragment.newInstance(dateString);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.weather_detail_container, detailFragment)
            //        .addToBackStack(null)
                    .commit();
        } else {                                              // start new DetailActivity on phone
            // Open a new DetailActivity, which will create a new DetailFragment containing the
            // weather details for the item selected by the user.
            Intent intent = new Intent(this, DetailActivity.class)
                  .putExtra(DetailFragment.DATE_KEY, dateString);
            startActivity(intent);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            /* Up until the end of Lesson 6, the View Location menu action was handled here.  We
               gave Google Maps the location and let Google Maps find it.  At the end of Lesson 6,
               we moved the View Location handling to ForecastFragment.
            case R.id.action_view_location:
                // On Kindle, default is that Google apps are not available; see
                // http://www.cnet.com/how-to/how-to-get-maps-gmail-on-the-kindle-fire-hd-without-rooting/
                // for how to manually install the Google Maps app and other Google apps so this
                // part of the project will work.  Other useful links for Android .apk files:
                // http://forum.xda-developers.com/showthread.php?t=1897380
                // http://www.androiddrawer.com/
                String location = mSharedPrefs.getString(getString(R.string.pref_location_key),
                                                        getString(R.string.pref_location_default));
                // Using Uri.parse().buildUpon... mangles the Uri so Google Maps does not understand it.
                Uri geoLocation = Uri.parse("geo:0,0?q=" + location);
                showMap(geoLocation);  */
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}