import * as net from 'net';

export function getPort(add: string | net.AddressInfo) {
    var port: number;
    if (typeof add === "string")
        port = parseInt(/\d+$/.exec(add)[0])
    else
        port = add.port;
    return port;
}