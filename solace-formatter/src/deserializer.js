const util = require('util')
const oflib = require('oflib-node')
const decode = require('./decoder.js')

function unpackData(data) {
    const switchStream = new oflib.Stream();
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
            const formatted = data.slice(0, 8)
            console.log("formatted: ")
            formatted[3] = 8
            console.dir(formatted)

            return unpackData(formatted)
        }
    }

    return msgs
}

module.exports = {
    unpack: unpackData
}
