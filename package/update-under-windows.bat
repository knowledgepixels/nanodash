if not defined in_subprocess (cmd /k set in_subprocess=y ^& %0 %*) & exit )

java -jar nanobench.jar -resetExtract -Dnanobench.run=update
