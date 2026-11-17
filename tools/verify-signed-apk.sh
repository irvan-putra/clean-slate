#!/usr/bin/env bash
# Pure text check of `apksigner verify --verbose --print-certs` and `aapt2 dump badging` output.
# Usage: verify-signed-apk.sh VERIFY_TXT BADGING_TXT
# Env (all required): EXPECTED_CERT_SHA256 EXPECTED_PACKAGE EXPECTED_VERSION_NAME EXPECTED_VERSION_CODE EXPECTED_LABEL
set -euo pipefail
v=${1:?verify.txt}; b=${2:?badging.txt}
: "${EXPECTED_CERT_SHA256:?}" "${EXPECTED_PACKAGE:?}" "${EXPECTED_VERSION_NAME:?}" "${EXPECTED_VERSION_CODE:?}" "${EXPECTED_LABEL:?}"
fail() { echo "VERIFY FAIL: $*" >&2; exit 1; }
[[ $EXPECTED_CERT_SHA256 =~ ^[0-9A-F]{64}$ ]] || fail "expected cert pin is not a 64-hex digest"
[[ $EXPECTED_VERSION_NAME =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] || fail "expected versionName is not X.Y.Z"
[[ $EXPECTED_VERSION_CODE =~ ^[1-9][0-9]{0,8}$ ]] || fail "expected versionCode is not a positive integer"
[ "$(grep -c '^Number of signers: ' "$v")" = 1 ] || fail "signer count line missing or repeated"
[ "$(sed -n 's/^Number of signers: //p' "$v")" = 1 ] || fail "must have exactly one signer"
grep -q '^Verifies$' "$v" || fail "apksigner did not report Verifies"
grep -q 'Verified using v2 scheme (APK Signature Scheme v2): true' "$v" || fail "v2 signature missing"
grep -q 'Verified using v1 scheme (JAR signing): false' "$v" || fail "unexpected v1 signature"
[ "$(grep -c '^Signer #[0-9]* certificate SHA-256 digest: ' "$v")" = 1 ] || fail "must list exactly one signer certificate"
got=$(sed -n 's/^Signer #1 certificate SHA-256 digest: //p' "$v" | tr 'a-f' 'A-F')
[ "$got" = "$EXPECTED_CERT_SHA256" ] || fail "certificate does not match the pin"
line=$(sed -n '1p' "$b")
[ "$line" = "package: name='$EXPECTED_PACKAGE' versionCode='$EXPECTED_VERSION_CODE' versionName='$EXPECTED_VERSION_NAME' platformBuildVersionName='${line##*platformBuildVersionName=\'}" ] \
  || [[ $line == "package: name='$EXPECTED_PACKAGE' versionCode='$EXPECTED_VERSION_CODE' versionName='$EXPECTED_VERSION_NAME'"* ]] || fail "package/version line differs: $line"
[ "$(grep -c "^application-label:'" "$b")" = 1 ] || fail "need exactly one default application-label"
[ "$(sed -n "s/^application-label:'\(.*\)'$/\1/p" "$b")" = "$EXPECTED_LABEL" ] || fail "launcher label is not exactly '$EXPECTED_LABEL'"
echo "VERIFY OK"
