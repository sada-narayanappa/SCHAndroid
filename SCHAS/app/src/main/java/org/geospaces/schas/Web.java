package org.geospaces.schas;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.Toast;

import com.commonsware.cwac.locpoll.*;

public class Web extends Activity {
    WebView webView = null;
    private PendingIntent pendingIntent;
    private Context context;
    Intent alarmIntent;

    private static final int PERIOD = 2 * 1000 * 60;  // 2 min
    private PendingIntent pi = null;
    private AlarmManager mgr = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);

        this.context = this;
        alarmIntent = new Intent(Web.this, LocationPoller.class);
        pendingIntent = PendingIntent.getBroadcast(Web.this, 0, alarmIntent, 0);
        webView = (WebView) findViewById(R.id.webView1);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webView.setInitialScale(1);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        webView.setScrollbarFadingEnabled(false);

        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl("http://www.google.com/maps");

        final Button button = (Button) findViewById(R.id.readButton);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.e("Web", "calling geospaces.org");
                webView.loadUrl("http://www.geospaces.org/");
            }
        });

        final Button button2 = (Button) findViewById(R.id.serviceButton);
        button2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //Toast.makeText(getApplicationContext(), "step 1", Toast.LENGTH_SHORT).show();
                //AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                //manager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 2000, 5000, pendingIntent);
                //Toast.makeText(Web.this, "Alarm Set", Toast.LENGTH_SHORT).show();
            }
        });

        final Button button3 = (Button) findViewById(R.id.uploadButton);
        button3.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                /*AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                manager.cancel(pendingIntent);
                Toast.makeText(Web.this, "Alarm Canceled", Toast.LENGTH_SHORT).show();*/
                mgr = (AlarmManager) getSystemService(ALARM_SERVICE);

                Intent i = new Intent(Web.this, LocationPoller.class);

                i.putExtra(LocationPoller.EXTRA_INTENT, new Intent(Web.this, GPSWakfulReciever.class));
                i.putExtra(LocationPoller.EXTRA_PROVIDER, LocationManager.GPS_PROVIDER);

                pi = PendingIntent.getBroadcast(Web.this, 0, i, PendingIntent.FLAG_CANCEL_CURRENT);

                mgr.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        SystemClock.elapsedRealtime(),
                        PERIOD,
                        pi);
                Toast.makeText(Web.this, "Location polling every 2 minutes begun", Toast.LENGTH_SHORT).show();
            }
        });
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
