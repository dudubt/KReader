package com.koolearn.android.kooreader;

import com.koolearn.kooreader.kooreader.KooAction;
import com.koolearn.kooreader.kooreader.KooReaderApp;

abstract class KooAndroidAction extends KooAction {
	protected final ReaderActivity BaseActivity;

	KooAndroidAction(ReaderActivity baseActivity, KooReaderApp kooreader) {
		super(kooreader);
		BaseActivity = baseActivity;
	}
}
