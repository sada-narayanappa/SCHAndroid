package org.geospaces.schas;

import android.app.Activity;
import android.content.Intent;
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
        Intent intent = null;

        switch (id) {
            case R.id.action_settings:
                return true;
            case R.id.action_test:
                intent = new Intent(a, Test.class);
                a.startActivity(intent);
                //a.setContentView(R.layout.activity_test);
                return true;
            case R.id.action_about:
                intent = new Intent(a, Welcome.class);
                a.startActivity(intent);
                return true;
            case R.id.action_web:
                intent = new Intent(a, Web.class);
                a.startActivity(intent);
                //a.setContentView(R.layout.activity_web);
                return true;
        }
        return false;
    }
}
