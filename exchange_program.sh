set -e # Exit early if any commands fail

(
  cd "$(dirname "$0")" # Ensure compile steps are run within the repository directory
)

# This ís to be run in the root of the project (ignoring tests)   
./gradlew clean build -x test  

# To test redis lettuce connection
java -cp build/classes/java/main com.project.matchingengine.RedisLettuceConnectTest
./gradlew runClass -PmainClass=com.project.matchingengine.RedisLettuceConnectTest

# To test the orderbook data structure
java -cp build/classes/java/main com.project.matchingengine.OrderBookVerification

# Run the application
./gradlew bootRun

# List all source files in the src directory, sorted
find src -type f | sort  