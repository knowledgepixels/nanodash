FROM openjdk

ADD package/nanobench.jar /
ADD package/run /
CMD "./run"
