"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const net = require("net");
var port = Number(process.argv[process.argv.length - 1]);
var sock;
if (process.platform == 'darwin') {
    sock = { write: console.log };
}
else {
    sock = net.connect(port, '127.0.0.1');
}
function loadURL(u) {
    sock.write("loadURL:" + u + "\n");
}
exports.loadURL = loadURL;
function setHttpPort(port) {
    port = Math.floor(port);
    if (port < 1)
        throw "port should > 0";
    sock.write("setHttpPort:" + port + "\n");
}
exports.setHttpPort = setHttpPort;
