package com.zmk.ink.reabble;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.IOException;
import java.io.BufferedReader;
import android.app.Instrumentation;

public class MainActivity extends AppCompatActivity {
    int refresh_type = 0;
    WebView webview;
    WebSettings mWebSettings;
    int screenWidth, screenHeight;
    private RelativeLayout loadingLayout;

    boolean pageLoaded = false;
    private String jsOnload = null;
    private String jsOnloadPad = null;
    
    // 使用线程池管理按键模拟任务，避免频繁创建线程导致性能和电量损耗
    private final ExecutorService keyExecutor = Executors.newSingleThreadExecutor();

    WebViewClient webViewClient = new WebViewClient() {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            return handleUrlLoading(view, request.getUrl().toString());
        }

        @SuppressWarnings("deprecation")
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return handleUrlLoading(view, url);
        }

        private boolean handleUrlLoading(WebView view, String url) {
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

            // 检查是否为主框架错误
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (!request.isForMainFrame()) return;
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
            injectJS();
            pageLoaded = true;
            showWebView(true);
        }
    };

    /**
     * 注入 JS 代码，使用缓存避免重复读取文件，并使用 evaluateJavascript 提高性能
     */
    private void injectJS() {
        if (jsOnload == null) {
            jsOnload = readJSFile("js/onload.js");
        }
        if (jsOnloadPad == null) {
            jsOnloadPad = readJSFile("js/onload_pad.js");
        }
        String js = refresh_type == 1 ? jsOnloadPad : jsOnload;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webview.evaluateJavascript(js, null);
        } else {
            webview.loadUrl("javascript:" + js + "void(0);");
        }
    }

    public void showWebView(boolean show) {
        if (show){
            // 墨水屏响应较慢，适当延迟隐藏加载层可以减少渲染闪烁，500ms 过长，改为 200ms
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                loadingLayout.setVisibility(View.GONE);
                webview.setVisibility(View.VISIBLE);
            }, 200);
            return;
        }
        loadingLayout.setVisibility(View.VISIBLE);
        // 不再将 webview 设为 GONE，避免触发全屏重绘，只需让加载层覆盖即可
        // webview.setVisibility(View.GONE);
    }

    Instrumentation inst = new Instrumentation();
    Runnable runnableUp = () -> inst.sendKeyDownUpSync(KeyEvent.KEYCODE_P);
    Runnable runnableDown = () -> inst.sendKeyDownUpSync(KeyEvent.KEYCODE_N);

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            keyExecutor.execute(keyCode==KeyEvent.KEYCODE_VOLUME_DOWN? runnableDown : runnableUp);
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

    @Override
    protected void onPause() {
        super.onPause();
        if (webview != null) {
            webview.onPause();
            webview.pauseTimers(); // 关键：暂停 JS 定时器，显著降低后台电量消耗
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webview != null) {
            webview.onResume();
            webview.resumeTimers();
        }
    }

    @Override
    protected void onDestroy() {
        if (webview != null) {
            webview.loadDataWithBaseURL(null, "", "text/html", "utf-8", null);
            webview.clearHistory();
            if (webview.getParent() != null) {
                ((android.view.ViewGroup) webview.getParent()).removeView(webview);
            }
            webview.destroy();
            webview = null;
        }
        keyExecutor.shutdown();
        super.onDestroy();
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
        mWebSettings.setUseWideViewPort(true); // 改为 true，对现代网页兼容性更好
        mWebSettings.setLoadWithOverviewMode(true);
        
        // 关键改进：启用缓存，显著减少网络流量和电量损耗，提升加载速度
        mWebSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        mWebSettings.setDomStorageEnabled(true); // 启用 DOM 存储，现代 Web 应用必备
        mWebSettings.setDatabaseEnabled(true);
        // AppCache 在编译版本为 API 33 时会报错，因为该 API 已被完全移除。直接移除此行，现代 Android 无需此配置。
        mWebSettings.setAllowFileAccess(true);
        mWebSettings.setLoadsImagesAutomatically(true);
        mWebSettings.setGeolocationEnabled(false); // 禁用地理位置以节省电量

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mWebSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        
        // 墨水屏优化：在某些设备上，禁用硬件加速可以减少残影
        // webview.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        webview.setWebViewClient(webViewClient);

        // set page zoom by screen wh
        String device_model = Build.MODEL;
        refresh_type = 1;
        int zoomPercent, fontSize;
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
    }

    private String readFile(String fileName) {
        try {
            InputStream inputStream = getAssets().open(fileName);
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();
            return new String(buffer, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    private String readJSFile(String fileName) {
        try {
            StringBuilder stringBuilder = new StringBuilder();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(getAssets().open(fileName), StandardCharsets.UTF_8));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line).append("\n"); // 添加换行符，防止因行注释导致整个脚本失效
            }
            bufferedReader.close();
            return stringBuilder.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }
}
