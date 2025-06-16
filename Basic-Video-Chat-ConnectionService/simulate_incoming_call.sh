adb shell am start \
  -n com.tokbox.sample.basicvideochat_connectionservice/.ui.MainActivity \
  --es action simulate_incoming \
  --es CALLER_NAME "Mom" \
  --es CALLER_ID "+44 01539 702257"