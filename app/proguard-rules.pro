# kGems keeps default AGP/R8 rules. Compose + MMD are handled by their own
# consumer ProGuard files. The :core module is plain Kotlin with no reflection,
# so no keep rules are needed — the board is generated procedurally, nothing
# loads by name.
