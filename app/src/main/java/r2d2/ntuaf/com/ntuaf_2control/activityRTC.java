package r2d2.ntuaf.com.ntuaf_2control;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Point;
import android.opengl.EGLContext;
import android.opengl.GLSurfaceView;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.Toast;

import com.facebook.Profile;

import org.json.JSONArray;
import org.json.JSONException;
import org.webrtc.MediaStream;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;
import fr.pchab.webrtcclient.WebRtcClient;
import fr.pchab.webrtcclient.PeerConnectionParameters;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class activityRTC extends Activity implements WebRtcClient.RtcListener {
    private final static int VIDEO_CALL_SENT = 666;
    private static final String VIDEO_CODEC_VP9 = "VP9";
    private static final String AUDIO_CODEC_OPUS = "opus";
    // Local preview screen position before call is connected.
    private static final int LOCAL_X_CONNECTING = 0;
    private static final int LOCAL_Y_CONNECTING = 0;
    private static final int LOCAL_WIDTH_CONNECTING = 100;
    private static final int LOCAL_HEIGHT_CONNECTING = 100;
    // Local preview screen position after call is connected.
    private static final int LOCAL_X_CONNECTED = 72;
    private static final int LOCAL_Y_CONNECTED = 72;
    private static final int LOCAL_WIDTH_CONNECTED = 25;
    private static final int LOCAL_HEIGHT_CONNECTED = 25;
    // Remote video screen position
    private static final int REMOTE_X = 0;
    private static final int REMOTE_Y = 0;
    private static final int REMOTE_WIDTH = 100;
    private static final int REMOTE_HEIGHT = 100;
    private VideoRendererGui.ScalingType scalingType = VideoRendererGui.ScalingType.SCALE_ASPECT_FILL;
    private GLSurfaceView vsv;
    private VideoRenderer.Callbacks localRender;
    private VideoRenderer.Callbacks remoteRender;
    private WebRtcClient client;
    private String mSocketAddress;
    private String callerId;

    private boolean mirror = false;

    private String TAG = "NTUAF-RTC";
    private String act_id = null;
    private String call_id = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "oncreate");

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(
                LayoutParams.FLAG_FULLSCREEN
                        | LayoutParams.FLAG_KEEP_SCREEN_ON
                        | LayoutParams.FLAG_DISMISS_KEYGUARD
                        | LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | LayoutParams.FLAG_TURN_SCREEN_ON);
        setContentView(R.layout.activity_rtc);
        mSocketAddress = getResources().getString(R.string.server_location);
        mSocketAddress += (":" + getResources().getString(R.string.port) + "/");

        vsv = (GLSurfaceView) findViewById(R.id.glview_call);
        vsv.setPreserveEGLContextOnPause(true);
        vsv.setKeepScreenOn(true);
        VideoRendererGui.setView(vsv, new Runnable() {
            @Override
            public void run() {
                init();
            }
        });

        // local and remote render
        remoteRender = VideoRendererGui.create(
                REMOTE_X, REMOTE_Y,
                REMOTE_WIDTH, REMOTE_HEIGHT, scalingType, false);
        localRender = VideoRendererGui.create(
                LOCAL_X_CONNECTING, LOCAL_Y_CONNECTING,
                LOCAL_WIDTH_CONNECTING, LOCAL_HEIGHT_CONNECTING, scalingType, true);

        final Intent intent = getIntent();
        final String action = intent.getAction();
        if (Intent.ACTION_VIEW.equals(action)) {
            final List<String> segments = intent.getData().getPathSegments();
            callerId = segments.get(0);
            Log.i(TAG, "caller_id:"+callerId);
        }


        act_id = intent.getStringExtra("act_id");
        if (act_id==null){
            this.finish();

            Log.i(TAG, "Act_id is null");
        }


        final Button btn_stop = (Button) findViewById(R.id.btn_stop_rtc);
        btn_stop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.i(TAG, "user press stop");
                stop_rtc();
                // Perform action on click
            }
        });
        final Button btn_share = (Button) findViewById(R.id.btn_fb_share);
        btn_share.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                sendmsg();
                Log.i(TAG, "user press share");
                // Perform action on click
            }
        });
    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        stop_rtc();
    }
    private void stop_rtc(){
        new AlertDialog.Builder(activityRTC.this)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setTitle("關閉遊戲")
                .setMessage("你確定要關閉嗎？")
                .setPositiveButton("是", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.i("NTUAF-webRTC", "HELLO");
//                        onDestroy();.finish();
                        activityRTC.this.finish();
                    }

                })
                .setNegativeButton("否", null)
                .show();
    }


    private void init() {
        Point displaySize = new Point();
        getWindowManager().getDefaultDisplay().getSize(displaySize);
        PeerConnectionParameters params = new PeerConnectionParameters(
                true, false, displaySize.x, displaySize.y, 30, 1, VIDEO_CODEC_VP9, true, 1, AUDIO_CODEC_OPUS, true);
        client = new WebRtcClient(this, mSocketAddress, params, VideoRendererGui.getEGLContext());
        Log.i(TAG, "msocketAddress: " + mSocketAddress + ", params: " + params);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i(TAG, "onpause");
        vsv.onPause();
        if(client != null) {
            client.onPause();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "onresume");
        vsv.onResume();

        if(client != null) {
            client.onResume();
        }
    }

    @Override
    public void onDestroy() {
        if(client != null) {
            client.onDestroy();
            Log.i(TAG, "rtc stop");
        }
        super.onDestroy();

    }

    @Override
    public void onCallReady(String callId) {
        Log.i(TAG, "oncallready");
        if (callerId != null) {
            try {
                answer(callerId);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            call(callId);
        }
    }

    public void answer(String callerId) throws JSONException {
        Log.i(TAG, "answer");
        client.sendMessage(callerId, "init", null);
        startCam();
    }

    public void call(String callId) {
        Log.i(TAG, "call");
        
        UpdateCallidTask task = new UpdateCallidTask();
        Profile profile = Profile.getCurrentProfile();
        call_id = callId;
        task.execute(profile.getId(), act_id, callId);
//        Toast.makeText(this, "開啟相機", Toast.LENGTH_LONG).show();
        startCam();
    }
    
    public void sendmsg(){
        if (call_id!=null){
            Intent msg = new Intent(Intent.ACTION_SEND);
            msg.putExtra(Intent.EXTRA_TEXT, mSocketAddress + call_id);
            msg.setType("text/plain");
            startActivityForResult(Intent.createChooser(msg, "Call someone :"), VIDEO_CALL_SENT);

        }else{
            Toast.makeText(this, "等待伺服器回應", Toast.LENGTH_SHORT).show();
        }
            
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "onactresult");
//        if (requestCode == VIDEO_CALL_SENT) {
//            startCam();
//        }
    }

    public void startCam() {
        Log.i(TAG, "start cam");
        // Camera settings
        Profile profile = Profile.getCurrentProfile();
        if (profile!=null){
            Log.i(TAG, "start capturing");
            client.start(profile.getId());
        }else{
            Log.i(TAG, "No user data");
            this.finish();
        }
//        client.start("android_test123");


    }

    @Override
    public void onStatusChanged(final String newStatus) {
        Log.i(TAG, "onstatuschange");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), newStatus, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onLocalStream(MediaStream localStream) {
        Log.i(TAG, "onlocalstream");
        localStream.videoTracks.get(0).addRenderer(new VideoRenderer(localRender));


        //the less param is to mirror or not
        VideoRendererGui.update(localRender, LOCAL_X_CONNECTING, LOCAL_Y_CONNECTING, LOCAL_WIDTH_CONNECTING, LOCAL_HEIGHT_CONNECTING, scalingType);

    }

    @Override
    public void onAddRemoteStream(MediaStream remoteStream, int endPoint) {
        Log.i(TAG, "onaddremotestream");
        remoteStream.videoTracks.get(0).addRenderer(new VideoRenderer(remoteRender));
        VideoRendererGui.update(remoteRender,
                REMOTE_X, REMOTE_Y,
                REMOTE_WIDTH, REMOTE_HEIGHT, scalingType);
        VideoRendererGui.update(localRender,
                LOCAL_X_CONNECTED, LOCAL_Y_CONNECTED,
                LOCAL_WIDTH_CONNECTED, LOCAL_HEIGHT_CONNECTED,
                scalingType);
    }

    @Override
    public void onRemoveRemoteStream(int endPoint) {
        Log.i(TAG, "onremoveremotestream");
        VideoRendererGui.update(localRender,
                LOCAL_X_CONNECTING, LOCAL_Y_CONNECTING,
                LOCAL_WIDTH_CONNECTING, LOCAL_HEIGHT_CONNECTING,
                scalingType);
    }

    public class UpdateCallidTask extends AsyncTask<String, Void, String[]> {

        private OkHttpClient client = new OkHttpClient();
        private String ac;


        /**
         * Take the String representing the complete forecast in JSON Format and
         * pull out the data we need to construct the Strings needed for the wireframes.
         * <p/>
         * Fortunately parsing is easy:  constructor takes the JSON string and converts it
         * into an Object hierarchy for us.
         */

        @Override
        protected String[] doInBackground(String... params) {
            Log.i("NTUAF_RTC", "start async task");

            String result;
            try {
                Log.i("NTUAF_ACT", "id from fb:" + params[0]);
                result = run(getString(R.string.server_location) + getString(R.string.api_upload_stream) + params[0]+"/"+params[1]+"/"+params[2]);
                Log.i("NTUAF_ACT", "result:"+result);
            } catch (IOException e) {
                Log.e("NTUAF_ACT", "Error(internet):" + e);
                return null;
            }

            return null;

//            try{
//                if (result!=null){
//                    String[] strArray = getuserDataFromJson(result);
//                    return strArray;
//                }else{
//                    return null;
//                }
//
//            }catch (JSONException e){
//                Log.e("NTUAF_ACT", "Error(JSON):" + e);
//                return null;
//            }
        }

        private String run(String url) throws IOException {
            Request request = new Request.Builder()
                    .url(url)
                    .build();

            Response response = client.newCall(request).execute();
            return response.body().string();
        }

        private String[] getuserDataFromJson(String JsonStr)
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

        @Override

        protected void onPostExecute(String[] result) {
            Log.i(TAG, "async finished");
//            if (result != null) {
//                mActAdapter.clear();
//                for(String dayForecastStr : result) {
//                    mActAdapter.add(dayForecastStr);
//                }
//            }
        }
    }
}