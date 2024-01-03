#!/bin/bash
set -e

# Run start hooks
for file in /hooks/start/*/*; do
    if [[ -r $file ]]; then
        echo "Sourcing start hook $file"
        source "$file"
    fi
done

echo "Starting main pod"
if [ -z "${POD_IP}" ]; then
    POD_IP="0.0.0.0"
fi

# Generate mpasswd file
echo "Generating mpasswd file..."
/opt/open-xchange/sbin/generatempasswd -A "$MASTER_ADMIN_USER" -P "$MASTER_ADMIN_PW"

echo "Copying extra files to /opt/open-xchange/etc"
if [ -d "/injections/etc/" ]; then
    for directory in /injections/etc/*; do
        cp --dereference -Rv "$directory"/* /opt/open-xchange/etc || true
    done
fi

# Copy extra files to /configuration/
echo "Copying extra files to /configuration..."
if [ -d "/injections/configuration/" ]; then
    for directory in /injections/configuration/*; do
        cp --dereference -Rv "$directory"/* /configuration/ || true
    done
fi

# Before Apply
for file in /hooks/beforeApply/*/*; do
    if [[ -r $file ]]; then
        echo "Sourcing beforeApply hook $file"
        source "$file"
    fi
done

# Applies configuration yaml files and renders templates
echo "Applying configuration yaml files..."
ox_props apply --directory /configuration/

if [[ -n "$GLOBAL_DB_ID" ]]; then
    yq -i ".default.id = $GLOBAL_DB_ID" /opt/open-xchange/etc/globaldb.yml
fi

# Before appsuite start
for file in /hooks/beforeAppsuiteStart/*/*; do
    if [[ -r $file ]]; then
        echo "Sourcing beforeAppsuiteStart hook $file"
        source "$file"
    fi
done

echo "Initialized configuration. Starting App Suite middleware..."
exec "$@"
