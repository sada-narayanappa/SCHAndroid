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

import com.commonsware.cwac.locpoll.*;

import org.geospaces.schas.utils.SCHASSettings;
import org.geospaces.schas.utils.db;

public class Web extends Activity {
    WebView webView = null;
    private PendingIntent pendingIntent;
    private Context context;
    Intent alarmIntent;

    private static final int PERIOD = 2 * 1000 * 60;  // 2 min
    private PendingIntent pi = null;
    private AlarmManager mgr = null;
    public String mapurl;

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

        String host = SCHASSettings.host;
        mapurl = "http://"+host+ "/SCHAS/html/maps/Openlayers3.html?mobile_id=" + SCHASSettings.deviceID;
        host =   (host == null ) ? "http://www.google.com/maps" : mapurl;

        Log.w("****", host);
        webView.loadUrl(host);
        webView.getSettings().setJavaScriptEnabled(true);

        findViewById(R.id.mapButton).setOnClickListener(mapButtonCB);
        findViewById(R.id.homeButton).setOnClickListener(homeButtonCB);
        findViewById(R.id.graphButton).setOnClickListener(graphButtonCB);
    }

    private View.OnClickListener mapButtonCB = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String host     = mapurl;
            Log.e("Web", ""+ host);
            webView.loadUrl(host);
            webView.getSettings().setJavaScriptEnabled(true);
        }
    };
    private View.OnClickListener homeButtonCB = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String host     = "http://" + SCHASSettings.host + "/geodata/";
            Log.e("Web", ""+ host);
            webView.loadUrl(host);
            webView.getSettings().setJavaScriptEnabled(true);
        }
    };
    private View.OnClickListener graphButtonCB = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String host     = mapurl;
            Log.e("Web", ""+ host);
            webView.loadUrl(host);
            webView.getSettings().setJavaScriptEnabled(true);

        }
    };

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
