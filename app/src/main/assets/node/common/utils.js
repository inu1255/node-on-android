"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
function getPort(add) {
    var port;
    if (typeof add === "string")
        port = parseInt(/\d+$/.exec(add)[0]);
    else
        port = add.port;
    return port;
}
exports.getPort = getPort;
