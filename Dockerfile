# 1. Estágio de Compilação (Build)
FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /app

# Copia arquivos de dependência primeiro para aproveitar cache do Docker
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline -B

# Copia o código fonte e compila o JAR
COPY src ./src
RUN ./mvnw clean package -DskipTests

# 2. Estágio de Execução (Runtime leve)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copia o .jar gerado no primeiro estágio
COPY --from=builder /app/target/*.jar app.jar

# Porta padrão da aplicação
EXPOSE 8080

# Comando para rodar o Spring Boot
ENTRYPOINT ["java", "-jar", "app.jar"]
