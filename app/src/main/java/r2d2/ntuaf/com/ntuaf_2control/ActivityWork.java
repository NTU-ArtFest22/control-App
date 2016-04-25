package r2d2.ntuaf.com.ntuaf_2control;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class ActivityWork extends AppCompatActivity {
    private Intent gpsService;
    private String act_id;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_work);
        Intent intent = getIntent();
        act_id = intent.getStringExtra("act_id");
        gpsService= new Intent(ActivityWork.this, GpsLogger.class)
                .putExtra("act_id", act_id)
                .putExtra("type", 1);
//        1 for gps 2 for battery only
        startService(gpsService);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(gpsService);
    }
}
