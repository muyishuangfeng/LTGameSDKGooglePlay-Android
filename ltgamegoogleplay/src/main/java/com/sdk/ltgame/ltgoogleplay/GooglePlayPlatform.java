package com.sdk.ltgame.ltgoogleplay;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.sdk.ltgame.ltgoogleplay.uikit.GooglePlayActivity;
import com.gentop.ltgame.ltgamesdkcore.common.LTGameOptions;
import com.gentop.ltgame.ltgamesdkcore.common.LTGameSdk;
import com.gentop.ltgame.ltgamesdkcore.common.Target;
import com.gentop.ltgame.ltgamesdkcore.impl.OnRechargeListener;
import com.gentop.ltgame.ltgamesdkcore.model.RechargeObject;
import com.gentop.ltgame.ltgamesdkcore.platform.AbsPlatform;
import com.gentop.ltgame.ltgamesdkcore.platform.IPlatform;
import com.gentop.ltgame.ltgamesdkcore.platform.PlatformFactory;
import com.gentop.ltgame.ltgamesdkcore.uikit.BaseActionActivity;
import com.gentop.ltgame.ltgamesdkcore.util.LTGameUtil;

import java.util.Map;

public class GooglePlayPlatform extends AbsPlatform {

    private GooglePlayHelper mHelper;


    private GooglePlayPlatform(Context context,boolean isServerTest, String appId, String appKey,
                               String adID, String packageID,int target) {
        super(context, isServerTest,appId, appKey, adID,packageID,target);
    }

    @Override
    public void recharge(Activity activity, int target, RechargeObject object, OnRechargeListener listener) {
        mHelper = new GooglePlayHelper(activity, object.getPublicKey(), object.getPayTest(), object.getSku(),
                object.getGoodsID(), object.getSelfRequestCode(), object.getParams(), listener);
        mHelper.init();
    }

    @Override
    public void onActivityResult(BaseActionActivity activity, int requestCode, int resultCode, Intent data) {
        mHelper.onActivityResult(requestCode, resultCode, data, mHelper.mRequestCode);
    }

    @Override
    public Class getUIKitClazz() {
        return GooglePlayActivity.class;
    }

    @Override
    public void recycle() {
        if (mHelper != null) {
            mHelper.release();
        }
    }

    /**
     * 工厂
     */
    public static class Factory implements PlatformFactory {

        @Override
        public IPlatform create(Context context, int target) {
            IPlatform platform = null;
            LTGameOptions options = LTGameSdk.options();
            if (!LTGameUtil.isAnyEmpty(options.getLtAppId(), options.getLtAppKey())) {
                platform = new GooglePlayPlatform(context, options.getISServerTest(),
                        options.getLtAppId(), options.getLtAppKey(),options.getAdID(),
                        options.getPackageID(), target);
            }
            return platform;
        }

        @Override
        public int getPlatformTarget() {
            return Target.PLATFORM_GOOGLE_PLAY;
        }

        @Override
        public boolean checkLoginPlatformTarget(int target) {
            return false;
        }

        @Override
        public boolean checkRechargePlatformTarget(int target) {
            return target == Target.RECHARGE_GOOGLE;
        }
    }
}
