#!/bin/bash

ODB_HOME=~/Downloads/odb/orientdb-community-2.2.37

rm log* 
$ODB_HOME/bin/console.sh "connect plocal:target/data1/graphdb/storage admin admin; check database"  > log1
$ODB_HOME/bin/console.sh "connect plocal:target/data2/graphdb/storage admin admin; check database"  > log2
