FROM tomcat:11.0.27

RUN sed -i 's/port="8080" protocol="HTTP\/1.1"/port="8080" protocol="HTTP\/1.1" maxHttpHeaderSize="65536"/' /usr/local/tomcat/conf/server.xml

COPY target/nanodash-*.war /usr/local/tomcat/webapps/ROOT.war
