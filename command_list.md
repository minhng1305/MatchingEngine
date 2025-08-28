# This is to be run in the root of the project (ignoring tests)
./gradlew clean build -x test

# To test redis lettuce connection
java -cp build/classes/java/main com.project.matchingengine.RedisLettuceConnectTest
./gradlew runClass -PmainClass=com.project.matchingengine.RedisLettuceConnectTest

# To test the orderbook data structure
java -cp build/classes/java/main com.project.matchingengine.OrderBookVerification

# build
./gradlew clean build

# Run the application
./gradlew bootRun

# List all source files in the src directory, sorted
find src -type f | sort

# zookeeper
bin/zookeeper-server-start.sh config/zookeeper.properties

# kafka server
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