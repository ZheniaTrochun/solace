var util = require('util')
var oflib = require('oflib-node')
var decode = require('./decoder.js')

var switchStream = new oflib.Stream();

module.exports = {
    unpack: (data) => {
        console.dir(data)
        const msgs = switchStream.process(data)
        msgs.forEach(msg => {
            if (msg.hasOwnProperty('message')) {
                const type = obj.message.header.type
                // decode ethernet message
                if (type == 'OFPT_PACKET_IN') {
                    const packet = decode.decodeethernet(obj.message.body.data, 0)
                    console.dir(packet)
                    msg.message.decodedEthernet = packet
                }
            } else {
                util.log('Error: Message is unparseable')
                console.dir(data)
            }
        })
        if (msgs.length >= 1) {
            return msgs[0]
        } else {
            util.log("ZERO decoded Error")
            console.dir(msgs)
        }
    }
}
