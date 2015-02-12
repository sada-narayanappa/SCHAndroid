package org.geospaces.schas;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.os.SystemClock;
import android.os.Bundle;
import android.text.InputType;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.commonsware.cwac.locpoll.LocationPoller;

import org.geospaces.schas.utils.SCHASSettings;
import org.geospaces.schas.utils.db;

import java.io.File;

public class UploadData extends Activity {

    private static final int PERIOD = 6 * 1000 * 60;  // 2 min
    private PendingIntent pi = null;
    private AlarmManager mgr = null;
    private String PEF_Text;
    private String FEV_Text;

    TextView tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        setContentView(R.layout.activity_upload_data);

        findViewById(R.id.serviceButton).setOnClickListener(start_service_button);
        findViewById(R.id.PFMButton).setOnClickListener(pfm_button);
        findViewById(R.id.updateStatus).setOnClickListener(updateStatusCB);
        findViewById(R.id.uploadButton).setOnClickListener(uploadCB);
        findViewById(R.id.resetButton).setOnClickListener(resetCB);

        tv = (TextView) findViewById(R.id.statusText);
        tv.setMovementMethod(new ScrollingMovementMethod());
    }

    protected void updateStatus() {
        StringBuffer sb = new StringBuffer(256);
        File f;
        f = db.getFile(db.FILE_NAME);

        sb.append(f.getName() + " : size=" + f.length() + "\n");
        f = db.getFile(db.FILE_READY);
        sb.append(f.getName() + " : size=" + f.length() + "\n");

        if (SCHASSettings.host == null ) {
            SCHASSettings.Initialize(null);
        }
        sb.append("URL: " + SCHASSettings.host + "\n");

        String []ls = db.read(db.FILE_NAME).split("\n");

        sb.append("DATA:" + ls[ls.length-1] + " ...\n");
        sb.append("SETTINGS:" + SCHASSettings.getSettings() + " ...\n");
        sb.append("WIFI:" + db.isWIFIOn(this.getApplicationContext()) + " ...\n");

        tv.setText(sb.toString());
    }

    private View.OnClickListener updateStatusCB = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            updateStatus();
        }
    };

    private View.OnClickListener resetCB = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            SCHASSettings.host = null;
            SCHASSettings.Initialize( null);

            db.delete();
        }
    };

    private View.OnClickListener uploadCB = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            updateStatus();

            Context ctx = UploadData.this.getApplicationContext();
            boolean d = db.rename(false);
            boolean r = db.Post(UploadData.this, ctx, "/aura/webroot/loc.jsp");
            if ( !d || !r) {
                String msg = "Redo:" + SCHASSettings.host + " No file to upload";
                Toast.makeText(UploadData.this, msg , Toast.LENGTH_SHORT).show();
            }
        }
    };

    public void setIntent( Intent i) {
        String str = "SetResult: " + i.getStringExtra("result");
        String url = "SetResult: " + i.getStringExtra("url");
        tv.setText(str);
        if (!str.startsWith("ERROR")) {
            db.delete();
        }
    }

    private View.OnClickListener start_service_button = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Button b = ((Button) findViewById(R.id.serviceButton));

            if (pi == null) {
                mgr = (AlarmManager) getSystemService(ALARM_SERVICE);

                Intent i = new Intent(UploadData.this, LocationPoller.class);

                i.putExtra(LocationPoller.EXTRA_INTENT, new Intent(UploadData.this, GPSWakfulReciever.class));
                i.putExtra(LocationPoller.EXTRA_PROVIDER, LocationManager.GPS_PROVIDER);

                pi = PendingIntent.getBroadcast(UploadData.this, 0, i, PendingIntent.FLAG_CANCEL_CURRENT);

                mgr.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        SystemClock.elapsedRealtime(),
                        PERIOD,
                        pi);
                Toast.makeText(UploadData.this, "Location polling every 2 minutes begun", Toast.LENGTH_SHORT).show();

                b.setText("Stop Service");
                b.setBackgroundColor(0xffff0000);

            } else {
                mgr.cancel(pi);
                pi = null;
                b.setText("Start Service");
                b.setBackgroundColor(0xff00ff00);
                Toast.makeText(UploadData.this, "Location polling STOPPED", Toast.LENGTH_SHORT).show();
            }
        }
    };

    private View.OnClickListener pfm_button = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String PEF = "";
            String FEV = "";

            InputTextPopUpCreator("Record PEF/FEV");
        }
    };

    public void InputTextPopUpCreator(String Label) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(Label);

        final LinearLayout ll = new LinearLayout(this);
        final EditText input = new EditText(this);
        final EditText input2 = new EditText(this);

        input.setHint("PEF");
        input2.setHint("FEV");

        ll.addView(input);
        ll.addView(input2);

        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input2.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        builder.setView(ll);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                PEF_Text = input.getText().toString();
                FEV_Text = input2.getText().toString();

                writeFile(PEF_Text, FEV_Text);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    public void writeFile(String s1, String s2) {
        PEF_Text = "";
        FEV_Text = "";
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.welcome, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (!HandleMenu.onOptionsItemSelected(item, this)) {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }
}
