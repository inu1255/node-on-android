import express = require("express");
import https = require('https');
import http = require('http');
import net = require("net");
const debug = require("util").debuglog("noginx");
import * as CertManager from 'node-easy-cert'
import { exec } from 'child_process';
import * as path from 'path'
import { ApplicationRequestHandler } from "express-serve-static-core";
import * as request from 'request'

let crtMgr: CertManager;

function init(dir?: string) {
    crtMgr = new CertManager({
        rootDirPath: dir || 'cert',
        defaultCertAttrs: [
            { name: 'countryName', value: 'CN' },
            { name: 'organizationName', value: 'yoursismine' },
            { shortName: 'ST', value: 'SC' },
            { shortName: 'OU', value: 'https://github.com/inu1255' }
        ]
    });
    return new Promise<{ keyPath: string, crtPath: string }>((resolve, reject) => {
        crtMgr.generateRootCA({
            commonName: 'noginx',
        }, function(error, keyPath, crtPath) {
            // 如果根证书已经存在，且没有设置overwrite为true，则需要捕获
            if (error && error != 'ROOT_CA_EXISTED') return reject(error)
            resolve({ keyPath, crtPath })
        });
    });
}

class App {
    protected app: express.Express;
    protected self_https: { [key: string]: number };
    protected server: http.Server;
    httpFilter: (url: string) => boolean;
    httpsFilter: (url: string) => boolean;
    port: number;
    use: ApplicationRequestHandler<express.Express>;
    constructor() {
        this.use = function() {
            this.app.use.apply(this.app, arguments);
        } as any;
        this.app = express();
        this.self_https = {};
        this.server = http.createServer(this.app);
        var that = this;
        this.httpFilter = function(url) {
            return !url.endsWith(":443") && !url.endsWith(":8443") && !url.endsWith(":4430");
        };
        this.server.on('connect', function(req, cltSocket, head) {
            var srvSocket: net.Socket;
            var ss = req.url.split(":");
            if (that.httpFilter(req.url)) {
                srvSocket = net.connect({ host: "localhost", port: that.port }, function() {
                    cltSocket.write("HTTP/1.1 200 Connection Established\r\n\r\n");
                    srvSocket.write(head);
                    cltSocket.pipe(srvSocket);
                    srvSocket.pipe(cltSocket);
                    srvSocket.on("error", debug);
                });
            } else if (!that.httpsFilter || that.httpsFilter(req.url)) {
                that.fakeSite(ss[0], function(port) {
                    srvSocket = net.connect({ host: "localhost", port }, function() {
                        cltSocket.write("HTTP/1.1 200 Connection Established\r\n\r\n");
                        srvSocket.write(head);
                        cltSocket.pipe(srvSocket);
                        srvSocket.pipe(cltSocket);
                    });
                    srvSocket.on("error", debug);
                });
            } else {
                srvSocket = net.connect({ host: ss[0], port: ss[1] }, function() {
                    cltSocket.write("HTTP/1.1 200 Connection Established\r\n\r\n");
                    srvSocket.write(head);
                    cltSocket.pipe(srvSocket);
                    srvSocket.pipe(cltSocket);
                    srvSocket.on("error", debug);
                });
            }
        });

        // TODO: 代理 WebSocket
        this.server.on('upgrade', function(req, cltSocket, head) {
            var ss = req.url.split(":");
            var srvSocket = net.connect({ host: ss[0], port: ss[1] });
            cltSocket.write("HTTP/1.1 200 Connection Established\r\n\r\n");
            srvSocket.write(head);
            cltSocket.pipe(srvSocket);
            srvSocket.pipe(cltSocket);
        });
    }
    fakeSite(domain: string, cb: (port: number) => void) {
        if (this.self_https[domain]) {
            cb(this.self_https[domain]);
            return;
        }
        crtMgr.getCertificate(domain, (error, key, cert) => {
            if (error) console.error(error)
            // 如果根证书还没有生成，需要先生成根证书
            if (error === 'ROOT_CA_NOT_EXISTS') {
                // handle the issue
            }
            var httpsServer = https.createServer({ key, cert }, this.app);
            httpsServer.listen(0, () => {
                let addr = httpsServer.address()
                if (typeof addr != "string") {
                    this.self_https[domain] = addr.port;
                    cb(addr.port);
                }
            });
        });
    }
    listen(port: number, listeningListener?: (port: number) => void): http.Server {
        if (!crtMgr) init();
        // 默认: 直接转发请求
        this.app.use(function(req, res, next) {
            forward(req).pipe(res).once('error', next);
        });
        return this.server.listen(port, () => {
            var add = this.server.address()
            if (typeof add === "string")
                this.port = parseInt(/\d+$/.exec(add)[0])
            else
                this.port = add.port;
            listeningListener(this.port)
        });
    }
    close(cb: (err?: Error) => void) {
        this.server.close(cb);
    }
}

/**
 * 设置证书保存目录
 * @param {String} dir 证书保存目录
 */
export function dir(dir: string) {
    if (crtMgr) return Promise.reject('不能重复设置证书目录');
    return init(dir);
};

export function install(keyPath: string) {
    // 证书需要被安装并信任，可以在此打开该目录并给出提示，也可以进行其他操作
    const isWin = /^win/.test(process.platform);
    const certDir = path.dirname(keyPath);
    if (isWin) {
        exec("start .", { cwd: certDir });
    } else {
        exec("open .", { cwd: certDir });
    }
}

/**
 * 获取express实例
 */
export function app() {
    return new App();
};

/**
 * 转发请求
 * @param {express.Request} req 
 */
export function forward(req: express.Request) {
    var url = req.url;
    if (url.indexOf(":") <= 0) {
        url = req.protocol + "://" + req.headers["host"] + url;
    }
    return request({
        url,
        method: req.method,
        headers: req.headers,
        body: req.body || req,
        maxRedirects: 0,
    });
};