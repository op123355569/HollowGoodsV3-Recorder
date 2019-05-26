# HollowGoodsV3-Recorder
### LastVersion：
[![](https://jitpack.io/v/op123355569/HollowGoodsV3-Recorder.svg)](https://jitpack.io/#op123355569/HollowGoodsV3-Recorder)

```
allprojects {
	repositories {
		...
		maven { url 'https://jitpack.io' }
	}
}
```

```
dependencies {
	implementation 'com.github.op123355569:HollowGoodsV3-Recorder:LastVersion'
}
```

```
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
```

```
AndroidAudioRecorder.with(this)
	// Required
	.setFilePath(AUDIO_FILE_PATH)
	.setColor(ContextCompat.getColor(this, R.color.recorder_bg))
	.setRequestCode(REQUEST_RECORD_AUDIO)

	// Optional
	.setSource(AudioSource.MIC)
	.setChannel(AudioChannel.STEREO)
	.setSampleRate(AudioSampleRate.HZ_48000)
	.setAutoStart(false)
	.setKeepDisplayOn(true)

	// Start recording
	.record();
```
