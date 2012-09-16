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

package com.fuzzymonkey.wirelessbatterymonitor.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;

import com.fuzzymonkey.wirelessbatterymonitor.R;
import com.fuzzymonkey.wirelessbatterymonitor.activity.MainDisplayActivity;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

public class MonitorService extends Service {
    private NotificationManager mNM;

    private static final String TAG = "FUZZYMONKEY MonitorService";

    /* Unique UUID for this application */
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    /* Member fields */
    private BluetoothAdapter mAdapter;
    private Handler mHandler = null;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;
    private String mDeviceName = null;

    /* Constants that indicate the current connection state */
    public static final int STATE_NONE = 0;       /* we're doing nothing */
    public static final int STATE_LISTEN = 1;     /* now listening for incoming connections */
    public static final int STATE_CONNECTING = 2; /* now initiating an outgoing connection */
    public static final int STATE_CONNECTED = 3;  /* now connected to a remote device */
    private int connectTries = 0;
    private static final boolean D = true;

    /* gps */
    protected static final int STATE_LOCATING = 1;
    protected static final int STATE_AVAILABLE = 2;
    protected static final int STATE_OUT_OF_SERVICE = 3;
    protected static final int STATE_TEMPORARILY_UNAVAILABLE = 4;
    protected static final int STATE_OFF = 5;
    private LocationManager locationManager;
    private LocationUpdateHandler mLocationListener = null;
    Location mLastLocation = null;

    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder {
        public MonitorService getService() {
            return MonitorService.this;
        }
    }

    @Override
    public void onCreate() {
        Log.v(TAG, "onCreate()");
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        /* Display a notification about us starting.  We put an icon in the status bar. */
        showNotification();

        /* try to auto connect
           Get a set of currently paired devices */
        Set<BluetoothDevice> pairedDevices = mAdapter.getBondedDevices();

        /* If there are paired devices, add each one to the ArrayAdapter */
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName().startsWith("RN") == true) {
                    alert("attempting to auto connect with " + device.getName());
                    /* Attempt to connect to the device */
                    connect(device);
                }
            }
        }

        locationManager =
            (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Received start id " + startId + ": " + intent);

        /* send a msg to update our newly connected display */
        sendUpdate();

        /* We want this service to continue running until it is explicitly
           stopped, so return sticky. */
        return START_STICKY;
    }

    public void sendUpdate() {
        sendDeviceName(mDeviceName);
        setState(mState);
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "onDestroy()");
        /* Cancel the persistent notification. */
        mNM.cancel(R.string.service_started);

        /* kill our threads since we are going away */
        stop();

        /* Tell the user we stopped. */
        alert("monitor service stopped");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Set the current state of the chat connection
     * @param state  An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        if (D) {
            Log.d(TAG, "setState() " + mState + " -> " + state);
        }
        mState = state;

        /* Give the new state to the Handler so the UI Activity can update */
        if (mHandler != null) {
            mHandler.obtainMessage(MainDisplayActivity.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
        }
    }

    /**
     * Return the current connection state. */
    public synchronized int getState() {
        return mState;
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     * @param device  The BluetoothDevice to connect
     */
    public synchronized void connect(BluetoothDevice device) {
        if (D) Log.d(TAG, "connect to: " + device);

        /* Cancel any thread attempting to make a connection */
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        /* Cancel any thread currently running a connection */
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        /* Start the thread to connect with the given device */
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     * @param socket  The BluetoothSocket on which the connection was made
     * @param device  The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        if (D) {
            Log.d(TAG, "connected");
        }

        /* Cancel the thread that completed the connection */
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        /* Cancel any thread currently running a connection */
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        /* Start the thread to manage the connection and perform transmissions */
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        mDeviceName = device.getName();
        sendDeviceName(mDeviceName);

        setState(STATE_CONNECTED);
    }

    private void sendDeviceName(String name) {
        /* Send the name of the connected device back to the UI Activity */
        if (mHandler != null) {
            Message msg = mHandler.obtainMessage(MainDisplayActivity.MESSAGE_DEVICE_NAME);
            Bundle bundle = new Bundle();
            bundle.putString(MainDisplayActivity.DEVICE_NAME, name);
            msg.setData(bundle);
            mHandler.sendMessage(msg);
        }
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        if (D) {
            Log.d(TAG, "stop");
        }
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        setState(STATE_NONE);
        /* no since in sending updates if we don't have anything connected */
        mHandler = null;
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        setState(STATE_LISTEN);

        /* Send a failure message back to the Activity */
        Log.v(TAG, "connectionFailed()");
        if (mHandler != null) {
            Message msg = mHandler.obtainMessage(MainDisplayActivity.MESSAGE_TOAST);
            Bundle bundle = new Bundle();
            bundle.putString(MainDisplayActivity.TOAST, "Unable to connect device");
            msg.setData(bundle);
            mHandler.sendMessage(msg);
        }
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        setState(STATE_LISTEN);

        /* Send a failure message back to the Activity */
        Log.v(TAG, "connectionFailed()");
        if (mHandler != null) {
            Message msg = mHandler.obtainMessage(MainDisplayActivity.MESSAGE_TOAST);
            Bundle bundle = new Bundle();
            bundle.putString(MainDisplayActivity.TOAST, "Device connection was lost");
            msg.setData(bundle);
            mHandler.sendMessage(msg);
        }
    }

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;

            /* Get a BluetoothSocket for a connection with the given BluetoothDevice */
            try {
                if (connectTries == 0) {
                    tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
                } else {
                    Method m = device.getClass().getMethod("createRfcommSocket", new Class[] {int.class});
                    tmp = (BluetoothSocket) m.invoke(device, 1);
                }
            } catch (Exception e) {
                Log.e(TAG, "create() failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread");
            setName("ConnectThread");

            /* Always cancel discovery because it will slow down a connection */
            mAdapter.cancelDiscovery();

            /* Make a connection to the BluetoothSocket */
            try {
                /* This is a blocking call and will only return on a
                   successful connection or an exception */
                mmSocket.connect();
            } catch (IOException e) {
                /* Close the socket */
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                /* this is our first failure */
                if (0 == connectTries) {
                    connectTries++;
                    connect(mmDevice);
                } else {
                    connectionFailed();
                }
                return;
            }

            /* Reset the ConnectThread because we're done */
            synchronized (MonitorService.this) {
                mConnectThread = null;
            }

            /* Start the connected thread */
            connected(mmSocket, mmDevice);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            /* Get the BluetoothSocket input and output streams */
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");

            /* Keep listening to the InputStream while connected */
            while (true) {
                try {
                    /* read line by lane and parse as we go */
                    /* TODO: investigate using packed binary for efficiency */
                    BufferedReader br = new BufferedReader(new InputStreamReader(mmInStream));
                    String line;
                    while ((line = br.readLine()) != null) {
                        //Log.v(TAG, "BT just read a line: " + line);
                        Scanner scanner = new Scanner(line);
                        scanner.useDelimiter("=");
                        if (line.contains("=") && scanner.hasNext()) {
                            String name = scanner.next();
                            String value = scanner.next();
                            //Log.v(TAG, "name : " + name.trim() + ", and value is : " + value.trim());

                            if (mHandler != null) {
                                if (name.trim().equalsIgnoreCase("s")) {
                                    Message msg = mHandler.obtainMessage(MainDisplayActivity.MESSAGE_SPEED);
                                    Bundle bundle = new Bundle();
                                    bundle.putString(MainDisplayActivity.TOAST, value.trim());
                                    msg.setData(bundle);
                                    mHandler.sendMessage(msg);
                                }
                                if (name.trim().equalsIgnoreCase("v")) {
                                    Message msg = mHandler.obtainMessage(MainDisplayActivity.MESSAGE_VOLTAGE);
                                    Bundle bundle = new Bundle();
                                    bundle.putString(MainDisplayActivity.TOAST, value.trim());
                                    msg.setData(bundle);
                                    mHandler.sendMessage(msg);
                                }
                                if (name.trim().equalsIgnoreCase("a")) {
                                    Message msg = mHandler.obtainMessage(MainDisplayActivity.MESSAGE_CURRENT);
                                    Bundle bundle = new Bundle();
                                    bundle.putString(MainDisplayActivity.TOAST, value.trim());
                                    msg.setData(bundle);
                                    mHandler.sendMessage(msg);
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    //Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                //Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    /**
     * Show a notification while this service is running.
     */
    private void showNotification() {
        /* In this sample, we'll use the same text for the ticker and the expanded notification */
        CharSequence text = getText(R.string.service_started);

        /* Set the icon, scrolling text and timestamp */
        Notification notification = new Notification(android.R.drawable.star_on, text,
                System.currentTimeMillis());

        /* The PendingIntent to launch our activity if the user selects this notification */
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainDisplayActivity.class), Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

        notification.flags |= Notification.FLAG_ONGOING_EVENT;

        /* Set the info for the views that show in the notification panel. */
        notification.setLatestEventInfo(this, getText(R.string.service_label),
                       text, contentIntent);

        /* Send the notification.
           We use a layout id because it is a unique number.  We use it later to cancel. */
        mNM.notify(R.string.service_started, notification);
    }

    public void setHandler(Handler handler) {
        /* TODO Auto-generated method stub */
        mHandler = handler;
    }

    private void alert(String string) {
        Log.v(TAG, "ALERT: " + string);
    }

    public class LocationUpdateHandler implements LocationListener {

        public void onLocationChanged(Location loc) {
                if (mLastLocation != null) {
                    if (mHandler != null) {
                        mHandler.obtainMessage(MainDisplayActivity.MESSAGE_GPS_SPEED, new Float(loc.getSpeed())).sendToTarget();
                        mHandler.obtainMessage(MainDisplayActivity.MESSAGE_GPS_ACCURACY, new Float(loc.getAccuracy())).sendToTarget();
                    }
                }
                mLastLocation = loc;
            }

            public void onProviderDisabled(String provider) {}

            public void onProviderEnabled(String provider) {}

        public void onStatusChanged(String provider, int status, Bundle extras) {
            //Log.v(TAG, "onStatusChanged()");
            Toast.makeText(getBaseContext(), "onStatusChanged()", Toast.LENGTH_SHORT).show();
            int state = STATE_LOCATING;
            switch (status) {
            case LocationProvider.AVAILABLE:
                state = STATE_AVAILABLE;
                break;
            case LocationProvider.OUT_OF_SERVICE:
                state = STATE_OUT_OF_SERVICE;
                break;
            case LocationProvider.TEMPORARILY_UNAVAILABLE:
                state = STATE_AVAILABLE;
                break;
            default:
                state = STATE_LOCATING;
                break;
            }
            if (mHandler != null) {
                mHandler.obtainMessage(MainDisplayActivity.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
            }
        }
    }

    public int turnOnGps() {
        Log.v(TAG, "turnOnGps()");
        mLocationListener  = new LocationUpdateHandler();

        /* make sure we have a gps */
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) == false) {
            return 0;
        }

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);
        if (mHandler != null) {
            mHandler.obtainMessage(MainDisplayActivity.MESSAGE_STATE_CHANGE, STATE_LOCATING, -1).sendToTarget();
        }
        return 1;
    }

    public int turnOffGps() {
        Log.v(TAG, "turnOffGps()");
        if (mLocationListener != null) {
            locationManager.removeUpdates(mLocationListener);
        }
        if (mHandler != null) {
            mHandler.obtainMessage(MainDisplayActivity.MESSAGE_STATE_CHANGE, STATE_OFF, -1).sendToTarget();
        }
        return 0;
    }
}
