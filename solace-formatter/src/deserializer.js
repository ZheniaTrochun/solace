var util = require('util')
var oflib = require('oflib-node')
var decode = require('./decoder.js')

var switchStream = new oflib.Stream();

module.exports = {
    unpack: (data) => {
        console.dir(data)
        const msgs = switchStream.process(Buffer.from(data, 'utf-8'))
        msgs.forEach(msg => {
            if (msg.hasOwnProperty('message')) {
                console.log("message parsed")
                console.dir(msg)
                const type = msg.message.header.type
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
        console.dir(msgs)
        if ((msgs.length == 1) && (msgs[0].hasOwnProperty('error'))) {
            if (msgs[0].error.desc.includes('OFPT_HELLO message at offset 0 has invalid length (') && (data.length > 8)) {
                console.log("trying to drop not needed data and retry")
                return unpack(data.substring(0, 8))
            }
        }
        return msgs
    }
}
