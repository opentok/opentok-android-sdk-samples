# Video Transformers Changelog

All notable changes to this project will be documented in this file.

## 2.27.2

### Added

- Support Vonage Media Processor library as a opt-in library.
- Supports Noise Suppression

### Fixed

- Decoupled Media Processor library

### Notes

For versions with Android Gradle Plugin prior to 7.4, on merging native builds, it raises a error due to "2 files found with path 'lib/arm64-v8a/libmltransformers.so' from inputs".

The issue can be solved by adding to Media-Transformers-Java build.gradle the following option.

```c
packagingOptions {
        pickFirst "**/libmltransformers.so"
        pickFirst "**/libmltransformersaudionoisesuppression.so"
    }
```



## 2.25.2

### Added

- Support pre-built transformers in the Vonage Media Processor library or create your own custom video transformer to apply to published video.


