# ncnn-android

Prebuilt NCNN Android+Vulkan release + compact Real-ESRGAN weights
(`realesr-animevideov3` ×2; ×4 + downscale if ×2 absent). Small JNI wrapper, CPU fallback
when Vulkan init fails. Inference runs in `android:process=":enhance"` behind a bound
service (files/shared-memory for pixels); watchdog + timeouts; 2 native crashes on a device
persist "enhancement disabled" + message. Pipeline: upscale only if shorter edge < 1.5×
display width; 128–192px tiles, 8–16px overlap, linear blend; WebP to disk cache keyed
(contentHash, page, modelId, scale), 500MB LRU cap + clear; queue priority current,+1..+3,
one inference at a time, cancel stale on scroll, pause on thermal≥MODERATE/Battery Saver.
