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
package com.rareventure.android;

import java.io.DataOutputStream;
import java.io.IOException;

import com.rareventure.android.AndroidPreferenceSet.AndroidPreferences;
import com.rareventure.gps2.GTG;
import com.rareventure.gps2.GTG.GTGEvent;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import com.rareventure.util.DebugLogFile;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import androidx.core.app.ActivityCompat;

public class GpsReader implements DataReader
{
	private Object lock = new Object();
	private GpsProcessor gpsProcessor;
	private GpsDataBuffer gpsDataBuffer;
	
	private ProcessThread processThread;
	
	private LocationListener locationListener = new LocationListener() {

		@Override
		public void onLocationChanged(Location location) {
			lastFixTime = location.getTime();
			Log.i(tag, "GPS fix received: time=" + lastFixTime);
			synchronized(processThread.lock) {
				int index = gpsDataBuffer.rawReadIndex;
				gpsDataBuffer.lat[index] = location.getLatitude();
				gpsDataBuffer.lon[index] = location.getLongitude();
				gpsDataBuffer.alt[index] = location.getAltitude();
				gpsDataBuffer.timeRead[index] = location.getTime();
				gpsDataBuffer.updateReadIndex();
				processThread.lock.notifyAll();
			}
		}

		@Override
		public void onProviderDisabled(String provider) {
//			Log.d(GpsTrailerService.TAG,"GPS provider disabled: "+provider);
			GTG.alert( GTG.GTGEvent.ERROR_GPS_DISABLED);
		}

		@Override
		public void onProviderEnabled(String provider) {
//			Log.d(GpsTrailerService.TAG,"GPS provider enabled: "+provider);
			GTG.alert( GTG.GTGEvent.ERROR_GPS_DISABLED, false);
		}

		@Override
		public void onStatusChanged(String provider, int status,
				Bundle extras) {
		}
	};

	private LocationManager lm;
	
	private boolean gpsOn;
	private static volatile boolean providerRegistered;
	public static boolean isProviderRegistered() { return providerRegistered; }
	private static volatile long lastFixTime;
	private static volatile String registrationState = "GPS not registered";

	/** A running service alone is not evidence of a GPS fix. No coordinates are logged. */
	public static String getDiagnosticStatus() {
		return registrationState + (lastFixTime == 0 ? "; no GPS fix received this process"
				: "; last GPS fix " + new java.util.Date(lastFixTime));
	}

	private void reportState(String state) {
		registrationState = state;
		Log.i(tag, getDiagnosticStatus());
		DebugLogFile.log(getDiagnosticStatus());
	}
	
	private Looper looper;
	private String tag;
	private DataOutputStream os;
	private Context ctx;
	
	public interface GpsProcessor {

		void processGpsData(double lon, double lat, double alt, long time);
	}

    /** Explicit provider selection shared by recording and the live map marker. */
    public static void requestGpsUpdates(LocationManager manager, long interval,
            LocationListener listener, Looper callbackLooper) {
        manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, interval, 0, listener, callbackLooper);
    }

    public GpsReader(DataOutputStream os, Context ctx, 
    		GpsProcessor gpsProcessor, 
    		String tag, Looper looper)
    {
    	this.ctx = ctx;
    	this.os = os;
    	this.tag = tag;
    	this.gpsProcessor = gpsProcessor;
    	this.gpsDataBuffer = new GpsDataBuffer(16);
    	
    	this.looper = looper;
    	
    	//TODO 3.2: handle multile levels of accuracy
    	//basically read from every gps system available
        lm = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
        
    }
    
    
    @Override
    public void setProcessThread(ProcessThread processThread)
    {
    	this.processThread = processThread;
    }
    

	@Override
	public boolean canProcess() {
		synchronized(processThread.lock) {
			return gpsDataBuffer.rawProcessIndex != gpsDataBuffer.rawReadIndex;
		}
	}

	@Override
	public void process() {
		final double lon, lat, alt;
		final long time;
		synchronized(processThread.lock) {
			if (!canProcess()) return;
			int index = gpsDataBuffer.rawProcessIndex;
			lon = gpsDataBuffer.lon[index];
			lat = gpsDataBuffer.lat[index];
			alt = gpsDataBuffer.alt[index];
			time = gpsDataBuffer.timeRead[index];
			gpsDataBuffer.updateProcessIndex();
		}
		// Database writes must not block callbacks or retain a reusable buffer slot.
		gpsProcessor.processGpsData(lon, lat, alt, time);
		if (os != null) writeTestData(lon, lat, alt, time);
	}

	@Override
	public void notifyShutdown() {
		turnOff();
	}

	private void writeTestData(double lon, double lat, double alt, long time) {
		synchronized(TestUtil.class)
		{
			try {
				TestUtil.writeMode(os, WriteConstants.MODE_WRITE_GPS_DATA2);
		
				TestUtil.writeDouble("lat",os, lat);
				TestUtil.writeDouble("lon",os, lon);
				TestUtil.writeDouble("alt",os, alt);
				TestUtil.writeLong("time",os, time);
			}
			catch(IOException e)
			{
				e.printStackTrace();
				throw new IllegalStateException(e); //punt but not a todo because this is test code
			}
		}        
	}

	public void turn(boolean on) {
		if (on) {
			turnOn();
		} else {
			turnOff();
		}
	}

	public void turnOn() {
    	synchronized(lock)
    	{
//    		Log.d(GpsTrailerService.TAG,"Turning on gps");
			if(!lm.isProviderEnabled( LocationManager.GPS_PROVIDER))
			{
				reportState("GPS provider disabled");
				GTG.alert( GTGEvent.ERROR_GPS_DISABLED);
				return;
			}
			if(ActivityCompat.checkSelfPermission(this.ctx, Manifest.permission.ACCESS_FINE_LOCATION)
					!= PackageManager.PERMISSION_GRANTED)
			{
				reportState("Precise location permission missing");
				GTG.alert( GTGEvent.ERROR_GPS_NO_PERMISSION);
//				Log.d(GpsTrailerService.TAG,"Failed no permission");
				return;
			}

    		if(gpsOn) {
//				Log.d(GpsTrailerService.TAG,"Gps already on");
				return; //already on
			}
            try {
                // Criteria may select fused/network (or null). The collector explicitly needs GPS.
                requestGpsUpdates(lm, prefs.gpsRecurringTimeMs, locationListener, looper);
                gpsOn = true;
                providerRegistered = true;
                reportState("GPS provider registered; waiting for updates");
            } catch (SecurityException e) {
                gpsOn = false;
                providerRegistered = false;
                reportState("GPS registration denied: " + e.getClass().getSimpleName());
                GTG.alert(GTGEvent.ERROR_GPS_NO_PERMISSION);
            } catch (IllegalArgumentException e) {
                gpsOn = false;
                providerRegistered = false;
                reportState("GPS provider unavailable: " + e.getClass().getSimpleName());
                GTG.alert(GTGEvent.ERROR_GPS_DISABLED);
            }
    	}
	}
	
	public void turnOff() {
    	synchronized(lock)
    	{
//			Log.d(GpsTrailerService.TAG,"Turning off gps");
    		if(!gpsOn)
    			return; //already off
			gpsOn = false;
			providerRegistered = false;
			reportState("GPS paused by recording schedule");
//			Log.d(GpsTrailerService.TAG,"Really offing gps");
            try {
                lm.removeUpdates(locationListener);
            } catch (SecurityException e) {
                reportState("GPS stopped after permission change");
            }
    	}
    	
    	//TODO x1: HACK ADDDS FAKE GPS LOCATION DATA
//    	Location l = new Location("test");
//    	l.setAltitude(9999);
//    	l.setLatitude(1);
//    	l.setLongitude(1);
//    	l.setBearing(0);
//    	this.locationListener.onLocationChanged(l);
	}
	
	public Preferences prefs = new Preferences();
	
	public static class Preferences implements AndroidPreferences
	{


		/**
		 * The time to set the internal android recurring timer for gps measurements.
		 */
		public long gpsRecurringTimeMs = 1000 * 60;
	}

	public boolean isGpsOn() {
		synchronized(lock)
		{
			return gpsOn;
		}
	}

}
