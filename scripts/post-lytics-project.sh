#!/usr/bin/env bash
# POST /projects — Lytics API (dev11 nonprod)
# Override secrets without editing the file, e.g.:
#   AUTH_TOKEN=xxx ORGANIZATION_UID=yyy ./scripts/post-lytics-project.sh

set -euo pipefail

BASE_URL="${LYTICS_BASE_URL:-https://dev11-lytics-api.csnonprod.com}"
ORG_UID="${ORGANIZATION_UID:-blt359b3f6470027539}"
AUTH_TOKEN="${AUTH_TOKEN:-blt533797208e347c81}"

curl --location "${BASE_URL}/projects" \
  --header 'x-cs-api-version: 1' \
  --header "organization_uid: ${ORG_UID}" \
  --header "authtoken: ${AUTH_TOKEN}" \
  --header 'Content-Type: application/json' \
  --data '{
  "name": "DNI Test POST",
  "domain": "www.google.com",
  "description": "desc",
  "connections": {
    "stackApiKeys": ["bltfc558aa1c06a6869"],
    "launchProjectUids": ["69724ef190419e263e1fcd03"],
    "personalizeProjectUids": ["69c0e3f34dfc30183b4e6a96"]
  }
}'
