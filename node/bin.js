#!/usr/bin/env node

var path = require('path')
var proc = require('child_process')
var minimist = require('minimist')

var argv = minimist(process.argv.slice(2), {
	alias: {
		b: 'build-tools',
		o: 'out'
	},
	default: {
		o: 'app.apk'
	}
})

switch (argv._[0]) {
	case "install":
		proc.spawnSync('cp', ['package.json', '../app/src/main/assets/'], {
			cwd: __dirname,
			stdio: 'inherit'
		});

		proc.spawnSync('npm', ['i', '--production'], {
			cwd: path.join(__dirname, '../app/src/main/assets/'),
			stdio: 'inherit'
		})
		break
	default:
		proc.spawnSync('npm', ['run', 'build'], {
			cwd: __dirname,
			stdio: 'inherit'
		})

		proc.spawnSync('cp', ['-r', 'src/public', '../app/src/main/assets/node'], {
			cwd: __dirname,
			stdio: 'inherit'
		})

		proc.spawnSync('cp', ['-r', 'src/sites', '../app/src/main/assets/node'], {
			cwd: __dirname,
			stdio: 'inherit'
		})

		proc.spawnSync('cp', ['-r', 'src/views', '../app/src/main/assets/node'], {
			cwd: __dirname,
			stdio: 'inherit'
		})

		proc.spawnSync('./gradlew', ['assembleDebug'], {
			cwd: path.join(__dirname, '..'),
			stdio: 'inherit'
		})

		proc.spawnSync('adb', ['-d', 'install', 'app/build/outputs/apk/debug/app-debug.apk'], {
			cwd: path.join(__dirname, '..'),
			stdio: 'inherit'
		})

		proc.spawnSync('adb', ['-d', 'shell', 'am', 'start', '-n', 'cn.inu1255.soulsign/.MainActivity'], {
			stdio: 'inherit'
		})
}