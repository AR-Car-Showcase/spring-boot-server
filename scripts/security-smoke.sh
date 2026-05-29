#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
API_BASE_URL="${BASE_URL%/}/api"
AUTH_BASE_URL="${BASE_URL%/}"
USERNAME="${USERNAME:-security_smoke_$(date +%s)}"
EMAIL="${EMAIL:-${USERNAME}@example.com}"
PASSWORD="${PASSWORD:-Pass@1234}"
VERIFICATION_CODE="${VERIFICATION_CODE:-}"
TEST_CUSTOMIZATIONS="${TEST_CUSTOMIZATIONS:-0}"
GOOGLE_ID_TOKEN="${GOOGLE_ID_TOKEN:-}"

command -v jq >/dev/null 2>&1 || {
  echo "jq is required for this smoke test." >&2
  exit 1
}

tmpdir="$(mktemp -d)"
trap 'rm -rf "$tmpdir"' EXIT

request_json() {
  local method="$1"
  local url="$2"
  local body="${3:-}"
  local output_file="$4"
  shift 4

  local cmd=(curl -sS -o "$output_file" -w "%{http_code}" -X "$method" "$url")
  for header in "$@"; do
    cmd+=(-H "$header")
  done

  if [[ -n "$body" ]]; then
    cmd+=(-H "Content-Type: application/json" -d "$body")
  fi

  "${cmd[@]}"
}

print_step() {
  echo
  echo "==> $1"
}

expect_status() {
  local actual="$1"
  local expected="$2"
  local description="$3"
  if [[ "$actual" != "$expected" ]]; then
    echo "Expected $description to return $expected but got $actual" >&2
    exit 1
  fi
}

print_step "Checking Google auth config"
request_json GET "${API_BASE_URL}/auth/google/config" "" "${tmpdir}/google-config.json" >/dev/null
cat "${tmpdir}/google-config.json" | jq .

print_step "Signing up a new user"
signup_payload="$(cat <<JSON
{"username":"${USERNAME}","email":"${EMAIL}","password":"${PASSWORD}"}
JSON
)"
signup_status="$(request_json POST "${API_BASE_URL}/auth/signup" "${signup_payload}" "${tmpdir}/signup.json")"
expect_status "${signup_status}" "200" "signup"
cat "${tmpdir}/signup.json" | jq .

verification_required="$(jq -r '.verificationRequired // false' "${tmpdir}/signup.json")"
if [[ "${verification_required}" != "true" ]]; then
  echo "Signup did not require verification. Exiting early because email verification is disabled." >&2
  exit 0
fi

print_step "Checking resend cooldown"
resend_payload="$(cat <<JSON
{"email":"${EMAIL}"}
JSON
)"
resend_status="$(request_json POST "${API_BASE_URL}/auth/resend-verification" "${resend_payload}" "${tmpdir}/resend.json")"
expect_status "${resend_status}" "429" "resend cooldown"
cat "${tmpdir}/resend.json" | jq .

if [[ -z "${VERIFICATION_CODE}" ]]; then
  echo
  read -r -p "Enter the email verification code for ${EMAIL}: " VERIFICATION_CODE
fi

print_step "Verifying email"
verify_payload="$(cat <<JSON
{"email":"${EMAIL}","code":"${VERIFICATION_CODE}"}
JSON
)"
verify_status="$(request_json POST "${API_BASE_URL}/auth/verify-email" "${verify_payload}" "${tmpdir}/verify.json")"
expect_status "${verify_status}" "200" "verify-email"
cat "${tmpdir}/verify.json" | jq .

print_step "Logging in"
login_payload="$(cat <<JSON
{"username":"${USERNAME}","password":"${PASSWORD}"}
JSON
)"
login_status="$(request_json POST "${AUTH_BASE_URL}/login" "${login_payload}" "${tmpdir}/login.json")"
expect_status "${login_status}" "200" "login"
cat "${tmpdir}/login.json" | jq .

ACCESS_TOKEN="$(jq -r '.accessToken // empty' "${tmpdir}/login.json")"
REFRESH_TOKEN="$(jq -r '.refreshToken // empty' "${tmpdir}/login.json")"

if [[ -z "${ACCESS_TOKEN}" || -z "${REFRESH_TOKEN}" ]]; then
  echo "Login did not return both accessToken and refreshToken." >&2
  exit 1
fi

print_step "Confirming protected endpoint is blocked without a token"
profile_unauth_status="$(request_json GET "${API_BASE_URL}/user/profile" "" "${tmpdir}/profile-unauth.json")"
expect_status "${profile_unauth_status}" "401" "unauthorized profile access"
cat "${tmpdir}/profile-unauth.json" | jq .

print_step "Reading profile with bearer token"
profile_status="$(request_json GET "${API_BASE_URL}/user/profile" "" "${tmpdir}/profile.json" "Authorization: Bearer ${ACCESS_TOKEN}")"
expect_status "${profile_status}" "200" "profile read"
cat "${tmpdir}/profile.json" | jq .

print_step "Refreshing session"
refresh_payload="$(cat <<JSON
{"refreshToken":"${REFRESH_TOKEN}"}
JSON
)"
refresh_status="$(request_json POST "${AUTH_BASE_URL}/refresh" "${refresh_payload}" "${tmpdir}/refresh.json")"
expect_status "${refresh_status}" "200" "refresh"
cat "${tmpdir}/refresh.json" | jq .

NEW_ACCESS_TOKEN="$(jq -r '.accessToken // empty' "${tmpdir}/refresh.json")"
NEW_REFRESH_TOKEN="$(jq -r '.refreshToken // empty' "${tmpdir}/refresh.json")"
if [[ -z "${NEW_ACCESS_TOKEN}" || -z "${NEW_REFRESH_TOKEN}" ]]; then
  echo "Refresh did not return a new token pair." >&2
  exit 1
fi

print_step "Logging out"
logout_payload="$(cat <<JSON
{"refreshToken":"${NEW_REFRESH_TOKEN}"}
JSON
)"
logout_status="$(request_json POST "${AUTH_BASE_URL}/logout" "${logout_payload}" "${tmpdir}/logout.json")"
expect_status "${logout_status}" "200" "logout"
cat "${tmpdir}/logout.json" | jq .

print_step "Confirming reused refresh token is rejected"
reuse_status="$(request_json POST "${AUTH_BASE_URL}/refresh" "${logout_payload}" "${tmpdir}/reuse.json")"
expect_status "${reuse_status}" "401" "revoked refresh token reuse"
cat "${tmpdir}/reuse.json" | jq .

print_step "Checking public car listing and like endpoints"
request_json GET "${API_BASE_URL}/cars/allcars" "" "${tmpdir}/cars.json" >/dev/null || true
car_id="$(jq -r '.[0].id // empty' "${tmpdir}/cars.json" 2>/dev/null || true)"
if [[ -n "${car_id}" ]]; then
  like_status="$(request_json POST "${API_BASE_URL}/likes/car/${car_id}" "" "${tmpdir}/like.json" "Authorization: Bearer ${NEW_ACCESS_TOKEN}")"
  expect_status "${like_status}" "200" "like create"

  check_like_status="$(request_json GET "${API_BASE_URL}/likes/check/${car_id}" "" "${tmpdir}/like-check.json" "Authorization: Bearer ${NEW_ACCESS_TOKEN}")"
  expect_status "${check_like_status}" "200" "like check"
  cat "${tmpdir}/like-check.json" | jq .

  unlike_status="$(request_json DELETE "${API_BASE_URL}/likes/car/${car_id}" "" "${tmpdir}/unlike.json" "Authorization: Bearer ${NEW_ACCESS_TOKEN}")"
  expect_status "${unlike_status}" "200" "like delete"
else
  echo "No cars were available to exercise like endpoints, so that step was skipped."
fi

if [[ "${TEST_CUSTOMIZATIONS}" == "1" && -n "${car_id}" ]]; then
  print_step "Testing customization endpoint"
  customization_payload="$(cat <<JSON
{"vehicleId":"${car_id}","materials":{"seat":"leather","body":"matte black"}}
JSON
)"
  customization_status="$(request_json POST "${API_BASE_URL}/customizations" "${customization_payload}" "${tmpdir}/customization.json" "Authorization: Bearer ${NEW_ACCESS_TOKEN}")"
  expect_status "${customization_status}" "200" "customization create"
  cat "${tmpdir}/customization.json" | jq .
fi

if [[ -n "${GOOGLE_ID_TOKEN}" ]]; then
  print_step "Testing Google login exchange"
  google_payload="$(cat <<JSON
{"idToken":"${GOOGLE_ID_TOKEN}"}
JSON
)"
  google_status="$(request_json POST "${AUTH_BASE_URL}/login/google" "${google_payload}" "${tmpdir}/google-login.json")"
  expect_status "${google_status}" "200" "google login"
  cat "${tmpdir}/google-login.json" | jq .
fi

echo
echo "Security smoke test finished successfully."
