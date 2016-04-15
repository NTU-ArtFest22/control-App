package r2d2.ntuaf.com.ntuaf_2control;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

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
import java.util.HashMap;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class GpsLogger extends Service implements LocationListener {
    String TAG = "GPS-log";
    private String act_id;
    private String artistID;
    private Socket client;
    private String host;
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
        artistID = profile.getId();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "ondestroy");

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onhello");
        host = getResources().getString(R.string.server_location);


        return super.onStartCommand(intent, flags, startId);
    }




    @Override
    public void onLocationChanged(Location location) {
        Log.i(TAG, "Location: " + location.getLatitude() + ", " + location.getLongitude());
        GPSlogTask task = new GPSlogTask();
        task.execute(act_id, artistID, String.valueOf(location.getLatitude()), String.valueOf(location.getLongitude()));
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



    public class GPSlogTask extends AsyncTask<String, Void, String[]> {
        private OkHttpClient client = new OkHttpClient();
        @Override
        protected String[] doInBackground(String... params) {
            Log.i("NTUAF_GPS", "start async task");
            String result;
            try {
                Log.i("NTUAF_GPS", "id from fb:" + params[0]);
                result = run(getString(R.string.server_location) + getString(R.string.api_gps_log) + params[0]+"/"+params[1]+"/"+params[2]+"/"+params[3]);
                Log.i("NTUAF_GPS", "result:"+result);
            } catch (IOException e) {
                Log.e("NTUAF_GPS", "Error(internet):" + e);
                return null;
            }
            try{
                return getDataFromJSON(result);
            }catch (JSONException e){
                Log.e("NTUAF_GPS", "Error(JSON):" + e);
                return null;
            }

        }

        private String run(String url) throws IOException {
            Request request = new Request.Builder()
                    .url(url)
                    .build();

            Response response = client.newCall(request).execute();
            return response.body().string();
        }

        private String[] getDataFromJSON(String JsonStr)
                throws JSONException {

            // These are the names of the JSON objects that need to be extracted.
            final String AF_LIST = "list";
            final String AF_ACT = "fb";
            final String AF_ID = "id";
            final String AF_GAME_NAME = "gameName";



            JSONArray act_list = new JSONArray(JsonStr);
            String[] result = new String[act_list.length()];
            List<String> act_id = new ArrayList<String>();

            return result;

        }

        protected void onPostExecute(String result) {
            Log.i(TAG, "async finished");
            if (result!=null){
                Log.i("NTUAF-GPS", "GPS log successfully");
            }
        }
    }
}
