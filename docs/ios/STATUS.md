# Nuraya iOS — port in progress

Baseline: Android 1.7.1, commit `7b9f23e0bfbc80a24b6265108deaa42785452063`.
Target: iPhone XR, iOS 16 or later. Android source and APK remain unchanged.

This branch is implementation work, not an App Store release or a verified parity claim.
The asset packaging step copies the original Android assets byte-for-byte and exports
explicit qari identity pairs, reading modes and vector paths from the Kotlin source.

macOS CI checks the Swift core against all 114 surahs and both 6,236-entry page indexes.
Physical iPhone testing and installation/signing must be completed before release.
