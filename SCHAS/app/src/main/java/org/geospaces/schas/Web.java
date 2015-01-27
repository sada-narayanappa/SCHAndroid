package org.geospaces.schas;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.Toast;


public class Web extends Activity {

    WebView webView = null;
    private PendingIntent pendingIntent;
    private Context context;
    Intent alarmIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);

        this.context = this;


        alarmIntent = new Intent(Web.this, GPSWakfulReciever.class);
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

        final Button button = (Button) findViewById(R.id.button1);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.e("Web", "calling geospaces.org");
                webView.loadUrl("https://www.google.com/");
            }
        });

        final Button button2 = (Button) findViewById(R.id.CollectData);
        button2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
               start();
            }
        });

        final Button button3 = (Button) findViewById(R.id.WS);
        button3.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                manager.cancel(pendingIntent);
                Toast.makeText(Web.this, "Alarm Canceled", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void start()
    {
        //Toast.makeText(getApplicationContext(), "step 1", Toast.LENGTH_SHORT).show();
        AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        int interval = 8000;
            manager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 2000, 1000*30, pendingIntent);
            Toast.makeText(Web.this, "Alarm Set", Toast.LENGTH_SHORT).show();

            //startService(new Intent(Web.this, GPService.class));

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.welcome, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if ( !HandleMenu.onOptionsItemSelected(item, this)) {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }
}
