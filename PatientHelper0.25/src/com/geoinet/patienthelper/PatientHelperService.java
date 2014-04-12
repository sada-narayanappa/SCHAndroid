package com.geoinet.patienthelper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import ioio.lib.api.AnalogInput;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.Uart;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOService;

public class PatientHelperService extends IOIOService{
	private final static int TEMP_INPUT = 45;
	private final static int HUMIDITY_INPUT = 42;
	private final static int GPS_INPUT = 10;
	private final static int NITROGENDIOXIDE_INPUT1 = 41;
	private final static int NITROGENDIOXIDE_INPUT2 = 40;
	private final static int OZONE_INPUT = 39;
	private final static int SD_OUTPUT = 12;//environ sensor on-board storage, not currently used
	private volatile boolean threadCheck=true;//used to limit a thread to one instance
	//Unique device ID based on bluetooth hardware
	private final static String android_id=BluetoothAdapter.getDefaultAdapter().getAddress().replace(":","");
	ArrayList<SensorData> buffer;
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	public void onCreate(){
		super.onCreate();
		registerReceiver(receiver, new IntentFilter("ACTION_READ"));
	}
	public void onDestroy(){
		super.onDestroy();
		unregisterReceiver(receiver);
	}
	public void stop(){
		super.stop();
	}
	public void onStart(Intent intent,int startId){
		super.onStart(intent, startId);
		buffer=new ArrayList<SensorData>();

	}
	/*
	 * Checks if the device is on WIFI
	 */
	@SuppressWarnings("static-access")
	public static boolean checkConnection(Context ctx) {
		ConnectivityManager conMgr = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo i = conMgr.getActiveNetworkInfo();
		boolean wifi=false;
		if (i == null){
			Log.d("checkConnection","Check connection Working");
			return false;
		}
		if(i.getType()==conMgr.TYPE_WIFI)
		{
			wifi=true;
		}
		if (!i.isConnected()||!wifi){//if not connected or not using wifi, return false
			Log.d("checkConnection","Check connection Working");
			return false;
		}
		if (!i.isAvailable()||!wifi){
			Log.d("checkConnection","Check connection Working");
			return false;
		}
		return true;
	}
	/*
	 * Checks if external storage is writable
	 */
	public boolean isExternalStorageWritable() {
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			Log.d("Storage","ExternalStorage method Working");
			return true;
		}
		return false;
	}
	/*
	 * Handles sending data to the server for the environmental data
	 * Also, if text files exist containing archived data, this service will load those to the server
	 */
	public void bufferCleanup()
	{
		//The size may change while we are in this method as the IOIO will still be communicating
		final int tempSize = buffer.size();
		//This is where we'd send the data up to the server, at the moment we're just inserting it into the local database

		if(tempSize > 50)//must have a buffer over 50
		{

			if(checkConnection(this)&&threadCheck==true){//should check if you are on wifi before attempting to send data
				threadCheck=false;
				Thread poster= new Thread(new Runnable()
				{
					public void run()
					{
						Log.d("ServicePoster", "Started");
						for(int i = 0; i < tempSize; i++)
						{
							//Remove the first element from the buffer
							SensorData tempData = buffer.remove(0);
							List<NameValuePair> data=new ArrayList<NameValuePair>(8);
							data.add(new BasicNameValuePair("temperature", Float.toString(tempData.temperature)));//add temperature
							data.add(new BasicNameValuePair("NO2", Float.toString(tempData.NO2)));//add NO2
							data.add(new BasicNameValuePair("humidity", Float.toString(tempData.humidity)));//add humidity
							data.add(new BasicNameValuePair("O3", Float.toString(tempData.O3)));//O3

							String [] gpsStrings=tempData.gps.split(",");
							if(gpsStrings.length>8&&tempData.gps.substring(0,tempData.gps.length()-5).contains("A")){
								String gpsNS=(gpsStrings[3].substring(0,2)+" degrees "+gpsStrings[3].substring(2)+" minutes "+gpsStrings[4]);
								String gpsEW=(gpsStrings[5].substring(0,3)+" degrees "+gpsStrings[5].substring(3)+" minutes "+gpsStrings[6]);
								String gpsVelocity=(gpsStrings[7]);
								data.add(new BasicNameValuePair("lat", gpsNS));
								data.add(new BasicNameValuePair("lon",gpsEW));
								data.add(new BasicNameValuePair("veloc",gpsVelocity));
							}
							else{//not sure if this is needed using Sada's code
								data.add(new BasicNameValuePair("gps", tempData.gps));
							}
							data.add(new BasicNameValuePair("timeRecorded", tempData.date));
							data.add(new BasicNameValuePair("id", android_id));
							data.add(new BasicNameValuePair("api_key", "1"));

							PostToServer postMan=new PostToServer();
							postMan.postResults(data,"http://www.geospaces.org/aura/webroot/env.jsp?");
							//http://www.geospaces.org/aura/webroot/env.jsp?
						} 
						//end of putting new data from buffer to server, starting reading old file and uploading
						File externalMem=Environment.getExternalStorageDirectory();
						File directory=new File (externalMem.getAbsolutePath()+"/PatientHelper");
						directory.mkdirs();
						File file=new File(directory,"environmentalData.txt");
						/*
						 * Reads the environment text file and uploads it
						 */
						if(file.exists()){
							BufferedReader br=null;
							String line;
							try {
								br=new BufferedReader(new FileReader(file));
							} catch (FileNotFoundException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							try {
								while((line=br.readLine())!=null){//Year-Month-Day,Hour:Minute:Second,Temperature,Humidity,Latitude,Longitude,Speed
									if(!(line.substring(0,4).equals("Year"))){
										String[] readData=line.split(",");
										List<NameValuePair> data=new ArrayList<NameValuePair>(4);
										data.add(new BasicNameValuePair("id",android_id));
										data.add(new BasicNameValuePair("api_key", "1"));
										data.add(new BasicNameValuePair("timeRecorded",readData[0]));
										data.add(new BasicNameValuePair("temperature",readData[1]));
										data.add(new BasicNameValuePair("humidity",readData[2]));
										data.add(new BasicNameValuePair("lat",readData[3]));
										data.add(new BasicNameValuePair("lon",readData[4]));
										data.add(new BasicNameValuePair("veloc",readData[5]));
										data.add(new BasicNameValuePair("NO2",readData[6]));
										data.add(new BasicNameValuePair("O3",readData[7]));
										data.add(new BasicNameValuePair("api_key", "1"));
										PostToServer postMan=new PostToServer();
										postMan.postResults(data,"http://mscs-php.uwstout.edu/geoinet/EBPHPCode/src/main/webapp/webroot/env.jsp");
									}
								}

							} catch (Exception e1) {
								// TODO Auto-generated catch block
								Log.e("Error Reading Environ Text File","Error Reading Environ Text File");
							}


							try {
								if(br!=null)
									br.close();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							file.delete();
						}  
						/*
						 * Reads the attack text file and uploads it
						 * 
						 */
						file=new File(directory,"attackData.txt");
						if(file.exists()){
							BufferedReader br=null;
							String line;
							try {
								br=new BufferedReader(new FileReader(file));
							} catch (FileNotFoundException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							try {
								while((line=br.readLine())!=null){//use split method
									if(!(line.substring(0,4).equals("Year"))){
										String[] readData=line.split(",");
										List<NameValuePair> filedata=new ArrayList<NameValuePair>(4);
										JSONObject values=new JSONObject();
										try{
										values.put("solution",readData[1]);
										values.put("severity",readData[2]);
										values.put("description",readData[3]);
										}
										catch(Exception e){
											Log.e("JSON Attack Service","Failed adding values");
										}
										filedata.add(new BasicNameValuePair("id",android_id));
										filedata.add(new BasicNameValuePair("api_key", "1"));
										filedata.add(new BasicNameValuePair("timeRecorded",readData[0]));
										filedata.add(new BasicNameValuePair("htype", "attack"));
										filedata.add(new BasicNameValuePair("val",values.toString()));
										filedata.add(new BasicNameValuePair("lat",readData[4]));
										filedata.add(new BasicNameValuePair("lon",readData[5]));
										filedata.add(new BasicNameValuePair("veloc",readData[6]));
										PostToServer filepostMan=new PostToServer();
										filepostMan.postResults(filedata,"http://www.geospaces.org/aura/webroot/health.jsp");
									}
								}

							} catch (IOException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}


							try {
								if(br!=null)
									br.close();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							file.delete();
						} 
						/*
						 * Reads the medication text file and uploads it
						 * 
						 */
						file=new File(directory,"medicationData.txt");
						if(file.exists()){
							BufferedReader br=null;
							String line;
							try {
								br=new BufferedReader(new FileReader(file));
							} catch (FileNotFoundException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							try {
								while((line=br.readLine())!=null){//use split method
									if(!(line.substring(0,4).equals("Year"))){
										String[] readData=line.split(",");
										List<NameValuePair> filedata=new ArrayList<NameValuePair>(3);
										JSONObject values=new JSONObject();
										try{
										values.put("medicationName",readData[1]);
										values.put("medicationDose",readData[2]);
										}
										catch(Exception e){
											Log.e("JSON Medication Service","Failed adding values");
										}
										filedata.add(new BasicNameValuePair("id",android_id));
										filedata.add(new BasicNameValuePair("api_key", "1"));
										filedata.add(new BasicNameValuePair("timeRecorded",readData[0]));
										filedata.add(new BasicNameValuePair("val",values.toString()));
										filedata.add(new BasicNameValuePair("Lat",readData[3]));
										filedata.add(new BasicNameValuePair("Lon",readData[4]));
										filedata.add(new BasicNameValuePair("Velocity",readData[5]));
										filedata.add(new BasicNameValuePair("htype", "medication"));
										PostToServer filepostMan=new PostToServer();
										filepostMan.postResults(filedata,"http://www.geospaces.org/aura/webroot/health.jsp");
									}
								}

							} catch (IOException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}


							try {
								if(br!=null)
									br.close();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							file.delete();
						}
						/*
						 * Reads the peakflow text file and uploads it
						 * 
						 */
						file=new File(directory,"peakFlowData.txt");
						if(file.exists()){
							BufferedReader br=null;
							String line;
							try {
								br=new BufferedReader(new FileReader(file));
							} catch (FileNotFoundException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							try {
								while((line=br.readLine())!=null){//use split method
									if(!(line.substring(0,4).equals("Year"))){
										String[] readData=line.split(",");
										List<NameValuePair> filedata=new ArrayList<NameValuePair>(3);
										JSONObject values=new JSONObject();
										try{
										values.put("fev",readData[1]);
										values.put("pef",readData[2]);
										}
										catch(Exception e){
											Log.e("JSON PeakFlow Service","Failed adding values");
										}
										filedata.add(new BasicNameValuePair("id",android_id));
										filedata.add(new BasicNameValuePair("api_key", "1"));
										filedata.add(new BasicNameValuePair("timeRecorded",readData[0]));
										filedata.add(new BasicNameValuePair("htype", "peakflow"));
										filedata.add(new BasicNameValuePair("val",values.toString()));
										filedata.add(new BasicNameValuePair("Lat",readData[3]));
										filedata.add(new BasicNameValuePair("Lon",readData[4]));
										filedata.add(new BasicNameValuePair("Velocity",readData[5]));
										PostToServer filepostMan=new PostToServer();
										filepostMan.postResults(filedata,"http://www.geospaces.org/aura/webroot/health.jsp");
									}
								}

							} catch (IOException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}


							try {
								if(br!=null)
									br.close();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							file.delete();
						}
						threadCheck=true;
						Log.d("ServicePoster", "Changed Boolean and Ended");
					}
				});
				poster.start();//starts the above code in a new thread
			}
			else{
				//writes to text file if connection is not available
				Log.d("DataDump", "Dumping buffer to textfile");
				for(int i = 0; i < tempSize; i++)
				{
					//Remove the first element from the buffer
					SensorData tempData = buffer.remove(0);


					//Attempt to put the removed data into text file
					try
					{
						String tempString=tempData.date+","+tempData.temperature+","+tempData.humidity;
						String [] gpsStrings=tempData.gps.split(",");
						if(gpsStrings.length>8&&tempData.gps.substring(0,tempData.gps.length()-5).contains("A")){
							String gpsNS=(gpsStrings[3].substring(0,2)+" degrees "+gpsStrings[3].substring(2)+" minutes "+gpsStrings[4]);
							String gpsEW=(gpsStrings[5].substring(0,3)+" degrees "+gpsStrings[5].substring(3)+" minutes "+gpsStrings[6]);
							String gpsVelocity=(gpsStrings[7]);
							tempString=tempString+","+gpsNS+","+gpsEW+","+gpsVelocity+","+tempData.NO2+","+tempData.O3+"\r\n";
						}
						else{
							tempString=tempString+", , , "+tempData.NO2+","+tempData.O3+" \r\n";
						}
						File externalMem=Environment.getExternalStorageDirectory();
						File directory=new File (externalMem.getAbsolutePath()+"/PatientHelper");
						directory.mkdirs();
						File file=new File(directory,"environmentalData.txt");
						//test
						if(file.exists()){
							FileWriter fw=new FileWriter(file,true);
							fw.write(tempString);
							fw.close();
						}
						else{//alter after other pins brought online

							FileWriter fw=new FileWriter(file,false);
							fw.write("Year-Month-Day Hour:Minute:Second,Temperature,Humidity,Latitude,Longitude,Speed\r\n");//add the information as to what is between each comma.
							fw.write(tempString);
							fw.close();
						}

					}
					catch(Exception e)
					{
						Log.e("BUFFER ERROR", e.toString());
					}

				}
			}

		}//for external write check
	}
	/*
	 * Contains environmental data,  must be updated for new inputs
	 */
	private class SensorData
	{
		public float temperature;
		public float humidity;
		public String gps;
		public String date;
		public float O3;
		public float NO2;

		public SensorData(float t, float h, String g,float NO2,float O3)
		{
			this.O3=03;
			this.NO2=NO2;
			temperature = t;
			humidity = h;
			gps = g;
			date=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
		}
	}
	/*
	 * IOIO specific class
	 */
	class Looper extends BaseIOIOLooper
	{
		//Inputs from the various sensors
		private DigitalOutput led;
		private AnalogInput tempInput;
		private AnalogInput humidInput;
		private AnalogInput NO2Input1;
		private AnalogInput NO2Input2;
		private AnalogInput O3Input;
		private Uart uartGPS_;
		//	private Uart uartSD_;
		private InputStream in_;

		//Once we've established connection with the AndroidIOIO we have to setup the pins
		protected void setup() throws ConnectionLostException
		{
			led = ioio_.openDigitalOutput(0, true);

			tempInput = ioio_.openAnalogInput(TEMP_INPUT);

			humidInput = ioio_.openAnalogInput(HUMIDITY_INPUT);

			NO2Input1 = ioio_.openAnalogInput(NITROGENDIOXIDE_INPUT1);
			NO2Input2 = ioio_.openAnalogInput(NITROGENDIOXIDE_INPUT2);

			O3Input = ioio_.openAnalogInput(OZONE_INPUT);

			uartGPS_ = ioio_.openUart(GPS_INPUT, 5, 4800, Uart.Parity.NONE,
					Uart.StopBits.ONE);
		}

		//The loop that begins running after setup and continues to run until connection is lost
		public void loop() throws ConnectionLostException
		{
			//Log.e("We started the loop", ": this is good");//testing

			led.write(false);//light on
			//Create the intent we are going to broadcast
			Intent intent = new Intent("ACTION_READ");
			char c = 0;
			String line = "";
			in_ = uartGPS_.getInputStream();
			String gprmc = "      ";
			int rx1 = 0;

			// Checks if GPS data is available
			try {
				if (in_.available() != 0) {
					while (!gprmc.equals("GPRMC"))// Keep gathering lines
						// until a GPRMC line is
						// sent
					{
						line = "";
						gprmc = "      ";
						c = 0;
						while (c != '$')// Keep gathering data until a full
							// line is sent
						{
							rx1 = in_.read();
							c = (char) rx1;
							line = line + c;
							in_ = uartGPS_.getInputStream();
						}
						if (line.length() > 7) // Checks if line is long
							// enough
						{
							gprmc = line.substring(0, 5); // Should contain
							// prefix for
							// data ex.
							// GPRMC,GPGGA,
							// GPGSV

						}
					}
				}
			} catch (IOException e) {

				e.printStackTrace();

			}
			// Add the GPS data to the intent
			intent.putExtra("text", line);
			try {
				// Attempt to read the temperature and humidity sensor data
				// and attach them to the intent
				intent.putExtra("temperature", tempInput.getVoltage());
				intent.putExtra("humidity", humidInput.getVoltage());
				intent.putExtra("NO2_1", NO2Input1.getVoltage());
				intent.putExtra("NO2_2", NO2Input2.getVoltage());
				intent.putExtra("O3", O3Input.getVoltage());
				// Send our intent back to the actual application
				sendBroadcast(intent);
			} 
			catch (Exception e)
			{
				e.printStackTrace();
				Log.e("Problem sending broadcast1", "Problem sending broadcast1");//testing
			}

			try
			{
				//Attempt to put the thread to sleep for 300 ms or data roughly every .3 seconds
				Thread.sleep(300);//for sleep 5000 you get data every 5 seconds, roughly 5 minutes to fill the buffer
			} 
			catch(InterruptedException e)
			{
				Log.e("Problem sending broadcast2","Problem sending broadcast2");//testing
			}
		}
	}

	//Create our looper when a AndroidIOIO is detected
	protected IOIOLooper createIOIOLooper()
	{
		return new Looper();
	}

	BroadcastReceiver receiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			//Log.e("Reached reciever", "Reached reciever");
			if(intent.getAction().equals("ACTION_READ"))
			{	
				float NO2_1 = intent.getExtras().getFloat("NO2_1");
				float NO2_2 = intent.getExtras().getFloat("NO2_2");
				float NO2 = 1 / (NO2_1 - (NO2_1 - NO2_2)) * (NO2_1 - NO2_2);
				NO2 = NO2ppm(NO2);

				float O3 = intent.getExtras().getFloat("O3");
				O3 = 0.3f / (5 - (5 - O3)) * (5 - O3);
				O3= O3ppm(O3);
				String gps = intent.getExtras().getString("text");
				//reenable after testing

				float temp = ((intent.getExtras().getFloat("temperature") - 0.5f) * 100.0f) * (9.0f/5.0f) + 32.0f;
				temp = Math.round(temp * 10) / 10.0f;

				float humid = ((intent.getExtras().getFloat("humidity") *161.0f / 3.3f) - 25.8f)/(1.0546f-.0026f*temp);
				humid = Math.round(humid *10) / 10.0f;

				SensorData incData = new SensorData(temp, humid, gps,NO2,O3);//change test back to gps with quotes
				buffer.add(incData);
				//Log.e("Added to buffer", "Added to buffer");
				if(threadCheck==true){
					bufferCleanup();
				}

			}
		}
	};
	private float O3ppm(float RSR0)

    {
           float ppm = 0;
           if (RSR0 < .105)           ppm = 10f;
           else if (RSR0 < .205)      ppm = 20f;
           else if (RSR0 < .315)      ppm = 30f;
           else if (RSR0 < .43) ppm = 40f;
           else if (RSR0 < .545)      ppm = 50f;
           else if (RSR0 < .65) ppm = 60f;
           else if (RSR0 < .75) ppm = 70f;
           else if (RSR0 < .85) ppm = 80f;
           else if (RSR0 < .95) ppm = 90f;
           else if (RSR0 < 1.45)      ppm = 100f;
           else if (RSR0 < 2.35)      ppm = 200f;
           else if (RSR0 < 3.2)       ppm = 300f;
           else if (RSR0 < 3.85)             ppm = 400f;
           else if (RSR0 < 4.4) ppm = 500f;
           else if (RSR0 < 4.85)      ppm = 600f;
           else if (RSR0 < 5.35)      ppm = 700f;
           else if (RSR0 < 5.85)      ppm = 800f;
           else if (RSR0 < 6.25)      ppm = 900f;
           else                              ppm = 1000f;
           return ppm;
    }
	private float NO2ppm(float RSR0)
    {
           float ppm = 0;
           if (RSR0 < .45)ppm = 0.05f;
           else if (RSR0 < .95) ppm = 0.1f;
           else if (RSR0 < 1.65)      ppm = 0.2f;
           else if (RSR0 < 2.3) ppm = 0.3f;
           else if (RSR0 < 2.9) ppm = 0.4f;
           else if (RSR0 < 3.6) ppm = 0.5f;
           else if (RSR0 < 4.3) ppm = 0.6f;
           else if (RSR0 < 4.8) ppm = 0.7f;
           else if (RSR0 < 5.45)      ppm = 0.8f;
           else if (RSR0 < 6.15)      ppm = 0.9f;
           else if (RSR0 < 8.2) ppm = 1f;
           else if (RSR0 < 15)        ppm = 2f;
           else if (RSR0 < 23.5)      ppm = 3f;
           else if (RSR0 < 31)        ppm = 4f;
           else                              ppm = 5f;
           return ppm;
    }
}
