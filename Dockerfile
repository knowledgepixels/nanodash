FROM tomcat:11.0.11

COPY target/nanodash-*.war /usr/local/tomcat/webapps/ROOT.war
