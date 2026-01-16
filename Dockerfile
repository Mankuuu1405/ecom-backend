# Use the official OpenJDK 17 image from Docker Hub
FROM eclipse-temurin:17-jdk-alpine

# Set working directory inside the container
WORKDIR /app

# Copy the compiled Java application JAR file into the container
COPY ./target/aim.jar /app

# Expose the port the Spring Boot application will run on
EXPOSE 8989

# Set environment variables for database connection (optional)
ENV DB_HOST=database-1.cxa6qyk0oyb9.ap-south-1.rds.amazonaws.com
ENV DB_PORT=3306
ENV DB_NAME=aim
ENV DB_USER=admin
ENV DB_PASSWORD=Admin123
# Command to run the application
CMD ["java", "-jar", "aim.jar"]
