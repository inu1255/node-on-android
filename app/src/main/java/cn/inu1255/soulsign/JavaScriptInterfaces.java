package cn.inu1255.soulsign;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.provider.Settings;
import android.security.KeyChain;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.cert.X509Certificate;

import cn.inu1255.soulsign.common.JSON;
import cn.inu1255.soulsign.common.PermissionUtil;
import cn.inu1255.soulsign.common.StatusBarUtil;
import cn.inu1255.soulsign.proxy.core.Constant;
import cn.inu1255.soulsign.proxy.core.LocalVpnService;
import cn.inu1255.soulsign.proxy.core.ProxyConfig;

public class JavaScriptInterfaces {
    private final String TAG = "SOULSIGN";
    private final MainActivity ctx;
    public static int node_http_port;

    JavaScriptInterfaces(MainActivity ctx) {
        this.ctx = ctx;
    }

    @JavascriptInterface
    public void toast(String msg, int duration) {
        Toast.makeText(this.ctx, msg, duration).show();
    }

    @JavascriptInterface
    public void toast(String msg) {
        Toast.makeText(this.ctx, msg, Toast.LENGTH_SHORT).show();
    }

    @JavascriptInterface
    public boolean installCert(String key, String name) {
        String CERT_FILE = Environment.getExternalStorageDirectory() + "/" + key;
        File certFile = new File(CERT_FILE);
        Intent intent = KeyChain.createInstallIntent();
        try {
            FileInputStream certIs = new FileInputStream(CERT_FILE);
            byte[] cert = new byte[(int) certFile.length()];
            certIs.read(cert);
            X509Certificate x509 = X509Certificate.getInstance(cert);
            intent.putExtra(KeyChain.EXTRA_CERTIFICATE, x509.getEncoded());
            intent.putExtra(KeyChain.EXTRA_NAME, name);
            ctx.startActivityForResult(intent, RequestCodes.INSTALL_CERT);  // this works but shows UI
            return true;
        } catch (Exception e) {
            Log.e(TAG, "installCert: " + e.getMessage());
        }
        return false;
    }

    /**
     * 启动代理
     *
     * @param agent 代理地址
     */
    @JavascriptInterface
    public boolean start(String agent) {
        Constant.proxy_ip = agent;
        Intent intent = LocalVpnService.prepare(ctx);
        if (intent == null) {
            ctx.startService(new Intent(ctx, LocalVpnService.class));
            return true;
        }
        ctx.startActivityForResult(intent, RequestCodes.START_VPN_SERVICE);
        return false;
    }

    @JavascriptInterface
    public void stop() {
        if (LocalVpnService.IsRunning) {
            LocalVpnService.IsRunning = false;
        }
    }

    @JavascriptInterface
    public void setDomainProxy(String domains, boolean status) {
        ProxyConfig.Instance.setDomainsProxy(domains.split(","), status);
    }

    @JavascriptInterface
    public String appList() {
        StringBuilder sb = new StringBuilder();
        PackageManager pm = ctx.getPackageManager();
        List<PackageInfo> packageInfos = pm.getInstalledPackages(0);
        for (int i = 0; i < packageInfos.size(); i++) {
            PackageInfo packageInfo = packageInfos.get(i);
            String name = packageInfo.applicationInfo.loadLabel(pm).toString();
            String pkg = packageInfo.packageName;
            int sys = (ApplicationInfo.FLAG_SYSTEM & packageInfo.applicationInfo.flags);
            sb.append(name);
            sb.append(',');
            sb.append(pkg);
            sb.append(',');
            sb.append(sys);
            sb.append('\n');
        }
        return sb.toString();
    }

    @JavascriptInterface
    public String getStatus() {
        Map data = new HashMap();
        data.put("running", LocalVpnService.IsRunning);
        data.put("send", LocalVpnService.m_SentBytes);
        data.put("recv", LocalVpnService.m_ReceivedBytes);
        data.put("all_proxy", ProxyConfig.Instance.m_all_proxy);
        return JSON.stringify(data);
    }

    @JavascriptInterface
    public int getNodeHttpPort() {
        return node_http_port;
    }

    @JavascriptInterface
    public void setDebug(boolean debug) {
        ctx.browser.setWebContentsDebuggingEnabled(debug);
    }

    /**
     * 获取状态栏高度
     */
    @JavascriptInterface
    public int getStatusBarHeight() {
        return StatusBarUtil.getStatusBarHeight(ctx);
    }

    @JavascriptInterface
    public boolean run(String cmd, String data) {
        return MainActivity.run(ctx, cmd, data);
    }

    @JavascriptInterface
    public void setStatusBar(boolean flag) {
        this.run("setStatusBar", flag ? "y" : "n");
    }

    @JavascriptInterface
    public void setStatusBarColor(String color) {
        this.run("setStatusBarColor", color);
    }

    @JavascriptInterface
    public void setTranslucentStatus() {
        this.run("setTranslucentStatus", "");
    }

    @JavascriptInterface
    public void setRootViewFitsSystemWindows(boolean fitSystemWindows) {
        this.run("setRootViewFitsSystemWindows", fitSystemWindows ? "y" : "n");
    }

    @JavascriptInterface
    public void setStatusBarDarkTheme(boolean dark) {
        this.run("setStatusBarDarkTheme", dark ? "y" : "n");
    }

    @JavascriptInterface
    public void openAppSetting() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", ctx.getPackageName(), null);
        intent.setData(uri);
        ctx.startActivityForResult(intent, PermissionUtil.REQUEST_STORAGE);
    }

    @JavascriptInterface
    public boolean shouldShowRequestPermissionRationale(String permission) {
        return ActivityCompat.shouldShowRequestPermissionRationale(ctx, "android.permission." + permission);
    }

    @JavascriptInterface
    public void requestPermission(String permissions) {
        ActivityCompat.requestPermissions(ctx, permissions.split(","), RequestCodes.PERMISSION_REQUEST);
    }

}