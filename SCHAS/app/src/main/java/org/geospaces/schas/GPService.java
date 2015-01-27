package org.geospaces.schas;

import android.app.IntentService;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class GPService extends IntentService {
    public GPService() {
        super("GPService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        for (int i=0; i<5; i++) {
            Log.i("SimpleWakefulReceiver", "Running service " + (i + 1)
                    + "/5 @ " + SystemClock.elapsedRealtime());
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
            }
        }
        Log.i("SimpleWakefulReceiver", "Completed service @ " + SystemClock.elapsedRealtime());
       // GPSWakfulReciever.completeWakefulIntent(intent);
    }

}
