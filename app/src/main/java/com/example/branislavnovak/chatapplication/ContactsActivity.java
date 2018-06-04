package com.example.branislavnovak.chatapplication;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;


public class ContactsActivity extends AppCompatActivity implements View.OnClickListener, ServiceConnection {

    private Button bLogout, bRefresh;
    private ListView list;
    private ChatDbHelper chatDbHelper;
    private Contact[] contacts;
    public static final String PREFERENCES_NAME = "PreferenceFile";
    public String userId;
    public ContactsAdapter adapter;

    private String contact_to_delete;
    private HttpHelper httphelper;
    private Handler handler;
    private static String BASE_URL = "http://18.205.194.168:80";
    private static String CONTACTS_URL = BASE_URL + "/contacts";
    private static String LOGOUT_URL = BASE_URL + "/logout";
    private static String DELETE_URL = BASE_URL + "/contacts/";

    private INotificationBinder mService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);

        bLogout = findViewById(R.id.logoutButton);
        bRefresh = findViewById(R.id.refreshButton);
        list = findViewById(R.id.listOfContacts);
        bLogout.setOnClickListener(this);
        bRefresh.setOnClickListener(this);

        // new chatDataBase instance and reading contacts from database
        // chatDbHelper = new ChatDbHelper();
        // contacts = chatDbHelper.readContacts();

        // Getting logged user userid, from SharedPreference file
        SharedPreferences sharedPref = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE);
        userId = sharedPref.getString("senderId", null);
        adapter = new ContactsAdapter(this);

        list.setAdapter(adapter);
        adapter.updateContacts(contacts);

        httphelper = new HttpHelper();
        handler = new Handler();

        bindService(new Intent(ContactsActivity.this, NotificationService.class), this, Context.BIND_AUTO_CREATE);

        list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, final int i, long l) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        JSONObject jsonObject = new JSONObject();
                        Contact contact_model = (Contact) adapter.getItem(i);
                        contact_to_delete = contact_model.getmUserName();

                        try{
                            jsonObject.put("username", contact_to_delete);

                            final boolean success = httphelper.httpDeleteContact(ContactsActivity.this, (DELETE_URL+contact_to_delete), jsonObject);

                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if(success){
                                        adapter.removeContact(i);
                                        updateContactList();
                                    }else{
                                        Toast.makeText(ContactsActivity.this, "Cannot delete user", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });

                        } catch (JSONException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }
                }).start();

                return true;
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mService != null) {
            unbindService(this);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.logoutButton:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            final boolean success = httphelper.logOutUserFromServer(ContactsActivity.this, LOGOUT_URL);

                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (success) {
                                        Intent i = new Intent(ContactsActivity.this, MainActivity.class);
                                        startActivity(i);
                                    } else {
                                        Toast.makeText(ContactsActivity.this, getText(R.string.cannot_logout_error), Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });

                        } catch (JSONException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();

                break;
            case R.id.nextButton:
                Intent i2 = new Intent(this, MessageActivity.class);
                startActivity(i2);
                break;
            case R.id.refreshButton:
                updateContactList();
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Deleting logged user from contacts list
        //deleteLoggedUserFromList();
        //updateContactList();
    }

    // Updating contacts list
    public void updateContactList(){
        new Thread(new Runnable() {
            Contact[] allContacts;
            @Override
            public void run() {
                try{
                    final JSONArray contacts = httphelper.getContactsFromServer(ContactsActivity.this, CONTACTS_URL);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if(contacts != null){
                                JSONObject jsonContact;
                                allContacts = new Contact[contacts.length()];

                                for (int i = 0; i < contacts.length(); i++){
                                    try{
                                        jsonContact = contacts.getJSONObject(i);
                                        allContacts[i] = new Contact(jsonContact.getString("username"));
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                                adapter.updateContacts(allContacts);
                            }
                        }
                    });

                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }).start();

    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        mService = INotificationBinder.Stub.asInterface(iBinder);
        try {
            mService.setCallback(new NotificationCallback());
        } catch (RemoteException e) {
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        mService = null;
    }


    private class NotificationCallback extends INotificationCallback.Stub {

        @Override
        public void onCallbackCall() throws RemoteException {

            final HttpHelper httpHelper = new HttpHelper();
            final Handler handler = new Handler();

            final NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getApplicationContext(), null)
                    .setSmallIcon(R.drawable.ic_launcher_background)
                    .setLargeIcon(BitmapFactory.decodeResource(getApplicationContext().getResources(),
                            R.mipmap.ic_launcher))
                    .setContentTitle(getText(R.string.app_name))
                    .setContentText(getText(R.string.have_new_message))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);

            final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());


            new Thread(new Runnable() {
                public void run() {
                    try {
                        final boolean response = httpHelper.getNotification(ContactsActivity.this);

                        handler.post(new Runnable() {
                            public void run() {
                                if (response) {
                                    // notificationId is a unique int for each notification that you must define
                                    notificationManager.notify(2, mBuilder.build());
                                }
                            }
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }
}
