#!/usr/bin/env bash

java -Dfile.encoding=UTF-8 -cp ./dummy-web-crawler.jar cn.xiayf.code.App -c ./conf.properties -f $1 -a $2
