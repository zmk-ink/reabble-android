package com.zmk.ink.reabble;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Instrumentation;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.graphics.Bitmap;

import java.util.Timer;

public class MainActivity extends AppCompatActivity {
    WebView webview;
    WebSettings mWebSettings;
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
        super.onCreate(savedInstanceState);
//        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);

        // get wh of screen
        WindowManager manager = this.getWindowManager();
        DisplayMetrics outMetrics = new DisplayMetrics();
        manager.getDefaultDisplay().getMetrics(outMetrics);
        screenWidth = outMetrics.widthPixels;
        screenHeight = outMetrics.heightPixels;
        ///
        this.webView();
        webview.loadUrl("https://reabble.cn/app");
    }

    Timer timer = new Timer();
    private TextView mTextView;

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
            zoomPercent = 204;
            fontSize = 16;
        }
        if (screenWidth > 720) {
            webview.zoomIn();
            zoomPercent = 200;
            fontSize = 17;
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


}
