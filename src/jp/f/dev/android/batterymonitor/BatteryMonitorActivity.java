package jp.f.dev.android.batterymonitor;

import java.io.File;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class BatteryMonitorActivity extends Activity {

    private static final String TAG = "BatteryMonitorActivity";

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

    }

    @Override
    protected void onResume() {
        super.onResume();

        // display current battery level
        TextView textView = (TextView) findViewById(R.id.textView1);
        textView.setText(getString(R.string.battery_level) + " : "
                + Util.getCurrentBatteryLevel(getApplicationContext()) + "%");

        final ToggleButton toggleButton = (ToggleButton) findViewById(R.id.toggleButton);
        // Listener to monitor state change of the application.
        // In case state changed, reflect to the toggle button
        ((BatteryMonitorApplication) getApplication())
                .setOnStateChangeListener(new OnStateChangeListener() {
                    // This call back would be some time lag from UI input.
                    @Override
                    public void onStateChanged(boolean newState) {
                        Log.d(TAG, "onStateChanged called, newState = "
                                + newState);
                        toggleButton.setClickable(true);
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
        default: {
            return super.onOptionsItemSelected(item);
        }
        }
    }
}