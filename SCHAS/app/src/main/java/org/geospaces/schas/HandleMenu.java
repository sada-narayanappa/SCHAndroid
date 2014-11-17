package org.geospaces.schas;

import android.app.Activity;
import android.content.Intent;
import android.view.Gravity;
import android.view.MenuItem;
import android.widget.Toast;

/**
 * Created by snarayan on 11/3/14.
 */

//Test
public class HandleMenu  {
    public static boolean onOptionsItemSelected(MenuItem item, Activity a ) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        int id = item.getItemId();
        Intent intent = null;

        switch (id) {
            case R.id.action_settings:
                if(a.getClass() != BluetoothSettings.class) {
                    intent = new Intent(a, BluetoothSettings.class);
                    a.startActivity(intent);
                    return true;
                }
                Toast.makeText(a.getBaseContext(), "current activity", Toast.LENGTH_SHORT).show();
                return false;

            case R.id.action_test:
                if(a.getClass() != Test.class) {
                    intent = new Intent(a, Test.class);
                    a.startActivity(intent);
                    return true;
                }
                Toast.makeText(a.getBaseContext(), "current activity", Toast.LENGTH_SHORT).show();
                return false;

            case R.id.action_about:
                if(a.getClass() != Welcome.class){
                    intent = new Intent(a, Welcome.class);
                    a.startActivity(intent);
                    return true;
                }
                Toast.makeText(a.getBaseContext(), "Current activity", Toast.LENGTH_SHORT).show();
                return false;
            case R.id.action_web:
                if(a.getClass() != Web.class) {
                    intent = new Intent(a, Web.class);
                    a.startActivity(intent);
                    //a.setContentView(R.layout.activity_web);
                    return true;
                }
                Toast.makeText(a.getBaseContext(), "Current activity", Toast.LENGTH_SHORT).show();
                return false;
            }
        return false;
    }
}
