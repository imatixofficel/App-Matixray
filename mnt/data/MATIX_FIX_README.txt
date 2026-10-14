Matixray Native - Build Fix

Error fixed:
MainScreen.kt:103 - Kotlin FUNCTION_CALL_EXPECTED / context(...) expected.

Cause:
The previous UI patch removed LocalContext while the existing move-refusal code still used context.toastError(...).

Fix:
- Added import androidx.compose.ui.platform.LocalContext
- Added val context = LocalContext.current

Apply:
Replace:
V2rayNG/app/src/main/java/com/v2ray/ang/ui/main/MainScreen.kt

This ZIP contains only the changed MainScreen.kt plus this README.
No other project files are included.
