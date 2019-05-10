const util = require('util')
const oflib = require('oflib-node/lib/oflib.js');
const ofpp = require('oflib-node/lib/ofp-1.1/ofp.js')

module.exports = {
	packMessage: (type, obj) => {
		console.dir(type)
		console.dir(obj)
		const typelower = type.toLowerCase().replace("ofpt_", "ofp_")
		const bufsize = ofpp.sizes[typelower] // todo
//		const buf = new Buffer(bufsize)
		const buf = new Buffer(1024) // todo

		const pack = oflib.pack(obj, buf, 0)

		console.dir(pack)
		if (!('error' in pack)) {
		    console.log("packed successfully, pack result:")
			console.dir(pack)
			console.log("packed successfully, buffer:")
			console.dir(buf)
			return buf
		} else {
			util.log("_sendPacket Error packing object " + util.inspect(pack))
		}

	},

	fillBuffer: (obj, buffer) => {
		const pack = oflib.pack(obj, buffer, 0)
	}
}