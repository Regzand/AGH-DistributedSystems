#!/bin/bash

gnome-terminal -- ./apache-zookeeper/bin/zkCli.sh -server localhost:2181
gnome-terminal -- ./apache-zookeeper/bin/zkCli.sh -server localhost:2182
gnome-terminal -- ./apache-zookeeper/bin/zkCli.sh -server localhost:2183

