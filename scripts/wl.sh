#!/bin/bash

while true
do

  id=$(docker ps | grep promat-service:devel | cut -d ' ' -f 1)

  if [ ! -z $id ]
  then
    docker logs -f $id
  fi

  sleep 3

done
