package jp.f.dev.android.batterymonitor;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.util.Log;

public final class BatteryMonitorApplication extends Application {

    private static final String TAG = "BatteryMonitorApplication";
    private static final String PREFERENCE_NAME = "setting";
    private static final String PREFERENCE_KEY = "monitoring";
    private static Boolean sMonitoring = null;
    private OnSharedPreferenceChangeListener mLister;
    

    /* package */final boolean getMonitorStatus() {

        // lock for getter - setter
        synchronized (this) {
            // singletop for performance.
            if (sMonitoring == null) {
                SharedPreferences pref = getSharedPreferences(PREFERENCE_NAME,
                        MODE_PRIVATE);
                sMonitoring = pref.getBoolean(PREFERENCE_KEY, false);
            }
        }
        return sMonitoring;
    }

    /* package */final void setMonitorStatus(final boolean monitoring) {

        // lock for getter - setter
        synchronized (this) {
            // An attribute to store status of recording.
            // Service and process can be killed by system during recording.
            // Need to store status attribute so that to handle state properly.
            SharedPreferences pref = getSharedPreferences(PREFERENCE_NAME,
                    MODE_PRIVATE);
            Editor edit = pref.edit();
            edit.putBoolean(PREFERENCE_KEY, monitoring);
            edit.commit();

            sMonitoring = monitoring;
        }
    }

    /* package */final void setOnStateChangeListener(
            final OnStateChangeListener listener) {

        // unset if already registered.
        unsetOnStateChangeLister();

        // bypass preference state change lister to original listener for
        // encapsulating
        SharedPreferences pref = getSharedPreferences(PREFERENCE_NAME,
                MODE_PRIVATE);

        //TODO should be synchronized?
        mLister = new OnSharedPreferenceChangeListener() {

            @Override
            public void onSharedPreferenceChanged(SharedPreferences pref,
                    String key) {
                Log.d(TAG, "onSharedPreferenceChanged called");
                if (PREFERENCE_KEY.equals(key)) {
                    listener.onStateChanged(pref.getBoolean(key, false));

                }
            }
        };
        pref.registerOnSharedPreferenceChangeListener(mLister);
    }

    /* package */final void unsetOnStateChangeLister() {

        //TODO should be synchronized?
        if (mLister != null) {
            SharedPreferences pref = getSharedPreferences(PREFERENCE_NAME,
                    MODE_PRIVATE);
            pref.unregisterOnSharedPreferenceChangeListener(mLister);
            mLister = null;
        }

    }


}
