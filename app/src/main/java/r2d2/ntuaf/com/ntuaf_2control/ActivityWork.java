package r2d2.ntuaf.com.ntuaf_2control;

import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.HashMap;

public class ActivityWork extends AppCompatActivity implements NfcAdapter.CreateNdefMessageCallback {
    private Intent gpsService;
    private String act_id, character;
    NfcAdapter mNfcAdapter;
    TextView textView, textView2;
    final String TAG="NTUAF-WORK";
    private Intent intent;
    private GpsLogger gpsLogger = new GpsLogger();



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        socket

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
        character =intent.getStringExtra("character");
        textView2 = (TextView) findViewById(R.id.act_name);

        gpsService= new Intent(ActivityWork.this, gpsLogger.getClass())
                .putExtra("act_id", act_id)
                .putExtra("type", 1)
                .putExtra("character", character);
//        1 for gps 2 for battery only
        startService(gpsService);

    }

    @Override
    public NdefMessage createNdefMessage(NfcEvent event) {
        String text = null;
        try {
            text = generateJSON();

        } catch (JSONException e) {
            Log.i(TAG, "JSONError:"+e);
        }
        Log.i(TAG, "Sending Msg");
        NdefMessage msg = new NdefMessage(
                new NdefRecord[] { NdefRecord.createMime(
                        "text/plain", text.getBytes())
                });
        return msg;

    }
    private String generateJSON() throws JSONException {
        JSONObject selfdata = new JSONObject();
        selfdata.accumulate("identity", "NTUAF-R2D2-Mstream");
        selfdata.accumulate("act_id", act_id);
        selfdata.accumulate("character", character);
        Log.i(TAG,"JSON"+selfdata.toString());
        return selfdata.toString();
    }
    @Override
    public void onResume() {
        super.onResume();
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

    }

    @Override
    public void onNewIntent(Intent intent) {
        // onResume gets called after this to handle the intent
        Log.i(TAG, "new intent get msg"+act_id);
        try {
            processIntent(intent);
        } catch (JSONException e) {
            Log.i(TAG, "Error:NFC:"+e);
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
        NdefMessage msg = (NdefMessage) rawMsgs[0];

        Log.i(TAG, new String(msg.getRecords()[0].getPayload()));
        parseData(new String(msg.getRecords()[0].getPayload()));

    }
    void parseData(String data) throws JSONException {
        JSONObject JSONdata = new JSONObject(data);
        if (JSONdata.has("NTUAF-R2D2-Mstream")){
            String other_act_id = JSONdata.getString("act_id");
            String other_character = JSONdata.getString("character");
            if (act_id==other_act_id){
                Log.i(TAG, "start character exchange");

            }
        }
    }
}

