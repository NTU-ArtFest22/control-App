package r2d2.ntuaf.com.ntuaf_2control;

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


    private Socket client;
    {
        String host = "https://ntuaf.ddns.net";
        try {
            client = IO.socket(host);
        } catch (URISyntaxException e) {
            Log.i(TAG, "no socket connection");
            finish();
        }
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        socket
        client.connect();
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

        intent = getIntent();

        act_id = intent.getStringExtra("act_id");
        character =intent.getStringExtra("character");
        textView2 = (TextView) findViewById(R.id.act_name);
        textView2.setText(act_id);


//        id register
        try{
            JSONObject info = new JSONObject();
            info.put("act_id", act_id);
            info.put("character", character);
            info.put("type", 3);
            client.emit("register_client_id", info);
            Log.i(TAG, "emit register_client_id");
        }catch (JSONException e){
            Log.i(TAG, "cannot create register info array");
            finish();
        }

        MessageHandler msghandler = new MessageHandler();
        client.on("register_status", msghandler.onRegisterStatus);

    }
    private class MessageHandler {

        private MessageHandler() {

        }

        private Emitter.Listener onRegisterStatus = new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                if (args[0].equals("success")){
                    Log.i(TAG, "regist successfully");
                }else{
                    Log.i(TAG, "regist failed");
                    finish();
                }
            }
        };

    }
    @Override
    public NdefMessage createNdefMessage(NfcEvent event) {
        String text = ("Beam me up, Android!\n\n" +
                "Beam Time: " + System.currentTimeMillis());
        NdefMessage msg = new NdefMessage(
                new NdefRecord[] { NdefRecord.createMime(
                        "application/r2d2.ntuaf.com.ntuaf_2control", text.getBytes())
                        /**
                         * The Android Application Record (AAR) is commented out. When a device
                         * receives a push with an AAR in it, the application specified in the AAR
                         * is guaranteed to run. The AAR overrides the tag dispatch system.
                         * You can add it back in to guarantee that this
                         * activity starts when receiving a beamed message. For now, this code
                         * uses the tag dispatch system.
                        */
                        //,NdefRecord.createApplicationRecord("com.example.android.beam")
                });
        return msg;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Check to see that the Activity started due to an Android Beam
        Log.i(TAG, "msg:"+NfcAdapter.ACTION_NDEF_DISCOVERED);
        Log.i(TAG, NfcAdapter.EXTRA_NDEF_MESSAGES);
        Log.i(TAG, "action"+getIntent().getAction());
        act_id = intent.getStringExtra("act_id");
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
            Log.i(TAG, "get msg"+act_id);

            processIntent(getIntent());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        client.disconnect();
    }

    @Override
    public void onNewIntent(Intent intent) {
        // onResume gets called after this to handle the intent
        Log.i(TAG, "new intent get msg"+act_id);
        processIntent(intent);
    }

    /**
     * Parses the NDEF Message from the intent and prints to the TextView
     */
    void processIntent(Intent intent) {
        textView = (TextView) findViewById(R.id.textView);
        Parcelable[] rawMsgs = intent.getParcelableArrayExtra(
                NfcAdapter.EXTRA_NDEF_MESSAGES);
        // only one message sent during the beam
        NdefMessage msg = (NdefMessage) rawMsgs[0];
        // record 0 contains the MIME type, record 1 is the AAR, if present
        textView.setText(new String(msg.getRecords()[0].getPayload()));
    }
}

