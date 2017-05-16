package org.geospaces.schas;

import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import org.geospaces.schas.Adapters.NotificationListAdapter;
import org.geospaces.schas.UtilityObjectClasses.FBNotification;
import org.geospaces.schas.utils.db;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DeviceNotificationLog extends AppCompatActivity {
    ListView notificationListView;
    List<FBNotification> notifications;
    ShareActionProvider mActionProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_notification_log);

        notifications = new ArrayList<FBNotification>();
        try {
            notifications = db.readNotificationsFromFile();
        } catch (IOException e) {
            e.printStackTrace();
            Log.i("notifications activity", "could not read notifications from file");
        }

        notificationListView = (ListView) findViewById(R.id.notificationLogList);
        NotificationListAdapter adapter = new NotificationListAdapter(this, notifications);
        notificationListView.setAdapter(adapter);
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
}
