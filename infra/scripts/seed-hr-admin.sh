#!/usr/bin/env bash

set -euo pipefail

KEYCLOAK_CONTAINER=${KEYCLOAK_CONTAINER:-hr-keycloak}
KEYCLOAK_URL=${KEYCLOAK_URL:-http://localhost:8080}
KEYCLOAK_ADMIN=${KEYCLOAK_ADMIN:-admin}
KEYCLOAK_ADMIN_PASSWORD=${KEYCLOAK_ADMIN_PASSWORD:-admin123}
REALM=${REALM:-hr}
USERNAME=${USERNAME:-stacey.smith@company.local}
PASSWORD=${PASSWORD:-ChangeMe123!}
EMPLOYEE_ID=${EMPLOYEE_ID:-HR-ADMIN-0001}
POSTGRES_CONTAINER=${POSTGRES_CONTAINER:-hr-postgres}
POSTGRES_DB=${POSTGRES_DB:-hrapp}
POSTGRES_USER=${POSTGRES_USER:-hr}
POSTGRES_PASSWORD=${POSTGRES_PASSWORD:-hr}

echo "Logging in to Keycloak admin API..."
docker exec "${KEYCLOAK_CONTAINER}" /opt/keycloak/bin/kcadm.sh config credentials \
  --server "${KEYCLOAK_URL}" \
  --realm master \
  --user "${KEYCLOAK_ADMIN}" \
  --password "${KEYCLOAK_ADMIN_PASSWORD}"

USER_ID=$(docker exec "${KEYCLOAK_CONTAINER}" /opt/keycloak/bin/kcadm.sh get "users?username=${USERNAME}" -r "${REALM}" --fields id --format csv --noquotes | tail -n1)

if [[ -z "${USER_ID}" ]]; then
  USER_ID=$(docker exec "${KEYCLOAK_CONTAINER}" /opt/keycloak/bin/kcadm.sh get "users?email=${USERNAME}" -r "${REALM}" --fields id --format csv --noquotes | tail -n1)
fi

if [[ -z "${USER_ID}" ]]; then
  echo "Creating user ${USERNAME}..."
  docker exec "${KEYCLOAK_CONTAINER}" /opt/keycloak/bin/kcadm.sh create users -r "${REALM}" \
    -s "username=${USERNAME}" \
    -s "enabled=true" \
    -s "firstName=Stacey" \
    -s "lastName=Smith" \
    -s "email=${USERNAME}"
fi

if [[ -z "${USER_ID}" ]]; then
  USER_ID=$(docker exec "${KEYCLOAK_CONTAINER}" /opt/keycloak/bin/kcadm.sh get "users?username=${USERNAME}" -r "${REALM}" --fields id --format csv --noquotes | tail -n1)
fi

if [[ -z "${USER_ID}" ]]; then
  USER_ID=$(docker exec "${KEYCLOAK_CONTAINER}" /opt/keycloak/bin/kcadm.sh get "users?email=${USERNAME}" -r "${REALM}" --fields id --format csv --noquotes | tail -n1)
fi

if [[ -z "${USER_ID}" ]]; then
  echo "Could not resolve user id for ${USERNAME}."
  exit 1
fi

CLIENT_ID=$(docker exec "${KEYCLOAK_CONTAINER}" /opt/keycloak/bin/kcadm.sh get clients -r "${REALM}" -q clientId=hr-frontend --fields id --format csv --noquotes | tail -n1)
ADMIN_CLIENT_ID=$(docker exec "${KEYCLOAK_CONTAINER}" /opt/keycloak/bin/kcadm.sh get clients -r "${REALM}" -q clientId=hr-admin-client --fields id --format csv --noquotes | tail -n1)

if [[ -z "${CLIENT_ID}" ]]; then
  echo "Could not find client hr-frontend in realm ${REALM}."
  exit 1
fi

if [[ -z "${ADMIN_CLIENT_ID}" ]]; then
  echo "Could not find client hr-admin-client in realm ${REALM}."
  exit 1
fi

ADMIN_SERVICE_USER_ID=$(docker exec "${KEYCLOAK_CONTAINER}" /opt/keycloak/bin/kcadm.sh get "clients/${ADMIN_CLIENT_ID}/service-account-user" -r "${REALM}" --fields id --format csv --noquotes | tail -n1)

if [[ -z "${ADMIN_SERVICE_USER_ID}" ]]; then
  echo "Could not resolve service-account user for hr-admin-client."
  exit 1
fi

echo "Setting password and attributes..."
docker exec "${KEYCLOAK_CONTAINER}" /opt/keycloak/bin/kcadm.sh set-password -r "${REALM}" --userid "${USER_ID}" --new-password "${PASSWORD}"
docker exec "${KEYCLOAK_CONTAINER}" /opt/keycloak/bin/kcadm.sh update "users/${USER_ID}" -r "${REALM}" -s "attributes.employee_id=[\"${EMPLOYEE_ID}\"]"
docker exec "${KEYCLOAK_CONTAINER}" /opt/keycloak/bin/kcadm.sh update "clients/${ADMIN_CLIENT_ID}" -r "${REALM}" -s 'fullScopeAllowed=true'

echo "Ensuring employee_id token mapper exists on hr-frontend client..."
if ! docker exec "${KEYCLOAK_CONTAINER}" /opt/keycloak/bin/kcadm.sh get "clients/${CLIENT_ID}/protocol-mappers/models" -r "${REALM}" | grep -q '"name" : "employee_id"'; then
  docker exec "${KEYCLOAK_CONTAINER}" /opt/keycloak/bin/kcadm.sh create "clients/${CLIENT_ID}/protocol-mappers/models" -r "${REALM}" \
    -s 'name=employee_id' \
    -s 'protocol=openid-connect' \
    -s 'protocolMapper=oidc-usermodel-attribute-mapper' \
    -s 'consentRequired=false' \
    -s 'config."user.attribute"=employee_id' \
    -s 'config."claim.name"=employee_id' \
    -s 'config."jsonType.label"=String' \
    -s 'config."id.token.claim"=true' \
    -s 'config."access.token.claim"=true' \
    -s 'config."userinfo.token.claim"=true' \
    -s 'config."multivalued"=false' || true
fi

echo "Assigning HR_ADMIN role..."
docker exec "${KEYCLOAK_CONTAINER}" /opt/keycloak/bin/kcadm.sh add-roles -r "${REALM}" --uid "${USER_ID}" --rolename HR_ADMIN

echo "Granting realm-management roles to hr-admin-client service account..."
docker exec "${KEYCLOAK_CONTAINER}" /opt/keycloak/bin/kcadm.sh add-roles -r "${REALM}" --uid "${ADMIN_SERVICE_USER_ID}" --cclientid realm-management --rolename query-users --rolename view-users --rolename manage-users --rolename view-realm

echo "Ensuring employee record exists for ${EMPLOYEE_ID}..."
docker exec -e PGPASSWORD="${POSTGRES_PASSWORD}" "${POSTGRES_CONTAINER}" psql -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" -c "
INSERT INTO employees (
  id,
  employee_id,
  first_name,
  last_name,
  job_title,
  date_of_birth,
  gender,
  date_of_hire,
  date_of_termination,
  home_address,
  mailing_address,
  telephone_number,
  email_address,
  created_at,
  updated_at
) VALUES (
  '8a8e7f8c-b2df-4af6-8b7c-5f74c034f301',
  '${EMPLOYEE_ID}',
  'Stacey',
  'Smith',
  'HR Administrator',
  DATE '1990-01-15',
  'Female',
  DATE '2021-08-01',
  NULL,
    '500 Market St, Austin, TX 78701, US',
    '500 Market St, Austin, TX 78701, US',
  '+1-512-555-0101',
  '${USERNAME}',
  NOW(),
  NOW()
)
ON CONFLICT (employee_id) DO UPDATE
SET
  first_name = EXCLUDED.first_name,
  last_name = EXCLUDED.last_name,
  job_title = EXCLUDED.job_title,
  email_address = EXCLUDED.email_address,
  updated_at = NOW();
"

echo "Done. User ${USERNAME} is seeded with role HR_ADMIN and employee_id=${EMPLOYEE_ID}."
