FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -B
COPY src ./src
RUN ./mvnw package -DskipTests -B
RUN java -Djarmode=tools -jar target/*.jar extract --layers --destination target/layers

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/layers/dependencies/ ./
COPY --from=build /app/target/layers/spring-boot-loader/ ./
COPY --from=build /app/target/layers/snapshot-dependencies/ ./
COPY --from=build /app/target/layers/application/ ./
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "repo-ranker-0.0.1-SNAPSHOT.jar"]
