"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const express = require("express");
const https = require("https");
const http = require("http");
const net = require("net");
const debug = require("util").debuglog("noginx");
const CertManager = require("node-easy-cert");
const child_process_1 = require("child_process");
const path = require("path");
const request = require("request");
let crtMgr;
function init(dir) {
    crtMgr = new CertManager({
        rootDirPath: dir || 'cert',
        defaultCertAttrs: [
            { name: 'countryName', value: 'CN' },
            { name: 'organizationName', value: 'yoursismine' },
            { shortName: 'ST', value: 'SC' },
            { shortName: 'OU', value: 'https://github.com/inu1255' }
        ]
    });
    return new Promise((resolve, reject) => {
        crtMgr.generateRootCA({
            commonName: 'noginx',
        }, function (error, keyPath, crtPath) {
            if (error && error != 'ROOT_CA_EXISTED')
                return reject(error);
            resolve({ keyPath, crtPath });
        });
    });
}
class App {
    constructor() {
        this.use = function () {
            this.app.use.apply(this.app, arguments);
        };
        this.app = express();
        this.self_https = {};
        this.server = http.createServer(this.app);
        var that = this;
        this.httpFilter = function (url) {
            return !url.endsWith(":443") && !url.endsWith(":8443") && !url.endsWith(":4430");
        };
        this.server.on('connect', function (req, cltSocket, head) {
            var srvSocket;
            var ss = req.url.split(":");
            if (that.httpFilter(req.url)) {
                srvSocket = net.connect({ host: "localhost", port: that.port }, function () {
                    cltSocket.write("HTTP/1.1 200 Connection Established\r\n\r\n");
                    srvSocket.write(head);
                    cltSocket.pipe(srvSocket);
                    srvSocket.pipe(cltSocket);
                    srvSocket.on("error", debug);
                });
            }
            else if (!that.httpsFilter || that.httpsFilter(req.url)) {
                that.fakeSite(ss[0], function (port) {
                    srvSocket = net.connect({ host: "localhost", port }, function () {
                        cltSocket.write("HTTP/1.1 200 Connection Established\r\n\r\n");
                        srvSocket.write(head);
                        cltSocket.pipe(srvSocket);
                        srvSocket.pipe(cltSocket);
                    });
                    srvSocket.on("error", debug);
                });
            }
            else {
                srvSocket = net.connect({ host: ss[0], port: ss[1] }, function () {
                    cltSocket.write("HTTP/1.1 200 Connection Established\r\n\r\n");
                    srvSocket.write(head);
                    cltSocket.pipe(srvSocket);
                    srvSocket.pipe(cltSocket);
                    srvSocket.on("error", debug);
                });
            }
        });
        this.server.on('upgrade', function (req, cltSocket, head) {
            var ss = req.url.split(":");
            var srvSocket = net.connect({ host: ss[0], port: ss[1] });
            cltSocket.write("HTTP/1.1 200 Connection Established\r\n\r\n");
            srvSocket.write(head);
            cltSocket.pipe(srvSocket);
            srvSocket.pipe(cltSocket);
        });
    }
    fakeSite(domain, cb) {
        if (this.self_https[domain]) {
            cb(this.self_https[domain]);
            return;
        }
        crtMgr.getCertificate(domain, (error, key, cert) => {
            if (error)
                console.error(error);
            if (error === 'ROOT_CA_NOT_EXISTS') {
            }
            var httpsServer = https.createServer({ key, cert }, this.app);
            httpsServer.listen(0, () => {
                let addr = httpsServer.address();
                if (typeof addr != "string") {
                    this.self_https[domain] = addr.port;
                    cb(addr.port);
                }
            });
        });
    }
    listen(port, listeningListener) {
        if (!crtMgr)
            init();
        this.app.use(function (req, res, next) {
            forward(req).pipe(res).once('error', next);
        });
        return this.server.listen(port, () => {
            var add = this.server.address();
            if (typeof add === "string")
                this.port = parseInt(/\d+$/.exec(add)[0]);
            else
                this.port = add.port;
            listeningListener(this.port);
        });
    }
    close(cb) {
        this.server.close(cb);
    }
}
function dir(dir) {
    if (crtMgr)
        return Promise.reject('不能重复设置证书目录');
    return init(dir);
}
exports.dir = dir;
;
function install(keyPath) {
    const isWin = /^win/.test(process.platform);
    const certDir = path.dirname(keyPath);
    if (isWin) {
        child_process_1.exec("start .", { cwd: certDir });
    }
    else {
        child_process_1.exec("open .", { cwd: certDir });
    }
}
exports.install = install;
function app() {
    return new App();
}
exports.app = app;
;
function forward(req) {
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
}
exports.forward = forward;
;
