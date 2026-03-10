package com.zmk.ink.reabble;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.View;
import com.zmk.ink.reabble.utils.FileUtil;
import com.zmk.ink.reabble.utils.NetUtil;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.io.BufferedReader;

public class MainActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "app_settings";
    private static final String KEY_BASE_URL = "base_url";
    // 需求：International -> https://reabble.com/，China Mainland -> https://reabble.cn/
    private static final String URL_INTERNATIONAL = "https://reabble.com"; // 国际版
    private static final String URL_MAINLAND = "https://reabble.cn";     // 国内版

    int refresh_type = 0;
    WebView webview;
    WebSettings mWebSettings;
    int screenWidth, screenHeight;
    private RelativeLayout loadingLayout;
    private ImageView loadingImage;
    private TextView loadingText;
    private View modeSelectorLayout;
    private Button btnInternational;
    private Button btnMainland;
    private String currentBaseUrl = URL_INTERNATIONAL;

    boolean pageLoaded = false;
    private String jsUserMenu = null;
    private String jsOnload = null;
    private String jsOnloadPad = null;
    
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

            // 自定义协议：从网页中触发地区重新选择
            if (url != null && url.startsWith("reabble://choose-region")) {
                resetRegionSelection();
                return true;
            }

            // 若当前已无网络，则直接展示本地错误页，不再依赖缓存内容
            if (!NetUtil.isNetworkConnected(view.getContext())) {
                showNetworkErrorPage();
                return true;
            }

            // 判断是否为页面刷新操作
            if (url.equals(view.getUrl())) {
                showWebView(false);
            }
            String appUrlPrefix = getBaseUrl() + "/app";
            if (url.contains(appUrlPrefix) || url.indexOf("file:///android_asset") == 0) {
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
        if (jsUserMenu == null) {
            jsUserMenu = readJSFile("js/user_menu.js");
        }
        if (jsOnload == null) {
            jsOnload = readJSFile("js/onload.js");
        }
        if (jsOnloadPad == null) {
            jsOnloadPad = readJSFile("js/onload_pad.js");
        }
        webview.evaluateJavascript(jsUserMenu, null);
        String js = refresh_type == 1 ? jsOnloadPad : jsOnload;
        webview.evaluateJavascript(js, null);
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
        super.onDestroy();
    }

    @SuppressLint("InvalidWakeLockTag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loadingLayout = findViewById(R.id.loadingLayout);
        loadingImage = findViewById(R.id.loadingImage);
        loadingText = findViewById(R.id.loadingText);
        modeSelectorLayout = findViewById(R.id.modeSelectorLayout);
        btnInternational = findViewById(R.id.btnInternational);
        btnMainland = findViewById(R.id.btnMainland);

        // get wh of screen
        WindowManager manager = this.getWindowManager();
        DisplayMetrics outMetrics = new DisplayMetrics();
        manager.getDefaultDisplay().getMetrics(outMetrics);
        screenWidth = outMetrics.widthPixels;
        screenHeight = outMetrics.heightPixels;
        ///
        this.initWebView();

        if (modeSelectorLayout != null && btnInternational != null && btnMainland != null) {
            btnInternational.setOnClickListener(v -> onModeSelected(URL_INTERNATIONAL));
            btnMainland.setOnClickListener(v -> onModeSelected(URL_MAINLAND));
        }

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String savedBaseUrl = prefs.getString(KEY_BASE_URL, null);

        if (savedBaseUrl == null && modeSelectorLayout != null) {
            // 首次进入：让用户选择地区
            showModeSelector();
        } else {
            // 已选择地区：直接使用上次选择的域名
            if (savedBaseUrl != null) {
                currentBaseUrl = savedBaseUrl;
            }
            if (NetUtil.isNetworkConnected(this)) {
                webview.loadUrl(getBaseUrl() + "/app");
            } else {
                showNetworkErrorPage();
            }
        }
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
        
        // 始终使用默认缓存策略，弱网/断网时不强制使用缓存，交由错误页处理
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

    private String getBaseUrl() {
        if (currentBaseUrl == null) {
            return URL_INTERNATIONAL;
        }
        return currentBaseUrl;
    }

    private void showModeSelector() {
        if (modeSelectorLayout == null) return;
        if (loadingLayout != null) loadingLayout.setVisibility(View.VISIBLE);
        modeSelectorLayout.setVisibility(View.VISIBLE);
        if (loadingImage != null) loadingImage.setVisibility(View.GONE);
        if (loadingText != null) loadingText.setVisibility(View.GONE);
        if (webview != null) webview.setVisibility(View.GONE);
    }

    private void onModeSelected(String baseUrl) {
        currentBaseUrl = baseUrl;

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putString(KEY_BASE_URL, baseUrl).apply();

        if (modeSelectorLayout != null) {
            modeSelectorLayout.setVisibility(View.GONE);
        }
        if (loadingImage != null) loadingImage.setVisibility(View.VISIBLE);
        if (loadingText != null) loadingText.setVisibility(View.VISIBLE);

        if (NetUtil.isNetworkConnected(this)) {
            webview.loadUrl(baseUrl + "/app");
        } else {
            showNetworkErrorPage();
        }
    }

    private void resetRegionSelection() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().remove(KEY_BASE_URL).apply();
        currentBaseUrl = null;

        showModeSelector();
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

    /**
     * 统一展示本地的网络错误页面，供首次进入和加载失败时复用
     */
    private void showNetworkErrorPage() {
        if (webview == null) return;
        String customHtml = FileUtil.readFile(
                getApplicationContext(),
                "NetworkError.html"
        );

        webview.loadDataWithBaseURL(
                getBaseUrl() + "/",
                customHtml,
                "text/html",
                "UTF-8",
                null
        );
    }
}
