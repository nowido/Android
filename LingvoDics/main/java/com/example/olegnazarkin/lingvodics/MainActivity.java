package com.example.olegnazarkin.lingvodics;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.VolleyError;

import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    // model
    private ArrayList<String> infoData = new ArrayList<String>();

    private Fetcher fetcher;

    private String bearer;

    private final String authFailedMessage = "Received authentication challenge is null";

    private String searchString;

    private boolean flagRetry;

    private class FetchHandler implements Fetcher.FetchCallback
    {
        public void consume(String s)
        {
            flagCommandIssued = false;

            if(s != null) {

                LingvoResponseParser lrp = new LingvoResponseParser(s, new LingvoResponseParser.LingvoDictionaryEntryHandler() {
                    @Override
                    public void consume(LingvoResponseParser.RootCollection rootCollection) {

                        int length = rootCollection.items.size();

                        for(int i = 0; i < length; ++i)
                        {

                            LingvoResponseParser.ArticleModel am = rootCollection.items.get(i);

                            String info = am.Title + "; " + am.Dictionary + " [";

                            int innerLength = am.Body.size();

                            for(int j = 0; j < innerLength; ++j)
                            {
                                LingvoResponseParser.ArticleNode an = am.Body.get(j);

                                info += an.Node;

                                if(j < innerLength - 1)
                                {
                                    info += ", ";
                                }
                            }

                            info += "]";

                            infoData.add(info);
                        }

                        if(length > 0)
                        {
                            infoDataAdapter.notifyDataSetChanged();
                        }
                    }
                });
            }
        }

        public void consumeError(VolleyError err)
        {
            flagCommandIssued = false;

            if(err.networkResponse == null)
            {
                String message = err.getCause().getMessage();

                if(message.equals(authFailedMessage))
                {
                    if(!flagRetry)
                    {
                        flagRetry = true;
                        ApplicationSingleton.getInstance().obtainToken();
                    }
                }
            }
            else
            {
                int code = err.networkResponse.statusCode;

                // both "No translation ..." and "Dictionary not found"
                // have 404 response code;
                // so we may distinguish 404 causes as "N" or "D" first character

                String outMessage = "Error has occured";

                if(code == 404)
                {
                    String message = new String(err.networkResponse.data);

                    char firstChar = message.charAt(1);

                    if(firstChar == 'N')
                    {
                        outMessage = "No translation found";
                    }
                    else if(firstChar == 'D')
                    {
                        outMessage = "No dictionary found";
                    }
                }

                infoData.add(outMessage);
                infoDataAdapter.notifyDataSetChanged();
            }
        }
    }

    private FetchHandler fetchHandler = new FetchHandler();

    private boolean flagCommandIssued = false;

    // view
    private EditText searchStringEdit;
    private ArrayAdapter<String> infoDataAdapter;

    // controller

    private void executeCommandSearch()
    {
        if(!flagCommandIssued)
        {
            HashMap<String, String> headers = new HashMap<String, String>();

            headers.put("Authorization", "Bearer " + bearer);
            headers.put("Accept", "text/json");

            searchString = searchStringEdit.getText().toString();

            String uri = ApplicationSingleton.serviceUri + "api/v1/Translation?text=" +
                    searchString + "&srcLang=1033&dstLang=1049";

            fetcher.asyncFetch(uri, headers, fetchHandler);

            flagCommandIssued = true;
        }
    }

    public void onSearchCommand(View v)
    {
        InputMethodManager imm = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);

        imm.hideSoftInputFromWindow(searchStringEdit.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

        executeCommandSearch();
    }

    private class EditorActionHandler implements TextView.OnEditorActionListener
    {
        public boolean onEditorAction(TextView tv, int actionId, KeyEvent event)
        {
            if(actionId == EditorInfo.IME_ACTION_DONE)
            {
                executeCommandSearch();
            }

            return false;
        }
    }

    private void prepareUiContent()
    {
        setContentView(R.layout.activity_main);

        searchStringEdit = (EditText) findViewById(R.id.searchStringEdit);
        searchStringEdit.setOnEditorActionListener(new EditorActionHandler());

        infoDataAdapter = new ArrayAdapter<String>(this, R.layout.info_item, infoData);

        ListView lv = (ListView) findViewById(R.id.infoListView);
        lv.setAdapter(infoDataAdapter);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        ApplicationSingleton as = ApplicationSingleton.getInstance();

        as.setActivity(this);

        bearer = as.getProcessPersistentStore().get(ApplicationSingleton.bearerKey);

        if(bearer != null)
        {
            prepareUiContent();
        }

        fetcher = Fetcher.getInstance(as.getApplicationContext());
    }

    public void initialize(String bearer)
    {
        if(bearer == null)
        {
            // to do use another layout view
            return;
        }

        this.bearer = bearer;

        if(!flagRetry)
        {
            prepareUiContent();
        }
        else
        {
            flagRetry = false;
            executeCommandSearch();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);

        // save edit content and search info
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);

        ApplicationSingleton as = ApplicationSingleton.getInstance();

        bearer = as.getProcessPersistentStore().get(ApplicationSingleton.bearerKey);

        fetcher = Fetcher.getInstance(as.getApplicationContext());
    }
}

// to do restore edit content, list content