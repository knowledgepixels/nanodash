# Backup User-Keys

This module provides a unified Docker container for backing up Nanodash user keys. It runs both an SSH daemon (to receive backups) and a Cron daemon (to send encrypted backups).

## Features

- **Automated Backup**: Runs twice daily (3:00 AM and 3:00 PM).
- **Encryption**: Keys are encrypted with AES-256-CBC before transfer.
- **Secure Transfer**: Uses `rsync` over SSH with key-based authentication.
- **Dual Mode**: Can act as both a sender and a receiver simultaneously.

## Deployment

Add the service to your `docker-compose.yml`:

```yaml
services:
  backup-keys:
    build: ./backup-keys
    image: nanodash/backup-keys:latest
    restart: unless-stopped
    ports:
      - "2222:22" # Port for receiving backups from others
    volumes:
      - ~/.nanopub:/root/.nanopub:ro # Mount keys to be backed up (READ ONLY)
    environment:
      - BACKUP_PASSWORD=your_secret_password
      - SERVER_NAME=nanodash-primary
      - TARGETS=root@remote-backup-server:22:/backups
      - SSH_PRIVATE_KEY=-----BEGIN OPENSSH PRIVATE KEY-----...
      - AUTHORIZED_KEYS=ssh-rsa AAAAB3Nza... (keys allowed to connect to this server)
```

## Configuration Variables

| Variable | Description | Required | start value |
|---|---|---|---|
| `BACKUP_PASSWORD` | Password used to encrypt the backup archive. | Yes | |
| `SERVER_NAME` | Name used in the backup filename. Defaults to hostname. | No | `hostname` |
| `TARGETS` | Space-separated list of targets: `user@host:port:/path`. | No | |
| `SSH_PRIVATE_KEY` | Private key content for authenticating with targets. | If sending | |
| `AUTHORIZED_KEYS` | Public keys allowed to SSH into this container. | If receiving | |

## How to Restore

1. **Locate the Backup**: Find the `.tar.gz.enc` file on the receiving server.
2. **Decrypt**:
   ```bash
   openssl aes-256-cbc -d -in filename.tar.gz.enc -out filename.tar.gz -pass pass:your_secret_password
   ```
3. **Extract**:
   ```bash
   tar -xzvf filename.tar.gz
   ```

## Development

Build the image:
```bash
docker build -t nanodash/backup-keys ./backup-keys
```
