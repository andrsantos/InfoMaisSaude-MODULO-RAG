# --- Estágio 1: Build (Compilação) ---
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copia o pom.xml e baixa as dependências (Cache)
COPY pom.xml .
# Dica: Isso faz o build ser mais rápido nas próximas vezes
RUN mvn dependency:go-offline

# Copia o código fonte
COPY src ./src

# Compila o projeto e gera o .jar (Pula os testes para ser mais rápido na cloud)
RUN mvn clean package -DskipTests

# --- Estágio 2: Run (Execução) ---
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copia apenas o arquivo .jar gerado no estágio anterior
# O asterisco *.jar pega qualquer nome que seu projeto tenha
COPY --from=build /app/target/*.jar app.jar

# Expõe a porta padrão do Spring Boot
EXPOSE 8080

# Comando para iniciar
ENTRYPOINT ["java", "-jar", "app.jar"]