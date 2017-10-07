package com.example.olegnazarkin.lingvodics;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.TextView;

import com.android.volley.VolleyError;

import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {


    // model
    private Fetcher fetcher;

    private String bearer;

    private static final String generalErrorMessage = "Error has occured";
    private static final String authFailedMessage = "Received authentication challenge is null";

    private String searchString;

    private boolean flagRetry;

    private static String prepareHtmlDataSchemeUrl(String htmlContent, String charset)
    {
        return "data:text/html;charset=" + charset + "," + htmlContent;
    }

    private class LingvoHandler implements LingvoResponseParser.LingvoDictionaryEntryHandler
    {
        public void consume(LingvoResponseParser.RootCollection rootCollection)
        {
            info.clear();
            titles.clear();

            int length = rootCollection.items.size();

            for(int i = 0; i < length; ++i)
            {
                LingvoResponseParser.ArticleModel am = rootCollection.items.get(i);

                String htmlContent = htmlStart + styleSheet + "<body>" + am.buildHtml() + htmlEnd;

                String dataSchemeUrl = prepareHtmlDataSchemeUrl(htmlContent, "ru");

                info.add(dataSchemeUrl);

                titles.add(am.Dictionary);
            }

            infoPagesAdapter.useInfoData(info, titles, true);
        }
    }

    private class FetchHandler implements Fetcher.FetchCallback
    {
        public void consume(String s)
        {
            flagCommandIssued = false;

            if(s != null) {

                LingvoResponseParser lrp = new LingvoResponseParser(new LingvoHandler());
                lrp.parse(s);
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

                String outMessage = generalErrorMessage;

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

                info.clear();
                titles.clear();

                String dataSchemeUrl = prepareHtmlDataSchemeUrl("<html><body><span>" + outMessage + "</span></body></html>", "ru");
                info.add(dataSchemeUrl);

                titles.add(generalErrorMessage);

                infoPagesAdapter.useInfoData(info, titles, true);
            }
        }
    }

    private FetchHandler fetchHandler = new FetchHandler();

    private boolean flagCommandIssued = false;

    // view
    private EditText searchStringEdit;

    private InfoPagesAdapter infoPagesAdapter;

    private static final String htmlStart = "<html lang=\"ru\"><meta charset=\"UTF-8\">";
    private static final String htmlEnd = "</body></html>";

    private static final String emptyInfo = htmlStart + "<body>Lingvo Dicks 1.0" + htmlEnd;
    private static String emptyInfoDataSchemeUrl = prepareHtmlDataSchemeUrl(emptyInfo, "ru");

    private static String styleSheet = buildStyleSheet();;

    private ArrayList<String> info = new ArrayList<>();
    private ArrayList<String> titles = new ArrayList<>();

    private static final String infoKey = "infoKey";
    private static final String titlesKey = "titlesKey";

    public class InfoPagesAdapter extends FragmentStatePagerAdapter
    {
        private ArrayList<String> info;
        private ArrayList<String> titles;

        private int infoLength;

        boolean flagUpdate = false;

        public InfoPagesAdapter(FragmentManager fm, ArrayList<String> info, ArrayList<String> titles)
        {
            super(fm);

            useInfoData(info, titles, false);
        }

        public void useInfoData(ArrayList<String> info, ArrayList<String> titles, boolean notify)
        {
            this.info = info;
            this.titles = titles;

            infoLength = info.size();

            if(notify)
            {
                flagUpdate = true;
                notifyDataSetChanged();
            }
        }

        @Override
        public Fragment getItem(int i)
        {
            Fragment f = new InfoPage();

            Bundle args = new Bundle();
            args.putString(InfoPage.infoKey, info.get(i));

            f.setArguments(args);

            return f;
        }

        @Override
        public int getCount()
        {
            return infoLength;
        }

        @Override
        public CharSequence getPageTitle(int position)
        {
            return titles.get(position);
        }

        @Override
        public int getItemPosition(Object object)
        {
            return flagUpdate ? POSITION_NONE : POSITION_UNCHANGED;
        }

        @Override
        public void finishUpdate(ViewGroup container)
        {
            super.finishUpdate(container);

            flagUpdate = false;
        }
    }

    public static class InfoPage extends Fragment
    {
        public static final String infoKey = "infoKey";

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
        {
            View rootView = inflater.inflate(R.layout.info_page, container, false);

            WebView wv = rootView.findViewById(R.id.htmlView);

            wv.loadUrl(getArguments().getString(infoKey));

            return rootView;
        }
    }

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

            String uriNormal = uri.replaceAll("\\s", "%20");

            fetcher.asyncFetch(uriNormal, headers, fetchHandler);

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
            if(actionId == EditorInfo.IME_ACTION_SEARCH)
            {
                onSearchCommand(tv);
            }

            return false;
        }
    }

    private static String buildStyleSheet()
    {
        String s = "<style>";

        HashMap<String, String> styles = new HashMap<>();

        styles.put("transcription", "color: brown;");
        styles.put("caption", "font-weight: bold; color: grey;");
        styles.put("abbrev", "color: grey;");
        styles.put("headline", "padding: 5px; background-color: beige;");
        styles.put("word", "font-weight: bold; color: darkslategrey;");
        styles.put("book", "font-style: italic; font-weight: bold; color: brown;");
        styles.put("sound", "color: blue;");
        styles.put("comment", "font-weight: lighter; color: grey;");
        styles.put("examples", "background-color: aliceblue;");
        styles.put("exampleitem", "font-weight: lighter; color: grey;");
        styles.put("example", "font-weight: lighter; color: grey;");
        styles.put("cardrefitem", "font-weight: lighter; color: cornflowerblue;");
        styles.put("cardref", "font-weight: lighter; color: cornflowerblue;");

        for (String key : styles.keySet())
        {
            s += "." + key + "{" + styles.get(key) + "}";
        }

        return s + "</style>";
    }

    private void prepareUiContent()
    {
        setContentView(R.layout.activity_main);

        searchStringEdit = (EditText) findViewById(R.id.searchStringEdit);
        searchStringEdit.setOnEditorActionListener(new EditorActionHandler());

        ViewPager viewPager = (ViewPager) findViewById(R.id.pagerView);

        infoPagesAdapter = new InfoPagesAdapter(getSupportFragmentManager(), info, titles);
        viewPager.setAdapter(infoPagesAdapter);
        viewPager.setOffscreenPageLimit(2);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        if(savedInstanceState == null)
        {
            info.add(emptyInfoDataSchemeUrl);
            titles.add("");
        }
        else
        {
            info = savedInstanceState.getStringArrayList(infoKey);
            titles = savedInstanceState.getStringArrayList(titlesKey);
        }

        ApplicationSingleton as = ApplicationSingleton.getInstance();

        as.setActivity(this);

        bearer = as.getProcessPersistentStore().get(ApplicationSingleton.bearerKey);

        if (bearer != null)
        {
            prepareUiContent();
        }

        fetcher = Fetcher.getInstance(as.getApplicationContext());
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);

        outState.putStringArrayList(infoKey, info);
        outState.putStringArrayList(titlesKey, titles);
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
}
