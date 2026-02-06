You should implement a backup solution for these keys. So in a new docker container there should run an ssh deamon for receiving the encrypted keys. Periodically, twice a day, probably by a cron task, a script should encrypt all the keys into one file and then use rsync or scp to back them up to an other server. The filename should contain the date and time in ISO format and the server name. The admin needs to configure the password for the encryption, and the target server's user/ip/port. For the ssh connection I want to use private key authentication. We also need a documentation for the server admin in how to set it up. Please ask me if some details are not specified enough. 



