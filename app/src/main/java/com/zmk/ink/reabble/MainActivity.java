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
import android.graphics.Bitmap;

public class MainActivity extends AppCompatActivity {
    int refresh_type = 0;//bar,
    WebView webview;
    WebSettings mWebSettings;
    int screenWidth, screenHeight;

    WebViewClient webViewClient = new WebViewClient() {
        //设置加载前的函数
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            if(url.contains("https://reabble.cn/app")){
                return;
            }

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
            view.goBack();
        }

        //设置结束加载函数
        @Override
        public void onPageFinished(WebView view, String url) {
            String jsRefresh;
            if (refresh_type==1) {
                jsRefresh = "if(document.getElementsByClassName(\"bRefresh\").length<1) {var intervalId = null;intervalId = setInterval(function(){ if (document.getElementsByClassName(\"js-12-d js-13-2r\").length!=0) { document.getElementsByClassName(\"js-12-d js-13-2r\")[2].insertAdjacentHTML(\"afterend\", '<button onclick=\"javascript: window.location.reload();\" type=\"button\" class=\"bRefresh js-51-24 js-52-25 js-53-26 js-54-27 js-29-17 js-48-21 js-7-1j js-40-1k js-6-18 js-30-19 js-31-1a js-32-1b js-33-1c js-8-8 js-34-1d js-35-1e js-36-1f js-37-1g js-38-1h js-39-1i js-16-j js-41-1l js-42-1m js-43-1n js-44-1o js-45-1p\"><svg style=\"height: 20px; width: 20px; color: rgb(0, 0, 255);\" xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 24 512 512\"><path d=\"M256,48C141.31,48,48,141.32,48,256c0,114.86,93.14,208,208,208,114.69,0,208-93.31,208-208C464,141.13,370.87,48,256,48Zm94,219a94,94,0,1,1-94-94h4.21l-24-24L256,129.2,315.8,189,256,248.8,236.2,229l27.92-27.92C261.72,201,259,201,256,201a66,66,0,1,0,66,66V253h28Z\"></path></svg></button>'); clearInterval(intervalId);}}, 10);};";
            }else{
//                    jsRefresh = " var intervalId = null;intervalId = setInterval(function(){if (document.getElementsByClassName(\"js-12-d js-13-2r\").length!=0) {clearInterval(intervalId);cn=\"js-6-18 js-30-19 js-31-1a js-32-1b js-33-1c js-8-4s js-34-1d js-35-1e js-36-1f js-37-1g js-38-1h js-39-1i js-9-9 js-28-13 js-29-17 js-48-4t js-14-14 js-51-24 js-52-25\";dddLi = document.getElementsByClassName(cn);ddd = document.getElementsByClassName(\"js-51-24 js-52-25 js-53-26 js-54-27 js-29-17 js-48-21   js-7-1j js-40-1k js-6-18 js-30-19 js-31-1a js-32-1b js-33-1c js-8-8 js-34-1d js-35-1e js-36-1f js-37-1g js-38-1h js-39-1i   js-16-j js-41-1l js-42-1m js-43-1n js-44-1o  js-45-1p\");ddd[ddd.length-1].onclick = function() {document.getElementsByTagName(\"body\")[0].innerHTML=\"hello\";if (dddLi.length!=0) {dddLi[0].insertAdjacentHTML(\"beforebegin\", '<div class=\"'+cn+'\" onclick=\"javascript: location.reload();\"><span class=\"js-9-9 js-13-4b js-28-13 js-9-9 js-13-4u js-28-13\"><svg viewBox=\"0 0 512 512\" class=\"js-9-1q js-47-1s js-14-2o js-29-2p js-38-1h js-40-2q\"><path fill=\"none\" d=\"M0 0h24v24H0z\"></path><path d=\"M256,48C141.31,48,48,141.32,48,256c0,114.86,93.14,208,208,208,114.69,0,208-93.31,208-208C464,141.13,370.87,48,256,48Zm94,219a94,94,0,1,1-94-94h4.21l-24-24L256,129.2,315.8,189,256,248.8,236.2,229l27.92-27.92C261.72,201,259,201,256,201a66,66,0,1,0,66,66V253h28Z\"></path></svg></span><span class=\"js-12-d js-63-4c js-34-1d\">刷新 Refresh</span></div>');}}}},10);";
                jsRefresh = "";
            }
            jsRefresh += " document.styleSheets[document.styleSheets.length-1].addRule(\".js-48-3x\", \"border:none;\");document.styleSheets[document.styleSheets.length-1].addRule(\".js-68-43\", \"border-left-style: none;border-bottom:1px dashed grey;\");";
            webview.loadUrl("javascript:"+ jsRefresh);
        }
    };
    Instrumentation inst = new Instrumentation();
    Runnable runnableUp = () -> inst.sendKeyDownUpSync(KeyEvent.KEYCODE_P);
    Runnable runnableDown = () -> inst.sendKeyDownUpSync(KeyEvent.KEYCODE_N);

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
//        getWindow().getDecorView().setSystemUiVisibility(8192);

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
                refresh_type = 1;
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
