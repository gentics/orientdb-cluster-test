#!/bin/bash

ODB_HOME=~/Downloads/odb/orientdb-community-3.0.15

rm log* 
$ODB_HOME/bin/console.sh "connect plocal:target/data1/storage admin admin; check database -v" >  log1
$ODB_HOME/bin/console.sh "connect plocal:target/data2/storage admin admin; check database -v"  > log2
$ODB_HOME/bin/console.sh "connect plocal:target/data3/storage admin admin; check database -v"  > log3
