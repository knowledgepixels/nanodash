FROM openjdk

ADD package/nanodash.jar /package/
ADD package/run /package/
CMD "package/run"
