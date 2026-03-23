#!/bin/bash
# Bootstrap script for QLever in Docker Compose.
# Builds an initial index from a seed file (if no index exists yet),
# then starts the QLever server.
#
# Binary locations in adfreiburg/qlever image:
#   /qlever/qlever-index  (formerly IndexBuilderMain)
#   /qlever/qlever-server (formerly ServerMain)

set -e

QLEVER_INDEX="/qlever/qlever-index"
QLEVER_SERVER="/qlever/qlever-server"

INDEX_DIR="/index"
DATA_DIR="/data"
INDEX_PREFIX="gams"

# Ensure index dir exists and is writable by the qlever user
mkdir -p "${INDEX_DIR}"
chmod 777 "${INDEX_DIR}"

# Check if index already exists
if [ -f "${INDEX_DIR}/${INDEX_PREFIX}.index.pos" ]; then
  echo "QLever index already exists, starting server..."
else
  echo "No existing index found. Building index from seed data..."
  ${QLEVER_INDEX} -f "${DATA_DIR}/seed.nt" \
    -i "${INDEX_DIR}/${INDEX_PREFIX}" \
    -s "${DATA_DIR}/settings.json" \
    -F nt \
    -W
  echo "Index built successfully."
fi

echo "Starting QLever server on port 7001..."
exec ${QLEVER_SERVER} -i "${INDEX_DIR}/${INDEX_PREFIX}" \
  -p 7001 \
  -m 1G \
  -c 512M \
  -e 512M \
  -t 30 \
  -j 4 \
  --default-query-timeout 30s \
  -a "${QLEVER_ACCESS_TOKEN:-gams-dev}"