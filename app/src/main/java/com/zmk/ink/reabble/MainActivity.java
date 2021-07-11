package com.zmk.ink.reabble;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.graphics.Bitmap;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    WebView webview;
    WebSettings mWebSettings;
    ActionBar bar;
    int screenWidth, screenHeight;
    LoadingDialog loadingDialog;

    WebViewClient webViewClient = new WebViewClient() {
        //设置加载前的函数
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            if(url.toString().contains("https://reabble.cn/app")){
                return;
            }

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
            view.goBack();
            return;

        }

        //设置结束加载函数
        @Override
        public void onPageFinished(WebView view, String url) {
//            loadingDialog.hide();
        }
    };
    Instrumentation inst = new Instrumentation();
    Runnable runnableUp = () -> {
        inst.sendKeyDownUpSync(KeyEvent.KEYCODE_PAGE_UP);
        inst.sendKeySync(new KeyEvent(KeyEvent.KEYCODE_SHIFT_LEFT,KeyEvent.KEYCODE_TAB));
    };
    Runnable runnableDown = () -> {
        inst.sendKeyDownUpSync(KeyEvent.KEYCODE_PAGE_DOWN);
        inst.sendKeySync(new KeyEvent(KeyEvent.KEYCODE_SHIFT_LEFT,KeyEvent.KEYCODE_TAB));
    };

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if(keyCode==KeyEvent.KEYCODE_VOLUME_DOWN || keyCode==KeyEvent.KEYCODE_VOLUME_UP){
            new Thread(keyCode==KeyEvent.KEYCODE_VOLUME_DOWN? runnableDown : runnableUp).start();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @SuppressLint("InvalidWakeLockTag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
//        this.dialogShow();
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        this.initConfig();
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (setFlags) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        setContentView(R.layout.activity_main);

        // get wh of screen
        WindowManager manager = this.getWindowManager();
        DisplayMetrics outMetrics = new DisplayMetrics();
        manager.getDefaultDisplay().getMetrics(outMetrics);
        screenWidth = outMetrics.widthPixels;
        screenHeight = outMetrics.heightPixels;
        ///
        this.actionBar();
        this.webView();
        webview.loadUrl("https://reabble.cn/app");
    }

    Timer timer = new Timer();
    private TextView mTextView;
    TimerTask task = new TimerTask() {
        @Override
        public void run() {
            MainActivity.this.runOnUiThread(() -> {
                Long sysTime = System.currentTimeMillis();
                mTextView.setText("≡");

//                if (setFlags) {
//                    mTextView.setText("≡");
//                } else {
//                    mTextView.setText(DateFormat.format("HH:mm", sysTime));
//                }
            });
        }
    };

    private void actionBar () {
        //获取ActionBar对象
        bar = getSupportActionBar();
        //自定义一个布局，并居中
        bar.setDisplayShowCustomEnabled(true);
        View v = LayoutInflater.from(getApplicationContext()).inflate(R.layout.bar, null);
        bar.setCustomView(v, new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.MATCH_PARENT));
        timer.schedule(task, 0, 1000);
        mTextView = findViewById(R.id.textView2);

        findViewById(R.id.textView2).setOnClickListener(view -> {
            setFlags = !setFlags;
            if (setFlags) {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                config("setflags", "1");
            } else {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                config("setflags", "0");
            }
        });
        findViewById(R.id.imageView).setOnClickListener(view -> {
              webview.reload();

//            //判断当前屏幕方向
//            if (getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
//                //切换竖屏
//                MainActivity.this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
//            } else {
//                //切换横屏
//                MainActivity.this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
//            }
//            mWebSettings.setUseWideViewPort(true);
        });
        ///
    }

    private void webView () {
        webview = findViewById(R.id.webview);
        mWebSettings = webview.getSettings();
        mWebSettings.setJavaScriptEnabled(true);
        mWebSettings.setSupportZoom(true);
        mWebSettings.setUseWideViewPort(false);
        mWebSettings.setBlockNetworkImage(false);
        mWebSettings.setLoadWithOverviewMode(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mWebSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        webview.setWebViewClient(webViewClient);

        // set page zoom by screen wh
        int zoomPercent = 0, fontSize = 0;
        if (screenWidth <= 720) {
            // bar.hide();
            zoomPercent = screenHeight > screenWidth ? 204 : 100; //204
            fontSize = 13;
        }
        if (screenWidth > 720) {
            webview.zoomIn();
            zoomPercent = screenHeight > screenWidth ? 275 : 150;
            fontSize = 15;
        }
        mWebSettings.setDefaultFontSize(fontSize);
        webview.setInitialScale(zoomPercent);
        ///

    }

    public void dialogShow() {
        if (loadingDialog==null) {
            loadingDialog = new LoadingDialog(this);
        }
        loadingDialog.show();
        loadingDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    SharedPreferences sp;
    boolean setFlags = false;
    private void initConfig(){
        Context ctx = MainActivity.this;
        sp = ctx.getSharedPreferences("SP", MODE_PRIVATE);
        if (config("setflags").equals("1")) {
            setFlags = true;
        }
    }

    private void config(String key, String value) {
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(key, value);
        editor.commit();
    }

    private String config(String key) {
        return sp.getString(key, "").toString();
    }
}
