# =====================================================================
# Dockerfile BASELINE (propositalmente inseguro) - corrigido no Lab 6.3.
# Problemas plantados:
#  - build "fat" numa unica stage (imagem grande, com Maven e fontes)
#  - roda como root
#  - segredo hardcoded via ENV dentro da imagem
# =====================================================================
FROM eclipse-temurin:17-jdk

WORKDIR /app
COPY . .

# A02 - segredo embutido na imagem (fica em plaintext nas layers)
ENV PORTAL_JWT_SECRET="portal-secret-2024"
ENV DB_PASSWORD="portal"

RUN ./mvnw -q -DskipTests package || mvn -q -DskipTests package

EXPOSE 8080
# roda como root (nenhum USER definido)
ENTRYPOINT ["java", "-jar", "target/portal-pedidos.jar"]
