if not defined in_subprocess (cmd /k set in_subprocess=y ^& %0 %*) & exit )

if exists .extract rmdir /S /Q .extract

java -jar nanobench.jar -httpPort 37373
