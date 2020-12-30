package com.ffyytt.phonecall;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Base64;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class ModelServer extends Model {
    private Context _context;
    ModelServer(Context context)
    {
        _context = context;
    }
    public String predict(Bitmap bitmap)
    {
        RequestQueue queue = Volley.newRequestQueue(_context);
        String url ="http://192.168.5.10:5555";
        final String str_bmp = getStringImage(bitmap);
        final String[] result = {""};
        StringRequest stringRequest = new StringRequest(Request.Method.POST,
                url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        result[0] = response.toString();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                result[0] = "-1";
            }

        }) {
            @Override
            protected Map<String, String> getParams()
                    throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("image", str_bmp);
                return params;
            }
        };
        queue.add(stringRequest);
        return result[0];
    }
    public String getStringImage(Bitmap bmp) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] imageBytes = baos.toByteArray();
        String encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT);
        return encodedImage;
    }
}
