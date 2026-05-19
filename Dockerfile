FROM eclipse-temurin:21

WORKDIR /app

COPY src/ src/
COPY lib/ lib/

RUN javac -cp "lib/postgresql-42.7.11.jar" src/*.java

CMD ["java", "-cp", "lib/postgresql-42.7.11.jar:src", "SQLVisualizerUI"]