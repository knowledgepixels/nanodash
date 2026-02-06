#!/bin/bash
set -e

echo "[Entrypoint] Starting backup-keys container..."

# 1. Setup SSH Keys (for Receiver Mode & Sender Auth)
# If mounted keys exist, use them. If not, generate host keys.
if [ ! -f /etc/ssh/ssh_host_rsa_key ]; then
    echo "[Entrypoint] Generating SSH host keys..."
    ssh-keygen -A
fi

# 2. Setup User SSH Access (Receiver Mode)
# We accept logins as 'root' for simplicity in this container context, 
# but keys must be provided via authorized_keys mounting or env var?
# The user request didn't specify how authorized_keys get there.
# Let's assume standard practice: mount keys to /root/.ssh/authorized_keys or pass via env.
if [ -n "$AUTHORIZED_KEYS" ]; then
    echo "[Entrypoint] Adding AUTHORIZED_KEYS to /root/.ssh/authorized_keys"
    echo "$AUTHORIZED_KEYS" >> /root/.ssh/authorized_keys
    chmod 600 /root/.ssh/authorized_keys
fi

# 3. Setup Client Key (Sender Mode)
# If we are sending backups, we need a private key to authenticate with targets.
if [ -n "$SSH_PRIVATE_KEY" ]; then
    echo "[Entrypoint] Setting up SSH private key for sending backups..."
    echo "$SSH_PRIVATE_KEY" > /root/.ssh/id_rsa
    chmod 600 /root/.ssh/id_rsa
    # Derive headers or public key if needed? rsync just uses the file.
fi

# 4. Start CRON
echo "[Entrypoint] Starting cron daemon..."
crond -b -L /var/log/cron.log

# 5. Start SSHD
echo "[Entrypoint] Starting SSH daemon..."
# -D: Do not detach and does not become a daemon.
# -e: Write debug logs to standard error instead of system log.
/usr/sbin/sshd -D -e
