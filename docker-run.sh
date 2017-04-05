# Run the project with Docker using Apache Tomcat 8.5.
#
# Setup:
# Place the solrwayback.properties-file you want in the project root directory.
# And of course, Docker is needed.

# Get the project version from Maven.
PROJECT_VERSION=$(mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version | grep -v '\[')

# Set the container name.
CONTAINER_NAME=solrwayback

# Stop the container if already running.
docker stop $CONTAINER_NAME

mvn package

docker run -it --rm -p 8888:8080 \
--volume $(pwd)/solrwayback.properties:/root/solrwayback.properties \
--volume $(pwd)/target/solrwayback-$PROJECT_VERSION:/usr/local/tomcat/webapps/solrwayback \
--name $CONTAINER_NAME \
tomcat:8.5-jre8
