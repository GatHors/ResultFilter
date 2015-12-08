#!/bin/bash
cd $(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
source /etc/profile
username=$1
hadoop fs -rm -r user/$username/input/*
hadoop fs -rm -r user/$username/output/*
#touch beforehadoop.txt
hadoop jar /home/gathors/proj/v-opencv/FeatureMatching/build/libs/FeatureMatching.jar $username request.jpg >/home/gathors/proj/v-opencv/user/$username/out.log
#touch afterhadoop.txt
mkdir /home/gathors/proj/v-opencv/user/$username/tmp
hadoop fs -get user/$username/output/*/part-r-00000 /home/gathors/proj/v-opencv/user/$username/tmp/
java -jar /home/gathors/proj/v-opencv/ResultFilter/build/libs/ResultFilter.jar $username 0.8 0.1
rm /home/gathors/apache-tomcat-8.0.28/webapps/image/images/$username/* 
i=1
while read line; do
    	filename=$(echo $line | awk '{print $1}')
	cp /home/gathors/pre/$filename /home/gathors/apache-tomcat-8.0.28/webapps/image/images/$username/$(printf "%02d\n" $i).jpg 
	i=$(( $i + 1 ))   
done < /home/gathors/proj/v-opencv/user/$username/tmp/output
#rm -rf /home/gathors/proj/v-opencv/user/$username/tmp
