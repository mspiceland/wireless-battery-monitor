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

package com.fuzzymonkey.wirelessbatterymonitor;

import com.fuzzymonkey.wirelessbatterymonitor.activity.MainDisplayActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class WirelessBatteryMonitor extends Activity implements OnClickListener {
    private static final String TAG = "FUZZYMONKEY";

    /**
      * Called when the activity is first created.
      */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Button start_button = (Button) findViewById(R.id.start_button);
        start_button.setOnClickListener(this);
    }

    public void onClick(View v) {
        if (v.getId() == R.id.start_button) {
                Intent myIntent = new Intent(this, MainDisplayActivity.class);
                myIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(myIntent);
        } else {
                Log.v(TAG, "clicked unknown ID");
        }
    }
}
