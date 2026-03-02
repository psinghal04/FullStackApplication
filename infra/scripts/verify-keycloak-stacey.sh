#!/usr/bin/env bash

set -euo pipefail

KEYCLOAK_URL=${KEYCLOAK_URL:-http://localhost:8080}
KEYCLOAK_ADMIN=${KEYCLOAK_ADMIN:-admin}
KEYCLOAK_ADMIN_PASSWORD=${KEYCLOAK_ADMIN_PASSWORD:-admin123}
REALM=${REALM:-hr}
EXPECTED_USERNAME=${EXPECTED_USERNAME:-stacey.smith@company.local}
TIMEOUT_SECONDS=${TIMEOUT_SECONDS:-60}

wait_for_keycloak() {
  local waited=0
  echo "Waiting for Keycloak readiness at ${KEYCLOAK_URL} (timeout=${TIMEOUT_SECONDS}s)..."

  while (( waited < TIMEOUT_SECONDS )); do
    if curl -fsS "${KEYCLOAK_URL}/realms/master/.well-known/openid-configuration" >/dev/null 2>&1; then
      echo "Keycloak is ready."
      return 0
    fi
    sleep 2
    waited=$((waited + 2))
  done

  echo "ERROR: Keycloak not ready within ${TIMEOUT_SECONDS}s"
  return 1
}

get_admin_token() {
  curl -fsS -X POST "${KEYCLOAK_URL}/realms/master/protocol/openid-connect/token" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "grant_type=password" \
    -d "client_id=admin-cli" \
    -d "username=${KEYCLOAK_ADMIN}" \
    -d "password=${KEYCLOAK_ADMIN_PASSWORD}" |
    sed -n 's/.*"access_token":"\([^"]*\)".*/\1/p'
}

verify_stacey() {
  local token="$1"
  local response

  response=$(curl -fsS \
    -H "Authorization: Bearer ${token}" \
    "${KEYCLOAK_URL}/admin/realms/${REALM}/users?username=${EXPECTED_USERNAME}")

  if echo "${response}" | grep -q "${EXPECTED_USERNAME}"; then
    echo "PASS: Found seeded user ${EXPECTED_USERNAME} in realm ${REALM}."
    return 0
  fi

  echo "ERROR: Seeded user ${EXPECTED_USERNAME} not found in realm ${REALM}."
  echo "Response: ${response}"
  return 1
}

wait_for_keycloak

token=$(get_admin_token)
if [[ -z "${token}" ]]; then
  echo "ERROR: Failed to retrieve admin access token from Keycloak."
  exit 1
fi

verify_stacey "${token}"
