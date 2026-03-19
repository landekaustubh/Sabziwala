FROM eclipse-temurin:17-jre-jammy

WORKDIR /app 

COPY target/*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", \
            "-Xms128m", \
			"-Xmx256m", \
			"-Duser.timezone=Asia/kolkata", \
			"-jar","app.jar",\
			"--server.port=8082"
			]
