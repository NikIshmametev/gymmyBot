docker-compose down
./gradlew build
docker build --build-arg JAR_FILE="build/libs/*.jar" -t telegram/gymmybot .
docker-compose -f docker-compose.yml build
