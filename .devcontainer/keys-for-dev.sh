#!/usr/bin/env bash
set -e

WORKSPACE="${MTOGO_WORKSPACE:-$(pwd)}"
KEY_DIR="$WORKSPACE/keys"
PRIVATE_KEY="$KEY_DIR/private.pem"
PUBLIC_KEY="$KEY_DIR/public.pem"

SECRET_NAME="auth_private_pkcs8.pem"
CONFIG_NAME="jwt_public.pem"

# Ensure keys exist
if [[ ! -f "$PRIVATE_KEY" || ! -f "$PUBLIC_KEY" ]]; then
    echo ">>> Local RSA keys not found. Generating..."
    mkdir -p "$KEY_DIR"

    openssl genpkey -algorithm RSA -out "$PRIVATE_KEY" -pkeyopt rsa_keygen_bits:2048
    openssl rsa -in "$PRIVATE_KEY" -pubout > "$PUBLIC_KEY"

    echo ">>> Keys created in $KEY_DIR/"
else
    echo ">>> Local RSA keys already exist."
fi

# Create Docker secret if not existing
if ! docker secret inspect "$SECRET_NAME" >/dev/null 2>&1; then
    echo ">>> Creating Docker secret: $SECRET_NAME"
    docker secret create "$SECRET_NAME" "$PRIVATE_KEY" >/dev/null
else
    echo ">>> Docker secret '$SECRET_NAME' already exists."
fi

# Create Docker config if not existing
if ! docker config inspect "$CONFIG_NAME" >/dev/null 2>&1; then
    echo ">>> Creating Docker config: $CONFIG_NAME"
    docker config create "$CONFIG_NAME" "$PUBLIC_KEY" >/dev/null
else
    echo ">>> Docker config '$CONFIG_NAME' already exists."
fi

echo ">>> Dev bootstrap completed successfully."
