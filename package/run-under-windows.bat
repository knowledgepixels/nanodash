if not defined in_subprocess (cmd /k set in_subprocess=y ^& %0 %*) & exit )

if exist .extract rmdir /S /Q .extract

java -jar nanodash.jar -httpPort 37373 -Dfile.encoding=UTF-8
