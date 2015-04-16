# Docker MQ Producer

Docker image that spams an AMQ instance(s) with messages. Useful for testing.

This is based upon the fabric8 mq producer project in https://github.com/fabric8io/quickstarts. The main difference is the use of a regular activemq component instead of hte fabric8 kubernetes aware component

## Building & Running

    mvn install docker:build
    docker run -it -e AMQ_BROKER_URL=tcp://mybroker:6162 --link mybroker:broker --name mqproducer blueprint/mq-producer:latest

## Publishing

    docker tag -f blueprint/mq-producer:latest abp-docker-blueprint.bintray.io/blueprint/mq-producer:latest
    docker push abp-docker-blueprint.bintray.io/blueprint/mq-producer:latest