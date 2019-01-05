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
    String error = "";
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
        if (res.optString("result").equals("ok")) {
            JSONObject data = res.optJSONObject("data");
            this.isLogin = data.optBoolean("islogin");
            this.error = "";
            if (this.isLogin) {
                this.account = data.optString("account");
                this.accttype = data.optString("accttype");
                if (this.accttype.equals("student")) {
                    this.accttypename = "學生";
                } else {
                    this.accttypename = "管理員";
                }
                this.name = data.optString("name");
            } else {
                this.accttype = "";
            }
        } else {
            this.isLogin = false;
            this.error = res.optString("result", "");
            this.accttype = "";
        }
        return this.isLogin;
    }
}
