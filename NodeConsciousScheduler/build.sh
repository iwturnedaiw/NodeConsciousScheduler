#!/bin/bash
LOG=./log/build_`date +%Y%m%d%H%M`.log
PKG=nodeconsciousscheduler
SRC=./src
TGT=./build/classes



build() {
  which java
  RET=$?

  if [ $RET -ne 0 ]; then
    echo "Not Found java"
    exit
  fi

  javac -sourcepath ${SRC}/${PKG}/*.java -d ${TGT}
  RET=$?

  echo -n "Building is " 
  if [ $RET -eq 0 ]; then
    echo "Success"
  else
    echo "Failure"
  fi
}


build 2>&1 | tee -a ${LOG}