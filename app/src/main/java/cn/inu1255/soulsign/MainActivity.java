package cn.inu1255.soulsign;

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.inu1255.soulsign.R;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URISyntaxException;

import cn.inu1255.soulsign.common.JSON;
import cn.inu1255.soulsign.common.PermissionUtil;
import cn.inu1255.soulsign.common.StatusBarUtil;
import cn.inu1255.soulsign.common.Utils;
import cn.inu1255.soulsign.proxy.core.LocalVpnService;

public class MainActivity extends AppCompatActivity implements LocalVpnService.onStatusChangedListener {
    public WebView browser;
    private NodeReceiver receiver;
    private final String TAG = "SOULSIGN";

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Intent i = new Intent(MainActivity.this, NodeService.class);
        stopService(i);
        LocalVpnService.removeOnStatusChangedListener(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        receiver = new NodeReceiver();
        IntentFilter filter = new IntentFilter("cn.inu1255.soulsign.ipc");
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter);

        browser = (WebView) findViewById(R.id.webview);
        browser.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (shouldOverrideUrlLoadingByApp(view, url)) {
                    return true;
                }
                return super.shouldOverrideUrlLoading(view, url);
            }
        });
        JavaScriptInterfaces _tzy = new JavaScriptInterfaces(this);
        browser.addJavascriptInterface(_tzy, "_tzy");
        WebSettings settings = browser.getSettings();
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);  // 开启 DOM storage 功能
        settings.setAllowFileAccessFromFileURLs(true); //Maybe you don't need this rule
        settings.setAllowUniversalAccessFromFileURLs(true);
        String appCachePath = this.getApplicationContext().getCacheDir().getAbsolutePath();
        settings.setAppCachePath(appCachePath);
        browser.setWebContentsDebuggingEnabled(true);

        checkStoragePermission();

        new

                Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ServerSocket server = new ServerSocket(0, 5, InetAddress.getByName("127.0.0.1"));

                    Intent i = new Intent(MainActivity.this, NodeService.class);
                    i.putExtra("ipc-port", "" + server.getLocalPort());
                    startService(i);

                    Socket socket = server.accept();
                    BufferedInputStream inp = new BufferedInputStream(socket.getInputStream());
                    BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());
                    BufferedReader br = new BufferedReader(new InputStreamReader(inp));

                    while (true) {
                        String line = br.readLine();
                        int index = line.indexOf(':');
                        String cmd = index < 0 ? line : line.substring(0, index);
                        String data = index < 0 ? "" : line.substring(index + 1);
                        MainActivity.run(MainActivity.this, cmd, data);
                    }
                } catch (Exception err) {
                    err.printStackTrace();
                }
            }
        }).

                start();
    }

    /**
     * 根据url的scheme处理跳转第三方app的业务
     */
    private boolean shouldOverrideUrlLoadingByApp(WebView view, String url) {
        if (url.startsWith("http") || url.startsWith("https") || url.startsWith("ftp")) {
            //不处理http, https, ftp的请求
            return false;
        }
        Intent intent;
        try {
            intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
        } catch (URISyntaxException e) {
            Log.d(TAG, "shouldOverrideUrlLoadingByApp: " + url, e);
            return false;
        }
        intent.setComponent(null);
        try {
            this.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            return false;
        }
        return true;
    }

    private void jsCall(String method, Object data) {
        String s = "";
        if (data != null) s = JSON.stringify(data);
        jsEval(method, s);
    }

    private void jsEval(String method, String data) {
        browser.loadUrl(String.format("javascript:" + method + "(" + data + ")"));
    }

    /**
     * VPN status change
     *
     * @param status
     * @param isRunning
     */
    @Override
    public void onStatusChanged(String status, Boolean isRunning) {
        Log.d(TAG, "onStatusChanged: " + status + "@" + isRunning);
    }

    private class NodeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String cmd = intent.getStringExtra("cmd");
            String data = intent.getStringExtra("data");
            MainActivity.this.onCommand(cmd, data);
        }

    }

    private String onCommand(String cmd, String data) {
        try {
            switch (cmd) {
                case "loadURL":
                    browser.loadUrl(data);
                    break;
                case "setHttpPort":
                    JavaScriptInterfaces.node_http_port = Integer.parseInt(data);
                    break;
                case "setStatusBar":
                    this.setStatusBar("y".equals(data));
                    break;
                case "setStatusBarColor":
                    int color = Color.parseColor(data);
                    StatusBarUtil.setStatusBarColor(this, color);
                    break;
                case "setTranslucentStatus":
                    StatusBarUtil.setTranslucentStatus(this);
                    break;
                case "setRootViewFitsSystemWindows":
                    StatusBarUtil.setRootViewFitsSystemWindows(this, "y".equals(data));
                    break;
                case "setStatusBarDarkTheme":
                    StatusBarUtil.setStatusBarDarkTheme(this, "y".equals(data));
                    break;
                default:
                    Log.d(TAG, "Cmd Not Found: " + cmd);
            }
        } catch (Exception e) {
            Log.e(TAG, "onCommand: " + cmd, e);
        }
        return "";
    }

    public void setStatusBar(boolean flag) {
        Window window = this.getWindow();
        View decorView = window.getDecorView();
        int option;
        if (flag) {
            option = decorView.getSystemUiVisibility();
            option &= ~View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN & ~View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        } else {
            option = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        }
        decorView.setSystemUiVisibility(option);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        switch (requestCode) {
            case RequestCodes.START_VPN_SERVICE:
                if (resultCode == RESULT_OK) {
                    this.startService(new Intent(this, LocalVpnService.class));
                }
                break;
            case RequestCodes.INSTALL_CERT:
                break;
            default:
                super.onActivityResult(requestCode, resultCode, intent);
        }
    }

    static public boolean run(Context ctx, String cmd, String data) {
        Intent in = new Intent("cn.inu1255.soulsign.ipc");
        in.putExtra("cmd", cmd);
        in.putExtra("data", data);
        return LocalBroadcastManager.getInstance(ctx).sendBroadcast(in);
    }

    private void checkStoragePermission() {
        if (!PermissionUtil.hasPermission(this, PermissionUtil.STORAGE)) {
            PermissionUtil.requestPermission(this, PermissionUtil.STORAGE, PermissionUtil.REQUEST_STORAGE);
        } else {
            onGranted();
        }
    }

    private void onGranted() {

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PermissionUtil.REQUEST_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    onGranted();
                break;
            case RequestCodes.PERMISSION_REQUEST:
                String data = "\"" + Utils.join(permissions, ",") + "\n" + Utils.join(grantResults, ",") + "\"";
                jsEval("onPermission", data);
                break;
            default:
                break;
        }
    }

}
