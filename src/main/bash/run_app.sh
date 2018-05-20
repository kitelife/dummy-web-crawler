#!/usr/bin/env bash

/home/work/risk/jdk-1.8-8u20/bin/java \
    -Dfile.encoding=UTF-8 \
    -cp ./risk-web-crawler.jar \
    com.baidu.cloud.App \
    -c ./conf.properties \
    -f $1 \
    -a $2