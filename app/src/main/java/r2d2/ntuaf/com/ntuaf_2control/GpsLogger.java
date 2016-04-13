package r2d2.ntuaf.com.ntuaf_2control;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.facebook.Profile;

public class GpsLogger extends Service implements LocationListener {
    String TAG = "GPS-log";
    private String act_id;
    private String artistID;
    public GpsLogger() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        Log.i(TAG, "onstart");
        Profile profile = Profile.getCurrentProfile();
        act_id = intent.getStringExtra("act_id");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "ondestroy");

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onhello");


        return super.onStartCommand(intent, flags, startId);
    }




    @Override
    public void onLocationChanged(Location location) {
        Log.i(TAG, "Location: " + location.getLatitude() + ", " + location.getLongitude());

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.i(TAG, "status change");
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.i(TAG, "provider enabled");
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.i(TAG, "provider disabled");
    }
}
