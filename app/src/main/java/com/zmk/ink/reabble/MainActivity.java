package com.zmk.ink.reabble;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;
import android.view.View;
import com.zmk.ink.reabble.utils.FileUtil;
import com.zmk.ink.reabble.utils.NetUtil;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {
    int refresh_type = 0;
    WebView webview;
    WebSettings mWebSettings;
    int screenWidth, screenHeight;
    private RelativeLayout loadingLayout;

    boolean pageLoaded = false;

    WebViewClient webViewClient = new WebViewClient() {
        @Deprecated
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();
            pageLoaded = false;
            // 判断是否为页面刷新操作
            if (url.equals(view.getUrl())) {
                showWebView(false);
            }
            if(url.contains("https://reabble.cn/app") || url.indexOf("file:///android_asset")==0){
                return false;
            }

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
            return true;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            showWebView(false);
        }
        @Override
        public void onLoadResource (WebView view,
                                    String url) {
            /**
            Log.d("dddd3", TimingUtil.logTimeDifference() + " ms ↖");
            Log.d("dddd4",   url);
            **/
            super.onLoadResource(view, url);
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();

            if (url.startsWith("https://www.inoreader.com/images/") ||
                url.startsWith("https://nettools1.oxyry.com/image?url=https%3A%2F%2Fwww.inoreader.com%2Fimages") ||
                url.startsWith("https://ana.oxyry.com/api/ana") ||
                url.startsWith("https://reabble.cn/static/img/icons/favicon.ico") ||
                url.startsWith("https://ana.oxyry.com/script.js")
            ) {
                String base64TransparentGif = "R0lGODlhAQABAIAAAP///wAAACH5BAEAAAAALAAAAAABAAEAAAICRAEAOw==";
                byte[] transparentGif = android.util.Base64.decode(base64TransparentGif, android.util.Base64.DEFAULT);
                InputStream data = new ByteArrayInputStream(transparentGif);
                return new WebResourceResponse("image/gif", "base64", data);
            }

            if (NetUtil.isGifUrl(url)) {
                String jpgFilePath = view.getContext().getCacheDir() + "/output.jpg";
                Bitmap bitmap = NetUtil.gifToJpg(url, jpgFilePath);

                if (bitmap != null) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                    InputStream inputStream = new ByteArrayInputStream(baos.toByteArray());
                    return new WebResourceResponse("image/jpeg", "UTF-8", inputStream);
                }
            }

            return null;
        }

        @Override
        public void onReceivedError (WebView view,
                                     WebResourceRequest request,
                                     WebResourceError error) {
            super.onReceivedError(view, request, error);
            if (pageLoaded) {
                return;
            }

            int errorCode = error.getErrorCode();
            if (errorCode == ERROR_HOST_LOOKUP
                || errorCode == ERROR_CONNECT
                || errorCode == ERROR_TIMEOUT
            ) {
                String customHtml = FileUtil.readFile(
                        getApplicationContext(),
                        "NetworkError.html"
                );

                view.loadDataWithBaseURL(null,
                        customHtml,
                        "text/html",
                        "UTF-8",
                        null);
            }
        }

        //设置结束加载函数
        @Override
        public void onPageFinished(WebView view, String url) {
            String jsRefresh = FileUtil.readJSFile(
                    getApplicationContext(),
                    refresh_type == 1 ? "js/onload_pad.js" : "js/onload.js"
            );
            webview.evaluateJavascript (jsRefresh +"void(0);", null);
            pageLoaded = true;
            showWebView(true);
        }
    };

    public void showWebView(boolean show) {
        if (show){
            new Handler().postDelayed(() -> {
                loadingLayout.setVisibility(View.GONE);
                webview.setVisibility(View.VISIBLE);
            }, 500);
            return;
        }
        loadingLayout.setVisibility(View.VISIBLE);
        webview.setVisibility(View.GONE);
    }

    Runnable runnableUp = () -> runOnUiThread(() -> webview.evaluateJavascript("window.reabblePageUp();void(0);", null));
    Runnable runnableDown = () -> runOnUiThread(() -> webview.evaluateJavascript("window.reabblePageDown();void(0);", null));

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            runOnUiThread(() -> {
                switch (keyCode) {
                    case KeyEvent.KEYCODE_VOLUME_DOWN:
                        runnableDown.run();
                        break;
                    case KeyEvent.KEYCODE_VOLUME_UP:
                        runnableUp.run();
                        break;
                    default:
                        break;
                }
            });
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

        loadingLayout = findViewById(R.id.loadingLayout);

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

    @SuppressLint("SetJavaScriptEnabled")
    private void initWebView() {
        webview = findViewById(R.id.webview);
        webview.requestFocus();

        mWebSettings = webview.getSettings();
        mWebSettings.setJavaScriptEnabled(true);
        mWebSettings.setSupportZoom(true);
        mWebSettings.setUseWideViewPort(false);
        mWebSettings.setBlockNetworkImage(false);
        mWebSettings.setLoadWithOverviewMode(true);
        mWebSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        mWebSettings.setAppCacheEnabled(true);
        mWebSettings.setDomStorageEnabled(true);

        mWebSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webview.setWebViewClient(webViewClient);

        // set page zoom by screen wh
        int zoomPercent, fontSize;
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
            case "Tab10CPro":
            case "Tab10":
            case "Tab10C":
                zoomPercent = 293;
                fontSize = 16;
                break;
            default:
                if (screenWidth >= 1860) {
                    zoomPercent = 293;
                    fontSize = 16;
                }
                else if (screenWidth > 720) {
                    zoomPercent = 200;
                    fontSize = 17;
                }
                else {
                    zoomPercent = 204;
                    fontSize = 16;
                }
        }
        mWebSettings.setDefaultFontSize(fontSize);
        webview.setInitialScale(zoomPercent);
        ///

    }





}
