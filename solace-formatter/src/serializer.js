const util = require('util')
const oflib = require('oflib-node/lib/oflib.js');
const ofpp = require('oflib-node/lib/ofp-1.1/ofp.js')

const h_sizes = { //fix
  "ofpt_header": 8,
  "ofpt_hello": 8,
  "ofpt_echo_reply": 8,
  "ofpt_echo_request": 8,
  "ofpt_features_request": 8,
  "ofpt_flow_mod": 72 + 8,
  "ofpt_packet_out": 16 + 8
}

module.exports = {
	packMessage: (type, obj) => {
		console.dir(type)
		console.dir(obj)
		const typelower = type.toLowerCase().replace("ofpt_", "ofp_")
		const bufsize = h_sizes[typelower] // todo
		const buf = new Buffer(bufsize)
//		const buf = new Buffer(1024) // todo

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