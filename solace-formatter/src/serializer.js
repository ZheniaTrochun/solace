const util = require('util')
const oflib = require('oflib-node/lib/oflib.js')

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
		const typelower = type.toLowerCase()
		const bufsize = h_sizes[typelower] // todo
		console.log(`buffer size = ${bufsize}`)
		const buf = new Buffer(bufsize)
//		const buf = new Buffer(1024) // todo
        // todo
        if ((type === 'OFPT_ECHO_REPLY') && (!obj.body)) {
            console.log('formatting OFPT_ECHO_REPLY...')
            obj.body = {}
        }

		const pack = oflib.pack(obj, buf, 0)

		console.dir(pack)
		if (!('error' in pack) && pack.hasOwnProperty('offset')) {
		    console.log("packed successfully, pack result:")
			console.dir(pack)
			console.log("packed successfully, buffer:")
			console.dir(buf)

			return buf.slice(0, pack.offset)
		} else {
			util.log("_sendPacket Error packing object " + util.inspect(pack))
		}

	},

	fillBuffer: (obj, buffer) => {
		const pack = oflib.pack(obj, buffer, 0)
	}
}