#!/system/bin/sh
PAYLOAD=""
PAYLOAD_FILE=""
TIMEOUT_SEC="10"
AM_BIN="$(command -v am)"
USER_ID="${CLAW_USER_ID:-0}"

print_usage() {
  echo "usage: claw_call.sh '<json>' [timeout_sec]"
  echo "   or: claw_call.sh --file /sdcard/req.json [timeout_sec]"
  echo "   or: echo '<json>' | claw_call.sh"
}

if [ "$1" = "--file" ]; then
  PAYLOAD_FILE="$2"
  shift 2
else
  PAYLOAD="$1"
  if [ $# -gt 0 ]; then
    shift 1
  fi
fi

if [ -n "$1" ]; then
  TIMEOUT_SEC="$1"
fi

if [ -z "$PAYLOAD" ] && [ -z "$PAYLOAD_FILE" ]; then
  if [ ! -t 0 ]; then
    PAYLOAD="$(cat)"
  fi
fi

RES_DIR="/sdcard/ClawWorkerResults"
mkdir -p "$RES_DIR" >/dev/null 2>&1

if [ -z "$PAYLOAD" ] && [ -z "$PAYLOAD_FILE" ]; then
  print_usage
  exit 2
fi

if [ -z "$AM_BIN" ]; then
  echo "am_not_found"
  exit 6
fi

PAYLOAD_CONTENT=""
if [ -n "$PAYLOAD_FILE" ]; then
  if [ ! -f "$PAYLOAD_FILE" ]; then
    echo "request_file_not_found:$PAYLOAD_FILE"
    exit 3
  fi
  PAYLOAD_CONTENT="$(cat "$PAYLOAD_FILE")"
else
  PAYLOAD_CONTENT="$PAYLOAD"
fi

REQ_ID=$(printf "%s" "$PAYLOAD_CONTENT" | sed -n 's/.*"id"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' | head -n 1)
if [ -z "$REQ_ID" ]; then
  echo "request_id_missing"
  exit 4
fi

PAYLOAD_B64="$(printf "%s" "$PAYLOAD_CONTENT" | base64 | tr -d '\n')"

RESULT_FILE="$RES_DIR/$REQ_ID.json"
rm -f "$RESULT_FILE" >/dev/null 2>&1

AM_OUT="$($AM_BIN broadcast \
  --user "$USER_ID" \
  -n com.example.clawworker/.ClawCommandReceiver \
  -a com.example.clawworker.ACTION_CALL \
  --es payload_b64 "$PAYLOAD_B64" 2>&1)"
echo "$AM_OUT" | grep -qiE "permission denial|not allowed|securityexception|asks to run as user" && {
  echo "$AM_OUT"
  exit 7
}

i=0
while [ "$i" -lt "$TIMEOUT_SEC" ]; do
  if [ -f "$RESULT_FILE" ]; then
    cat "$RESULT_FILE"
    exit 0
  fi
  sleep 1
  i=$((i+1))
done

echo "result_timeout:$RESULT_FILE"
exit 5
