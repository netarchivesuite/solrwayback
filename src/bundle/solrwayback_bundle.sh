#!/bin/bash
# Check if an argument was provided
if [ -z "$1" ]; then
    echo "Usage: $0 {start|stop}"
    exit 1
fi

case "$1" in
    start)
        echo "Starting solr..."
        ./solr-9/bin/solr start -c -m 4g

        echo "Starting SolrWayback in tomcat..."
        ./tomcat-9/bin/startup.sh

        printf "\n\nStarted SolrWayback\n"
        ;;
    stop)
        echo "Stopping SolrWayback in tomcat..."
        ./tomcat-9/bin/shutdown.sh

        echo "Stopping solr..."
        ./solr-9/bin/solr stop

        printf "\n\nStopped SolrWayback\n"
        ;;
    *)
        echo "Invalid option: $1"
        echo "Usage: $0 {start|stop}"
        exit 2
        ;;
esac
