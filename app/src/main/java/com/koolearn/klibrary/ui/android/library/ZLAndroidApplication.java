package com.koolearn.klibrary.ui.android.library;

import android.app.Application;

import com.koolearn.android.kooreader.config.ConfigShadow;
import com.koolearn.klibrary.ui.android.image.ZLAndroidImageManager;

public abstract class ZLAndroidApplication extends Application {
	private ZLAndroidLibrary myLibrary;
	private ConfigShadow myConfig;

	@Override
	public void onCreate() {
		super.onCreate();
		// this is a workaround for strange issue on some devices:
		//    NoClassDefFoundError for android.os.AsyncTask
		try {
			Class.forName("android.os.AsyncTask"); // 一个解决bug的奇怪的方法
		} catch (Throwable t) {
		}

		myConfig = new ConfigShadow(this); // 绑定服务,创建config.db
		new ZLAndroidImageManager();
		myLibrary = new ZLAndroidLibrary(this); // 一些获取屏幕宽高,版本号的方法
		initImageLoader();
	}

	public final ZLAndroidLibrary library() {
		return myLibrary;
	}

	/**
	 * UIL图片加载配置
	 */
	private void initImageLoader() {
		// 个性化参数配置
//		ImageLoaderConfiguration configuration = ImageLoaderConfiguration.createDefault(this);
//		ImageLoader.getInstance().init(configuration);
	}
}