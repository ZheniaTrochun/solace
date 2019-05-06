const util = require('util')
const oflib = require('oflib-node');
const ofpp = require('oflib-node/lib/ofp-1.0/ofp.js')

module.exports = {
	packMessage: (type, obj) => {
		console.dir(type)
		console.dir(obj)
		const typelower = type.toLowerCase()
		const bufsize = ofpp.h_sizes[typelower] // todo
		const buf = new Buffer(bufsize);

		const pack = oflib.pack(obj, buf, 0)

		console.dir(pack)
		if (!('error' in pack)) {
			return pack
		} else {
			util.log("_sendPacket Error packing object " + util.inspect(pack))
		}

	},

	fillBuffer: (obj, buffer) => {
		const pack = oflib.pack(obj, buffer, 0)
	}
}