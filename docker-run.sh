# Run the project with Docker using Apache Tomcat 8.5.
#
# Setup:
# Place the solrwayback.properties-file you want in the project root directory.
# And of course, Docker is needed.

# Get the project version from Maven.
PROJECT_VERSION=$(mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version | grep -v '\[')

# Set the container name.
CONTAINER_NAME=solrwayback
IMAGE_NAME=tomcat:8.5-jre8

# Stop the container if already running.
docker stop "$CONTAINER_NAME"

mvn package

docker pull "$IMAGE_NAME"

docker run -it --rm -p 8888:8080 \
--volume $(pwd)/src/test/resources/properties/solrwayback.properties:/root/solrwayback.properties \
--volume $(pwd)/src/test/resources/properties/solrwaybackweb.properties:/root/solrwaybackweb.properties \
--volume $(pwd)/target/solrwayback-$PROJECT_VERSION:/usr/local/tomcat/webapps/solrwayback \
--volume $(pwd)/src/test/resources/example_warc/:/netarkiv/warc/ \
--volume $(pwd)/src/test/resources/example_arc/:/netarkiv/arc/ \
--name "$CONTAINER_NAME" \
"$IMAGE_NAME"
