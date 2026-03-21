package cn.kikirepository.tk;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

import cn.kikirepository.tk.BuildConfig;

/**
 * 主 Activity：托管 WebView，加载目标网站
 * 功能：
 *   - Cookie 持久化（CookieManager）
 *   - 文件上传支持（Android 5.0+）
 *   - 返回键回退 WebView 历史
 *   - 错误页处理
 *   - 禁用长按选中
 */
public class MainActivity extends AppCompatActivity {

    /** 目标网站 URL */
    private static final String TARGET_URL = "https://tk.kikirepository.cn/";

    /** 文件选择请求码 */
    private static final int REQUEST_SELECT_FILE = 100;
    private static final int REQUEST_PERMISSION = 101;

    private WebView webView;
    private ProgressBar progressBar;

    /** 文件上传回调（Android 5.0+） */
    private ValueCallback<Uri[]> uploadMessageArray;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化视图
        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);

        // 配置 WebView
        setupWebView();

        // 配置 Cookie
        setupCookieManager();

        // 加载目标 URL
        webView.loadUrl(TARGET_URL);
    }

    /**
     * 配置 WebView 各项参数
     */
    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings settings = webView.getSettings();

        // ── JavaScript ──
        settings.setJavaScriptEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);

        // ── 存储 ──
        settings.setDomStorageEnabled(true);       // localStorage / sessionStorage
        settings.setDatabaseEnabled(true);         // Web SQL Database

        // ── 缓存 ──
        settings.setCacheMode(WebSettings.LOAD_DEFAULT); // 正常时走缓存，离线时用缓存

        // ── 混合内容（HTTPS 页面加载 HTTP 资源） ──
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        // ── 缩放 ──
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);    // 隐藏默认缩放按钮

        // ── 视口 ──
        settings.setUseWideViewPort(true);         // 使用 viewport meta
        settings.setLoadWithOverviewMode(true);    // 页面放大到屏幕宽度

        // ── 文件访问 ──
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        // ── 媒体自动播放 ──
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            settings.setMediaPlaybackRequiresUserGesture(false);
        }

        // ── User-Agent：保留原 UA 并追加标识 ──
        String originalUA = settings.getUserAgentString();
        settings.setUserAgentString(originalUA + " TKAndroidApp/1.0");

        // ── 深色模式支持 ──
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            WebSettingsCompat.setForceDark(settings, WebSettingsCompat.FORCE_DARK_AUTO);
        }

        // ── WebViewClient：在 APP 内打开所有链接 ──
        webView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress(0);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);
                // 页面加载完毕后持久化 Cookie
                CookieManager.getInstance().flush();
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request,
                                         WebResourceError error) {
                super.onReceivedError(view, request, error);
                // 仅对主框架报错时显示提示
                if (request.isForMainFrame()) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(MainActivity.this,
                            "页面加载失败，请检查网络连接", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                // 站外链接在浏览器打开，站内链接在 APP 内打开
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    if (url.contains("kikirepository.cn")) {
                        // 站内链接：在 WebView 内加载
                        return false;
                    } else {
                        // 站外链接：调用系统浏览器
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(intent);
                        return true;
                    }
                }
                // tel: / mailto: 等协议交给系统处理
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            }
        });

        // ── WebChromeClient：支持进度条、文件上传、地理位置 ──
        webView.setWebChromeClient(new WebChromeClient() {

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
                if (newProgress == 100) {
                    progressBar.setVisibility(View.GONE);
                } else {
                    progressBar.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
            }

            // ── 文件上传（Android 5.0+） ──
            @Override
            public boolean onShowFileChooser(WebView webView,
                                             ValueCallback<Uri[]> filePathCallback,
                                             FileChooserParams fileChooserParams) {
                // 先取消之前未完成的上传
                if (uploadMessageArray != null) {
                    uploadMessageArray.onReceiveValue(null);
                    uploadMessageArray = null;
                }
                uploadMessageArray = filePathCallback;

                // 检查存储权限
                if (!checkStoragePermission()) {
                    requestStoragePermission();
                    return false;
                }

                openFileChooser();
                return true;
            }

            // ── 地理位置权限 ──
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin,
                                                           GeolocationPermissions.Callback callback) {
                callback.invoke(origin, true, false);
            }
        });

        // 禁用长按选中
        webView.setOnLongClickListener(v -> true);
        webView.setLongClickable(false);

        // 开启 WebView 调试（仅 Debug 构建）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG);
        }
    }

    /**
     * 配置 CookieManager，启用第三方 Cookie 并持久化
     */
    private void setupCookieManager() {
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        // Android 5.0+ 支持第三方 Cookie
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.setAcceptThirdPartyCookies(webView, true);
        }
    }

    // ──────────────── 文件上传相关 ────────────────

    /**
     * 打开文件选择器
     */
    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        // 支持多选
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(Intent.createChooser(intent, "选择文件"), REQUEST_SELECT_FILE);
    }

    /**
     * 检查存储权限
     */
    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    /**
     * 请求存储权限
     */
    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.READ_MEDIA_IMAGES,
                            Manifest.permission.READ_MEDIA_VIDEO
                    }, REQUEST_PERMISSION);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openFileChooser();
            } else {
                Toast.makeText(this, "需要存储权限才能上传文件", Toast.LENGTH_SHORT).show();
                if (uploadMessageArray != null) {
                    uploadMessageArray.onReceiveValue(null);
                    uploadMessageArray = null;
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SELECT_FILE) {
            if (uploadMessageArray == null) return;
            Uri[] results = null;
            if (resultCode == Activity.RESULT_OK && data != null) {
                // 多文件选择
                if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount();
                    results = new Uri[count];
                    for (int i = 0; i < count; i++) {
                        results[i] = data.getClipData().getItemAt(i).getUri();
                    }
                } else if (data.getData() != null) {
                    results = new Uri[]{data.getData()};
                }
            }
            uploadMessageArray.onReceiveValue(results);
            uploadMessageArray = null;
        }
    }

    // ──────────────── 返回键处理 ────────────────

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    // ──────────────── 生命周期 ────────────────

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
        webView.resumeTimers();
    }

    @Override
    protected void onPause() {
        super.onPause();
        webView.onPause();
        webView.pauseTimers();
        // 暂停时立即持久化 Cookie
        CookieManager.getInstance().flush();
    }

    @Override
    protected void onDestroy() {
        // 销毁时持久化 Cookie 并清理 WebView
        CookieManager.getInstance().flush();
        if (webView != null) {
            webView.stopLoading();
            webView.clearHistory();
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
