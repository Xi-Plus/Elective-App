package com.xiplus.elective;

import android.content.Context;
import android.widget.Toast;

import com.xiplus.elective.Api;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;

public class User {
    boolean isLogin = false;
    String accttype = "";
    private Context context;

    public User(Context context) {
        this.context = context;
//        this.checkLogin();
    }

    public Boolean checkLogin() {
        Map<String,Object> parm = new LinkedHashMap<>();
        parm.put("action", "checklogin");
        JSONObject res = new Api(context).post(parm);
        this.isLogin = (Boolean) res.opt("result");
        return this.isLogin;
    }
}
