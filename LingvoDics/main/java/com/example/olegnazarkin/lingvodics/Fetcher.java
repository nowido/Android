package com.example.olegnazarkin.lingvodics;

import android.content.Context;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.UnsupportedEncodingException;
import java.util.Map;

public class Fetcher {

    public interface FetchCallback
    {
        void consume(String s);
        void consumeError(VolleyError err);
    }

    private static Fetcher instance;
    private static Context ctx;
    private RequestQueue queue;

    //
    private class ResponseListener implements Response.Listener<String>
    {
        private FetchCallback callback;

        public ResponseListener(FetchCallback callback)
        {
            this.callback = callback;
        }

        public void onResponse(String s)
        {
            callback.consume(s);
        }
    }

    private class ErrorListener implements Response.ErrorListener
    {
        private FetchCallback callback;

        public ErrorListener(FetchCallback callback)
        {
            this.callback = callback;
        }

        public void onErrorResponse(VolleyError err) { callback.consumeError(err); }
    }

    //
    public Fetcher(Context context)
    {
        ctx = context;
        queue = getRequestQueue();
    }

    public RequestQueue getRequestQueue()
    {
        if(queue == null)
        {
            queue = Volley.newRequestQueue(ctx.getApplicationContext());
        }
        return queue;
    }

    public static synchronized Fetcher getInstance(Context ctx)
    {
        if(instance == null)
        {
            instance = new Fetcher(ctx);
        }
        return instance;
    }

    public void asyncFetch(String uri, FetchCallback callback)
    {
        queue.add(new StringRequest(Request.Method.GET, uri,
                new ResponseListener(callback), new ErrorListener(callback)));
    }

    public void asyncFetch(String uri, final Map<String, String> headers, FetchCallback callback)
    {
        queue.add(new StringRequest(Request.Method.GET, uri,
                new ResponseListener(callback), new ErrorListener(callback))
        {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError
            {
                return headers;
            }
        });
    }

    public void asyncPost(String uri, final Map<String, String> headers, final String body, FetchCallback callback)
    {
        queue.add(new StringRequest(Request.Method.POST, uri,
                new ResponseListener(callback), new ErrorListener(callback))
        {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError
            {
                return headers;
            }

            @Override
            public byte[] getBody() throws AuthFailureError
            {
                try {
                    return (body == null) ? null : body.getBytes("utf-8");
                }
                catch (UnsupportedEncodingException e)
                {
                    return null;
                }
            }
        });
    }
}
