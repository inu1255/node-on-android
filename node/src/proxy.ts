import * as noginx from "./noginx";
import * as cofs from "fs-extra";

const proxy = noginx.app();

proxy.httpFilter = url => {
    return !/:(443|4430|8443)$/.test(url)
}

proxy.httpsFilter = url => {
    console.log(url)
    return /:(443|4430|8443)$/.test(url)
}

proxy.use(async function(req, res, next) {
    if (req.method.toLowerCase() === "get") {
        let filename = process.cwd() + '/sites/' + req.headers["host"] + req.path;
        if (await cofs.pathExists(filename)) {
            res.setHeader('Access-Control-Allow-Origin', '*')
            res.sendFile(filename)
            return
        }
    }
    next();
});

export function start(dir?: string): Promise<number> {
    return noginx.dir(dir).then(_ => new Promise((resolve, reject) => {
        let server = proxy.listen(0, resolve);
        server.once('error', reject)
    }))
}