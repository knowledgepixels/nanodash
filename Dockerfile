FROM openjdk

ADD package/nanobench.jar /package/
ADD package/run /package/
CMD "package/run"
