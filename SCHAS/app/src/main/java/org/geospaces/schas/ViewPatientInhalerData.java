package org.geospaces.schas;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.DataPointInterface;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.OnDataPointTapListener;
import com.jjoe64.graphview.series.Series;

import org.geospaces.schas.Services.StepCounter;
import org.geospaces.schas.utils.db;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class ViewPatientInhalerData extends AppCompatActivity {

    private ShareActionProvider mActionProvider;
    public GraphView inhalerDataGraph;
    public Context mContext;
    public String jsonString;
    LineGraphSeries<DataPoint> series;
    DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
    DateFormat onClickFormat = new SimpleDateFormat("MM/dd/yyyy");
    public Calendar calendar;

    StepCounter mService;
    boolean mBound;

    public Button startButton;
    public Button stopButton;
    public TextView stepsText;
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            StepCounter.LocalBinder binder = (StepCounter.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_patient_inhaler_data);
        mContext = this;

        inhalerDataGraph = (GraphView) findViewById(R.id.inhalerDataGraph);

        calendar = Calendar.getInstance();
        Date today = calendar.getTime();
        calendar.add(Calendar.DATE, -6);
        Date oneWeekAgo = calendar.getTime();

        GridLabelRenderer labelRenderer = inhalerDataGraph.getGridLabelRenderer();
        Viewport graphViewport = inhalerDataGraph.getViewport();

        //set number formatter for y values
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(0);
        nf.setMinimumIntegerDigits(1);

        //set date label formatter
        labelRenderer.setLabelFormatter(new DefaultLabelFormatter(nf, nf) {
            @Override
            public String formatLabel(double value, boolean isValueX) {
                if (isValueX){
                    //show dates without the year
                    Date date = new Date((long) value);
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(date);
                    String returnString = "\n" + (calendar.get(Calendar.MONTH) + 1) + "/" + calendar.get(Calendar.DAY_OF_MONTH);
                    return returnString;
                }
                else {
                    //show the normal y values
                    return super.formatLabel(value, isValueX);
                }
            }
        });

        labelRenderer.setNumHorizontalLabels(7);
        labelRenderer.setNumVerticalLabels(6);
        labelRenderer.setPadding(32);
        //inhalerDataGraph.getGridLabelRenderer().setVerticalAxisTitle("Inhaler Usage");
        //inhalerDataGraph.getGridLabelRenderer().setHorizontalAxisTitle("Date");
        labelRenderer.setHorizontalLabelsAngle(50);

        //set max and min data labels
        graphViewport.setXAxisBoundsManual(true);
        graphViewport.setMinX(oneWeekAgo.getTime());
        graphViewport.setMaxX(today.getTime());

        //set y axis data labels
        graphViewport.setYAxisBoundsManual(true);
        graphViewport.setMinY(0);
        graphViewport.setMaxY(10);

        labelRenderer.setHumanRounding(false);

        stepsText = (TextView) findViewById(R.id.stepsText);
        stopButton = (Button) findViewById(R.id.stopPedometer);
        startButton = (Button) findViewById(R.id.startPedometer);

        stopButton.setOnClickListener(stopButtonListener);
        startButton.setOnClickListener(startButtonListener);

        stepsText.setText(StepCounter.currentNumberOfSteps + "\nSteps Taken");

        if (db.isNetworkAvailable(mContext)){
            new RetrieveInhalerDataFromServer().execute();
        }
        else{
            Toast.makeText(mContext, "Could not download inhaler data\nno network connection available", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.welcome, menu);

        MenuItem shareItem = menu.findItem(R.menu.welcome);

        // To retrieve the Action Provider
        mActionProvider = (ShareActionProvider)
                MenuItemCompat.getActionProvider(shareItem);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (!HandleMenu.onOptionsItemSelected(item, this)) {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    protected void onResume(){
        StepCounter.viewerPageIsForeground = true;
        super.onResume();
    }

    @Override
    protected void onPause(){
        StepCounter.viewerPageIsForeground = false;
        super.onPause();
    }

    public void buildDataGraph(String jsonString) {

        ArrayList<DataPoint> dataArray = new ArrayList<DataPoint>();
        ArrayList<DateAndIntPair> dateArray = new ArrayList<DateAndIntPair>();
        ArrayList<DateAndIntPair> secondDateArray = new ArrayList<DateAndIntPair>();

        for (int i = 0; i <7; i++){
            Date dataDate = calendar.getTime();
            dateArray.add(new DateAndIntPair(dataDate, 0));
            calendar.add(Calendar.DATE, 1);
        }

        try{
            String[] splitJSON = jsonString.split("=");
            JSONObject jsonRootObject = new JSONObject(splitJSON[1]);

            JSONArray rowsArray = jsonRootObject.optJSONArray("rows");
            JSONArray nextJSONArray = rowsArray.getJSONArray(0);
            int i = 1;
            while(nextJSONArray != null){
                String measuredDate = nextJSONArray.getString(0);
                String inhalerUsage = nextJSONArray.getString(1);
                Date date = (Date) format.parse(measuredDate);
                secondDateArray.add(new DateAndIntPair(date, Integer.valueOf(inhalerUsage)));
                nextJSONArray = rowsArray.optJSONArray(i);
                i++;
            }
        }
        catch(JSONException e){
            Log.i("JSON Parser", e.getMessage());
        }
        catch(Exception e){
            Log.i("JSON Parser", e.getMessage());
        }


        for (DateAndIntPair pair : dateArray){
            boolean addToDataArray = true;
            for (DateAndIntPair secondPair : secondDateArray){
                if ( format.format(pair.date).equals(format.format(secondPair.date)) ){
                    DataPoint data = new DataPoint(secondPair.date, secondPair.inhalerInt);
                    dataArray.add(data);
                    addToDataArray = false;
                }
            }
            if (addToDataArray){
                DataPoint data = new DataPoint(pair.date, pair.inhalerInt);
                dataArray.add(data);
            }
        }

        series = new LineGraphSeries<>(dataArray.toArray(new DataPoint[dataArray.size()]));
        series.setDrawDataPoints(true);
        series.setDataPointsRadius(10);
        series.setOnDataPointTapListener(new OnDataPointTapListener() {
            @Override
            public void onTap(Series series, DataPointInterface dataPoint) {
                Date date = new Date((long) dataPoint.getX());
                Toast.makeText(mContext, "Date: " + onClickFormat.format(date) + " Inhaler Usage: " + (int) dataPoint.getY(), Toast.LENGTH_SHORT).show();
            }
        });
        inhalerDataGraph.addSeries(series);
    }

    private View.OnClickListener stopButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent stopStepCounterIntent = new Intent(mContext, StepCounter.class);
            stopService(stopStepCounterIntent);
        }
    };

    private View.OnClickListener startButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent startStepCounterIntent = new Intent(mContext, StepCounter.class);
            startService(startStepCounterIntent);
        }
    };

    public void updateStepsCounter(int steps){
        stepsText.setText(steps + "\nSteps Taken");
    }

    private class RetrieveInhalerDataFromServer extends AsyncTask<String, Void, String>{

        @Override
        protected String doInBackground(String... params) {
            String jsonStringFromQuery = "";

            String mobileId = Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.ANDROID_ID);
            String queryUrl = "http://www.smartconnectedhealth.org/aura/webroot/db.jsp?qn=46&mobile_id=" + mobileId + "&time=" + 7 + "%20days";
            HttpURLConnection urlConnection = null;

            try {
                URL url = new URL(queryUrl);
                urlConnection = (HttpURLConnection) url.openConnection();
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                StringBuilder builder = new StringBuilder(in.available());

                String currentLine = reader.readLine();
                while(currentLine != null){
                    builder.append(currentLine);
                    currentLine = reader.readLine();
                }
                jsonStringFromQuery = builder.toString();
            }
            catch (java.net.MalformedURLException exception){
                Toast.makeText(mContext, "The URL for getting data was malformed", Toast.LENGTH_SHORT).show();
            }
            catch (IOException e){
                Toast.makeText(mContext, "There was an exception opening the url connection", Toast.LENGTH_SHORT).show();
            }
            finally {
                if (urlConnection != null){
                    urlConnection.disconnect();
                }
            }

            return jsonStringFromQuery;
        }

        @Override
        protected void onPostExecute(String jsonString){
            buildDataGraph(jsonString);
        }
    }

    private class DateAndIntPair{
        public Date date;
        public int inhalerInt;

        DateAndIntPair(Date date, int inhalerInt){
            this.date = date;
            this.inhalerInt = inhalerInt;
        }

    }


}
