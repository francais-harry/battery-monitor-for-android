package jp.f.dev.android.batterymonitor;

import java.io.File;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ToggleButton;

public final class BatteryMonitorActivity extends Activity {

    private static final String TAG = "BatteryMonitorActivity";
    private static final int PERIOD_PICKER_DIALOG_ID = 1;
    private static final int ABOUT_DIALOG_ID = 2;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        TextView fileNameView = (TextView) findViewById(R.id.textView3);
        fileNameView.setText(getString(R.string.file_name_description)
                + Util.FILE_NAME);

        // get monitoring status and set it to toggle button
        final boolean monitoring = ((BatteryMonitorApplication) getApplication())
                .getMonitorStatus();
        final ToggleButton toggleButton = (ToggleButton) findViewById(R.id.toggleButton);
        toggleButton.setChecked(monitoring);

        // is it better to move this logic to onResume?
        // Listener to monitor input from UI.
        toggleButton.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                    boolean isChecked) {
                Log.d(TAG, "onCheckedChanged called, isChecked = " + isChecked);
                if (isChecked == true) {
                    Intent intent = new Intent(BatteryMonitorActivity.this,
                            BatteryMonitorService.class);
                    intent.setAction(BatteryMonitorService.ACTION_START);
                    startService(intent);
                    // it takes time to start monitoring.
                    // need to block intent calling by state change complete.
                    buttonView.setClickable(false);
                } else {
                    Intent intent = new Intent(BatteryMonitorActivity.this,
                            BatteryMonitorService.class);
                    intent.setAction(BatteryMonitorService.ACTION_STOP);
                    startService(intent);
                    // it takes time to start monitoring.
                    // need to block intent calling by state change complete.
                    buttonView.setClickable(false);
                }

            }
        });

        final Button periodPickerButton = (Button) findViewById(R.id.button1);
        periodPickerButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                showDialog(PERIOD_PICKER_DIALOG_ID, null);
            }
        });
        
        final TextView periodDescription = (TextView) findViewById(R.id.textView4);
        final long period = ((BatteryMonitorApplication) getApplication()).getMonitorPeriod();

        periodDescription.setText(getPeriodString(period));

    }

    private String getPeriodString(long period) {
        String periodString = null;

        String[] periodArray = getResources().getStringArray(R.array.spinner_period_items);

        if (period == AlarmManager.INTERVAL_FIFTEEN_MINUTES) {
            periodString = periodArray[0];
        } else if (period == AlarmManager.INTERVAL_HALF_HOUR) {
            periodString = periodArray[1];
        } else if (period == AlarmManager.INTERVAL_HOUR) {
            periodString = periodArray[2];
        }
        return periodString;
    }
    
    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        switch (id) {
        case PERIOD_PICKER_DIALOG_ID: {
            // AlarmManager supports 15 min as minimum period for inexactRepeat.
            // For the purpose of battery save, this app offers limited choise only.
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            int selectedItemId = 0;
            final long period = ((BatteryMonitorApplication) getApplication())
                    .getMonitorPeriod();
            if (period == AlarmManager.INTERVAL_FIFTEEN_MINUTES) {
                selectedItemId = 0;
            } else if (period == AlarmManager.INTERVAL_HALF_HOUR) {
                selectedItemId = 1;
            } else if (period == AlarmManager.INTERVAL_HOUR) {
                selectedItemId = 2;
            }

            builder.setSingleChoiceItems(R.array.spinner_period_items, selectedItemId,
                    new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Log.i(TAG, "Picker selected, id = " + which);

                    long period = 0;
                    switch (which) {
                    case 0:
                        period = AlarmManager.INTERVAL_FIFTEEN_MINUTES;
                        break;
                    case 1:
                        period = AlarmManager.INTERVAL_HALF_HOUR;
                        break;
                    case 2:
                        period = AlarmManager.INTERVAL_HOUR;
                        break;
                    }
                    Intent intent = new Intent(BatteryMonitorActivity.this, BatteryMonitorService.class);
                    intent.setAction(BatteryMonitorService.ACTION_SET_PERIOD);
                    intent.putExtra(BatteryMonitorService.EXTRA_PERIOD, period);
                    startService(intent);

                    dismissDialog(PERIOD_PICKER_DIALOG_ID);
                }
            });
            return builder.create();

        }
        case ABOUT_DIALOG_ID: {
            String appNameAndVersion = null;
            try {
                PackageInfo pi = getPackageManager().getPackageInfo(
                        this.getApplicationContext().getPackageName(), 0);
                appNameAndVersion = getResources().getString(R.string.app_name)
                        + " " + pi.versionName;
            } catch (NameNotFoundException e) {
                Log.w(TAG, "NameNotFoundException", e);
            }

            return new AlertDialog.Builder(this)
                    .setTitle(
                            getResources().getString(R.string.menu_about) + ": "
                                    + appNameAndVersion)
                    .setMessage(Util.convertRawTestToString(this, R.raw.test))
                    .setPositiveButton(android.R.string.ok, null).create();
        }
        default:
            return super.onCreateDialog(id, args);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // display current battery level
        TextView textView = (TextView) findViewById(R.id.textView1);
        textView.setText(getString(R.string.battery_level) + " : "
                + Util.getCurrentBatteryLevel(getApplicationContext()) + "%");

        final ToggleButton toggleButton = (ToggleButton) findViewById(R.id.toggleButton);
        final TextView periodDescription = (TextView) findViewById(R.id.textView4);
        // Listener to monitor state change of the application.
        // In case state changed, reflect to the toggle button and period description.
        ((BatteryMonitorApplication) getApplication())
                .setOnStateChangeListener(new OnStateChangeListener() {
                    // This call back would be some time lag from UI input.
                    @Override
                    public void onStateChanged(boolean newState, long newPeriod) {
                        Log.d(TAG, "onStateChanged called, newState = "
                                + newState);
                        toggleButton.setClickable(true);
                        periodDescription.setText(getPeriodString(newPeriod));
                    }
                });

    }

    @Override
    protected void onPause() {
        // need to release listener in onPause.
        // There is risk that system sometimes does not call onDestory of
        // Activity.
        ((BatteryMonitorApplication) getApplication())
                .unsetOnStateChangeLister();

        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        final File file = new File(Util.FILE_PATH);
        if (!file.exists()) {
            menu.findItem(R.id.menu_delete).setEnabled(false);
            menu.findItem(R.id.menu_share).setEnabled(false);
        } else {
            menu.findItem(R.id.menu_delete).setEnabled(true);
            menu.findItem(R.id.menu_share).setEnabled(true);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final File file = new File(Util.FILE_PATH);
        switch (item.getItemId()) {
        case R.id.menu_delete: {
            file.delete();
            Toast.makeText(this, R.string.toast_file_deleted, Toast.LENGTH_LONG)
                    .show();
            return true;
        }
        case R.id.menu_share: {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
            intent.setType("text/comma-separated-values");
            startActivity(Intent.createChooser(intent, null));
            return true;
        }
        case R.id.menu_about: {
            showDialog(ABOUT_DIALOG_ID, null);
            return true;
        }
        default: {
            return super.onOptionsItemSelected(item);
        }
        }
    }
}