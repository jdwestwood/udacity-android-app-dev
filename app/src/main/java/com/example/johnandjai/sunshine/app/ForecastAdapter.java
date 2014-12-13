package com.example.johnandjai.sunshine.app;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by John and Jai on 11/6/2014.
 */

/**
 * {@Link ForecastAdapter} exposes a list of weather forecasts from a {@Link Cursor} to a
 * {@Link ListView}.
 */
public class ForecastAdapter extends CursorAdapter {
    // For SimpleCursorAdapter, we must use the same list item layout for all list members; we
    // write our own CursorAdapter to use one view for the first list item (today's forecast)
    // and another view for the other list items (forecasts for future days).
    public ForecastAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }

    private final String LOG_TAG = getClass().getSimpleName();
    private boolean mUseTodayLayout;
    private final int VIEW_TYPE_TODAY = 0;              // list_item_forecast_today.xml
    private final int VIEW_TYPE_FUTURE_DAY = 1;         // list_item_forecast.xml

    // public method to set whether to use the large layout for Today's forecast
    public void setUseTodayLayout(boolean useTodayLayout) {
        mUseTodayLayout = useTodayLayout;
    }

    @Override
    public int getItemViewType(int position) {
        // Use VIEW_TYPE_TODAY for the first list item.
        return (position == 0 && mUseTodayLayout) ? VIEW_TYPE_TODAY : VIEW_TYPE_FUTURE_DAY;
    }

    @Override
    public int getViewTypeCount() {
        // ForecastAdapter uses two view layouts for list items, defined in list_item_forecast_today.xml
        // and list_item_forecast.xml
        return 2;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        // makes a new view to hold the data pointed to by the cursor.
        int viewType = getItemViewType(cursor.getPosition());
        int layoutId = -1;
        switch (viewType) {
            case VIEW_TYPE_TODAY:
                layoutId = R.layout.list_item_forecast_today;
                break;
            case VIEW_TYPE_FUTURE_DAY:
                layoutId = R.layout.list_item_forecast;
                break;
            default:
                Log.v(LOG_TAG, "Unexpected viewType in the newView method");
        }
        // LayoutInflater.from is a static method that obtains the LayoutInflater instance
        // from the given context.
        View view =  LayoutInflater.from(context).inflate(layoutId, parent, false);
        // get references to the child views (ImageView, TextView's) of this list item so they can
        // be referenced in bindView.
        ViewHolder viewHolder = new ViewHolder(view);
        // use setTag to associate any object with a view.
        view.setTag(viewHolder);
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // view is the existing view, returned earlier by newView.  We have two list item views
        // based on list_item_forecast.xml and list_item_forecast_today.xml.  In the code below
        // we can use the same R.id.list_item_xxx resources because each view uses the same
        // id strings.

        // Our viewHolder already contains references to the relevant views, so set the
        // appropriate values through the viewHolder instead of costly findViewById calls.
        ViewHolder viewHolder = (ViewHolder) view.getTag();

        // Read the date from cursor.
        String dateString = cursor.getString(ForecastFragment.COL_WEATHER_DATE);
        // Read weather icon ID from cursor.
        int weatherID = cursor.getInt(ForecastFragment.COL_WEATHER_WEATHER_ID);
        int viewType = getItemViewType(cursor.getPosition());
        switch (viewType) {
            case VIEW_TYPE_TODAY:
                viewHolder.iconView.setImageResource(Utility.getArtResourceForWeatherCondition(weatherID));
                // Format the date, depending on whether it is Today, Tomorrow, this week, or next week.
                viewHolder.dateView.setText(Utility.getFriendlyDayString(context, dateString));
                break;
            case VIEW_TYPE_FUTURE_DAY:
                viewHolder.iconView.setImageResource(Utility.getIconResourceForWeatherCondition(weatherID));
                // Format the date, depending on whether it is Today, Tomorrow, this week, or next week.
                // Note that VIEW_TYPE_FUTURE_DAY is used for Today when Today's list item forecast
                // format is the same as it is for future days (i.e., when the MainActivity contains
                // both the ForecastFragment and the DetailFragment as on tablets in landscape orientation.
                viewHolder.dateView.setText(Utility.getDayName(context, dateString));
                break;
            default:
                Log.v(LOG_TAG, "Unexpected viewType in bindView method");
        }

        // Read weather forecast from cursor.
        String description = cursor.getString(ForecastFragment.COL_WEATHER_DESC);
        // Set the weather description
        viewHolder.descView.setText(description);
        // For accessibility
        viewHolder.iconView.setContentDescription(description);

        // Read user preference for metric or imperial temperature units.
        boolean isMetric = Utility.isMetric(context);

        // Read high temperature from cursor.
        double high = cursor.getDouble(ForecastFragment.COL_WEATHER_MAX_TEMP);
        viewHolder.highView.setText(Utility.formatTemperature(context, high, isMetric));

        // Read low temperature from cursor.
        double low = cursor.getDouble(ForecastFragment.COL_WEATHER_MIN_TEMP);
        viewHolder.lowView.setText(Utility.formatTemperature(context, low, isMetric));
    }

    /**
     * Cache of the children views for a forecast list item.
     */
    public static class ViewHolder {
        public final ImageView iconView;
        public final TextView dateView;
        public final TextView descView;
        public final TextView highView;
        public final TextView lowView;

        public ViewHolder(View view) {
            iconView = (ImageView) view.findViewById(R.id.list_item_icon);
            dateView = (TextView) view.findViewById(R.id.list_item_date_textview);
            descView = (TextView) view.findViewById(R.id.list_item_forecast_textview);
            highView = (TextView) view.findViewById(R.id.list_item_high_textview);
            lowView = (TextView) view.findViewById(R.id.list_item_low_textview);
        }
    }
}
