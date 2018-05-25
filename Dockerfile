FROM java:8-alpine
MAINTAINER Your Name <you@example.com>

ADD target/mdc-test-0.0.1-SNAPSHOT-standalone.jar /mdc-test/app.jar

EXPOSE 8080

CMD ["java", "-jar", "/mdc-test/app.jar"]
