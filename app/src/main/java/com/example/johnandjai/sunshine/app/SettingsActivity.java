//  In this class, @TargetApi(Build.VERSION_CODES.HONEYCOMB) (API 11) means that if Lint detects
//  that I am using something newer in the method than my android:minSdkVersion, but as old or
//  older than API 11, Lint will not complain. If the method references something that wasn't added
//  until a newer API (e.g. API 14), then a Lint error would appear, because my @TargetApi
//  annotation says that I only fixed the code to work on API and below, not API 14 and below.
package com.example.johnandjai.sunshine.app;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.text.TextUtils;
import android.view.MenuItem;

import com.example.johnandjai.sunshine.app.data.WeatherContract;
import com.example.johnandjai.sunshine.app.sync.SunshineSyncAdapter;

import java.util.List;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends PreferenceActivity {

    private static final String LOG_TAG = SettingsActivity.class.getSimpleName();
    /**
     * Determines whether to always show the simplified settings UI, where
     * settings are presented in a single list. When false, settings are shown
     * as a master/detail two-pane view on tablets. When true, a single pane is
     * shown on tablets.
     */
    private static final boolean ALWAYS_SIMPLE_PREFS = false;

    /* In the Udacity course, we added the @Override for this method in Lesson 5 to prevent a new
       MainActivity from being created when we exit the SettingsActivity.  This method does not
       appear to be necessary using the version of SettingsActivity that I got from the Android
       documentation in order to be compatible with the Kindle.  Maybe needed for Sophia's Samsung
       phone??
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)                  // API 16
    @Override
    public Intent getParentActivityIntent() {
        // FLAG_ACTIVITY_CLEAR_TOP indicates we should check if the MainActivity is already running
        // in our task and to use that one instead of creating a fresh MainActivity, which will not
        // have the DetailFragment populated with data.
        return super.getParentActivityIntent().addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    } */

    // create a new boolean to track whether we are calling onPreferenceChange in the
    // binding step or if it's being called on an actual preference change later:
    private static boolean mBindingPreference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();                               // does nothing if API Level < 11
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setupActionBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // Show the Up button in the action bar if API of user's device SDK is >= 11;
            // getActionBar method was introduced in API 11.
            getActionBar().setDisplayHomeAsUpEnabled(true);         // show the Up button
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button. In the case of this
            // activity, the Up button is shown. Use NavUtils to allow users
            // to navigate up one level in the application structure. For
            // more details, see the Navigation pattern on Android Design:
            //
            // http://developer.android.com/design/patterns/navigation.html#up-vs-back
            //
            // TODO: If Settings has multiple levels, Up should navigate up
            // that hierarchy.

            // Settings activity is finished; go back to the activity we came from (either the
            // Main activity or the Details activity.
            finish();
            // Navigate to the parent activity for the Settings activity defined in AndroidManifest.xml,
            // regardless of which activity the user reached the Settings activity from.
//            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
// ***  Used in Sunshine  ***
        setupSimplePreferencesScreen();
    }

    /**
     * Kindle HD 7 device is using this simplified UI *
     * Shows the simplified settings UI if the device API < 11 or if device is not an
     * extra-large tablet; a simplified, single-pane UI will be shown.
     */
    private void setupSimplePreferencesScreen() {
        if (!isSimplePreferences(this)) {
            return;
        }
        // In the simplified UI, PreferenceFragments are not used at all and we instead
        // use the older PreferenceActivity APIs, which is why the method calls
        // are struck out.

// ***  Used in Sunshine on the Kindle  (i.e., ignore the strike-outs) ***
        // Add 'general' preferences:  Location and Temperature units in this case
        addPreferencesFromResource(R.xml.pref_general);
        // Attach an event listener to each preference item using the bindPreferenceSummaryToValue
        // method defined below and trigger the listener with the current value of the item.
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_location_key)));
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_units_key)));
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_notifications_key)));

        /* Original code when the Settings Activity is created from the
           New - Activity - Settings Activity menu

        // Add 'notifications' preferences, and a corresponding header.
        PreferenceCategory fakeHeader = new PreferenceCategory(this);
        fakeHeader.setTitle(R.string.pref_header_notifications);
        getPreferenceScreen().addPreference(fakeHeader);
        addPreferencesFromResource(R.xml.z_pref_notification);

        // Add 'data and sync' preferences, and a corresponding header.
        fakeHeader = new PreferenceCategory(this);
        fakeHeader.setTitle(R.string.pref_header_data_sync);
        getPreferenceScreen().addPreference(fakeHeader);
        addPreferencesFromResource(R.xml.z_pref_data_sync);

        // Bind the summaries of preferences to their values. When their values change, their
        // summaries are updated to reflect the new value, per the Android Design guidelines.
        // The argument to findPreference is the key name defined in the XML file, and the
        // value is the summary text that appears below the Settings item indicating the
        // current value of the setting.
        bindPreferenceSummaryToValue(findPreference("notifications_new_message_ringtone"));
        bindPreferenceSummaryToValue(findPreference("sync_frequency"));
        */
    }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        mBindingPreference = true;
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        Object value;
        SharedPreferences sharedPreferences =
                       PreferenceManager.getDefaultSharedPreferences(preference.getContext());
        if (preference instanceof CheckBoxPreference) {
            CheckBoxPreference checkBoxPreference = (CheckBoxPreference) preference;
            value = sharedPreferences.getBoolean(checkBoxPreference.getKey(), true);
        } else if (preference instanceof ListPreference) {
            ListPreference listPreference = (ListPreference) preference;
            value = sharedPreferences.getString(listPreference.getKey(), "");
        } else {
            value = sharedPreferences.getString(preference.getKey(), "");
        }
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, value);
        mBindingPreference = false;
    }

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener
            = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();
            Context context = preference.getContext();
            if (!mBindingPreference) {
                // if preference has already been bound
                if (preference.getKey().equals(context.getString(R.string.pref_location_key))) {
                    /* In Lessons 1-5, we used FetchWeatherTask to get weather data

                    // if location was changed, get new weather data.
                    FetchWeatherTask weatherTask = new FetchWeatherTask(context);
                    String location = stringValue;
                    weatherTask.execute(location); */

                    /* In Lesson 6, we implemented SunshineSyncAdapter to get weather data */
                    SunshineSyncAdapter.syncImmediately(context);

                } else {
                    // notify content URI to allow the cursor to update that weather may be affected
                    context.getContentResolver()
                            .notifyChange(WeatherContract.WeatherEntry.CONTENT_URI, null);
                }
            }

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                // stringValue is the entry value for the chosen entry label
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the chosen entry label.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } else if (preference instanceof CheckBoxPreference) {
                // Summary value set in pref_general.xml using :summaryOff and :summaryOn.
                // Log.e(LOG_TAG, "Set Checkbox to " + stringValue);

            } else if (preference instanceof RingtonePreference) {
                // For ringtone preferences, look up the correct display value
                // using RingtoneManager.
                if (TextUtils.isEmpty(stringValue)) {
                    // Empty values correspond to 'silent' (no ringtone).
                    preference.setSummary(R.string.pref_ringtone_silent);

                } else {
                    Ringtone ringtone = RingtoneManager.getRingtone(
                            preference.getContext(), Uri.parse(stringValue));

                    if (ringtone == null) {
                        // Clear the summary if there was a lookup error.
                        preference.setSummary(null);
                    } else {
                        // Set the summary to reflect the new ringtone display
                        // name.
                        String name = ringtone.getTitle(preference.getContext());
                        preference.setSummary(name);
                    }
                }

            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    /** {@inheritDoc} */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this) && !isSimplePreferences(this);
    }

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
        & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * Determines whether the simplified settings UI should be shown. This is
     * true if this is forced via {@link #ALWAYS_SIMPLE_PREFS}, or the device
     * doesn't have newer APIs like {@link PreferenceFragment}, or the device
     * doesn't have an extra-large screen. In these cases, a single-pane
     * "simplified" settings UI should be shown.
     */
    private static boolean isSimplePreferences(Context context) {
        return ALWAYS_SIMPLE_PREFS
                || Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB
                || !isXLargeTablet(context);
    }

    /** {@inheritDoc} */
    // if an extra-large tablet and API Level >= 11, call loadHeadersFromResource, which will
    // create Settings with headers; the group of Settings under each header will be created
    // in the PreferenceFragments for each section (see below).
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)   // make sure do not use anything with API higher than 11
    public void onBuildHeaders(List<Header> target) {    // onBuildHeaders called only if API >= 11
        if (!isSimplePreferences(this)) {
            // create Settings headers from XML file; Settings groups under each header are specified
            // in XML files for each group; the XML headers file specifies the PreferenceFragment
            // to create for each Settings group.
            loadHeadersFromResource(R.xml.z_pref_headers, target);
        }
    }

    /**
     * Boilerplate for General Preferences category from the Android documentation website.
     * Not used in Sunshine.  This fragment shows General Preferences only. It is used when the
     * activity is showing a two-pane settings UI.  Instantiated in z_pref_headers.xmlml
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("example_text"));
            bindPreferenceSummaryToValue(findPreference("example_list"));
        }
    }

    /**
     * Boilerplate for Notification Preferences category from the Android documentation website.
     * Not used in Sunshine.  This fragment shows Notification Preferences only. It is used when the
     * activity is showing a two-pane settings UI.  Instantiated in z_pref_headers.xmlml
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class NotificationPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.z_pref_notification);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("notifications_new_message_ringtone"));
        }
    }

    /**
     * Boilerplate for Sync Preferences category from the Android documentation website.
     * Not used in Sunshine.  This fragment shows Sync Preferences only. It is used when the
     * activity is showing a two-pane settings UI.  Instantiated in z_pref_headers.xmlml
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class DataSyncPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.z_pref_data_sync);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("sync_frequency"));
        }
    }
}
