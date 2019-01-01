package com.xiplus.elective;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.xiplus.elective.User;

import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private TextView mLoginTextView;
    private Button mLoginButton;
    private Button mLogoutButton;
    private User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.user = new User(getApplicationContext());
        mLoginTextView = findViewById(R.id.loginText);
        mLoginButton = findViewById(R.id.btn_login);
        mLogoutButton = findViewById(R.id.btn_logout);
        new checkLogin(user).execute((Void) null);
    }

    @Override
    protected void onStart() {
        System.out.println("Main onStart");
        super.onStart();

        new checkLogin(user).execute((Void) null);
    }

    public void goElective(View view) {
        Intent intent = new Intent(MainActivity.this, ElectiveActivity.class);
        startActivity(intent);
    }

    public void goLogin(View view) {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
    }

    public void goLogout(View view) {
        new userLogout(user).execute((Void) null);
    }

    public class checkLogin extends AsyncTask<Void, Void, Void> {
        private User user;

        checkLogin(User user) {
            this.user = user;
        }

        @Override
        protected Void doInBackground(Void... temp) {
            this.user.checkLogin();
            return null;
        }

        @Override
        protected void onPostExecute(Void temp) {
            if (this.user.isLogin) {
                mLoginTextView.setText(String.format("已登入：(%s) %s / %s", this.user.accttypename, this.user.account, this.user.name));
                mLoginButton.setVisibility(View.GONE);
                mLogoutButton.setVisibility(View.VISIBLE);
            } else {
                mLoginTextView.setText("未登入");
                mLoginButton.setVisibility(View.VISIBLE);
                mLogoutButton.setVisibility(View.GONE);
            }
        }
    }

    public class userLogout extends AsyncTask<Void, Void, JSONObject> {
        private User user;

        userLogout(User user) {
            this.user = user;
        }

        @Override
        protected JSONObject doInBackground(Void... temp) {
            Map<String,Object> parm = new LinkedHashMap<>();
            parm.put("action", "logout");
            return new Api(getApplicationContext()).post(parm);
        }

        @Override
        protected void onPostExecute(JSONObject res) {
            final String result = (String) res.opt("result");

            switch (result) {
                case "success":
                    mLoginTextView.setText("未登入");
                    mLoginButton.setVisibility(View.VISIBLE);
                    mLogoutButton.setVisibility(View.GONE);
                    Toast.makeText(getApplicationContext(), "已登出", Toast.LENGTH_SHORT).show();
                    break;

                case "failed":

            }
        }
    }
}