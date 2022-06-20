package com.zmk.ink.reabble;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Instrumentation;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
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
        inst.sendKeyDownUpSync(KeyEvent.KEYCODE_P);
    };
    Runnable runnableDown = () -> {
        inst.sendKeyDownUpSync(KeyEvent.KEYCODE_N);
    };

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            new Thread(keyCode==KeyEvent.KEYCODE_VOLUME_DOWN? runnableDown : runnableUp).start();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @SuppressLint("InvalidWakeLockTag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // get wh of screen
        WindowManager manager = this.getWindowManager();
        DisplayMetrics outMetrics = new DisplayMetrics();
        manager.getDefaultDisplay().getMetrics(outMetrics);
        screenWidth = outMetrics.widthPixels;
        screenHeight = outMetrics.heightPixels;
        ///
        this.initWebView();
        webview.loadUrl("https://reabble.cn/app");
    }

    Timer timer = new Timer();
    private TextView mTextView;

    private void initWebView() {
        webview = findViewById(R.id.webview);
        webview.requestFocus();

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
        String device_model = Build.MODEL;

        switch (device_model) {
            case "SC801a":
            case "SC801c":
                webview.zoomIn();
                zoomPercent = 100;
                fontSize = 25;
                break;
            case "MiDuoKanReaderPro":
                zoomPercent = 150;
                fontSize = 25;
                break;
            case "Poke4S":
                zoomPercent = 150;
                fontSize = 16;
                break;
            default:
                if (screenWidth > 720) {
                    zoomPercent = 200;
                    fontSize = 17;
                } else {
                    zoomPercent = 204;
                    fontSize = 16;
                }
        }
        mWebSettings.setDefaultFontSize(fontSize);
        webview.setInitialScale(zoomPercent);
        ///
    }

}
