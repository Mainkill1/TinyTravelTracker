/** 
    Copyright 2015 Tim Engler, Rareventure LLC

    This file is part of Tiny Travel Tracker.

    Tiny Travel Tracker is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Tiny Travel Tracker is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Tiny Travel Tracker.  If not, see <http://www.gnu.org/licenses/>.

*/
package com.rareventure.gps2.bootup;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.rareventure.gps2.GTG;
import com.rareventure.gps2.GpsTrailerService;

public class GpsTrailerReceiver extends BroadcastReceiver
{
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d("GTG", "GpsTrailerReceiver called");

		// Boot may create a fresh process. Once preferences are loaded, preserve
		// live UI changes that may still be waiting for their background save.
		if (!GTG.Requirement.PREFS_LOADED.isFulfilled())
			GTG.prefSet.loadAndroidPreferencesFromSharedPrefs(context);
		if (!GTG.prefs.isCollectData)
			return;

		ContextCompat.startForegroundService(context, new Intent(context,
				GpsTrailerService.class));
	}
}
