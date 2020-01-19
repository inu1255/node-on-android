"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const express = require("express");
const android = require("./android");
const proxy = require("./proxy");
const utils = require("./common/utils");
const cofs = require("fs-extra");
const https = require("https");
process.on('uncaughtException', console.error);
const app = express();
function startApp() {
    return new Promise((resolve, reject) => {
        let server = app.listen(0, function () {
            var port = utils.getPort(server.address());
            resolve(port);
        });
        server.once('error', reject);
    });
}
async function main() {
    process.chdir(__dirname);
    let proxyport = await proxy.start();
    cofs.createReadStream('cert/rootCA.crt').pipe(cofs.createWriteStream('/sdcard/rootCA.crt'));
    app.set("view engine", "pug");
    app.set("views", __dirname + "/views");
    app.use(express.static('public'));
    app.get("/api/proxy/port", function (req, res) {
        res.json({ no: 200, data: proxyport });
    });
    app.get("/api/proxy/test", function (req, res) {
        https.get("https://ext.gaomuxuexi.com/hello", function (res0) {
            res0.pipe(res);
        }).end();
    });
    app.get('/', function (req, res) {
        res.render('index', { proxyport });
    });
    startApp().then(port => {
        android.setHttpPort(port);
        console.log(port);
        console.log(process.cwd());
        console.log(__dirname);
        android.loadURL(`http://localhost:${port}?port=` + proxyport);
    });
}
main().catch(console.error);
