package com.koolearn.android.kooreader;

import com.koolearn.klibrary.text.model.ZLTextModel;
import com.koolearn.klibrary.text.view.ZLReaderView;
import com.koolearn.kooreader.kooreader.KooReaderApp;

class ShowSettingAction extends KooAndroidAction {
	ShowSettingAction(ReaderActivity baseActivity, KooReaderApp kooreader) {
		super(baseActivity, kooreader);
	}

	@Override
	public boolean isVisible() {
		final ZLReaderView view = (ZLReaderView)Reader.getCurrentView();
		final ZLTextModel textModel = view.getModel();
		return textModel != null && textModel.getParagraphsNumber() != 0;
	}

	@Override
	protected void run(Object ... params) {
		BaseActivity.setting();
	}
}
