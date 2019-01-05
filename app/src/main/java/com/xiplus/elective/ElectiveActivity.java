package com.xiplus.elective;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ElectiveActivity extends AppCompatActivity {

    private TextView mLoginTextView;
    private Spinner mDaySpinner;
    private Spinner mPeriodSpinner;
    private TableLayout mSearchResult;
    private TableLayout mElectiveResult;
    private TableLayout mCalendarResult;
    private View mProgressView;

    private User user;
    private final String[] dayString = new String[]{"", "1", "2", "3", "4", "5", "6", "7"};
    private final String[] periodString = new String[]{"", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13"};
    private Map<View, String> searchClassid = new LinkedHashMap<>();
    private Map<View, String> electiveClassid = new LinkedHashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_elective);

        mLoginTextView = findViewById(R.id.loginText);
        mDaySpinner = findViewById(R.id.select_day);
        mPeriodSpinner = findViewById(R.id.select_period);
        mSearchResult = findViewById(R.id.search_result);
        mElectiveResult = findViewById(R.id.elective_table);
        mCalendarResult = findViewById(R.id.calendar_table);
        mProgressView = findViewById(R.id.search_progress);

        this.user = new User(getApplicationContext());
        new ElectiveActivity.checkLogin(user).execute((Void) null);
        showProgress(true);
        new ElectiveActivity.showElective(user).execute((Void) null);
    }

    public void goSearch() {
        String day = dayString[mDaySpinner.getSelectedItemPosition()];
        String period = periodString[mPeriodSpinner.getSelectedItemPosition()];

        showProgress(true);
        new ElectiveActivity.doSearch(day, period, user).execute((Void) null);
    }

    public void goSearch(View view) {
        goSearch();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
        mProgressView.animate().setDuration(shortAnimTime).alpha(
                show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
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
            } else {
                switch (this.user.error) {
                    case "no_network":
                    case "timeout":
                        mLoginTextView.setText("沒有網路連線");
                        break;

                    default:
                        mLoginTextView.setText("未登入");
                }
            }
        }
    }

    public class doSearch extends AsyncTask<Void, Void, JSONObject> {
        private String day;
        private String period;
        private User user;

        doSearch(String day, String period, User user) {
            this.day = day;
            this.period = period;
            this.user = user;
        }

        @Override
        protected JSONObject doInBackground(Void... temp) {
            Map<String,Object> parm = new LinkedHashMap<>();
            parm.put("action", "search");
            parm.put("day", this.day);
            parm.put("period", this.period);
            JSONObject res = new Api(getApplicationContext()).post(parm);
            user.checkLogin();
            return res;
        }

        @Override
        protected void onPostExecute(JSONObject res) {
            if (!res.optString("result").equals("ok")) {
                switch (res.optString("result", "")) {
                    case "no_network":
                        Toast.makeText(getApplicationContext(), "查詢失敗，請檢查網路連線", Toast.LENGTH_SHORT).show();
                        break;

                    case "timeout":
                        Toast.makeText(getApplicationContext(), "查詢失敗，連線逾時，請重試", Toast.LENGTH_SHORT).show();
                        break;

                    default:
                        Toast.makeText(getApplicationContext(), "查詢失敗，未知錯誤", Toast.LENGTH_SHORT).show();
                        break;
                }
                showProgress(false);
                return;
            }
            JSONObject classes = res.optJSONObject("data");
            mSearchResult.removeAllViewsInLayout();
            Iterator<String> classids = classes.keys();

            TableRow.LayoutParams params = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, 100);
            params.setMargins(0, 0, 20, 0);
            TableRow row;
            TextView tv;

            row = new TableRow(ElectiveActivity.this);
            if (classids.hasNext()) {
                if (user.isLogin) {
                    tv = new TextView(ElectiveActivity.this);
                    tv.setText("選課");
                    row.addView(tv, params);
                }
                for (String col : new String[]{"編號", "名稱", "學分數", "時間"}) {
                    tv = new TextView(ElectiveActivity.this);
                    tv.setText(col);
                    row.addView(tv, params);
                }
            } else {
                tv = new TextView(ElectiveActivity.this);
                tv.setText("查無任何結果");
                row.addView(tv, params);
            }
            mSearchResult.addView(row);

            String[] clonames = new String[]{"classid", "name", "credit", "timestr"};
            searchClassid.clear();
            try {
                while (classids.hasNext()) {
                    final String classid = classids.next();
                    JSONObject cla = (JSONObject) classes.get(classid);

                    row = new TableRow(ElectiveActivity.this);
                    if (user.isLogin) {
                        switch (cla.getString("elective")) {
                            case "ok":
                                Button btn = new Button(ElectiveActivity.this);
                                btn.setText("選課");
                                btn.setOnClickListener(new View.OnClickListener() {
                                    @Override public void onClick(View v) {
                                        showProgress(true);
                                        new ElectiveActivity.doElective(v).execute((Void) null);
                                    }
                                });
                                row.addView(btn);
                                searchClassid.put(btn, classid);
                                break;

                            case "collision":
                                tv = new TextView(ElectiveActivity.this);
                                tv.setText("衝堂");
                                tv.setGravity(Gravity.CENTER);
                                row.addView(tv, params);
                                break;

                            case "selected":
                                tv = new TextView(ElectiveActivity.this);
                                tv.setText("已選");
                                tv.setGravity(Gravity.CENTER);
                                row.addView(tv, params);
                                break;

                            default:
                                tv = new TextView(ElectiveActivity.this);
                                tv.setText("未知");
                                row.addView(tv, params);
                                break;
                        }
                    }
                    for (String col : clonames) {
                        tv = new TextView(ElectiveActivity.this);
                        tv.setText(cla.getString(col));
                        row.addView(tv, params);
                    }
                    mSearchResult.addView(row);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            showProgress(false);
            findViewById(R.id.search_result_group).setVisibility(View.VISIBLE);
        }
    }

    public class showElective extends AsyncTask<Void, Void, Void> {
        private User user;
        private JSONObject elective;
        private JSONObject calendar;

        showElective(User user) {
            this.user = user;
        }

        @Override
        protected Void doInBackground(Void... temp) {
            if (!this.user.checkLogin()) {
                return null;
            }

            Map<String,Object> parm = new LinkedHashMap<>();
            parm.put("action", "getelective");
            this.elective = new Api(getApplicationContext()).post(parm);

            Map<String,Object> parm2 = new LinkedHashMap<>();
            parm2.put("action", "getcalendar");
            this.calendar = new Api(getApplicationContext()).post(parm2);

            return null;
        }

        @Override
        protected void onPostExecute(Void temp) {
            if (!this.user.isLogin) {
                findViewById(R.id.elective_table_group).setVisibility(View.GONE);
                findViewById(R.id.calendar_table_group).setVisibility(View.GONE);
                showProgress(false);
                return;
            }

            TableRow.LayoutParams params = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT);
            params.setMargins(4, 4, 4, 4);
            TableRow row;
            TextView tv;

            if (this.elective.optString("result").equals("ok")) {
                mElectiveResult.removeAllViewsInLayout();
                JSONObject classes = this.elective.optJSONObject("data");
                Iterator<String> classids = classes.keys();

                row = new TableRow(ElectiveActivity.this);
                if (classids.hasNext()) {
                    for (String col : new String[]{"退選", "編號", "名稱", "學分數", "時間"}) {
                        tv = new TextView(ElectiveActivity.this);
                        tv.setText(col);
                        row.addView(tv, params);
                    }
                } else {
                    tv = new TextView(ElectiveActivity.this);
                    tv.setText("查無任何結果");
                    row.addView(tv, params);
                }
                mElectiveResult.addView(row);

                String[] clonames = new String[]{"classid", "name", "credit", "timestr"};
                electiveClassid.clear();
                try {
                    while (classids.hasNext()) {
                        String classid = classids.next();
                        JSONObject cla = (JSONObject) classes.get(classid);

                        row = new TableRow(ElectiveActivity.this);

                        Button btn = new Button(ElectiveActivity.this);
                        btn.setText("退選");
                        btn.setOnClickListener(new View.OnClickListener() {
                            @Override public void onClick(View v) {
                                showProgress(true);
                                new ElectiveActivity.doUnelective(v).execute((Void) null);
                            }
                        });
                        row.addView(btn);
                        electiveClassid.put(btn, classid);
                        for (String col : clonames) {
                            tv = new TextView(ElectiveActivity.this);
                            tv.setText(cla.getString(col));
                            row.addView(tv, params);
                        }

                        mElectiveResult.addView(row);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                findViewById(R.id.elective_table_group).setVisibility(View.VISIBLE);
            }

            if (this.calendar.optString("result").equals("ok")) {
                mCalendarResult.removeAllViewsInLayout();
                JSONObject classes = this.calendar.optJSONObject("data");

                row = new TableRow(ElectiveActivity.this);
                String[] dayname = new String[]{"", "一", "二", "三", "四", "五", "六", "日"};
                for (int day = 0; day <=7 ; day++) {
                    tv = new TextView(ElectiveActivity.this);
                    tv.setText(dayname[day]);
                    tv.setGravity(Gravity.CENTER);
                    row.addView(tv, params);
                }
                mCalendarResult.addView(row);

                for (int period = 1; period <= 13; period++) {
                    String periodstr = String.valueOf(period);

                    row = new TableRow(ElectiveActivity.this);
                    tv = new TextView(ElectiveActivity.this);
                    tv.setText(String.format("第%d節", period));
                    tv.setGravity(Gravity.CENTER);
                    row.addView(tv, params);

                    for (int day = 1; day <= 7; day++) {
                        String daystr = String.valueOf(day);

                        String claname = "";
                        if (classes != null && classes.has(daystr) && classes.optJSONObject(daystr).has(periodstr)) {
                            claname = classes.optJSONObject(daystr).optString(periodstr);
                        }
                        tv = new TextView(ElectiveActivity.this);
                        tv.setText(claname);
                        tv.setGravity(Gravity.CENTER);
                        row.addView(tv, params);
                    }

                    mCalendarResult.addView(row);
                }

                findViewById(R.id.calendar_table_group).setVisibility(View.VISIBLE);
            }

            showProgress(false);
        }
    }

    public class doElective extends AsyncTask<Void, Void, JSONObject> {
        private View btn;

        doElective(View v) {
            this.btn = v;
        }

        @Override
        protected JSONObject doInBackground(Void... temp) {
            Map<String,Object> parm = new LinkedHashMap<>();
            parm.put("action", "elective");
            parm.put("classid", searchClassid.get(btn));
            return new Api(getApplicationContext()).post(parm);
        }

        @Override
        protected void onPostExecute(JSONObject res) {
            switch (res.optString("result")) {
                case "success":
                    Toast.makeText(getApplicationContext(), "選課成功", Toast.LENGTH_SHORT).show();
                    new ElectiveActivity.showElective(user).execute((Void) null);
                    goSearch();
                    break;

                case "no_network":
                case "timeout":
                    Toast.makeText(getApplicationContext(), "選課失敗，請檢查網路連線並重試", Toast.LENGTH_SHORT).show();
                    break;

                default:
                    Toast.makeText(getApplicationContext(), "選課失敗", Toast.LENGTH_SHORT).show();
            }
            showProgress(false);
        }
    }

    public class doUnelective extends AsyncTask<Void, Void, JSONObject> {
        private View btn;

        doUnelective(View v) {
            this.btn = v;
        }

        @Override
        protected JSONObject doInBackground(Void... temp) {
            Map<String,Object> parm = new LinkedHashMap<>();
            parm.put("action", "unelective");
            parm.put("classid", electiveClassid.get(btn));
            return new Api(getApplicationContext()).post(parm);
        }

        @Override
        protected void onPostExecute(JSONObject res) {
            switch (res.optString("result")) {
                case "success":
                    Toast.makeText(getApplicationContext(), "退選成功", Toast.LENGTH_SHORT).show();
                    new ElectiveActivity.showElective(user).execute((Void) null);
                    goSearch();
                    break;

                case "no_network":
                case "timeout":
                    Toast.makeText(getApplicationContext(), "退選失敗，請檢查網路連線並重試", Toast.LENGTH_SHORT).show();
                    break;

                default:
                    Toast.makeText(getApplicationContext(), "退選失敗", Toast.LENGTH_SHORT).show();
            }
            showProgress(false);
        }
    }
}
