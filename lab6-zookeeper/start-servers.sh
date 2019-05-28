#!/bin/bash

rm -rf /tmp/zookeeper

mkdir -p /tmp/zookeeper/zoo1 /tmp/zookeeper/zoo2 /tmp/zookeeper/zoo3

echo "1" >> /tmp/zookeeper/zoo1/myid
echo "2" >> /tmp/zookeeper/zoo2/myid
echo "3" >> /tmp/zookeeper/zoo3/myid

gnome-terminal -- ./apache-zookeeper/bin/zkServer.sh start-foreground zoo1.cfg
gnome-terminal -- ./apache-zookeeper/bin/zkServer.sh start-foreground zoo2.cfg
gnome-terminal -- ./apache-zookeeper/bin/zkServer.sh start-foreground zoo3.cfg

