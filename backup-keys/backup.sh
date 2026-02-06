#!/bin/bash
set -e

# Configuration checks
if [ -z "$BACKUP_PASSWORD" ]; then
    echo "ERROR: BACKUP_PASSWORD env var is not set."
    exit 1
fi

if [ -z "$SERVER_NAME" ]; then
    SERVER_NAME=$(hostname)
fi

DATE_STR=$(date -I'seconds')
FILENAME="nanodash-keys-backup_${SERVER_NAME}_${DATE_STR}.tar.gz.enc"
SOURCE_DIR="/root/.nanopub/nanodash-users"

echo "Starting backup at $(date)"
echo "Source: $SOURCE_DIR"
echo "Filename: $FILENAME"

# Create a temporary directory for the archive
TMP_DIR=$(mktemp -d)
ARCHIVE_PATH="$TMP_DIR/$FILENAME"

# 1. Archive and Encrypt
# Check if source exists
if [ ! -d "$SOURCE_DIR" ]; then
    echo "WARNING: Source directory $SOURCE_DIR does not exist. Skipping backup."
    rm -rf "$TMP_DIR"
    exit 0
fi

# Tar and encrypt
# -C changes to parent dir so tar doesn't include full absolute path
tar -cz -C "$(dirname "$SOURCE_DIR")" "$(basename "$SOURCE_DIR")" | \
    openssl enc -aes-256-cbc -e -pass pass:"$BACKUP_PASSWORD" -out "$ARCHIVE_PATH"

echo "Backup created and encrypted size: $(du -h "$ARCHIVE_PATH" | cut -f1)"

# 2. Transfer to Targets
# TARGETS env var expected format: "user@host:port/path user@host:port/path"
# The destination path is optional, default to home or whatever rsync defaults to.
# Since user asked for "target server's user/ip/port", lets assume defaults if not fully specified, but standard rsync syntax is best.
# Format: user@host:/path (port handled via -e 'ssh -p PORT')

if [ -z "$TARGETS" ]; then
    echo "WARNING: No TARGETS configured. Backup created locally but not transferred."
else
    # We need to handle potential custom ports. Rsync syntax with custom port is tricky if mixed in string.
    # The user request said: "configure ... target server's user/ip/port".
    # Let's assume TARGETS is a space separated list of config strings like "user@host:port:/dest/path"
    # Or simplified: if we just support scp-like syntax "user@host:/path", port needs to be separate or we parse it.
    
    # Let's try to be robust. We can iterate.
    # We'll use a specific format for TARGETS to allow ports:
    # "user@host:port:/path" -> Valid rsync target? standard rsync is host:/path. Port is ssh option.
    # Let's define the format as: user@host:port:path (path optional)
    
    for TARGET_DEF in $TARGETS; do
        echo "Processing target: $TARGET_DEF"
        
        # Parse connection details using regex or IFS
        # Expected format: user@host:port:/path or user@host:port (path defaults to ~)
        # Note: IPv6 might break this simple parsing but let's assume IPv4/hostname.
        
        # Extract User@Host
        if [[ "$TARGET_DEF" =~ ^([^@]+@[^:]+):([0-9]+)(:(.*))?$ ]]; then
             # Format: user@host:port[:path]
             DEST="${BASH_REMATCH[1]}"
             PORT="${BASH_REMATCH[2]}"
             PATH_SUFFIX="${BASH_REMATCH[4]}"
             
             if [ -z "$PATH_SUFFIX" ]; then
                 PATH_SUFFIX="" # default to home
             fi
             
             # If path suffix is empty, it means we just drop it in home? 
             # Rsync destination: user@host:path
             DESTINATION="${DEST}:${PATH_SUFFIX}"
             
             echo "Transferring to $DESTINATION on port $PORT..."
             rsync -av -e "ssh -p $PORT -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null" "$ARCHIVE_PATH" "$DESTINATION"
             
        elif [[ "$TARGET_DEF" =~ ^([^@]+@[^:]+)(:(.*))?$ ]]; then
             # Format: user@host[:path] (implicit port 22)
             DEST="${BASH_REMATCH[1]}"
             PATH_SUFFIX="${BASH_REMATCH[3]}"
             
             DESTINATION="${DEST}:${PATH_SUFFIX}"
             echo "Transferring to $DESTINATION (default port)..."
             rsync -av -e "ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null" "$ARCHIVE_PATH" "$DESTINATION"
        else
            echo "ERROR: Could not parse target definition: $TARGET_DEF"
        fi
    done
fi

# Cleanup
rm -rf "$TMP_DIR"
echo "Backup job completed at $(date)"
