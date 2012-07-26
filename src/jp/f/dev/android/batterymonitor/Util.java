package jp.f.dev.android.batterymonitor;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Environment;

public final class Util {
    public static final String FILE_NAME = "BatteryMonitor.csv";
    public static final String FILE_PATH = Environment
            .getExternalStorageDirectory() + "/" + FILE_NAME;

    public static final int getCurrentBatteryLevel(Context context) {
        Intent intent = context.registerReceiver(null, new IntentFilter(
                Intent.ACTION_BATTERY_CHANGED));
        return intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
    }
}
