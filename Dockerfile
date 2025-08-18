FROM maven:3.8.5-openjdk-8
EXPOSE 8080

RUN mkdir -p /authlete/app

ADD . /authlete/app

WORKDIR /authlete/app

# Import the root certificate of Open Banking Brasil Sandbox
RUN mvn -s /usr/share/maven/ref/settings-docker.xml clean install \
    && java-oauth-server-migration/certs/import-certificate.sh java-oauth-server-migration/certs/Open_Banking_Brasil_Sandbox_Root_G2.pem

CMD ["mvn", "-s", "/usr/share/maven/ref/settings-docker.xml", "-f", "java-oauth-server-migration/pom.xml", "clean", "jetty:run"]
