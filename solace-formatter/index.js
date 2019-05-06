'use strict'

const express = require('express')
const util = require('util')
const bodyParser = require('body-parser')
const serializer = require('./src/serializer.js')
const deserializer = require('./src/deserializer.js')

const PORT = 8080

const app = express()

app.use(bodyParser.urlencoded({ extended: false }))
app.use(bodyParser.json())

app.get('/ping', (req, res) => res.send("pong"))

app.post('/pack', (req, res) => {
	console.log("/pack request received")
	console.dir(req)
	res.send(serializer.packMessage(req.body.message, req.body.type))
})

app.post('/unpack', (req, res) => {
	console.log("/unpack request received")
	console.dir(req)
	res.send(deserializer.unpack(req.body))
})

app.listen(PORT, () => util.log(`Formatter started on port ${PORT}`))