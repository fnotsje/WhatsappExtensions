package com.suraj.waext;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.HashMap;

public class ReminderActivity extends Activity {

    private static HashMap<String, String> contactHashMap;
    private String contactName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        if (contactHashMap == null) {
            contactHashMap = getContactsHashMap();
        }

        Bundle bundle = getIntent().getExtras();

        if (bundle != null) {
            String contactNumber = bundle.getString("contactNumber");

            //if launched for setting up reminder
            if (contactNumber != null) {
                contactName = contactHashMap.get(contactNumber);
                setContentView(R.layout.activity_reminder);
                setUpForReminder();

            }//if launched for reminding user
            else {
                setContentView(R.layout.activity_reminder_open);
                contactName = bundle.getString("contactName");

                setUpForOpen();
            }
        } else {
            Toast.makeText(getApplicationContext(), R.string.bundle_null_error, Toast.LENGTH_SHORT).show();
        }

        startService(new Intent(ReminderActivity.this, ReminderService.class));

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ReminderService.jobsRemaining == 0) {
            Intent stopIntent = new Intent(getApplicationContext(), ReminderService.class);
            stopIntent.putExtra("stopService", true);
            getApplicationContext().startService(stopIntent);
        }
    }

    private void setUpForOpen() {
        ((TextView) (findViewById(R.id.tvreminderwhom))).setText(getString(R.string.reply_to,contactName));

        (findViewById(R.id.btnremindercancel)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ReminderActivity.this.finish();
            }
        });


        (findViewById(R.id.btnreminderok)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent newIntent = new Intent();
                newIntent.setComponent(new ComponentName("com.whatsapp", "com.whatsapp.Main"));
                newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                newIntent.putExtra("displayname", contactName);
                getApplicationContext().startActivity(newIntent);
                ReminderActivity.this.finish();

            }
        });
    }

    private void setUpForReminder() {
        (findViewById(R.id.btnremindercancel)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ReminderActivity.this.finish();
            }
        });


        (findViewById(R.id.btnreminderok)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final long timer = getTimer(((Spinner) findViewById(R.id.spinreminders)).getSelectedItemPosition());

                ReminderService.jobsRemaining++;

                ReminderService.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String contactName = ReminderActivity.this.contactName;
                            Thread.sleep(timer);
                            Intent intent = new Intent();
                            intent.setAction("com.suraj.waext.ReminderIntent");
                            intent.putExtra("contactName", contactName);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            sendBroadcast(intent);

                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                    }
                });

                Thread thread = new Thread() {
                    @Override
                    public void run() {

                    }
                };

                //thread.setDaemon(true);
                //thread.start();
                ReminderActivity.this.finish();
            }
        });
    }

    private long getTimer(int selectedItemPosition) {
        switch (selectedItemPosition) {
            case 0:
                return 15 * 60 * 1000;
            case 1:
                return 30 * 60 * 1000;
            case 2:
                return 60 * 60 * 1000;
            case 3:
                return 2 * 60 * 60 * 1000;
        }

        return 0;
    }

    public HashMap<String, String> getContactsHashMap() {
        Process process = null;
        Runtime runtime = Runtime.getRuntime();
        OutputStreamWriter outputStreamWriter;

        HashMap<String, String> hashMap = new HashMap<>();

        try {

            String command = "/data/data/com.whatsapp/databases/wa.db 'Select display_name, jid FROM wa_contacts WHERE is_whatsapp_user=1';";
            process = runtime.exec("su");

            outputStreamWriter = new OutputStreamWriter(process.getOutputStream());

            outputStreamWriter.write("sqlite3 " + command);

            outputStreamWriter.flush();
            outputStreamWriter.close();
            outputStreamWriter.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            process.waitFor();

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String s;
            StringBuffer op = new StringBuffer();

            while ((s = bufferedReader.readLine()) != null) {
                op.append(s + "\n");
            }

            String arr[] = op.toString().split("\n");

            Arrays.sort(arr);

            for (String contact : arr) {
                String potential[] = contact.split("\\|");
                //Log.i("con", potential[0]);

                if (potential.length < 2)
                    continue;

                hashMap.put(potential[1].split("@")[0], potential[0]);

            }


        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return hashMap;

    }

}
