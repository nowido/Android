package com.example.olegnazarkin.lingvodics;

import android.app.Application;
import android.content.SharedPreferences;

import com.android.volley.VolleyError;

import java.util.Date;
import java.util.HashMap;

public class ApplicationSingleton extends Application
{
    private static final String preferencesKey = "app_preferences";

    public static final String serviceUri = "https://developers.lingvolive.com/";
    public static final String apiKey = "MjExMDAwMGUtMmQwZC00N2NmLTkzZTgtNGUwMGE4MGI5MDNmOjBlZDZkYWJkNzlkZTQwNTA4Yzg3NWFjYjBiOWU5Zjc3";

    public static final String bearerKey = "bearer";

    private static final String bearerTimeKey = "bearerTimeKey";
    private static final long dayLengthMs = 24 * 60 * 60 * 1000;

    private static ApplicationSingleton instance;

    private MainActivity activity;

    private HashMap<String, String> processPersistentStore = new HashMap<>();

    private String bearer;

    private void readSettings()
    {
        SharedPreferences sharedPreferences = getSharedPreferences(preferencesKey, MODE_PRIVATE);

        // bearer lives one day only; so check up if it is up to date

        long savedBearerTime = sharedPreferences.getLong(bearerTimeKey, 0);

        if(savedBearerTime != 0)
        {
            long currentTime = new Date().getTime();

            if(currentTime - savedBearerTime < dayLengthMs)
            {
                bearer = sharedPreferences.getString(bearerKey, null);

                if(bearer != null)
                {
                    processPersistentStore.put(bearerKey, bearer);
                }
            }
        }
    }

    public void obtainToken()
    {
        HashMap<String, String> headers = new HashMap<String, String>();

        headers.put("Authorization", "Basic " + apiKey);

        Fetcher fetcher = Fetcher.getInstance(this.getApplicationContext());

        fetcher.asyncPost(serviceUri + "api/v1.1/authenticate", headers, "", new Fetcher.FetchCallback() {
            @Override
            public void consume(String s) {

                bearer = s;

                if(bearer != null)
                {
                    SharedPreferences sharedPreferences = getSharedPreferences(preferencesKey, MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putLong(bearerTimeKey, new Date().getTime());
                    editor.putString(bearerKey, bearer);
                    editor.commit();

                    processPersistentStore.put(bearerKey, bearer);
                }

                if(activity != null)
                {
                    activity.initialize(bearer);
                }
            }

            @Override
            public void consumeError(VolleyError err){}
        });
    }

    public static ApplicationSingleton getInstance()
    {
        return instance;
    }

    @Override
    public void onCreate()
    {
        super.onCreate();

        instance = this;

        readSettings();

        if(bearer == null)
        {
            obtainToken();
        }
        else if(activity != null)
        {
            activity.initialize(bearer);
        }
    }

    public void setActivity(MainActivity a)
    {
        activity = a;
    }

    public HashMap<String, String> getProcessPersistentStore()
    {
        return processPersistentStore;
    }
}
