#!/usr/bin/env bash
# Synthetic tests for verify-signed-apk.sh: one good case and a set of must-fail cases. No secrets, no APK needed.
set -uo pipefail
here=$(cd "$(dirname "$0")" && pwd); t=$(mktemp -d); trap 'rm -rf "$t"' EXIT
PIN=$(printf 'AB%.0s' $(seq 32))
export EXPECTED_CERT_SHA256=$PIN EXPECTED_PACKAGE=com.vanipy.demo EXPECTED_VERSION_NAME=1.2.3 EXPECTED_VERSION_CODE=7 EXPECTED_LABEL=demo
good_v() { printf 'Verifies\nVerified using v1 scheme (JAR signing): false\nVerified using v2 scheme (APK Signature Scheme v2): true\nVerified using v3 scheme (APK Signature Scheme v3): true\nNumber of signers: 1\nSigner #1 certificate DN: CN=demo\nSigner #1 certificate SHA-256 digest: %s\n' "$1" > "$t/v.txt"; }
good_b() { printf "package: name='%s' versionCode='%s' versionName='%s' platformBuildVersionName='14'\napplication-label:'%s'\n" "$1" "$2" "$3" "$4" > "$t/b.txt"; }
run() { bash "$here/verify-signed-apk.sh" "$t/v.txt" "$t/b.txt" >/dev/null 2>&1; }
n=0; bad=0
expect() { n=$((n+1)); if run; then r=pass; else r=fail; fi; [ "$r" = "$1" ] || { echo "case $n ($2): expected $1 got $r"; bad=1; }; }
good_v "$(echo $PIN | tr A-F a-f)"; good_b com.vanipy.demo 7 1.2.3 demo; expect pass "good, lowercase cert hex accepted"
good_v "$PIN"; good_b com.vanipy.demo 7 1.2.3 demo; expect pass "good"
good_v "$(printf 'CD%.0s' $(seq 32))"; expect fail "wrong certificate"
good_v "$PIN"; printf 'Number of signers: 1\n' >> "$t/v.txt"; expect fail "repeated signer count line"
good_v "$PIN"; sed -i 's/Number of signers: 1/Number of signers: 2/' "$t/v.txt"; expect fail "two signers"
good_v "$PIN"; printf 'Signer #2 certificate SHA-256 digest: %s\n' "$PIN" >> "$t/v.txt"; expect fail "second listed signer"
good_v "$PIN"; sed -i 's/v1 scheme (JAR signing): false/v1 scheme (JAR signing): true/' "$t/v.txt"; expect fail "v1 signature present"
good_v "$PIN"; sed -i 's/^Verifies$/DOES NOT VERIFY/' "$t/v.txt"; expect fail "not verified"
good_v "$PIN"; good_b com.vanipy.other 7 1.2.3 demo; expect fail "wrong package"
good_b com.vanipy.demo 8 1.2.3 demo; expect fail "wrong versionCode"
good_b com.vanipy.demo 7 1.2.4 demo; expect fail "wrong versionName"
good_b com.vanipy.demo 7 1.2.3 Demo; expect fail "capitalized label"
good_b com.vanipy.demo 7 1.2.3 "demo 1.2.3"; expect fail "label with version"
good_b com.vanipy.demo 7 1.2.3 demo; printf "application-label:'demo'\n" >> "$t/b.txt"; expect fail "two labels"
good_b com.vanipy.demo 7 1.2.3-r1 demo; expect fail "bad versionName shape"
good_b com.vanipy.demo 7 1.2.3 demo; EXPECTED_CERT_SHA256=REPLACE_AFTER_KEY_CREATION expect fail "placeholder pin"
[ $bad = 0 ] && echo "verifier tests: $n cases OK" || { echo "verifier tests FAILED"; exit 1; }
