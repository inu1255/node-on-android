package cn.inu1255.soulsign.proxy.core;

import cn.inu1255.soulsign.proxy.tunnel.Config;
import cn.inu1255.soulsign.proxy.tunnel.RawTunnel;
import cn.inu1255.soulsign.proxy.tunnel.Tunnel;
import cn.inu1255.soulsign.proxy.tunnel.httpconnect.HttpConnectConfig;
import cn.inu1255.soulsign.proxy.tunnel.httpconnect.HttpConnectTunnel;

import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import cn.inu1255.soulsign.proxy.tunnel.shadowsocks.ShadowsocksConfig;
import cn.inu1255.soulsign.proxy.tunnel.shadowsocks.ShadowsocksTunnel;

public class TunnelFactory {

    public static Tunnel wrap(SocketChannel channel, Selector selector) {
        return new RawTunnel(channel, selector);
    }

    public static Tunnel createTunnelByConfig(InetSocketAddress destAddress, Selector selector) throws Exception {
        if (destAddress.isUnresolved()) {
            Config config = ProxyConfig.Instance.getDefaultTunnelConfig(destAddress);
            if (config instanceof HttpConnectConfig) {
                return new HttpConnectTunnel((HttpConnectConfig) config, selector);
            }
            if (config instanceof ShadowsocksConfig) {
                return new ShadowsocksTunnel((ShadowsocksConfig) config, selector);
            }
            throw new Exception("The config is unknown.");
        } else {
            return new RawTunnel(destAddress, selector);
        }
    }

}
