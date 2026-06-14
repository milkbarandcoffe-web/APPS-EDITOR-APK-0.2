package com.danielz.bollaviewer;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int REQ_OVERLAY = 1001;
    private static final int REQ_SETTINGS = 1002;
    private WebView webView;
    private TextView xButton;
    private SharedPreferences prefs;
    private GestureDetector xGesture;
    private boolean bollaAttiva = false;
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        prefs = getSharedPreferences("bollaviewer", MODE_PRIVATE);
        setupUI();
        if (!Settings.canDrawOverlays(this))
            startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())), REQ_OVERLAY);
    }

    private void setupUI() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xFF000000);
        webView = new WebView(this);
        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setLoadWithOverviewMode(true);
        ws.setUseWideViewPort(true);
        ws.setBuiltInZoomControls(false);
        ws.setDisplayZoomControls(false);
        ws.setUserAgentString("Mozilla/5.0 (Linux; Android 13; SM-S911B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36");
        android.webkit.CookieManager.getInstance().setAcceptCookie(true);
        android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        webView.setWebViewClient(new WebViewClient());
        String url = prefs.getString("url_webview", "");
        if (!url.isEmpty()) webView.loadUrl(url);
        root.addView(webView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        xButton = new TextView(this);
        xButton.setText("x");
        xButton.setTextSize(18);
        xButton.setTextColor(0xFFFFFFFF);
        xButton.setAlpha(prefs.getInt("x_alpha", 180) / 255f);
        xButton.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams xp = new FrameLayout.LayoutParams(dp(44), dp(44));
        xp.gravity = Gravity.TOP | Gravity.END;
        xp.topMargin = dp(8); xp.rightMargin = dp(8);
        root.addView(xButton, xp);

        xGesture = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onSingleTapConfirmed(MotionEvent e) { openSettings(); return true; }
            @Override public boolean onDoubleTap(MotionEvent e) { toggleBolla(); return true; }
        });
        xButton.setOnTouchListener((v, e) -> { xGesture.onTouchEvent(e); return true; });
        xButton.setOnLongClickListener(v -> { hideXTemporarily(); return true; });
        setContentView(root);
    }

    private void hideXTemporarily() {
        xButton.setVisibility(android.view.View.INVISIBLE);
        Toast.makeText(this, "X nascosta per 10s", Toast.LENGTH_SHORT).show();
        handler.postDelayed(() -> xButton.setVisibility(android.view.View.VISIBLE), 10000);
    }

    private void openSettings() {
        startActivityForResult(new Intent(this, SettingsActivity.class), REQ_SETTINGS);
    }

    private void toggleBolla() {
        if (bollaAttiva) {
            stopService(new Intent(this, BubbleService.class));
            bollaAttiva = false;
            Toast.makeText(this, "Bolla disattivata", Toast.LENGTH_SHORT).show();
        } else {
            startForegroundService(new Intent(this, BubbleService.class));
            bollaAttiva = true;
            Toast.makeText(this, "Bolla attivata", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        if (req == REQ_SETTINGS) {
            String url = prefs.getString("url_webview", "");
            if (!url.isEmpty() && !url.equals(webView.getUrl())) {
                webView.loadUrl(url);
            }
            xButton.setAlpha(prefs.getInt("x_alpha", 180) / 255f);
        }
        if (req == REQ_OVERLAY && !Settings.canDrawOverlays(this))
            Toast.makeText(this, "Permesso overlay necessario", Toast.LENGTH_LONG).show();
    }

    @Override public void onBackPressed() { if (webView.canGoBack()) webView.goBack(); else super.onBackPressed(); }
    @Override protected void onPause() { super.onPause(); android.webkit.CookieManager.getInstance().flush(); }
    @Override protected void onResume() { super.onResume(); }
    @Override protected void onDestroy() { webView.destroy(); super.onDestroy(); }
    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density); }
}
