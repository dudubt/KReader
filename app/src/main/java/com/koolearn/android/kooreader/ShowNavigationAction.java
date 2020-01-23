package com.koolearn.android.kooreader;

import com.koolearn.klibrary.text.model.ZLTextModel;
import com.koolearn.klibrary.text.view.ZLReaderView;
import com.koolearn.kooreader.kooreader.KooReaderApp;

class ShowNavigationAction extends KooAndroidAction {
	ShowNavigationAction(ReaderActivity baseActivity, KooReaderApp kooReaderApp) {
		super(baseActivity, kooReaderApp);
	}

	@Override
	public boolean isVisible() {
		final ZLReaderView view = (ZLReaderView)Reader.getCurrentView();
		final ZLTextModel textModel = view.getModel();
		return textModel != null && textModel.getParagraphsNumber() != 0;
	}

	@Override
	protected void run(Object ... params) {
		BaseActivity.navigate();
	}
}
