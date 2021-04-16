# Testing
To make sure the app is working, `adb shell` can be used to test whether files are updated correctly.

```shell
// test if performance parameter is updated properly
su -c cat /sys/module/msm_performance/parameters/cpu_m*_freq
```

```shell
// Get max and min, never change
cat /sys/devices/system/cpu/cpu*/cpufreq/cpuinfo_m*_freq

// Get current max and min
cat /sys/devices/system/cpu/cpu*/cpufreq/scaling_m*_freq
```
