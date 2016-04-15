package r2d2.ntuaf.com.ntuaf_2control;

import android.Manifest;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import com.facebook.Profile;
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class GpsLogger extends Service{
    String TAG = "NTU-GPS-log";
    private String act_id;
    private String artistID;


    private Handler handler = new Handler();
    private LocationManager locMgr;
    private MyLocationlistener locMgrListener;
    String x, y, tempx, tempy;
    int level;

    public GpsLogger() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
    @Override
    public void onCreate() {
        super.onCreate();
        locMgr = (LocationManager) getSystemService(LOCATION_SERVICE);
        locMgrListener = new MyLocationlistener();
        Log.d(TAG, "onCreate() executed");


    }
    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        Log.i(TAG, "onstart");


        handler.postDelayed(showTime, 2000);

        this.registerReceiver(this.batteryInfoReceiver,	new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "ondestroy");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onhello");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "loss permission");
            Toast.makeText(GpsLogger.this, "缺少定位權限", Toast.LENGTH_SHORT).show();
        }
        Profile profile = Profile.getCurrentProfile();
        act_id = intent.getStringExtra("act_id");
        artistID = profile.getId();
        locMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 0, locMgrListener);


        return super.onStartCommand(intent, flags, startId);
    }

    private BroadcastReceiver batteryInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            level= intent.getIntExtra(BatteryManager.EXTRA_LEVEL,0);

        }
    };

    private Runnable showTime = new Runnable() {
        public void run() {
//log目前時間

            if (tempx!=x || tempy!=y){
                tempx = x;
                tempy = y;
                GPSlogTask task = new GPSlogTask();
                task.execute(act_id, artistID, tempx, tempy, Integer.toString(level));

                Log.i(TAG, new Date().toString()+": "+x+"and"+y+" Battery:"+level);
            }
            handler.postDelayed(this, 2000);
        }
    };


    public class MyLocationlistener implements LocationListener{
        @Override
        public void onLocationChanged(Location location) {
            x = Double.toString(location.getLongitude());
            y = Double.toString(location.getLatitude());
        }

        @Override
        public void onProviderDisabled(String provider) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }
    }


    public class GPSlogTask extends AsyncTask<String, Void, Void> {
        private OkHttpClient client = new OkHttpClient();
        @Override
        protected Void doInBackground(String... params) {
//            Log.i(TAG, "start async task");
            String result;
            try {

                result = run(getString(R.string.server_location) + getString(R.string.api_gps_log) + params[0]+"/"+params[1]+"/"+params[2]+"/"+params[3]+"/"+params[4]);
//                Log.i(TAG, "result:"+result);
            } catch (IOException e) {
                Log.e(TAG, "Error(internet):" + e);
                return null;
            }
            return null;
        }

        private String run(String url) throws IOException {
            Request request = new Request.Builder()
                    .url(url)
                    .build();

            Response response = client.newCall(request).execute();
            return response.body().string();
        }

    }
}
