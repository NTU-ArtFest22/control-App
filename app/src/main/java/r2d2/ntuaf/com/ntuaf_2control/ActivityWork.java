package r2d2.ntuaf.com.ntuaf_2control;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class ActivityWork extends Activity implements NfcAdapter.CreateNdefMessageCallback {
    private Intent gpsService;
    private String act_id, character;
    NfcAdapter mNfcAdapter;
    TextView textView, textView2;
    final String TAG = "NTUAF-WORK";
    private Intent intent;
    private GpsLogger gpsLogger = new GpsLogger();

    private String[] class_name;
    private String act_name = "", self_class_name = "";
    private TextView txt_act_name, txt_class_name;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        List<String> class_array = new ArrayList<String>();
        class_array.add("角色尚未確定");
        class_array.add("農民");
        class_array.add("商人");
        class_array.add("僧侶");
        class_array.add("國王");
        class_name = new String[class_array.size()];
        class_array.toArray(class_name);


        setContentView(R.layout.activity_work);
        TextView textView = (TextView) findViewById(R.id.textView);
        // Check for available NFC Adapter
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null) {
            Toast.makeText(this, "NFC is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        // Register callback
        mNfcAdapter.setNdefPushMessageCallback(this, this);
//        get data from previous act
        intent = getIntent();
        act_id = intent.getStringExtra("act_id");
        character = intent.getStringExtra("character");
        txt_act_name = (TextView) findViewById(R.id.act_name);
        txt_class_name = (TextView) findViewById(R.id.txt_class);

        gpsService = new Intent(ActivityWork.this, gpsLogger.getClass())
                .putExtra("act_id", act_id)
                .putExtra("type", 1)
                .putExtra("character", character);
//        1 for gps 2 for battery only
        startService(gpsService);

        MessageHandler msghandler = new MessageHandler();
        gpsLogger.client.on("new_character_data", msghandler.onNewclass);


        gpsLogger.client.on("register_status", msghandler.onRegisterStatus);

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    @Override
    public NdefMessage createNdefMessage(NfcEvent event) {
        String text = null;
        try {
            text = generateJSON();

        } catch (JSONException e) {
            Log.i(TAG, "JSONError:" + e);
        }
        Log.i(TAG, "Sending Msg");
        NdefMessage msg = new NdefMessage(
                new NdefRecord[]{NdefRecord.createMime(
                        "text/plain", text.getBytes())
                });
        return msg;

    }

    private String generateJSON() throws JSONException {
        JSONObject selfdata = new JSONObject();
        selfdata.accumulate("identity", "NTUAF-R2D2-Mstream");
        selfdata.accumulate("act_id", act_id);
        selfdata.accumulate("character", character);
        Log.i(TAG, "JSON" + selfdata.toString());
        return selfdata.toString();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "=====onResume=====");


        NfcAdapter nfc = NfcAdapter.getDefaultAdapter(this);

        if (nfc != null && nfc.isEnabled()) {
            PendingIntent nfcIntent = PendingIntent.getActivity(this, 200,
                    new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

            nfc.enableForegroundDispatch(this, nfcIntent, null, null);


        } else {

        }
//        gpsLogger.client.emit("new_mission_server", "123");
    }

    @Override
    protected void onPause() {
        super.onPause();
        NfcAdapter nfc = NfcAdapter.getDefaultAdapter(this);
        if (nfc != null && nfc.isEnabled()) {
            nfc.disableForegroundDispatch(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(gpsService);
    }

    @Override
    public void onNewIntent(Intent intent) {
        // onResume gets called after this to handle the intent
        Log.i(TAG, "new intent get msg" + act_id);
        try {
            processIntent(intent);
            txtupdate();
        } catch (JSONException e) {
            Log.i(TAG, "Error:NFC:" + e);
        }
    }

    /**
     * Parses the NDEF Message from the intent and prints to the TextView
     */
    void processIntent(Intent intent) throws JSONException {
        textView = (TextView) findViewById(R.id.textView);
        Parcelable[] rawMsgs = intent.getParcelableArrayExtra(
                NfcAdapter.EXTRA_NDEF_MESSAGES);
        // only one message sent during the beam
        if (rawMsgs.length > 0) {
            NdefMessage msg = (NdefMessage) rawMsgs[0];

            Log.i(TAG, new String(msg.getRecords()[0].getPayload()));
            parseData(new String(msg.getRecords()[0].getPayload()));

        } else {
            Log.i(TAG, "No msg send");
        }

    }

    @Override
    public void onBackPressed() {

        stop_rtc();

    }

    @Override
    protected void onStart() {
        super.onStart();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Log.i(TAG, "=====OnStart=====");
        txtupdate();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "ActivityWork Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://r2d2.ntuaf.com.ntuaf_2control/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    private void stop_rtc() {
        new AlertDialog.Builder(ActivityWork.this)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setTitle("關閉遊戲")
                .setMessage("你確定要關閉嗎？")
                .setPositiveButton("是", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.i("NTUAF-webRTC", "HELLO");
//                        onDestroy();.finish();
                        stopService(gpsService);
                        ActivityWork.this.finish();
                        stopService(gpsService);
                        if (mNfcAdapter != null) {

                        }
                        finish();

                    }

                })
                .setNegativeButton("否", null)

                .show();
    }

    void parseData(String data) throws JSONException {
        JSONObject JSONdata = new JSONObject(data);
        if (JSONdata.has("identity")) {
            String other_act_id = JSONdata.getString("act_id");
            String other_character = JSONdata.getString("character");
            if (act_id == null) {
                finish();
                return;
            }
            if (act_id.equals(other_act_id)) {
                Log.i(TAG, "start character exchange");
                try {
                    askforexchange(other_character);
                } catch (JSONException e) {
                    Toast.makeText(ActivityWork.this, "發生錯誤！再試一次", Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "JSON error" + e);
                }
            } else {
//                cannot change
                Toast.makeText(ActivityWork.this, "不同活動啦!!!", Toast.LENGTH_SHORT).show();
                Log.i(TAG, "cannot change due to the different act");
            }
        }
    }

    void askforexchange(String other_character) throws JSONException {
        JSONObject exchange_data = new JSONObject();
        exchange_data.accumulate("act_id", act_id);
        exchange_data.accumulate("self_character", character);
        exchange_data.accumulate("other_character", other_character);
        Log.i(TAG, "Socket:JSON" + exchange_data.toString());
        gpsLogger.client.emit("exchange_request", exchange_data);

    }

    void askforupdate() throws JSONException {
        JSONObject info_array = new JSONObject();
        info_array.accumulate("act_id", act_id);
        info_array.accumulate("self_character", character);
        gpsLogger.client.emit("update_request", info_array);
        Log.i(TAG, "ask for update");

    }

    void txtupdate() {
        txt_class_name.setText(self_class_name);
        txt_act_name.setText(act_name);
    }

    Handler handler = new Handler() {

        public void handleMessage(Message msg) {
            switch (msg.what) {
                // 當收到的Message的代號為我們剛剛訂的代號就做下面的動作。
                case 1:
                    // 重繪UI
                    txtupdate();
                    break;
                case 2:
//                    toast

                    break;
                case 3:
                    try {
                        askforupdate();
                    } catch (JSONException e) {
                        Log.i(TAG, "Error while updating data:" + e);
                        Toast.makeText(ActivityWork.this, "連線錯誤!!!", Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
            super.handleMessage(msg);
        }

    };

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "ActivityWork Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://r2d2.ntuaf.com.ntuaf_2control/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }

    public class MessageHandler {

        private MessageHandler() {

        }

        private Emitter.Listener onNewclass = new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.i(TAG, "new class come！！！==========================");


                final String AF_GROUP = "group";
                final String AF_NAME = "name";
                final String AF_CLASS = "sclass";
                final String AF_CHARACTER = "character";


                try {

                    JSONObject data = (JSONObject) args[0];
                    JSONArray grouplist = data.getJSONArray(AF_GROUP);
                    for (int i = 0; i < grouplist.length(); i++) {
                        if (grouplist.getJSONObject(i).getString(AF_CHARACTER).equals(character)) {
                            Log.i(TAG, "character" + grouplist.getJSONObject(i).getString(AF_CHARACTER));
                            int class_num = Integer.valueOf(grouplist.getJSONObject(i).getString(AF_CLASS));
                            Log.i(TAG, "classnum:" + class_num + " " + class_name[class_num]);
                            if (class_num <= 4 && class_num >= 0) {
                                self_class_name = class_name[class_num];
                                act_name = data.getString(AF_NAME);
                                Message m = new Message();
                                // 定義 Message的代號，handler才知道這個號碼是不是自己該處理的。
                                m.what = 1;
                                handler.sendMessage(m);
                                Log.i(TAG, act_name);
                                break;
                            }

                        }
                    }
                    Log.i(TAG, "finish update ");
                } catch (JSONException e) {
                    Log.i(TAG, "JSONerror while update class:" + e);
                }

            }
        };

        private Emitter.Listener onRegisterStatus = new Emitter.Listener() {

            public void call(Object... args) {
                Log.i(TAG, "new status");
                if (args[0].equals("success")) {


                        Message m = new Message();
                        // 定義 Message的代號，handler才知道這個號碼是不是自己該處理的。
                        m.what = 3;
                        handler.sendMessage(m);

                }
            }
        };

    }
}

