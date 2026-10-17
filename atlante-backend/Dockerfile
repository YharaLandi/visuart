# --- build: compila il jar con Maven
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

# --- run: solo il JRE e il jar
FROM eclipse-temurin:25-jre
WORKDIR /app
RUN useradd --system --uid 1001 atlante && mkdir -p /app/storage/images && chown -R atlante /app
COPY --from=build /app/target/atlante-backend-*.jar app.jar
COPY --chown=atlante storage ./storage
USER atlante
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
# Render passa la porta in $PORT
ENTRYPOINT ["sh", "-c", "java -XX:MaxRAMPercentage=75 -Dserver.port=${PORT:-8080} -jar app.jar"]
