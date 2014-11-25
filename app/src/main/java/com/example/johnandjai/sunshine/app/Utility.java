package com.example.johnandjai.sunshine.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.example.johnandjai.sunshine.app.data.WeatherContract;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by John and Jai on 10/24/2014.
 */
public class Utility {
    // Format used for storing dates in the database.  Also used for converting those strings
    // back into date objects for comparison/processing.
    public static final String DATE_FORMAT = "yyyyMMdd";

    public static String getPreferredLocation(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(context.getString(R.string.pref_location_key),
                context.getString(R.string.pref_location_default));
    }

    public static boolean isMetric(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(context.getString(R.string.pref_units_key),
                context.getString(R.string.pref_units_metric))
                .equals(context.getString(R.string.pref_units_metric));
    }

    public static String formatDate(String dateString) {
        // dateString is 'yyyyMMdd', as stored in the database; returns a string like Nov 7, 2014
        Date date = WeatherContract.getDateFromDb(dateString);
        return DateFormat.getDateInstance().format(date);
    }

    /**
     * Helper method to convert the database representation of the date into something to display
     * to users.  As classy and polished a user experience as "20140102" is, we can do better.
     *
     * @param context Context to use for resource localization
     * @param dateString The db formatted date string, expected to be of the form specified
     *                in Utility.DATE_FORMAT
     * @return a user-friendly representation of the date.
     */
    public static String getFriendlyDayString(Context context, String dateString) {
        // 'Today, June 8'
        // 'Tomorrow'
        // 'Wednesday' ... 'Sunday' for the next five days after tomorrow
        // 'Mon Jun 8' ... 'Sun Jun 14' for days for the week after that
        Date todayDate = new Date();
        String todayString = WeatherContract.getDbDateString(todayDate);
        Date inputDate = WeatherContract.getDateFromDb(dateString);

        // If the date we're building the String for is today's date, the format is
        // 'Today, June 24'
        if (todayString.equals(dateString)) {
            String today = context.getString(R.string.today);
            // see the definition of the string template 'format_full_friendly_date' in
            // strings.xml to see how we pass 'today' and the return value of
            // getFormattedMonthDay to getString in order to create the string from
            // the string template.
            return context.getString(R.string.format_full_friendly_date,
                                     today,
                                     getFormattedMonthDay(context, dateString));
        } else {
            Calendar cal = Calendar.getInstance();
            cal.setTime(todayDate);
            cal.add(Calendar.DATE, 7);
            String weekFutureString = WeatherContract.getDbDateString(cal.getTime());

            if (dateString.compareTo(weekFutureString) < 0) {
                // If the input date is less than a week in the future, return just the
                // day name.
                return getDayName(context, dateString);
            } else {
                // Use the form 'Mon Jun 3"
                SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM d");
                return shortenedDateFormat.format(inputDate);
            }
        }
    }

    /**
     * Given a day, returns just the name to use for that day.
     * E.g "today", "tomorrow", "wednesday".
     *
     * @param context Context to use for resource localization
     * @param dateString The db formatted date string, expected to be of the form specified
     *                in Utility.DATE_FORMAT
     * @return
     */
    public static String getDayName(Context context, String dateString) {
        SimpleDateFormat dbDateFormat = new SimpleDateFormat(Utility.DATE_FORMAT);
        // need a Try...Catch because .parse may throw an exception
        try {
            Date inputDate = dbDateFormat.parse(dateString);
            Date todayDate = new Date();
            // If the date is today, return the localized version of "Today" instead of
            // the actual day name.
            if (WeatherContract.getDbDateString(todayDate).equals(dateString)) {
                return context.getString(R.string.today);
            } else {
                // If the date is set for tomorrow, the format is "Tomorrow".
                Calendar cal = Calendar.getInstance();
                cal.setTime(todayDate);
                cal.add(Calendar.DATE, 1);
                Date tomorrowDate = cal.getTime();
                if (WeatherContract.getDbDateString(tomorrowDate).equals(dateString)) {
                    return context.getString(R.string.tomorrow);
                } else {
                    // Format is just the day of the week, e.g., 'Wednesday'
                    SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE");
                    return dayFormat.format(inputDate);
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
            // Could not process the date correctly.
            return "";
        }
    }

    /**
     * Converts db date format to the format "Month day", e.g "June 24".
     * @param context Context to use for resource localization
     * @param dateString The db formatted date string, expected to be of the form specified
     *                in Utility.DATE_FORMAT
     * @return The day in the form of a string formatted "December 6"
     */
    public static String getFormattedMonthDay(Context context, String dateString) {
        SimpleDateFormat dbDateFormat = new SimpleDateFormat(Utility.DATE_FORMAT);
        try {
            Date inputDate = dbDateFormat.parse(dateString);
            SimpleDateFormat monthDayFormat = new SimpleDateFormat("MMMM d");
            return monthDayFormat.format(inputDate);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String formatTemperature(Context context, double temperature, boolean isMetric) {
        double temp;
        if (!isMetric) {
            temp = 9*temperature/5 + 32;
        } else {
            temp = temperature;
        }
        // see the definition of the string template 'format_temperature' in
        // strings.xml to see how we pass 'temp' getString in order to create the
        // string from the string template.
        return context.getString(R.string.format_temperature, temp);
    }

    /**
     * Formats the db humidity.
     * @param context Context to use for resource localization
     * @param humidity The db humidity in %
     * @return The humidity string in the form "Humidity: 84%"
     */
    public static String formatHumidity(Context context, double humidity) {
        return context.getString(R.string.format_humidity, humidity);
    }

    /**
     * Formats the db wind speed and direction.
     * @param context Context to use for resource localization
     * @param windSpeed The db wind speed in meters per second
     * @param windDirection The db wind direction -180 deg to +180 deg, 0 deg is north
     * @return The wind string in the form "Wind: 6 km/h NW%" or Wind: "9 mph NW"
     */
    public static String formatWind(Context context, double windSpeed, double windDirection, boolean isMetric) {
        String windUnits;
        double wind;
        if (isMetric) {
            windUnits = "km/h";
            wind = 3.6 * windSpeed;               // 1 km/h = 3.6 meters/sec
        } else {
            windUnits = "mph";
            wind = 2.237 * windSpeed;             // 1 mph = 2.237 meters/sec
        }
        return context.getString(R.string.format_wind, wind, windUnits, getDirectionFromDegrees(windDirection));
    }

    private static String getDirectionFromDegrees(double windDirection) {
        String N_S = "";
        String E_W = "";
        if (windDirection > -67.5 && windDirection < 67.5) {
            N_S = "N";
        } else if (windDirection < -157.5 || windDirection > 157.5) {
            N_S = "S";
        }
        if (windDirection >= -157.5 && windDirection <= -67.5) {
            E_W = "W";
        } else if (windDirection >= 22.5 || windDirection <= 67.5) {
            E_W = "E";
        }
        return N_S + E_W;
    }

    /**
     * Formats the db wind speed and direction.
     * @param context Context to use for resource localization
     * @param pressure The db pressure in hPa
     * @return The pressure string in the form "Pressure: 1014 hPa" or Pressure: "30.12 inHg"
     */
    public static String formatPressure(Context context, double pressure, boolean isMetric) {
        String pressureUnits;
        double press;
        if (isMetric) {
            pressureUnits = "hPa";
            press = pressure;
        } else {
            pressureUnits = "inHg";
            press = 0.02953 * pressure;
        }
        return context.getString(R.string.format_pressure, press, pressureUnits);
    }

    /**
     * Helper method to provide the icon resource id according to the weather condition id returned
     * by the OpenWeatherMap call.
     * @param weatherId from OpenWeatherMap API response
     * @return resource id for the corresponding icon. -1 if no relation is found.
     */
    public static int getIconResourceForWeatherCondition(int weatherId) {
        // Based on weather code data found at:
        // http://bugs.openweathermap.org/projects/api/wiki/Weather_Condition_Codes
        if (weatherId >= 200 && weatherId <= 232) {
            return R.drawable.ic_storm;
        } else if (weatherId >= 300 && weatherId <= 321) {
            return R.drawable.ic_light_rain;
        } else if (weatherId >= 500 && weatherId <= 504) {
            return R.drawable.ic_rain;
        } else if (weatherId == 511) {
            return R.drawable.ic_snow;
        } else if (weatherId >= 520 && weatherId <= 531) {
            return R.drawable.ic_rain;
        } else if (weatherId >= 600 && weatherId <= 622) {
            return R.drawable.ic_snow;
        } else if (weatherId >= 701 && weatherId <= 761) {
            return R.drawable.ic_fog;
        } else if (weatherId == 761 || weatherId == 781) {
            return R.drawable.ic_storm;
        } else if (weatherId == 800) {
            return R.drawable.ic_clear;
        } else if (weatherId == 801) {
            return R.drawable.ic_light_clouds;
        } else if (weatherId >= 802 && weatherId <= 804) {
            return R.drawable.ic_cloudy;
        }
        return -1;
    }

    /**
     * Helper method to provide the art resource id according to the weather condition id returned
     * by the OpenWeatherMap call.
     * @param weatherId from OpenWeatherMap API response
     * @return resource id for the corresponding image. -1 if no relation is found.
     */
    public static int getArtResourceForWeatherCondition(int weatherId) {
        // Based on weather code data found at:
        // http://bugs.openweathermap.org/projects/api/wiki/Weather_Condition_Codes
        if (weatherId >= 200 && weatherId <= 232) {
            return R.drawable.art_storm;
        } else if (weatherId >= 300 && weatherId <= 321) {
            return R.drawable.art_light_rain;
        } else if (weatherId >= 500 && weatherId <= 504) {
            return R.drawable.art_rain;
        } else if (weatherId == 511) {
            return R.drawable.art_snow;
        } else if (weatherId >= 520 && weatherId <= 531) {
            return R.drawable.art_rain;
        } else if (weatherId >= 600 && weatherId <= 622) {
            return R.drawable.art_snow;
        } else if (weatherId >= 701 && weatherId <= 761) {
            return R.drawable.art_fog;
        } else if (weatherId == 761 || weatherId == 781) {
            return R.drawable.art_storm;
        } else if (weatherId == 800) {
            return R.drawable.art_clear;
        } else if (weatherId == 801) {
            return R.drawable.art_light_clouds;
        } else if (weatherId >= 802 && weatherId <= 804) {
            return R.drawable.art_clouds;
        }
        return -1;
    }
}
