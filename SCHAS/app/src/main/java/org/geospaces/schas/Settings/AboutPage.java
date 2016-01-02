package org.geospaces.schas.Settings;


import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import org.geospaces.schas.R;

public class AboutPage extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        //replace the message text with whatever we want to say on the about page, needs to be passed in from the welcome activity or upload data activity?
        TextView info = (TextView) findViewById(R.id.info_text);
        String message = "This is where all of the text about the phone \n and application will go!";
        info.setText(message);
    }
}
