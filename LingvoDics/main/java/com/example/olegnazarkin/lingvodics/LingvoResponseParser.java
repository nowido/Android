package com.example.olegnazarkin.lingvodics;

import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

class LingvoResponseParser
{
    public interface LingvoDictionaryEntryHandler
    {
        void consume(RootCollection rootCollection);
    }

    public class ArticleNode
    {
        public String Node;
        public String Text;
        public boolean IsOptional;

        public ArticleNode(JSONObject entry)
        {
            try
            {
                Node = entry.getString("Node");
                Text = entry.getString("Text");
                IsOptional = entry.getBoolean("IsOptional");
            }
            catch (JSONException e){}
        }
    }

    public class ArticleModel
    {
        public String Title;
        public String Dictionary;
        public List<ArticleNode> Body = new ArrayList<>();

        public ArticleModel(JSONObject entry)
        {
            try {
                Title = entry.getString("Title");
                Dictionary = entry.getString("Dictionary");

                JSONArray bc = entry.getJSONArray("Body");

                int length = bc.length();

                for(int i = 0; i < length; ++i)
                {
                    Body.add(new ArticleNode(bc.getJSONObject(i)));
                }
            }
            catch (JSONException e){}
        }
    }

    public class RootCollection
    {
        public List<ArticleModel> items = new ArrayList<>();
    }

    private LingvoDictionaryEntryHandler handler;

    private class ParserTask extends AsyncTask<String, Integer, RootCollection>
    {
        private LingvoDictionaryEntryHandler handler;

        public ParserTask(LingvoDictionaryEntryHandler handler)
        {
            this.handler = handler;
        }

        protected RootCollection doInBackground(String... jsonItems)
        {
            String json = jsonItems[0];

            RootCollection collection = new RootCollection();

            try
            {
                JSONArray rc = new JSONArray(json);

                int length = rc.length();

                for(int i = 0; i < length; ++i)
                {
                    collection.items.add(new ArticleModel(rc.getJSONObject(i)));
                }
            }
            catch (JSONException e)
            {}

            return collection;
        }

        protected void onPostExecute(RootCollection result)
        {
            if(handler != null)
            {
                handler.consume(result);
            }
        }
    }

    public LingvoResponseParser(String json, LingvoDictionaryEntryHandler handler)
    {
        ParserTask pt = new ParserTask(handler);
        pt.execute(json);
    }
}
