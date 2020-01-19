"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const noginx = require("./noginx");
const cofs = require("fs-extra");
const proxy = noginx.app();
proxy.httpFilter = url => {
    return !/:(443|4430|8443)$/.test(url);
};
proxy.httpsFilter = url => {
    console.log(url);
    return /:(443|4430|8443)$/.test(url);
};
proxy.use(async function (req, res, next) {
    if (req.method.toLowerCase() === "get") {
        let filename = process.cwd() + '/sites/' + req.headers["host"] + req.path;
        if (await cofs.pathExists(filename)) {
            res.setHeader('Access-Control-Allow-Origin', '*');
            res.sendFile(filename);
            return;
        }
    }
    next();
});
function start(dir) {
    return noginx.dir(dir).then(_ => new Promise((resolve, reject) => {
        let server = proxy.listen(0, resolve);
        server.once('error', reject);
    }));
}
exports.start = start;
