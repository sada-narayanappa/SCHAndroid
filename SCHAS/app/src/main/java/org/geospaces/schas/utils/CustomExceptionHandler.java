package org.geospaces.schas.utils;

import android.app.Activity;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;


/**
 * Created by Erik on 7/18/2016.
 */
public class CustomExceptionHandler extends Activity implements Thread.UncaughtExceptionHandler {

    private Thread.UncaughtExceptionHandler defaultHandler;

    //include more here if needed for file and server url, this just covers sending to

    public CustomExceptionHandler() {
        this.defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
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

        defaultHandler.uncaughtException(thread, ex);
    }
}
