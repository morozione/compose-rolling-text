# Consumer proguard rules for compose-rolling-text library
# These rules will be applied to apps that consume this library

# Keep the public API
-keep class io.github.morozione.rollingtext.RollingAnimatedTextKt { *; }
