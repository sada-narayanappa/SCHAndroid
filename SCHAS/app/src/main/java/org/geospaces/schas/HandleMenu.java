package org.geospaces.schas;

import android.app.Activity;
import android.view.MenuItem;

/**
 * Created by snarayan on 11/3/14.
 */
public class HandleMenu  {
    public static boolean onOptionsItemSelected(MenuItem item, Activity a ) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        int id = item.getItemId();
        switch (id) {
            case R.id.action_settings:
                return true;
            case R.id.action_test:
                a.setContentView(R.layout.activity_test);
                return true;
            case R.id.action_about:
                a.setContentView(R.layout.activity_welcome);
                return true;
        }
        return false;
    }
}
