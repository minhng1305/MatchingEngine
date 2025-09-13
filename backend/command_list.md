# To test redis lettuce connection
java -cp build/classes/java/main com.project.matchingengine.RedisLettuceConnectTest
./gradlew runClass -PmainClass=com.project.matchingengine.RedisLettuceConnectTest

# To test the orderbook data structure
java -cp build/classes/java/main com.project.matchingengine.OrderBookVerification

# build
./gradlew clean build

# build without running test
./gradlew clean build -x test

# Run the application
./gradlew bootRun

# List all source files in the src directory, sorted
find src -type f | sort

# zookeeper: run 1st
bin/zookeeper-server-start.sh config/zookeeper.properties

# kafka server: run 2nd
bin/kafka-server-start.sh config/server.properties

# to see all topics
bin/kafka-topics.sh --bootstrap-server localhost:9092 --list

# pretty
./gradlew :spotlessApply

# sql
brew services start postgresql

# redis
brew services start redis

# see all sockets
netstat -an | grep LISTEN

javac -h cpp/include -d /tmp src/main/java/com/example/exchange/jni/MatchingEngineJNI.java


# Docker commands:
# 1. Get JDK:
docker run -dit openjdk:22-jdk
# 2. Copy jar file into container:
docker cp <jar file from local project> <container_name>:/tmp
# 3. Create image:
docker commit --change='CMD ["java", "-jar", "/tmp/demo-0.0.1-SNAPSHOT.jar"]' <container_name> minhnguyen/test-demo:v1
# 4. Run the image:
docker run minhnguyen/test-demo:v1 

# View docker container file structure:
docker exec <container_name> ls

