#!/bin/bash

#Create admin user
ssh-keygen -f $GERRIT_HOME/.ssh/id_rsa -t rsa -N ''
publicKey=$(cat $GERRIT_HOME/.ssh/id_rsa.pub) && sed -i  -- "s|<%= @userkey %>|$publicKey|g" setupAdminUser.sql

cd gerrit && java -jar $GERRIT_WAR gsql < ../setupAdminUser.sql

#Start gerrit to create projects and users
/home/gerrit2/gerrit/bin/gerrit.sh start

ssh -oStrictHostKeyChecking=no -p 29418 -l admin localhost gerrit create-project 'egerrit/test'

ssh -oStrictHostKeyChecking=no -p 29418 -l admin localhost gerrit create-project 'egerrit/RCPTTtest'

#Need to flush the caches, otherwise the creation of the first account is failing
ssh -oStrictHostKeyChecking=no -p 29418 -l admin localhost gerrit flush-caches

ssh -oStrictHostKeyChecking=no -p 29418 -l admin localhost gerrit create-account --full-name test1 --email test1@localhost.com --http-password test1 test1

ssh -oStrictHostKeyChecking=no -p 29418 -l admin localhost gerrit create-account --full-name test2 --email test2@localhost.com --http-password test2 test2

/home/gerrit2/gerrit/bin/gerrit.sh stop

