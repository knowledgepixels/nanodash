FROM alpine:latest

# Install required packages
RUN apk add --no-cache \
    openssl \
    openssh-client \
    tar \
    gzip \
    busybox-suid

# Setup directories
WORKDIR /root

# Copy backup script
COPY backup.sh /usr/local/bin/backup.sh
RUN chmod +x /usr/local/bin/backup.sh

# Setup Cron
# Run twice a day: at 03:00 and 15:00
RUN echo "0 3,15 * * * /usr/local/bin/backup.sh >> /var/log/cron.log 2>&1" > /etc/crontabs/root

# Create logs file
RUN touch /var/log/cron.log

# Configure Entrypoint
# Start crond in foreground and log level 8 (default)
CMD ["crond", "-f", "-d", "8"]
