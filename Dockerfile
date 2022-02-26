FROM maven:3-jdk-8 AS MAVEN_TOOL_CHAIN

# Collect all the dependencies:
COPY pom.xml /tmp/
WORKDIR /tmp/
RUN mvn -B -q -s /usr/share/maven/ref/settings-docker.xml dependency:resolve-plugins dependency:go-offline

# Build the actual app:
COPY src /tmp/src/
RUN mvn -B -s /usr/share/maven/ref/settings-docker.xml package -DskipTests


# Setup runtime container:
FROM tomcat:9.0-jre8-alpine

# Support envsubst:
RUN apk add gettext

# Copy in the fresh WAR:
COPY --from=MAVEN_TOOL_CHAIN /tmp/target/solrwayback-*.war $CATALINA_HOME/webapps/solrwayback.war

# Copy in the properties files that are used to configure the service into HOME:
COPY docker/solrwayback.properties /root/solrwayback.properties.template
COPY docker/solrwaybackweb.properties /root/solrwaybackweb.properties.template
COPY docker/docker-entrypoint.sh /

# Setup a service health check:
HEALTHCHECK --interval=1m --timeout=3s CMD wget --quiet --tries=1 --spider http://localhost:8080/solrwayback/ || exit 1

# Default configuration:
ENV SOLR_URL=http://localhost:8983/solr/netarchivebuilder/
ENV BASE_URL=http://localhost:8080/solrwayback/
ENV COLLECTION_NAME=netarkivet.dk

# Use a wrapper script to run envsubst:
CMD "/docker-entrypoint.sh"