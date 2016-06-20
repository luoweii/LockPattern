package com.luowei.lockpattern;

import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private static final String LOCK_PATTERN = "LOCK_PATTERN";
    private LockPatternView lpv;
    private TextView tvHint;
    private TextView tvReset;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        lpv = (LockPatternView) findViewById(R.id.lpv);
        tvHint = (TextView) findViewById(R.id.tvHint);
        tvReset = (TextView) findViewById(R.id.tvReset);
        setup();
    }

    private void setup() {
        SharedPreferences spf = getSharedPreferences(getPackageName(), 0);
        String lock = spf.getString(LOCK_PATTERN, "");
        if (lock.equals("")) {
            tvHint.setText("请设置新密码");
            final String[] newPwd = {""};
            lpv.setOnLockListener(new LockPatternView.OnLockListener() {
                @Override
                public boolean onLock(String pwd) {
                    if (pwd.length() < 3) {
                        tvHint.setText("密码至少3位");
                        return false;
                    }
                    if (newPwd[0].equals("")) {
                        newPwd[0] = pwd;
                        tvHint.setText("请再输一次密码");
                        return true;
                    } else if (newPwd[0].equals(pwd)) {
                        tvHint.setText("设置成功");
                        saveLock(pwd);
                        setOnLockListener();
                        return true;
                    } else {
                        tvHint.setText("两次密码不一致");
                        return false;
                    }
                }
            });
        } else {
            setOnLockListener();
        }
    }

    private void saveLock(String str) {
        SharedPreferences spf = getSharedPreferences(getPackageName(), 0);
        SharedPreferences.Editor editor = spf.edit();
        editor.putString(LOCK_PATTERN, str);
        editor.commit();
    }

    public void setOnLockListener() {
        lpv.setOnLockListener(new LockPatternView.OnLockListener() {
            @Override
            public boolean onLock(String pwd) {
                SharedPreferences spf = getSharedPreferences(getPackageName(), 0);
                if (spf.getString(LOCK_PATTERN,"").equals(pwd)) {
                    tvHint.setText("解锁成功");
                    return true;
                }
                tvHint.setText("密码错误");
                return false;
            }
        });
    }

    public void onClickReset(View view) {
        saveLock("");
        setup();
    }
}
