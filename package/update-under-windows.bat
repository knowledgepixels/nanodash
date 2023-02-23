if not defined in_subprocess (cmd /k set in_subprocess=y ^& %0 %*) & exit )

java -jar nanodash.jar -resetExtract -Dnanodash.run=update -Dfile.encoding=UTF-8
