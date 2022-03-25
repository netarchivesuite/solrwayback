#!/bin/sh

# If no custom configuration files have been added...
# Generate from templates, with substituted environment variables:
if [ ! -f $HOME/solrwayback.properties ]; then
    echo "Generating $HOME/solrwayback.properties from $HOME/solrwayback.properties.template..."
    envsubst < $HOME/solrwayback.properties.template > $HOME/solrwayback.properties
    cat $HOME/solrwayback.properties
fi
if [ ! -f $HOME/solrwaybackweb.properties ]; then
    echo "Generating $HOME/solrwaybackweb.properties from $HOME/solrwaybackweb.properties.template..."
    envsubst < $HOME/solrwaybackweb.properties.template > $HOME/solrwaybackweb.properties
    cat $HOME/solrwaybackweb.properties
fi

# If an alternative WAR name is set, use that:
if [[ ! -z "$ALT_WAR_NAME" ]]; then
    mv ${CATALINA_HOME}/webapps/solrwayback.war ${CATALINA_HOME}/webapps/${ALT_WAR_NAME}.war
fi

# And now start Tomcat:
catalina.sh run
