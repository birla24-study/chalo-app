package com.chalo.app;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.webkit.WebView;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import com.getcapacitor.BridgeActivity;
import com.getcapacitor.BridgeWebViewClient;
import java.util.Locale;

public class MainActivity extends BridgeActivity {

    private float topInsetDp = 0f;
    private float bottomInsetDp = 0f;
    private float leftInsetDp = 0f;
    private float rightInsetDp = 0f;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Force light mode programmatically
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        
        // Fit the layout content between the status and navigation/system bars
        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, true);

        // Force fitsSystemWindows programmatically on root content and WebView views
        View contentView = findViewById(android.R.id.content);
        if (contentView != null) {
            contentView.setFitsSystemWindows(true);
        }

        if (getBridge() != null && getBridge().getWebView() != null) {
            View webView = getBridge().getWebView();
            webView.setFitsSystemWindows(true);
            android.view.ViewParent parent = webView.getParent();
            if (parent instanceof View) {
                ((View) parent).setFitsSystemWindows(true);
            }
        }

        // Set up Android ViewCompat window insets listener to dynamically fetch safe area padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            Insets navigationBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            Insets displayCutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout());

            // Compute absolute safe dimensions avoiding overlapping cuts
            int top = Math.max(statusBars.top, displayCutout.top);
            int bottom = Math.max(navigationBars.bottom, displayCutout.bottom);
            int left = Math.max(navigationBars.left, displayCutout.left);
            int right = Math.max(navigationBars.right, displayCutout.right);

            // Programmatically apply system bar heights as margins to the WebView container
            if (getBridge() != null && getBridge().getWebView() != null) {
                View webView = getBridge().getWebView();
                android.view.ViewGroup.LayoutParams lp = webView.getLayoutParams();
                if (lp instanceof android.view.ViewGroup.MarginLayoutParams) {
                    android.view.ViewGroup.MarginLayoutParams marginLp = (android.view.ViewGroup.MarginLayoutParams) lp;
                    marginLp.topMargin = top;
                    marginLp.bottomMargin = bottom;
                    marginLp.leftMargin = left;
                    marginLp.rightMargin = right;
                    webView.setLayoutParams(marginLp);
                }
            }

            // Since the WebView is constrained between the status bar and bottom system navigation,
            // the relative safe area insets inside the WebView are zero.
            topInsetDp = 0f;
            bottomInsetDp = 0f;
            leftInsetDp = 0f;
            rightInsetDp = 0f;

            // Trigger instant CSS custom property injector
            injectSafeAreInsets();

            return insets;
        });
        
        // Handle back button / gesture to navigate back in webview history
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                WebView webView = getBridge().getWebView();
                if (webView != null && webView.canGoBack()) {
                    webView.goBack();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                    setEnabled(true);
                }
            }
        });
        
        // Force status bar and navigation bar colors to white
        window.setStatusBarColor(Color.WHITE);
        window.setNavigationBarColor(Color.WHITE);
        
        // Ensure status bar and navigation bar icons are dark (for light/white backgrounds)
        WindowInsetsControllerCompat windowInsetsController = WindowCompat.getInsetsController(window, window.getDecorView());
        if (windowInsetsController != null) {
            windowInsetsController.setAppearanceLightStatusBars(true);
            windowInsetsController.setAppearanceLightNavigationBars(true);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        if (getBridge() != null && getBridge().getWebView() != null) {
            WebView webView = getBridge().getWebView();

            // Disable overscroll bubblegum animation on the root WebView
            webView.setOverScrollMode(View.OVER_SCROLL_NEVER);

            // Register custom WebViewClient extending Capacitor's BridgeWebViewClient
            webView.setWebViewClient(new BridgeWebViewClient(getBridge()) {
                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    // Force inject safe area custom variables on page load completion
                    injectSafeAreInsets();
                }
            });
        }
    }

    private void injectSafeAreInsets() {
        if (getBridge() != null && getBridge().getWebView() != null) {
            getBridge().getWebView().post(() -> {
                String js = String.format(Locale.US,
                    "(function() {" +
                    "  const r = document.documentElement;" +
                    "  r.style.setProperty('--safe-area-inset-top', '%.2fpx');" +
                    "  r.style.setProperty('--safe-area-inset-bottom', '%.2fpx');" +
                    "  r.style.setProperty('--safe-area-inset-left', '%.2fpx');" +
                    "  r.style.setProperty('--safe-area-inset-right', '%.2fpx');" +
                    "})();",
                    topInsetDp, bottomInsetDp, leftInsetDp, rightInsetDp
                );
                getBridge().getWebView().evaluateJavascript(js, null);
            });
        }
    }
}
