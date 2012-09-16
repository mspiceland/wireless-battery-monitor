/*
* Copyright (c) 2011 Michael Spiceland
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.fuzzymonkey.wirelessbatterymonitor.activity;

import com.fuzzymonkey.wirelessbatterymonitor.R;
import com.fuzzymonkey.wirelessbatterymonitor.service.MonitorService;
import com.fuzzymonkey.wirelessbatterymonitor.view.BatteryView;
import com.fuzzymonkey.wirelessbatterymonitor.view.SpeedometerView;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainDisplayActivity extends Activity {
    public static final String TAG = "FUZZYMONKEY";

    /* Message types sent from the BluetoothChatService Handler */
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    public static final int MESSAGE_HR = 6;
    public static final int MESSAGE_DIST = 7;
    public static final int MESSAGE_ZONE = 9;
    public static final int MESSAGE_BATTERY = 10;
    public static final int MESSAGE_VOLTAGE = 11;
    public static final int MESSAGE_CURRENT = 12;
    public static final int MESSAGE_SPEED = 13;
    public static final int MESSAGE_GPS_SPEED = 14;
    public static final int MESSAGE_GPS_ACCURACY = 15;

    /* gps modes */
    public static final int SPEED_MODE_NORMAL = 1;
    public static final int SPEED_MODE_GPS = 2;
    public int speed_mode = SPEED_MODE_NORMAL;

    /* Key names received from the BluetoothChatService Handler */

    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    /* Intent request codes */
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    /* Views */
    private TextView mTitle;
    private TextView mVoltageView;
    private TextView mCurrentView;
    private SpeedometerView mSpeedometer;
    private BatteryView mBattery;

    /* Name of the connected device */
    private String mConnectedDeviceName = null;
    /* Local Bluetooth adapter */
    private BluetoothAdapter mBluetoothAdapter = null;
    /* Member object for the chat services */
    private boolean mIsBound;
    private MonitorService mMonitorService = null;

    private PowerManager.WakeLock wl;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG,"onCreate() start");
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.maindisplay);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            alert(getString(R.string.device_doesnt_support_bt));
            finish();
            return;
        }

        mSpeedometer = (SpeedometerView) findViewById(R.id.speedometer);
        mVoltageView = (TextView) findViewById(R.id.voltage_value);
        mCurrentView = (TextView) findViewById(R.id.current_value);

        mBattery = (BatteryView) findViewById(R.id.battery);

        /* Set up the custom title */
        mTitle = (TextView) findViewById(R.id.title_left_text);
        mTitle.setText(R.string.app_name);
        mTitle = (TextView) findViewById(R.id.title_right_text);

        mSpeedometer.setLabel("mph");
        mSpeedometer.setMaxValue(40);

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "DoNotDimScreen");

        final ToggleButton gpstoggle = (ToggleButton) findViewById(R.id.gpstoggle);
        gpstoggle.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                /* Perform action on clicks */
                if (gpstoggle.isChecked()) {                    
                    if (mMonitorService != null) {
                        if(mMonitorService.turnOnGps() == 0) {
                            gpstoggle.setChecked(false);
                            speed_mode = SPEED_MODE_NORMAL;
                            Toast.makeText(MainDisplayActivity.this, "GPS is not available", Toast.LENGTH_SHORT).show();
                        } else {
                            speed_mode = SPEED_MODE_GPS;
                            Toast.makeText(MainDisplayActivity.this, "using GPS for speed", Toast.LENGTH_SHORT).show();
                        }
                    }
                } else {
                    mMonitorService.turnOffGps();
                    speed_mode = SPEED_MODE_NORMAL;
                    Toast.makeText(MainDisplayActivity.this, "using speed sensor for speed", Toast.LENGTH_SHORT).show();
                }
            }
        });

        Log.v(TAG,"onCreate() stop");
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.v(TAG,"onStart() start");
        /* If BT is not on, request that it is turned on
           setupMonitor() will be called during onActivityResult() */
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            if (mMonitorService == null) setupMonitor();
        }

        Log.v(TAG,"onStart() stop");
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            Log.d(TAG, "back button pressed");

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.want_to_stop)
                   .setCancelable(false)
                   .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                       public void onClick(DialogInterface dialog, int id) {
                           stopService(new Intent(MainDisplayActivity.this,
                                   MonitorService.class));
                           finish();
                       }
                   })
                   .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
            return super.onKeyDown(KeyEvent.KEYCODE_SHIFT_LEFT, event); // noop
        }

        return super.onKeyDown(keyCode, event);
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            /* This is called when the connection with the service has been
               established, giving us the service object we can use to
               interact with the service.  Because we have bound to a explicit
               service that we know is running in our own process, we can
               cast its IBinder to a concrete class and directly access it. */
            /* TODO: see if this needs to be the LocalBinder */
            mMonitorService = ((MonitorService.LocalBinder)service).getService();
            mMonitorService.setHandler(mHandler);
            mMonitorService.sendUpdate();
        }

        public void onServiceDisconnected(ComponentName className) {
            /* This is called when the connection with the service has been
               unexpectedly disconnected -- that is, its process crashed.
               Because it is running in our same process, we should never
               see this happen. */
            mMonitorService = null;
            Toast.makeText(MainDisplayActivity.this, "monitor service disconnected",
                    Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "onDestroy() starting");

        if (mIsBound) {
            /* Detach our existing connection. */
            unbindService(mConnection);
            mIsBound = false;
        }

        Log.v(TAG, "onDestroy() done");
    }

    private void setupMonitor() {
        /* Establish a connection with the service.  We use an explicit
           class name because we want a specific service implementation that
           we know will be running in our own process (and thus won't be
           supporting component replacement by other applications). */
        bindService(new Intent(MainDisplayActivity.this, 
                MonitorService.class), mConnection, Context.BIND_AUTO_CREATE);

        /* start the service manually (this should stick around) */
        startService(new Intent(MainDisplayActivity.this,
                MonitorService.class));
        mIsBound = true;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case REQUEST_CONNECT_DEVICE:
            /* When DeviceListActivity returns with a device to connect */
            if (resultCode == Activity.RESULT_OK) {
                /* Get the device MAC address */
                String address = data.getExtras()
                                     .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                /* Get the BLuetoothDevice object */
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                /* Attempt to connect to the device */
                mMonitorService.connect(device);
            }
            break;
        case REQUEST_ENABLE_BT:
            /* When the request to enable Bluetooth returns */
            if (resultCode == Activity.RESULT_OK) {
                /* Bluetooth is now enabled */
                setupMonitor();
            } else {
                /* User did not enable Bluetooth or an error occured */
                Toast.makeText(this, getString(R.string.bt_not_enabled_leaving), Toast.LENGTH_SHORT).show();
                finish();
            }
            break;
        }
    }

    private void alert(String string) {
        Toast.makeText(getBaseContext(), string, Toast.LENGTH_SHORT).show();
    }

    /* The Handler that gets information back from the BluetoothChatService */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                /* updates the title bar (def want this) */
                switch (msg.arg1) {
                case MonitorService.STATE_CONNECTED:
                    mTitle.setText(R.string.title_connected_to);
                    mTitle.append(mConnectedDeviceName);
                    break;
                case MonitorService.STATE_CONNECTING:
                    mTitle.setText(R.string.title_connecting);
                    break;
                case MonitorService.STATE_LISTEN:
                case MonitorService.STATE_NONE:
                    mTitle.setText(R.string.title_not_connected);
                    break;
                }
                break;
            case MESSAGE_SPEED:
                String speed = msg.getData().getString(TOAST);
                if (speed_mode == SPEED_MODE_NORMAL)
                    mSpeedometer.setValue(Float.valueOf(speed).floatValue());
                break;
            case MESSAGE_VOLTAGE:
                String newVoltage = msg.getData().getString(TOAST);
                mVoltageView.setText(newVoltage);
                mBattery.setValue(Double.valueOf(newVoltage.trim()).doubleValue());
                break;
            case MESSAGE_CURRENT:
                mCurrentView.setText((msg.getData().getString(TOAST)));
                break;
            case MESSAGE_DEVICE_NAME:
                /* save the connected device's name */
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_GPS_SPEED:
                Float gpsSpeed = (Float) msg.obj;
                if (speed_mode == SPEED_MODE_GPS) {
                    mSpeedometer.setValue(gpsSpeed.floatValue() * (float)2.23693629);
                }
                break;
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.scan:
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.pairing_msg)
                   .setCancelable(false)
                   .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                       public void onClick(DialogInterface dialog, int id) {
                           /* Launch the DeviceListActivity to see devices and do scan */
                           Intent serverIntent = new Intent(MainDisplayActivity.this, DeviceListActivity.class);
                           startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
                       }
                   });
            AlertDialog alert = builder.create();
            alert.show();

            return true;
        case R.id.stop_activity:
            if (mMonitorService != null) {
                mMonitorService.stop();
            }

            stopService(new Intent(MainDisplayActivity.this,
                    MonitorService.class));

            finish();
            return true;
        }
        return false;
    }

    public void onInit(int status) {

    }

    @Override
    protected void onPause() {
        super.onPause();
        wl.release();
    }

    @Override
    protected void onResume() {
        super.onResume();
        wl.acquire();
    }
}
