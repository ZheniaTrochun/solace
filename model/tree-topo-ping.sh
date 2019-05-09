#!/usr/bin/env bash

docker-compose -f ../docker-compose.yaml build
docker-compose -f ../docker-compose.yaml up -d

sleep 30

echo $(docker-compose -f ../docker-compose.yaml ps)

sleep 30

mn --switch ovs --controller remote,ip=127.0.0.1,port=6633 --topo tree,depth=2,fanout=2 --test pingall