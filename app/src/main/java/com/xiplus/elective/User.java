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
    String account = "";
    String name = "";
    String accttype = "";
    String accttypename = "";
    private Context context;

    public User(Context context) {
        this.context = context;
    }

    public Boolean checkLogin() {
        Map<String,Object> parm = new LinkedHashMap<>();
        parm.put("action", "checklogin");
        JSONObject res = new Api(context).post(parm);
        this.isLogin = res.optBoolean("islogin");
        if (this.isLogin) {
            this.account = res.optString("account");
            this.accttype = res.optString("accttype");
            if (this.accttype.equals("student")) {
                this.accttypename = "學生";
            } else {
                this.accttypename = "管理員";
            }
            this.name = res.optString("name");
        } else {
            this.accttype = "";
        }
        return this.isLogin;
    }
}
