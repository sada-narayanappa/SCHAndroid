package org.geospaces.schas.utils;

import android.app.Activity;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Properties;


/**
 * Created by Erik on 7/18/2016.
 */
public class CustomExceptionHandler extends Activity implements Thread.UncaughtExceptionHandler {

    private Thread.UncaughtExceptionHandler defaultHandler;

    private String emailDestination;

    private String emailSource;

    Properties properties;

    //include more here if needed for file and server url, this just covers sending to

    public CustomExceptionHandler() {
//        this.emailDestination = "schas.debug@gmail.com";
//        this.emailSource = "web@gmail.com";
//        properties = System.getProperties();
//        properties.setProperty("mail.smtp.host", "smtp.gmail.com");
        //this.defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        String filename = "DEBUG_STACKTRACE.txt";
        //add current date and time formatted for me here
        final Writer result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);
        ex.printStackTrace(printWriter);
        String stacktrace = result.toString();
        printWriter.close();

        //use db static method to write to file
        try {
            db.WriteToFile(filename, stacktrace);
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.exit(2);
    }
}
