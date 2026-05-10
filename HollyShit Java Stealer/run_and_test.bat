@echo off
mvn clean package -DskipTests
java -jar target\stealer-1.0.jar
pause
