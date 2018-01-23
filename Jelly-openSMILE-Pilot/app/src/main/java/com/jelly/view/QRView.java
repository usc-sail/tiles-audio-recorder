package com.jelly.view;

import com.jelly.view.base.BaseView;
import com.jelly.view.base.InternetView;
import com.jelly.view.base.ProgressableView;
import com.jelly.view.base.View;

/**
 * Created by Tiantian on 27/11/17.
 */

public interface QRView extends BaseView, InternetView, ProgressableView, View {
    void handleCameraPermission();
    void permissionGranted();
    void permissionRejected();
    void showToast(String message);
}
