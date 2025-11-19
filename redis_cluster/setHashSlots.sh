#!/bin/bash
for i in $(seq 10923 16383)
do
/usr/local/bin/redis-cli -h 172.18.0.4 -p 6381 cluster addslots $i
done
