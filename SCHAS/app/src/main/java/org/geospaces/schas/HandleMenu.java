package org.geospaces.schas;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.widget.Toast;

import org.geospaces.schas.Settings.LegalNotices;

import static android.support.v4.app.ActivityCompat.startActivityForResult;

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

//        if (GoogleMaps.class.isAssignableFrom(a.getClass())){
//            db.PrepareTextFile();
//        }

        switch (id) {
            case R.id.action_test:
                if(a.getClass() != Test.class) {
                    intent = new Intent(a, Test.class);
                    a.startActivity(intent);
                    return true;
                }
                Toast.makeText(a.getBaseContext(), "current activity", Toast.LENGTH_SHORT).show();
                return false;

            case R.id.action_settings_menu:
                if(a.getClass() != SettingsActivity.class) {
                    intent = new Intent(a, SettingsActivity.class);
                    a.startActivity(intent);
                    return true;
                }
                Toast.makeText(a.getBaseContext(), "current activity", Toast.LENGTH_SHORT).show();
                return false;
            case R.id.action_ForgetInhalerCap:
                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which){
                            case DialogInterface.BUTTON_NEGATIVE:
                                // Do Nothing
                                break;

                            case DialogInterface.BUTTON_POSITIVE:
                                // Clear the shared preference
                                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(UploadData.GetContext());
                                SharedPreferences.Editor editor = sharedPref.edit();
                                editor.remove("Familiar_MAC_Address");
                                editor.commit();
                                break;
                        }
                    }
                };

                AlertDialog.Builder builder = new AlertDialog.Builder(UploadData.GetContext());
                builder.setMessage("Are you sure you want to forget the Inhaler Cap?").setPositiveButton("Yes", dialogClickListener)
                        .setNegativeButton("No", dialogClickListener).show();
                return false;

            case R.id.action_UploadData:
                if(a.getClass() != UploadData.class) {
                    intent = new Intent(a, UploadData.class);
                    a.startActivity(intent);
                    return true;
                }
                Toast.makeText(a.getBaseContext(), "Current activity", Toast.LENGTH_SHORT).show();
                return false;

            case R.id.action_legal_menu:
                if(a.getClass() != LegalNotices.class) {
                intent = new Intent(a, LegalNotices.class);
                a.startActivity(intent);
                //a.setContentView(R.layout.activity_web);
                return true;
            }
                Toast.makeText(a.getBaseContext(), "current activity", Toast.LENGTH_SHORT).show();
                return false;

            case R.id.action_splash:
                if(a.getClass() != Welcome.class) {
                    intent = new Intent(a, Welcome.class);
                    a.startActivity(intent);
                    return true;
                }
            Toast.makeText(a.getBaseContext(), "Current activity", Toast.LENGTH_SHORT).show();
            return false;

            case R.id.action_patientData:
                if(a.getClass() != ViewPatientInhalerData.class) {
                    intent = new Intent(a, ViewPatientInhalerData.class);
                    a.startActivity(intent);
                    return true;
                }
            Toast.makeText(a.getBaseContext(), "current activity", Toast.LENGTH_SHORT).show();
            return false;

//            case R.id.action_step_counter:
//                if(a.getClass() != StepCounterView.class) {
//                    intent = new Intent(a, StepCounterView.class);
//                    a.startActivity(intent);
//                    return true;
//                }
//            Toast.makeText(a.getBaseContext(), "current activity", Toast.LENGTH_SHORT).show();
//            return false;

            case R.id.action_emailFirebaseToken:
                SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(a.getBaseContext());
                String username = SP.getString("username", "NA");
                String token = SP.getString("Firebase_Registration_Token","Not Generated");
                Intent mailIntent = new Intent();
                mailIntent.setData(Uri.parse("mailto:"));
                mailIntent.setAction(Intent.ACTION_SENDTO);
                mailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] {"schas.debug@gmail.com"});
                mailIntent.putExtra(Intent.EXTRA_SUBJECT, "Firebase Token ID");
                mailIntent.putExtra(Intent.EXTRA_TEXT, "Username: " + username + "\nFirebase Token: " + token);

                try
                {
                    startActivityForResult(a, Intent.createChooser(mailIntent, "Please choose a mail client to send the Firebase token from."), 1, new Bundle() );
                }
                catch (android.content.ActivityNotFoundException exception)
                {
                    Toast.makeText(a.getBaseContext(), "No email clients installed.", Toast.LENGTH_SHORT).show();
                }
        }
        return false;
    }
}
