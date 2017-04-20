package org.projects.shoppinglist;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;
import android.widget.DatePicker;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class DatePreference extends DialogPreference {
    private int lastDate = 0;
    private int lastMonth = 0;
    private int lastYear = 0;

    private String dateval; //Datoen som er valgt i et passende format.
    private CharSequence mSummary; //Summary til på præferencen, hvilket vil være den valgte dato
    private DatePicker picker = null; //Date picker

    private static Calendar cal = Calendar.getInstance();
    private static SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd"); //Format datoen skal være i
    private static String defaultDate = format1.format(cal.getTime()); //Laver d.d. til i et passende format.
    private boolean set = false; //Styre om der er valgt en dato, eller default bruges. - Har betydninger for logikken klassen har.

    private static String SETTINGS_DATETIMEKEY = "date"; //Nøgle til præferencens indhold


    //Få den valgte dato
    public static String getCurrentDate(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(SETTINGS_DATETIMEKEY, defaultDate);
    }

    //Få den valgte år
    public static int getYear(String dateval) {
        String[] pieces = dateval.split("-");
        return (Integer.parseInt(pieces[0]));
    }

    //Få den valgte måned
    public static int getMonth(String dateval) {
        String[] pieces = dateval.split("-");
        return (Integer.parseInt(pieces[1]));
    }

    //Få den valgte dag
    public static int getDate(String dateval) {
        String[] pieces = dateval.split("-");
        return (Integer.parseInt(pieces[2]));
    }


    public DatePreference(Context ctxt, AttributeSet attrs) {
        super(ctxt, attrs);

        //Sætter tekster til knapper dialogen har
        setPositiveButtonText("Set");
        setNegativeButtonText("Cancel");

        setDefaultValue(defaultDate); //Sætter default værdi.
    }

    @Override
    protected View onCreateDialogView() {
        //Sørger for, at tage højde for ændre enheder.

        picker = new DatePicker(getContext());

        // setCalendarViewShown(false) attribute er kun tilgængelig fra API level 11
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            picker.setCalendarViewShown(false);
        }

        return (picker);
    }

    @Override
    protected void onBindDialogView(View v) {
        super.onBindDialogView(v);

        //Setter dato i datepicker
        picker.updateDate(lastYear, lastMonth-1, lastDate);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            //Opdater gemte dato til, det valgte.
            lastYear = picker.getYear();
            lastMonth = picker.getMonth()+1;
            lastDate = picker.getDayOfMonth();

            String dateval = String.valueOf(lastYear) + "-"
                    + String.valueOf(lastMonth) + "-"
                    + String.valueOf(lastDate);

            if (callChangeListener(dateval)) {
                persistString(dateval);
            }
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return defaultDate;
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        dateval = null;

        //Finder ud af, om der er gemt en værdi, eller om der skal bruges default.
        if (restoreValue) {
            if (defaultValue == null) {
                  dateval = getPersistedString(defaultDate);
                  set = true;
            }
        }


        if(!set){
            //Sætter default dato, da ingen værdi er valgt.
            dateval = defaultDate;
        }

        //Får dato værdi, ud fra dataval variablen.
        lastYear = getYear(dateval);
        lastMonth = getMonth(dateval);
        lastDate = getDate(dateval);

        setSummary(dateval); //Sætter summary til det nye
    }

    /**public void setText(String text) {
        final boolean wasBlocking = shouldDisableDependents();

        dateval = text;

        persistString(text);

        final boolean isBlocking = shouldDisableDependents();
        if (isBlocking != wasBlocking) {
            notifyDependencyChange(isBlocking);
        }
    }*/


    /** Får summary */
    public CharSequence getSummary() {
        return mSummary;
    }

    /** Sætter summary */
    public void setSummary(CharSequence summary) {
        if (summary == null && mSummary != null || summary != null
                && !summary.equals(mSummary)) {
            mSummary = summary;
            notifyChanged();
        }
    }
}