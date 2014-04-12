package com.geoinet.patienthelper;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TabHost.TabSpec;

public class PatientHelper_Main extends Activity
{

	//Flag we use to determine if the user wanted to log incoming information
	public boolean logging = false;

	//Unique device ID based on bluetooth hardware
	private final static String android_id=BluetoothAdapter.getDefaultAdapter().getAddress().replace(":","");
	//Various views for the actual UI
	TabHost tabHost;
	EditText phoneNumber;
	private SeekBar mSeekBar;
	private Spinner spinner;
	private Spinner doseSpinner;
	private TextView mProgressText;
	private TextView mTrackingText;
	private EditText medName;
	private EditText description;
	private Button logButton;
	private Button submit;
	private ArrayAdapter adapter;
	private ArrayAdapter doseAdapter;
	String solution = "";
	static String radioButtonSelected = "date";
	private TextView statusBT;
	private TextView fev1;
	private TextView pef;
	//peakflow communication
	BluetoothAdapter mBluetoothAdapter=null;
	BluetoothSocket mmSocket=null;
	BluetoothDevice mmDevice=null;
	OutputStream mmOutputStream=null;
	InputStream mmInputStream=null;
	Thread workerThread;
	byte[] readBuffer;
	int readBufferPosition;
	int counter;
	volatile boolean stopWorker;

	//location services
	private static double gpsLat=-1;
	private static double gpsLon=-1;
	private static double gpsVeloc=-1;

	public void onSaveInstanceState(Bundle savedInstanceState){
		super.onSaveInstanceState(savedInstanceState);
		savedInstanceState.putDouble("gpsLat",gpsLat);
		savedInstanceState.putDouble("gpsLat",gpsLon);
		savedInstanceState.putDouble("gpsLat",gpsVeloc);
	}
	//Create the database and set up the tabs
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		//retrieves location if screen is redrawn
		if(savedInstanceState!=null){
			gpsLat=savedInstanceState.getDouble("gpsLat");
			gpsLon=savedInstanceState.getDouble("gpsLon");
			gpsVeloc=savedInstanceState.getDouble("gpsVeloc");
		}
		
		setContentView(R.layout.activity_main);
		//turns on the Service monitoring environmental data
		if(isMyServiceRunning()){
			//service is running, do not start another
			Log.e("Service running", "service running");
		}
		else{//service was not running, start it
			this.startService(new Intent(this,PatientHelperService.class));
		}
		//checks if the device has bluetooth, if yes, turns it on, also simultaniously checks for peakflow device
		findBT();
		//makes sure peakflow threads shut off
		stopWorker=false;
		//sets up our applications gui
		TabSetup();
		
		//location listener class
		LocationManager locManag=(LocationManager)
				getSystemService(Context.LOCATION_SERVICE);
		LocationListener locListen=new LocationListener(){
			public void onLocationChanged(Location location){
				gpsLat=location.getLatitude();
				gpsLon=location.getLongitude();
				gpsVeloc=location.getSpeed();
			}

			@Override
			public void onProviderDisabled(String provider) {
				// TODO Auto-generated method stub
				Toast.makeText(getApplicationContext(), "GPS Not Responding", Toast.LENGTH_SHORT).show();
			}

			@Override
			public void onProviderEnabled(String provider) {
				// TODO Auto-generated method stub
				Toast.makeText(getApplicationContext(), "GPS Available", Toast.LENGTH_SHORT).show();
			}

			@Override
			public void onStatusChanged(String provider,
					int status, Bundle extras) {
				// TODO Auto-generated method stub
				
			}
			
		};
		//uses the above defined location listener to return location to the app
		locManag.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000*30,0,locListen);
		locManag.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,1000*60*2,0,locListen);
		
	}

	public void onResume()
	{
		super.onResume();
		if(isMyServiceRunning()){
			//service is running, do not start another
			Log.e("Service running", "service running");
		}
		else{//service was not running, start it
			this.startService(new Intent(this,PatientHelperService.class));
		}
		//Initialize our buffer
		stopWorker=false;
	}

	public void onPause()
	{
		super.onPause();
		if(isMyServiceRunning()){
			//service is running, do not start another
			Log.e("Service running", "service running");
		}
		else{//service was not running, start it
			this.startService(new Intent(this,PatientHelperService.class));
		}
		stopWorker=true;
		try {
			closeBT();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public void onDestroy(){
		if(isMyServiceRunning()){
			//service is running, do not start another
			Log.e("Service running", "service running");
		}
		else{//service was not running, start it
			this.startService(new Intent(this,PatientHelperService.class));
		}
		super.onDestroy();
		stopWorker=true;
		try {
			closeBT();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.patient_helper__main, menu);
		return true;
	}

	public void TabSetup()
	{
		tabHost = (TabHost) findViewById(R.id.tabhost);
		tabHost.setup();

		//Tab 2: Medication
		TabSpec tabSpec = tabHost.newTabSpec("tag2");
		tabSpec.setContent(R.id.tab2);
		tabSpec.setIndicator("Medication");
		tabHost.addTab(tabSpec);
		medName=(EditText) findViewById(R.id.medName);
		Button scheduledMedSubmit=(Button) findViewById(R.id.scheduledMedSubmit);
		scheduledMedSubmit.setOnClickListener(buttonPressed);
		doseSpinner = (Spinner) findViewById(R.id.doseSpinner);//trial code for dose spinner
		doseAdapter= ArrayAdapter.createFromResource(this, R.array.doses, android.R.layout.simple_spinner_item);
		doseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		doseSpinner.setAdapter(doseAdapter);

		//Tab 3: PeakFlow Usage Tools
		tabSpec = tabHost.newTabSpec("tag3");
		tabSpec.setContent(R.id.tab3);
		tabSpec.setIndicator("Peak Flow Meter");
		tabHost.addTab(tabSpec);
		statusBT= (TextView) findViewById(R.id.statusBT);
		fev1=(TextView) findViewById(R.id.fev1data);
		pef=(TextView) findViewById(R.id.pefdata);

		//Tab 4: Post Attack Breakdown
		tabSpec = tabHost.newTabSpec("tag4");
		tabSpec.setContent(R.id.tab4);
		tabSpec.setIndicator("Attack");
		tabHost.addTab(tabSpec);
		logButton = (Button) findViewById(R.id.sensorLog);
		logButton.setOnClickListener(buttonPressed);

		spinner = (Spinner) findViewById(R.id.solutionSpinner);
		adapter = ArrayAdapter.createFromResource(this, R.array.solutions, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
		submit = (Button) findViewById(R.id.submit);
		submit.setOnClickListener(buttonPressed);

		mSeekBar = (SeekBar) findViewById(R.id.severityBar);
		mSeekBar.setOnSeekBarChangeListener(seekChange);
		mProgressText = (TextView) findViewById(R.id.progress);
		mTrackingText = (TextView) findViewById(R.id.tracking);
		description = (EditText) findViewById(R.id.description);
		mSeekBar.setProgress(50);

		//Tab 5: Graphical Analysis
		tabSpec = tabHost.newTabSpec("tag1");
		tabSpec.setContent(R.id.tab1);
		tabSpec.setIndicator("Graphs");
		tabHost.addTab(tabSpec);


	}

	//Listener for all of the buttons by using a switch
	OnClickListener buttonPressed = new OnClickListener()
	{
		public void onClick(View arg0)
		{
			switch (arg0.getId()) //need to make dose counter
			{
			//Create and send a text to the number indicated, should give nearest street address, but not currently implemented
			case R.id.scheduledMedSubmit:
				AlertDialog.Builder tempAlertBuilder2=new AlertDialog.Builder(PatientHelper_Main.this);
				tempAlertBuilder2.setTitle("Submit Data")
				.setMessage("Do you want to submit this data?")
				.setNeutralButton("No",null)
				.setPositiveButton("Yes",new DialogInterface.OnClickListener(){
					public void onClick(DialogInterface arg0, int arg1) {
						boolean diditwork=true;
						try{
							final String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
							String medicationName = medName.getText().toString();
							//trial code
							int sol = doseSpinner.getSelectedItemPosition();//0 or null-inhaler,1-Med,2-PaperBag,3-Other
							String tempDoses="No Solution Selected";
							if(sol==1){
								tempDoses="2 Doses";
							}
							else if(sol==2){
								tempDoses="3 Doses";
							}
							else if(sol==3){
								tempDoses="More";
							}
							else{
								tempDoses="1 Dose";
							}
							
							File externalMem2=Environment.getExternalStorageDirectory();
							File directory2=new File (externalMem2.getAbsolutePath()+"/PatientHelper");
							directory2.mkdirs();
							File file2=new File(directory2,"medicationData.txt");
							final String tempData=", "+medicationName+", "+tempDoses+", "+gpsLat+", "+gpsLon+", "+gpsVeloc+" \r\n";
							final String medNameData=medicationName;
							final String medDoseData=tempDoses;//get dose data from field
							ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
							NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
							if(mWifi.isConnected()){
								Thread submitMed=new Thread(new Runnable()
								{
									public void run()
									{     
										List<NameValuePair> data=new ArrayList<NameValuePair>(3);
										JSONObject values=new JSONObject();
										try{
										values.put("medicationName",medNameData);
										values.put("medicationDose",medDoseData);
										}
										catch(Exception e){
											Log.e("JSON Medication","Failed adding values");
										}
										data.add(new BasicNameValuePair("id",android_id ));
										data.add(new BasicNameValuePair("api_key", "1"));
										data.add(new BasicNameValuePair("timeRecorded",date));
										//data.add(new BasicNameValuePair("medicationName", medNameData));
										//data.add(new BasicNameValuePair("medicationDose", medDoseData));
										data.add(new BasicNameValuePair("val", values.toString()));
										data.add(new BasicNameValuePair("htype", "medication"));
										data.add(new BasicNameValuePair("lat", gpsLat+""));
										data.add(new BasicNameValuePair("lon", gpsLon+""));
										data.add(new BasicNameValuePair("veloc", gpsVeloc+""));
										PostToServer postMan=new PostToServer();
										final String temp=postMan.postResults(data,"http://www.geospaces.org/aura/webroot/health.jsp");
									}
								});
								submitMed.start();
							}
							else{
								if(file2.exists()){
									FileWriter fw=new FileWriter(file2,true);
									fw.write(date+tempData);
									fw.close();
								}
								else{
									FileWriter fw=new FileWriter(file2,false);
									fw.write("Year-Month-Day Hour:Minute:Second,Medication Name,Doses,Lat,Lon,Velocity\r\n");//add the information as to what is between each comma.
									fw.write(date+tempData);
									fw.close();
								}
							}
						}
						catch(Exception e){
							diditwork=false;
							Log.e("WRITING ERROR", e.toString());
						}
						finally 
						{
							if (diditwork)
							{
								Toast.makeText(getApplicationContext(), "Information Saved", Toast.LENGTH_SHORT).show();
							}
							else
							{
								Toast.makeText(getApplicationContext(), "Error Saving", Toast.LENGTH_SHORT).show();
							}
						}
					}

				});
				AlertDialog dialog2=tempAlertBuilder2.create();
				dialog2.show();
				break;


				// Will submit data to the database
			case R.id.submit:

				AlertDialog.Builder tempAlertBuilder=new AlertDialog.Builder(PatientHelper_Main.this);
				tempAlertBuilder.setTitle("Submit Data")
				.setMessage("Do you want to submit this data?")
				.setNeutralButton("No",null)
				.setPositiveButton("Yes",new DialogInterface.OnClickListener(){
					public void onClick(DialogInterface arg0, int arg1) {
						boolean didItWork = true;
						try 
						{
							int severity = mSeekBar.getProgress();
							int sol = spinner.getSelectedItemPosition();//0 or null-inhaler,1-Med,2-PaperBag,3-Other
							String tempsol="No Solution Selected";
							if(sol==1){
								tempsol="Medication";
							}
							else if(sol==2){
								tempsol="PaperBag";
							}
							else if(sol==3){
								tempsol="Other";
							}
							else{
								tempsol="Inhaler";
							}
							String descript = description.getText().toString();
							final String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
							final String tempString=", "+tempsol+", "+severity+", "+descript+", "+gpsLat+", "+gpsLon+", "+gpsVeloc+" \r\n";
							final String solutionData=tempsol;
							final int severityData=severity;
							final String description=descript;
							ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
							NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
							if(mWifi.isConnected()){
								Thread submitAttack=new Thread(new Runnable()
								{
									public void run()
									{     
										List<NameValuePair> data=new ArrayList<NameValuePair>(3);//need to break down remaining
										JSONObject values=new JSONObject();
										try{
										values.put("solution",solutionData);
										values.put("severity",severityData);
										values.put("description",description);
										}
										catch(Exception e){
											Log.e("JSON Attack","Failed adding values");
										}
										data.add(new BasicNameValuePair("id",android_id ));
										data.add(new BasicNameValuePair("api_key", "1"));
										data.add(new BasicNameValuePair("timeRecorded",date));
										data.add(new BasicNameValuePair("val",values.toString() ));
										data.add(new BasicNameValuePair("htype", "attack"));
										data.add(new BasicNameValuePair("lat", gpsLat+""));
										data.add(new BasicNameValuePair("lon", gpsLon+""));
										data.add(new BasicNameValuePair("veloc", gpsVeloc+""));
										PostToServer postMan=new PostToServer();
										final String temp=postMan.postResults(data,"http://www.geospaces.org/aura/webroot/health.jsp");
									}
								});
								submitAttack.start();
							}
							else{
								File externalMem2=Environment.getExternalStorageDirectory();
								File directory2=new File (externalMem2.getAbsolutePath()+"/PatientHelper");
								directory2.mkdirs();
								File file2=new File(directory2,"attackData.txt");
								//add code from buffercleanup in service once all data types have been set for csv saving
								if(file2.exists()){
									FileWriter fw=new FileWriter(file2,true);
									fw.write(date+tempString);
									fw.close();
								}
								else{

									FileWriter fw=new FileWriter(file2,false);
									fw.write("Year-Month-Day Hour:Minute:Second, Solution, Severity, Unusual Conditions,Lat,Lon,Velocity\r\n");//add the information as to what is between each comma.
									fw.write(date+tempString);
									fw.close();
								}
							}

						} 
						catch (Exception e) 
						{
							didItWork = false;
							Log.e("WRITING ERROR", e.toString());
						}
						finally 
						{
							if (didItWork)
							{
								Toast.makeText(getApplicationContext(), "Information Saved", Toast.LENGTH_SHORT).show();
							}
							else
							{
								Toast.makeText(getApplicationContext(), "Error Saving", Toast.LENGTH_SHORT).show();
							}
						}
					}

				});
				AlertDialog dialog=tempAlertBuilder.create();
				dialog.show();


				break;

			case R.id.sensorLog:
				logging = !logging;
				if(logging == true)
				{
					logButton.setText("End");
					try {
						openBT();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						statusBT.setText("Connection Failed.\n Please re-attempt connection.");
						e.printStackTrace();
					}
				}
				else
				{
					logButton.setText("Start");
					try {
						closeBT();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					stopWorker=true;
				}
				break;
			}
		}
	};

	//Changes text when the progress bar is changed.
	OnSeekBarChangeListener seekChange = new OnSeekBarChangeListener()
	{
		public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
			mProgressText.setText(progress + " / 100");
		}
		public void onStartTrackingTouch(SeekBar seekBar)
		{			
		}
		public void onStopTrackingTouch(SeekBar seekBar)
		{			
		}
	};
	/*
	 * Finds the peakflow device
	 */
	void findBT()
	{
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if(mBluetoothAdapter == null)
		{
			Toast.makeText(getApplicationContext(), "Your device does not support Bluetooth, and is therefore incompatable.", Toast.LENGTH_LONG).show();
			onDestroy();
		}

		if(!mBluetoothAdapter.isEnabled())
		{
			Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBluetooth, 0);
		}

		Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
		if(pairedDevices.size() > 0)
		{
			for(BluetoothDevice device : pairedDevices)
			{
				if(device.getName().contains("ASMA")) 
				{
					mmDevice = device;
					break;
				}
			}
		}
		//myLabel.setText("Bluetooth Device Found");
	}
	/*
	 * Opens the bt socket for the peakflow and starts listening for data
	 */
	void openBT() throws IOException
	{
		UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Standard SerialPortService ID
		mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);        
		mmSocket.connect();
		mmOutputStream = mmSocket.getOutputStream();
		mmInputStream = mmSocket.getInputStream();

		beginListenForData();

		statusBT.setText("Successful Connection");//lets user know bt is open
	}
	/* Very large method, I should likely break it down
	 * It starts listening for the data from the peakflow
	 * and handles the prompt to send data, as well as the actual sending
	 * It was convenient to place it in one method as network management is required to be in a new thread
	 */
	void beginListenForData()
	{
		final Handler handler = new Handler(); 
		final byte delimiter = 3; //This is the ASCII code for a newline character

		stopWorker = false;
		readBufferPosition = 0;
		readBuffer = new byte[1024];
		workerThread = new Thread(new Runnable()
		{
			public void run()
			{                
				while(!Thread.currentThread().isInterrupted() && !stopWorker)
				{
					try 
					{
						int bytesAvailable = mmInputStream.available();                        
						if(bytesAvailable > 0)
						{
							byte[] packetBytes = new byte[bytesAvailable];
							mmInputStream.read(packetBytes);
							for(int i=0;i<bytesAvailable;i++)
							{
								byte b = packetBytes[i];
								if(b == delimiter)
								{
									byte[] encodedBytes = new byte[readBufferPosition];
									System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
									final String data = new String(encodedBytes, "US-ASCII");
									readBufferPosition = 0;
									handler.post(new Runnable()
									{
										boolean yes=false;
										public void run()
										{
											final double fev;
											final int pef2;

											if(data.length()>=20){
												int temp=data.indexOf('D');
												fev=(double)Integer.parseInt(data.substring(temp+11,temp+14))/100;
												pef2=Integer.parseInt(data.substring(temp+14,temp+17));
												fev1.setText("FEV1: "+fev);
												pef.setText("PEF: "+pef2);
												statusBT.setText("Collection Successful");//change to print data somewhere
												AlertDialog.Builder tempAlertBuilder2=new AlertDialog.Builder(PatientHelper_Main.this);
												tempAlertBuilder2.setTitle("Submit Data")
												.setMessage("Do you want to submit this data?\n"+"The FEV1 was: "+fev+" and PEF was: "+pef2+".")
												.setNeutralButton("No",null)
												.setPositiveButton("Yes",new DialogInterface.OnClickListener(){
													public void onClick(DialogInterface arg0, int arg1) {
														yes=true;
														try{
															final String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
															final String remainingString=", "+fev+", "+pef2+", "+gpsLat+", "+gpsLon+", "+gpsVeloc+" \r\n";
															final double fevData=fev;
															final int pefData=pef2;
															ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
															NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
															if(mWifi.isConnected()){
																Thread submitAttack=new Thread(new Runnable()
																{
																	public void run()
																	{     
																		List<NameValuePair> data=new ArrayList<NameValuePair>(3);//break remaining into better pairs
																		JSONObject values=new JSONObject();
																		try{
																		values.put("fev",fev);
																		values.put("pef",pef2);
																		}
																		catch(Exception e){
																			Log.e("JSON PeakFlow","Failed adding values");
																		}
																		data.add(new BasicNameValuePair("id",android_id ));
																		data.add(new BasicNameValuePair("api_key", "1"));
																		data.add(new BasicNameValuePair("timeRecorded",date));
																		data.add(new BasicNameValuePair("val", values.toString()));
																		data.add(new BasicNameValuePair("htype", "peakflow"));
																		data.add(new BasicNameValuePair("lat", gpsLat+""));
																		data.add(new BasicNameValuePair("lon", gpsLon+""));
																		data.add(new BasicNameValuePair("veloc", gpsVeloc+""));
																		PostToServer postMan=new PostToServer();
																		final String temp=postMan.postResults(data,"http://www.geospaces.org/aura/webroot/health.jsp");
																	}
																});
																submitAttack.start();
															}
															else{
																String tempString=date+", "+fev+", "+pef2+"\r\n";
																File externalMem2=Environment.getExternalStorageDirectory();
																File directory2=new File (externalMem2.getAbsolutePath()+"/PatientHelper");
																directory2.mkdirs();
																File file2=new File(directory2,"peakFlowData.txt");


																if(file2.exists()){
																	FileWriter fw=new FileWriter(file2,true);
																	fw.write(date+tempString);
																	fw.close();
																}
																else{
																	FileWriter fw=new FileWriter(file2,false);
																	fw.write("Year-Month-Day Hour:Minute:Second, FEV1, PEF,Lat,Lon,Velocity\r\n");
																	fw.write(date+tempString);
																	fw.close();
																}
															}
														}
														catch(Exception e){
															yes=false;
															Log.e("WRITING ERROR", e.toString());
														}
														finally 
														{
															if (yes)
															{
																Toast.makeText(getApplicationContext(), "Information Saved", Toast.LENGTH_SHORT).show();
															}
															else
															{
																Toast.makeText(getApplicationContext(), "Error Saving", Toast.LENGTH_SHORT).show();
															}
														}
													}


												});
												AlertDialog dialog2=tempAlertBuilder2.create();
												dialog2.show();
												//if(yes){//probably garbage
												//}
											}
											else if(data.contains("CPD")){
												statusBT.setText("Device shutdown, please press end.");
											}
											else{
												statusBT.setText("Invalid Statement Recieved");
											}
										}
									});

								}
								else
								{
									readBuffer[readBufferPosition++] = b;
								}
							}
						}
					} 
					catch (IOException ex) 
					{
						stopWorker = true;
					}
				}
			}
		});

		workerThread.start();
	}
	//closes the thread handling peakflow communications
	void closeBT() throws IOException
	{
		stopWorker = true;
		if(mmOutputStream!=null)
			mmOutputStream.close();
		if(mmInputStream!=null)
			mmInputStream.close();
		if(mmSocket!=null)
			mmSocket.close();
		statusBT.setText("Connection Closed");
	}
	//checks if PatientHelperService is running(the environmental data collector)
	private boolean isMyServiceRunning() {
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if (PatientHelperService.class.getName().equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

}