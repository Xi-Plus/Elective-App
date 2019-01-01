package com.xiplus.elective;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

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
        new ElectiveActivity.showElective(user).execute((Void) null);
    }

    public void goSearch(View view) {
        String day = dayString[mDaySpinner.getSelectedItemPosition()];
        String period = periodString[mPeriodSpinner.getSelectedItemPosition()];

        showProgress(true);
        new ElectiveActivity.doSearch(day, period).execute((Void) null);
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
                mLoginTextView.setText("未登入");
            }
        }
    }

    public class doSearch extends AsyncTask<Void, Void, JSONObject> {
        private String day;
        private String period;

        doSearch(String day, String period) {
            this.day = day;
            this.period = period;
        }

        @Override
        protected JSONObject doInBackground(Void... temp) {
            Map<String,Object> parm = new LinkedHashMap<>();
            parm.put("action", "search");
            parm.put("day", this.day);
            parm.put("period", this.period);
            JSONObject res = new Api(getApplicationContext()).post(parm);
            return res;
        }

        @Override
        protected void onPostExecute(JSONObject res) {
            JSONObject classes = (JSONObject) res.opt("result");
            System.out.println(classes.toString());
            mSearchResult.removeAllViewsInLayout();
            Iterator<String> classids = classes.keys();

            TableRow row = new TableRow(ElectiveActivity.this);
            for (String col : new String[]{"編號", "名稱", "學分數", "時間"}) {
                TextView tv = new TextView(ElectiveActivity.this);
                tv.setText(col);

                TableRow.LayoutParams params = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT);
                params.setMargins(4, 4, 4, 4);

                row.addView(tv, params);
            }
            mSearchResult.addView(row);

            String[] clonames = new String[]{"classid", "name", "credit", "timestr"};
            try {
                while (classids.hasNext()) {
                    String classid = classids.next();
                    System.out.println("classid="+classid);
                    JSONObject cla = (JSONObject) classes.get(classid);

                    row = new TableRow(ElectiveActivity.this);
                    for (String col : clonames) {
                        TextView tv = new TextView(ElectiveActivity.this);
                        tv.setText(cla.getString(col));

                        TableRow.LayoutParams params = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT);
                        params.setMargins(0, 0, 20, 0);

                        System.out.println(col+"="+cla.getString(col));
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
                System.out.println("not login, skip show");
                return;
            }

            if (this.elective.optString("result").equals("ok")) {
                System.out.println("show elective");

                mElectiveResult.removeAllViewsInLayout();
                JSONObject classes = this.elective.optJSONObject("data");
                Iterator<String> classids = classes.keys();

                TableRow row = new TableRow(ElectiveActivity.this);
                for (String col : new String[]{"編號", "名稱", "學分數", "時間", "退選"}) {
                    TextView tv = new TextView(ElectiveActivity.this);
                    tv.setText(col);

                    TableRow.LayoutParams params = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT);
                    params.setMargins(4, 4, 4, 4);

                    row.addView(tv, params);
                }
                mElectiveResult.addView(row);

                String[] clonames = new String[]{"classid", "name", "credit", "timestr"};
                try {
                    while (classids.hasNext()) {
                        String classid = classids.next();
                        System.out.println("classid="+classid);
                        JSONObject cla = (JSONObject) classes.get(classid);

                        row = new TableRow(ElectiveActivity.this);
                        for (String col : clonames) {
                            TextView tv = new TextView(ElectiveActivity.this);
                            tv.setText(cla.getString(col));
                            TableRow.LayoutParams params = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT);
                            params.setMargins(0, 0, 20, 0);
                            row.addView(tv, params);
                        }

                        TextView tv = new TextView(ElectiveActivity.this);
                        tv.setText("退選");
                        TableRow.LayoutParams params = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT);
                        params.setMargins(0, 0, 20, 0);
                        row.addView(tv, params);

                        mElectiveResult.addView(row);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                findViewById(R.id.elective_table_group).setVisibility(View.VISIBLE);
            }

            if (this.calendar.optString("result").equals("ok")) {
                System.out.println("show calendar");

                mCalendarResult.removeAllViewsInLayout();
                JSONObject classes = this.calendar.optJSONObject("data");
                Iterator<String> classids = classes.keys();

                TableRow.LayoutParams params = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT);
                params.setMargins(4, 4, 4, 4);
                TextView tv;
                TableRow row;

                row = new TableRow(ElectiveActivity.this);
                String[] dayname = new String[]{"", "一", "二", "三", "四", "五", "六", "日"};
                for (int day = 0; day <=7 ; day++) {
                    tv = new TextView(ElectiveActivity.this);
                    tv.setText(dayname[day]);
                    row.addView(tv, params);
                }
                mCalendarResult.addView(row);

                for (int period = 1; period <= 13; period++) {
                    String periodstr = String.valueOf(period);

                    row = new TableRow(ElectiveActivity.this);
                    tv = new TextView(ElectiveActivity.this);
                    tv.setText(String.format("第%d節", period));
                    row.addView(tv, params);

                    for (int day = 1; day <= 7; day++) {
                        String daystr = String.valueOf(day);

                        String claname = "";
                        if (classes.has(daystr) && classes.optJSONObject(daystr).has(periodstr)) {
                            System.out.println(classes.optJSONObject(daystr).optString(periodstr));
                            claname = classes.optJSONObject(daystr).optString(periodstr);
                            System.out.println(daystr+" "+periodstr+" "+claname);
                        }
                        tv = new TextView(ElectiveActivity.this);
                        tv.setText(claname);
                        row.addView(tv, params);
                    }

                    mCalendarResult.addView(row);
                }

                findViewById(R.id.calendar_table_group).setVisibility(View.VISIBLE);
            }

            showProgress(false);
        }
    }
}
