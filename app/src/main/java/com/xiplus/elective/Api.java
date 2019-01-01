package com.xiplus.elective;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.JsonReader;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.CookieManager;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URL;
import java.net.URLEncoder;
import android.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import static android.content.Context.MODE_PRIVATE;


public class Api extends Application {
    private final String api = "https://xiplus.ddns.net/elective/api";
    private final String PREFS_NAME = "elective";
    private final String COOKIE_NAME = "elective_cookie";
    private Context context;

    public Api(Context context) {
        this.context = context;
    }

    private CookieManager getCookieManager() {
        CookieManager cookieManager = new CookieManager();
        CookieStore cookieStore = cookieManager.getCookieStore();

        SharedPreferences prefs = this.context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String temp = prefs.getString(COOKIE_NAME, "");
        Map<String,String> cookies = (Map<String,String>) this.unserialize(temp);

        for (Map.Entry<String,String> cookie : cookies.entrySet()) {
            cookieStore.add(null, new HttpCookie(cookie.getKey(), cookie.getValue()));
        }
        return cookieManager;
    }

    private void saveCookieManager(CookieManager cookieManager) {
        CookieStore cookieStore = cookieManager.getCookieStore();

        SharedPreferences.Editor editor = this.context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();

        Map<String,String> cookies3 = new LinkedHashMap<>();
        for (HttpCookie ck : cookieStore.getCookies()) {
            cookies3.put(ck.getName(), ck.getValue());
        }
        editor.putString(COOKIE_NAME, this.serialize(cookies3));
        editor.apply();
    }

    public String serialize(Map<String,String> obj) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream os = new ObjectOutputStream(bos);
            os.writeObject(obj);
            String result = Base64.encodeToString(bos.toByteArray(), Base64.DEFAULT);
            os.close();
            return result;
        } catch (IOException e) {
            System.out.println("serialize Error");
            e.printStackTrace();
            return "";
        }
    }

    private Map<String,String> unserialize(String str) {
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(Base64.decode(str, Base64.DEFAULT));
            ObjectInputStream oInputStream = new ObjectInputStream(bis);
            Map<String,String> res = (Map<String,String>) oInputStream.readObject();
            return res;
        } catch (Exception e) {
            System.out.println("unserialize Error");
            e.printStackTrace();
            return new LinkedHashMap<>();
        }
    }

    public JSONObject post(Map<String,Object> parm) {
        System.out.println("start api =" + parm.toString());

        StringBuilder postData = new StringBuilder();
        for (Map.Entry<String,Object> param : parm.entrySet()) {
            if (postData.length() != 0) postData.append('&');
            try {
                postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
                postData.append('=');
                postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                return this.error();
            }
        }

        try {
            CookieManager cookieManager = getCookieManager();
            CookieStore cookieStore = cookieManager.getCookieStore();

            URL url = new URL(api);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
            conn.setRequestProperty("Accept","application/json");
            for (HttpCookie cookie : cookieStore.getCookies()) {
                conn.setRequestProperty("Cookie", cookie.getName() + "=" + cookie.getValue());
            }
            conn.setDoOutput(true);
            conn.setDoInput(true);

            DataOutputStream os = new DataOutputStream(conn.getOutputStream());
            os.writeBytes(postData.toString());
            os.flush();
            os.close();

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            List<String> cookies2 = conn.getHeaderFields().get("Set-Cookie");
            if (cookies2 != null) {
                for (String cookie : cookies2) {
                    cookieStore.add(null, HttpCookie.parse(cookie).get(0));
                }
            }

            conn.disconnect();

            System.out.println("response=" + response.toString());

            JSONObject reader = new JSONObject(response.toString());

            this.saveCookieManager(cookieManager);

            return reader;

        } catch (IOException e) {
            e.printStackTrace();
            return this.error();
        } catch (JSONException e) {
            e.printStackTrace();
            return this.error();
        }
    }

    public JSONObject error() {
        JSONObject result = new JSONObject();
        try {
            result.put("result", "failed");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return result;
    }
}
