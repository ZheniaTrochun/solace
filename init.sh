#!/usr/bin/env bash

docker pull influxdb
docker pull grafana/grafana
docker pull redis
docker pull telegraf
docker pull hseeberger/scala-sbt

mkdir -p /tmp/docker/grafana/data
mkdir -p /tmp/docker/influxdb/data

mkdir -p ./grafana/data
mkdir -p ./influxdb/data
