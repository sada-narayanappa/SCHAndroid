package org.geospaces.schas.utils;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import java.io.File;
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
    //a new blank email dedicated for this debugging purpose

    public CustomExceptionHandler() {
        this.emailDestination = "schas.debug@gmail.com";
        this.emailSource = "web@gmail.com";
        properties = System.getProperties();
        properties.setProperty("mail.smtp.host", "smtp.gmail.com");
        this.defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        //add current date and time formatted for me here
        final Writer result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);
        ex.printStackTrace(printWriter);
        String stacktrace = result.toString();
        printWriter.close();
        String filename = "DEBUG_STACKTRACE.txt";

        //use db static method to write to file
        try {
            db.WriteToFile(filename, stacktrace);
        } catch (IOException e) {
            e.printStackTrace();
        }

        File file = db.getFile(filename);
        Uri uri = Uri.fromFile(file);

        //send to email here
        Intent mailIntent = new Intent();
        mailIntent.setAction(Intent.ACTION_SENDTO);
        mailIntent.setType("plain/text");
        mailIntent.putExtra(Intent.EXTRA_EMAIL, "schas.debug@gmail.com");
        mailIntent.putExtra(Intent.EXTRA_SUBJECT, "Application has Crashed!");
        mailIntent.putExtra(Intent.EXTRA_TEXT, "There has been a crash within the android application," +
                "please fix this issue Mr. Dev!");
        mailIntent.putExtra(Intent.EXTRA_STREAM, uri);

        try {
            startActivity(Intent.createChooser(mailIntent, "Please Choose a Mail Client"));
        }
        catch(android.content.ActivityNotFoundException exception){
            Log.i("CustomExceptionHandler", exception.toString());
        }

        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(10);
    }
}
