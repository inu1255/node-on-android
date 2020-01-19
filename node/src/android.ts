import * as net from 'net';

var port = Number(process.argv[process.argv.length - 1])
var sock: { write: Function }
if (process.platform == 'darwin') {
    sock = { write: console.log }
} else {
    sock = net.connect(port, '127.0.0.1')
}

export function loadURL(u: string) {
    sock.write("loadURL:" + u + "\n")
}

export function setHttpPort(port: number) {
    port = Math.floor(port)
    if (port < 1) throw "port should > 0"
    sock.write("setHttpPort:" + port + "\n")
}