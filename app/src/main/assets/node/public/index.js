function start(url) {
	_tzy.setDomainProxy('inu1255.cn', true)
	_tzy.start(url)
}

function stop() {
	_tzy.stop()
}

function installCert() {
	_tzy.installCert('rootCA.crt', 'noginx')
}