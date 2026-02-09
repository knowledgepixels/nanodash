#!/bin/sh

# Fail on error
set -e

# Configuration
SOURCE_DIR="${HOME}/.nanopub/nanodash-users"
BACKUP_DIR="/tmp/backup_stage"
TIMESTAMP=$(date -Iseconds)
# Ensure SOURCE_SERVER_NAME is set, default to hostname if not
SERVER_NAME="${SOURCE_SERVER_NAME:-$(hostname)}"
ARCHIVE_NAME="keys_backup_${SERVER_NAME}_${TIMESTAMP}.tar.gz"
ENCRYPTED_NAME="${ARCHIVE_NAME}.enc"

# Check for required environment variables
if [ -z "$BACKUP_ENCRYPTION_PASSWORD" ]; then
    echo "Error: BACKUP_ENCRYPTION_PASSWORD environment variable is not set."
    exit 1
fi

if [ -z "$TARGET_SERVER" ] || [ -z "$TARGET_USER" ]; then
    echo "Error: TARGET_SERVER and TARGET_USER environment variables must be set."
    exit 1
fi

# Create staging directory
mkdir -p "$BACKUP_DIR"

echo "Starting backup at $(date)..."

# 1. Archive
if [ -d "$SOURCE_DIR" ]; then
    echo "Archiving keys from $SOURCE_DIR..."
    tar -czf "${BACKUP_DIR}/${ARCHIVE_NAME}" -C "$(dirname "$SOURCE_DIR")" "$(basename "$SOURCE_DIR")"
else
    echo "Error: Source directory $SOURCE_DIR does not exist."
    exit 1
fi

# 2. Encrypt
echo "Encrypting archive..."
# Using openssl aes-256-cbc. 
# -pbkdf2 is recommended for newer openssl versions to derive key from password.
openssl enc -aes-256-cbc -pbkdf2 -salt -in "${BACKUP_DIR}/${ARCHIVE_NAME}" -out "${BACKUP_DIR}/${ENCRYPTED_NAME}" -pass env:BACKUP_ENCRYPTION_PASSWORD

# 3. Transfer
echo "Transferring backup to ${TARGET_USER}@${TARGET_SERVER}..."
# Ensure target directory hierarchy exists (requires ssh access to execute commands, or assumes it exists)
# The requirements say: "stored on the target server at /var/backup/<source_server_name>/<filename>"
TARGET_PATH="/var/backup/${SERVER_NAME}/${ENCRYPTED_NAME}"

# We attempt to create the directory first. This might fail if the user doesn't have permissions to run mkdir,
# but usually backup users are set up to allow this or the dir is pre-created.
# If strict scp-only is enforced, this step might fail. We'll try it.
ssh -o StrictHostKeyChecking=no "${TARGET_USER}@${TARGET_SERVER}" "mkdir -p /var/backup/${SERVER_NAME}" || echo "Warning: Could not create remote directory, assuming it exists or SCP will handle it..."

scp -o StrictHostKeyChecking=no "${BACKUP_DIR}/${ENCRYPTED_NAME}" "${TARGET_USER}@${TARGET_SERVER}:${TARGET_PATH}"

# 4. Cleanup
echo "Cleaning up..."
rm -f "${BACKUP_DIR}/${ARCHIVE_NAME}" "${BACKUP_DIR}/${ENCRYPTED_NAME}"

echo "Backup completed successfully at $(date)."
