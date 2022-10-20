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
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends AppCompatActivity {
    int refresh_type = 0;//bar,
    WebView webview;
    WebSettings mWebSettings;
    int screenWidth, screenHeight;

    WebViewClient webViewClient = new WebViewClient() {
        @Deprecated
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();
            if(url.contains("https://reabble.cn/app") || url.indexOf("file:///android_asset")==0){
                return false;
            }

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
            return true;
        }

        @Override
        public void onLoadResource (WebView view,
                                    String url) {
            if (url.contains("https://reabble.cn/api/app-resources.js")) {
                webview.loadUrl("javascript:document.getElementsByClassName(\"app-loading\")[0].innerHTML=\"LOADING\";void(0);");
            }
            webview.loadUrl("javascript:var tagCenters = document.getElementsByTagName(\"center\");for (let i=0; i<tagCenters.length; i++) {if (tagCenters[i].innerHTML.indexOf(\"Inoreader\")==-1){tagCenters[i].style.display=\"block\";}};void(0);");
        }
        @Override
        public void onReceivedError (WebView view,
                                     WebResourceRequest request,
                                     WebResourceError error) {
            super.onReceivedError(view, request, error);

            if (error.getDescription().toString().contains("net::ERR_ADDRESS_UNREACHABLE") || view.getUrl().equals("https://reabble.cn/app") || view.getUrl().contains("file:///android_asset/")) {
                webview.loadUrl("file:///android_asset/index.html");
            }
        }
        //设置结束加载函数
        @Override
        public void onPageFinished(WebView view, String url) {
            String jsRefresh;
            if (refresh_type==1) {
                jsRefresh = "if(document.getElementsByClassName(\"bRefresh\").length<1) {var intervalId = null;intervalId = setInterval(function(){ if (document.getElementsByClassName(\"js-12-f js-13-2t\").length!=0 && document.getElementsByClassName(\"bRefresh\").length<1) { document.getElementsByClassName(\"js-12-f js-13-2t\")[2].insertAdjacentHTML(\"afterend\", '<button onclick=\"javascript: window.location.reload();\" type=\"button\" class=\"bRefresh js-51-26 js-52-27 js-53-28 js-54-29 js-29-17 js-48-22 js-7-1l js-40-1m js-6-1a js-30-1b js-31-1c js-32-1d js-33-1e js-8-8 js-34-1f js-35-1g js-36-1h js-37-1i js-38-1j js-39-1k   js-16-l js-41-1n js-42-1o js-43-1p js-44-1q  js-45-1r\"><svg xmlns=\"http://www.w3.org/2000/svg\" width=\"18\" height=\"18\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\" class=\"feather feather-refresh-cw\"><polyline points=\"23 4 23 10 17 10\"></polyline><polyline points=\"1 20 1 14 7 14\"></polyline><path d=\"M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15\"></path></svg></button>'); clearInterval(intervalId);}}, 1000);};setTimeout(function(){clearInterval(intervalId);}, 10000);";
            }else{
//                    jsRefresh = " var intervalId = null;intervalId = setInterval(function(){if (document.getElementsByClassName(\"js-12-d js-13-2r\").length!=0) {clearInterval(intervalId);cn=\"js-6-18 js-30-19 js-31-1a js-32-1b js-33-1c js-8-4s js-34-1d js-35-1e js-36-1f js-37-1g js-38-1h js-39-1i js-9-9 js-28-13 js-29-17 js-48-4t js-14-14 js-51-24 js-52-25\";dddLi = document.getElementsByClassName(cn);ddd = document.getElementsByClassName(\"js-51-24 js-52-25 js-53-26 js-54-27 js-29-17 js-48-21   js-7-1j js-40-1k js-6-18 js-30-19 js-31-1a js-32-1b js-33-1c js-8-8 js-34-1d js-35-1e js-36-1f js-37-1g js-38-1h js-39-1i   js-16-j js-41-1l js-42-1m js-43-1n js-44-1o  js-45-1p\");ddd[ddd.length-1].onclick = function() {document.getElementsByTagName(\"body\")[0].innerHTML=\"hello\";if (dddLi.length!=0) {dddLi[0].insertAdjacentHTML(\"beforebegin\", '<div class=\"'+cn+'\" onclick=\"javascript: location.reload();\"><span class=\"js-9-9 js-13-4b js-28-13 js-9-9 js-13-4u js-28-13\"><svg viewBox=\"0 0 512 512\" class=\"js-9-1q js-47-1s js-14-2o js-29-2p js-38-1h js-40-2q\"><path fill=\"none\" d=\"M0 0h24v24H0z\"></path><path d=\"M256,48C141.31,48,48,141.32,48,256c0,114.86,93.14,208,208,208,114.69,0,208-93.31,208-208C464,141.13,370.87,48,256,48Zm94,219a94,94,0,1,1-94-94h4.21l-24-24L256,129.2,315.8,189,256,248.8,236.2,229l27.92-27.92C261.72,201,259,201,256,201a66,66,0,1,0,66,66V253h28Z\"></path></svg></span><span class=\"js-12-d js-63-4c js-34-1d\">刷新 Refresh</span></div>');}}}},10);";
                jsRefresh = "";
            }
            jsRefresh += "styleSheets = document.styleSheets[document.styleSheets.length-1];styleSheets.addRule(\".js-49-3e\", \"border:none;\");styleSheets.addRule(\".js-29-44\", \"border-left-style: none;border-bottom:1px dashed #aaaaaa;\");document.styleSheets[0].insertRule('center{ display: none }',0);styleSheets.addRule(\".js-26-12 .js-24-z .js-5-14 .js-9-9 .js-28-15 .js-14-16 .js-29-17 .js-6-18\", \"border:12px;\");";
            webview.loadUrl("javascript:"+ jsRefresh +"void(0);");
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

    @Override
    public void onBackPressed() {
        if (webview.canGoBack()) {
            webview.goBack();
        }else{
            super.onBackPressed();
        }
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
        refresh_type = 1;

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
            case "Poke4":
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
