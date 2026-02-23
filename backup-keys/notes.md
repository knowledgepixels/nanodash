Idea of backing up user keys:
Periodically, twice a day, by a cron task, a script will encrypt all the keys from  ~/.nanopub/nanodash-users/
into one backup file and then use scp to back it up to an other server.  The password for encryption must be defined by the administrator and stored on a secure place. 

Set-Up:
On the source and the target server, the admin initially must create the user named "backup_user" which is used for the ssh connection. The public key of the source-server's "backup_user" user must be added to the authorized_keys in the target server. The private key of this user is mounted into the container of the source server to be used for the ssh (scp) connection.

Source server:
`sudo adduser backup_user`
`ssh-keygen -t rsa -b 4096 -C "backup_user@<server>"

Target server:
`sudo adduser backup_user`
`sudo -H -u backup_user bash`
`mkdir ~/.ssh`
`vim ~/.ssh/authorized_keys`
Add the public key of the source server's "backup_user" user to the authorized_keys file.
`exit`
Create a directory for the backup files:
`sudo mkdir /var/backup`
`sudo chown backup_user /var/backup`

Env-Variables:
- BACKUP_ENCRYPTION_PASSWORD: The password used for encrypting the backup file.
- TARGET_SERVER: The target server where the backup file will be stored.
- TARGET_USER: The user on the target server who will receive the backup file.
- SOURCE_SERVER_NAME: The name of the source server, used for naming the backup file.

For decryption of the backup file on the target server:
openssl enc -d -aes-256-cbc -pbkdf2 -in <backup-file.tar.gz.enc> -out backup.tar.gz -k <password>

