FROM alpine:latest

# Install dependencies
# bash: for scripts
# openssh: for sshd and ssh-client
# openssl: for encryption
# rsync: for file transfer
# busybox-suid: for cron (sometimes needed on alpine for crond to work properly with arbitrary users, though usually root is fine)
RUN apk add --no-cache bash openssh openssl rsync tzdata

# Create directory for backup storage (receiver mode)
RUN mkdir -p /backups

# Setup SSH for receiver
RUN mkdir -p /root/.ssh && chmod 700 /root/.ssh
# We will generate host keys in entrypoint if missing to avoid stale keys in image

# Setup backup source directory (mounted volume)
# We expect /root/.nanopub to be mounted
RUN mkdir -p /root/.nanopub

# Copy scripts
COPY backup.sh /usr/local/bin/backup.sh
COPY entrypoint.sh /entrypoint.sh

RUN chmod +x /usr/local/bin/backup.sh /entrypoint.sh

# Setup cron (default schedule: 0 3,15 * * * -> 3am and 3pm)
# We can make this configurable if needed, but hardcoded for now as per "twice a day"
RUN echo "0 3,15 * * * /usr/local/bin/backup.sh >> /var/log/backup.log 2>&1" > /etc/crontabs/root

EXPOSE 22

ENTRYPOINT ["/entrypoint.sh"]
