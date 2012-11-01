#!/bin/bash

# test script for serengeti

echo "************************start run HDFS test*****************"
serengeti --cmdfile /tmp/impala/hdfs.script
echo "************************end run HDFS test*****************"

echo "************************start run MR test*****************"
serengeti --cmdfile /tmp/impala/mapreduce.script
echo "************************end run MR test*****************"

echo "************************start run Pig test*****************"
serengeti --cmdfile /tmp/impala/pig.script
echo "************************end run Pig test*****************"

echo "************************start run Hive test*****************"
serengeti --cmdfile /tmp/impala/hive.script
echo "************************end run Hive test*****************"


echo "************************start run R test*****************"
serengeti --cmdfile /tmp/impala/r.script
echo "************************end run R test*****************"
