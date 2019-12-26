package com.sdk.ltgame.ltgoogleplay;

import android.app.Activity;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import com.gentop.ltgame.ltgamesdkcore.exception.LTGameError;
import com.sdk.ltgame.ltgoogleplay.util.IabHelper;
import com.sdk.ltgame.ltgoogleplay.util.IabResult;
import com.sdk.ltgame.ltgoogleplay.util.Inventory;
import com.sdk.ltgame.ltgoogleplay.util.Purchase;
import com.gentop.ltgame.ltgamesdkcore.common.Target;
import com.gentop.ltgame.ltgamesdkcore.impl.OnRechargeListener;
import com.gentop.ltgame.ltgamesdkcore.model.RechargeResult;
import com.google.gson.Gson;
import com.sdk.ltgame.ltnet.base.Constants;
import com.sdk.ltgame.ltnet.manager.LoginRealizeManager;
import com.sdk.ltgame.ltnet.model.GoogleModel;
import com.sdk.ltgame.ltnet.util.PreferencesUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class GooglePlayHelper {

    private static final String TAG = GooglePlayHelper.class.getSimpleName();
    private static IabHelper mHelper;
    private static boolean mSetupDone = false;
    //订单号
    private String mOrderID;
    //商品集合
    private List<String> mGoodsList = new ArrayList<>();
    private WeakReference<Activity> mActivityRef;
    //公钥
    private String mPublicKey;
    private int mRechargeTarget;
    private OnRechargeListener mListener;
    //是否是沙盒账号
    private int mPayTest;
    //商品ID
    private String mProductID;
    //请求码
    int mRequestCode;
    //自定义参数
    private Map<String, Object> mParams;
    //商品
    private String mSku;


    GooglePlayHelper(Activity activity, String mPublicKey, int payTest,
                     String sku, String productID, int requestCode, Map<String, Object> mParams,
                     OnRechargeListener mListener) {
        this.mActivityRef = new WeakReference<>(activity);
        this.mPublicKey = mPublicKey;
        this.mPayTest = payTest;
        this.mSku = sku;
        this.mProductID = productID;
        this.mRequestCode = requestCode;
        this.mParams = mParams;
        this.mRechargeTarget = Target.RECHARGE_GOOGLE;
        this.mListener = mListener;
    }

    public GooglePlayHelper(Activity activity, String mPublicKey) {
        this.mActivityRef = new WeakReference<>(activity);
        this.mPublicKey = mPublicKey;
        this.mRechargeTarget = Target.RECHARGE_GOOGLE;
    }


    /**
     * 补单操作
     */
    public void queryOrder() {
        //创建谷歌帮助类
        mHelper = new IabHelper(mActivityRef.get(), mPublicKey);
        mHelper.enableDebugLogging(true);
        if (mHelper != null) {
            mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
                @Override
                public void onIabSetupFinished(IabResult result) {
                    if (result.isSuccess()) {
                        try {
                            mHelper.queryInventoryAsync(true, null, null,
                                    new IabHelper.QueryInventoryFinishedListener() {
                                        @Override
                                        public void onQueryInventoryFinished(IabResult result, Inventory inv) {
                                            if (result.isSuccess()) {
                                                if (inv.getAllPurchases() != null) {
                                                    if (inv.getAllPurchases().size() > 0) {
                                                        for (int i = 0; i < inv.getAllPurchases().size(); i++) {
                                                            if (inv.getAllPurchases().get(i).getToken() != null && inv.getAllPurchases().get(i).getDeveloperPayload() != null) {
                                                                uploadToServer3(inv.getAllPurchases().get(i), inv.getAllPurchases().get(i).getToken(), inv.getAllPurchases().get(i).getDeveloperPayload());
                                                            }

                                                        }
                                                    }

                                                }

                                            }
                                        }

                                    });
                        } catch (IabHelper.IabAsyncInProgressException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }
    }




    /**
     * 初始化
     */
    void init() {
        //创建谷歌帮助类
        mHelper = new IabHelper(mActivityRef.get(), mPublicKey);
        mHelper.enableDebugLogging(true);
        if (mHelper != null) {
            mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
                @Override
                public void onIabSetupFinished(IabResult result) {
                    if (!result.isSuccess()) {
                        mSetupDone = false;
                    } else {
                        mSetupDone = true;
                        try {
                            mHelper.queryInventoryAsync(true, null, null,
                                    new IabHelper.QueryInventoryFinishedListener() {
                                        @Override
                                        public void onQueryInventoryFinished(IabResult result, Inventory inv) {
                                            if (result.isSuccess()) {
                                                if (inv.getAllPurchases() != null) {
                                                    if (inv.getAllPurchases().size() > 0) {
                                                        mGoodsList = getGoodsList(inv.getAllPurchases());
                                                        for (int i = 0; i < inv.getAllPurchases().size(); i++) {
                                                            consumeProduct(inv.getAllPurchases().get(i));
                                                        }
                                                    } else {
                                                        recharge();
                                                    }

                                                }

                                            }
                                        }

                                    });
                        } catch (IabHelper.IabAsyncInProgressException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }
    }

    /**
     * 消费掉商品
     */
    private void consumeProduct(Purchase purchase) {
        try {
            mHelper.consumeAsync(purchase, new IabHelper.OnConsumeFinishedListener() {
                @Override
                public void onConsumeFinished(Purchase purchase, IabResult result) {
                    if (purchase.getToken() != null && purchase.getDeveloperPayload() != null) {
                        uploadToServer2(purchase.getToken(), purchase.getDeveloperPayload());
                    }
                }
            });
        } catch (IabHelper.IabAsyncInProgressException e) {
            e.printStackTrace();
        }
    }

    /**
     * 消费掉商品
     */
    private void consumeProduct2(Purchase purchase) {
        try {
            mHelper.consumeAsync(purchase, new IabHelper.OnConsumeFinishedListener() {
                @Override
                public void onConsumeFinished(Purchase purchase, IabResult result) {
                }
            });
        } catch (IabHelper.IabAsyncInProgressException e) {
            e.printStackTrace();
        }
    }

    /**
     * 购买
     */
    private void recharge() {
        if (mSetupDone) {
            if (!TextUtils.isEmpty(PreferencesUtils.getString(mActivityRef.get(),
                    Constants.USER_API_TOKEN))) {
                getLTOrderID(mParams);
            } else {
                mListener.onState(mActivityRef.get(), RechargeResult.failOf("order create failed:user token is empty"));
            }
        } else {
            if (!TextUtils.isEmpty(mPublicKey)) {
                //创建谷歌帮助类
                mHelper = new IabHelper(mActivityRef.get(), mPublicKey);
                mHelper.enableDebugLogging(true);
                mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
                    @Override
                    public void onIabSetupFinished(IabResult result) {
                        if (result.isFailure()) {
                            mSetupDone = false;
                        }
                        if (result.isSuccess()) {
                            mSetupDone = true;
                        }
                    }
                });
            }

        }
    }


    /**
     * 获取乐推订单ID
     *
     * @param params 集合
     */
    private void getLTOrderID(Map<String, Object> params) {
        LoginRealizeManager.createOrder(mActivityRef.get(), mProductID, params, new OnRechargeListener() {

            @Override
            public void onState(Activity activity, RechargeResult result) {
                if (result != null) {
                    if (result.getResultModel() != null) {
                        if (result.getResultModel().getData() != null) {
                            if (!result.getResultModel().getResult().equals("NO")) {
                                if (result.getResultModel().getData().getLt_order_id() != null) {
                                    mOrderID = result.getResultModel().getData().getLt_order_id();
                                    try {
                                        if (mHelper == null) return;
                                        mHelper.queryInventoryAsync(true, mGoodsList, mGoodsList,
                                                new IabHelper.QueryInventoryFinishedListener() {
                                                    @Override
                                                    public void onQueryInventoryFinished(IabResult result, Inventory inv) {
                                                        if (result != null) {
                                                            if (result.isSuccess() && inv.hasPurchase(mSku)) {
                                                                //消费, 并下一步, 这里Demo里面我没做提示,将购买了,但是没消费掉的商品直接消费掉, 正常应该
                                                                //给用户一个提示,存在未完成的支付订单,是否完成支付
                                                                consumeProduct(inv.getPurchase(mSku));
                                                            } else {
                                                                getProduct(mRequestCode, mSku);
                                                            }
                                                        }
                                                    }

                                                });
                                    } catch (IabHelper.IabAsyncInProgressException e) {
                                        e.printStackTrace();
                                    }
                                }
                            } else {
                                mListener.onState(mActivityRef.get(), RechargeResult.failOf(result.getResultModel().getMsg()));
                            }

                        }

                    }
                }
            }

        });
    }


    /**
     * 产品获取
     *
     * @param REQUEST_CODE 请求码
     * @param SKU          产品唯一id, 填写你自己添加的商品id
     */
    private void getProduct(int REQUEST_CODE, final String SKU) {
        if (!TextUtils.isEmpty(mOrderID)) {
            try {
                mHelper.launchPurchaseFlow(mActivityRef.get(), SKU, REQUEST_CODE, new IabHelper.OnIabPurchaseFinishedListener() {
                    @Override
                    public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
                        if (result.isFailure()) {
                            return;
                        }
                        if (purchase.getSku().equals(SKU)) {
                            //购买成功，调用消耗
                            consumeProduct(purchase);
                        }
                    }
                }, mOrderID);
            } catch (IabHelper.IabAsyncInProgressException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 回调
     *
     * @param requestCode     请求码
     * @param resultCode      结果码
     * @param data            数据
     * @param selfRequestCode 自定义请求码
     */
    void onActivityResult(int requestCode, int resultCode, Intent data, int selfRequestCode) {
        //将回调交给帮助类来处理, 否则会出现支付正在进行的错误
        if (mHelper == null) return;
        mHelper.handleActivityResult(requestCode, resultCode, data);
        if (requestCode == selfRequestCode) {
            int responseCode = data.getIntExtra("RESPONSE_CODE", 0);
            //订单信息
            String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");
            String dataSignature = data.getStringExtra("INAPP_DATA_SIGNATURE");
            switch (responseCode) {
                case 0: {//成功
                    if (!TextUtils.isEmpty(purchaseData)) {
                        GoogleModel googleModel = new Gson().fromJson(purchaseData, GoogleModel.class);
                        Map<String, Object> params = new WeakHashMap<>();
                        params.put("purchase_token", googleModel.getPurchaseToken());
                        params.put("lt_order_id", mOrderID);
                        uploadToServer(googleModel.getPurchaseToken(), mSku);
                    }
                }
                break;
                case 1: {//取消
                    mListener.onState(mActivityRef.get(), RechargeResult.failOf("1"));
                }
                break;
                case 2: {//网络异常
                    mListener.onState(mActivityRef.get(), RechargeResult.failOf("2"));
                }
                break;
                case 3: {//不支持购买
                    mListener.onState(mActivityRef.get(), RechargeResult.failOf("3"));
                }
                break;
                case 4: {//商品不可购买
                    mListener.onState(mActivityRef.get(), RechargeResult.failOf("4"));
                }
                break;
                case 5: {//提供给 API 的无效参数
                    mListener.onState(mActivityRef.get(), RechargeResult.failOf("5"));
                }
                break;
                case 6: {//错误
                    mListener.onState(mActivityRef.get(), RechargeResult.failOf("6"));
                }
                break;
                case 7: {//未消耗掉
                    mListener.onState(mActivityRef.get(), RechargeResult.failOf("7"));
                }
                break;
                case 8: {//不可购买
                    mListener.onState(mActivityRef.get(), RechargeResult.failOf("8"));
                }
                break;
            }
        }

    }

    /**
     * 上传到服务器验证
     */
    private void uploadToServer(final String purchaseToken, final String productID) {
        LoginRealizeManager.googlePlay(mActivityRef.get(),
                purchaseToken, mOrderID, mPayTest, new OnRechargeListener() {

                    @Override
                    public void onState(Activity activity, RechargeResult result) {
                        if (result != null) {
                            if (result.getResultModel() != null) {
                                if (result.getResultModel().getCode() == 200) {
                                    mListener.onState(mActivityRef.get(), RechargeResult.successOf(result.getResultModel()));
                                    if (mHelper == null) {
                                        mHelper = new IabHelper(mActivityRef.get(), mPublicKey);
                                        mHelper.enableDebugLogging(true);
                                        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
                                            @Override
                                            public void onIabSetupFinished(IabResult result) {
                                                if (result.isSuccess()) {
                                                    try {
                                                        mHelper.queryInventoryAsync(true, mGoodsList, mGoodsList,
                                                                new IabHelper.QueryInventoryFinishedListener() {
                                                                    @Override
                                                                    public void onQueryInventoryFinished(IabResult result, Inventory inv) {
                                                                        if (result != null) {
                                                                            if (result.isSuccess() && inv.hasPurchase(productID)) {
                                                                                //消费, 并下一步, 这里Demo里面我没做提示,将购买了,但是没消费掉的商品直接消费掉, 正常应该
                                                                                //给用户一个提示,存在未完成的支付订单,是否完成支付
                                                                                consumeProduct2(inv.getPurchase(productID));
                                                                            }
                                                                        }
                                                                    }

                                                                });
                                                    } catch (IabHelper.IabAsyncInProgressException e) {
                                                        e.printStackTrace();
                                                    }
                                                }
                                            }
                                        });
                                    }

                                }
                            }

                        }

                    }

                });
    }

    /**
     * 补单
     */
    private void uploadToServer2(final String purchaseToken, final String productID) {
        LoginRealizeManager.googlePlay(mActivityRef.get(),
                purchaseToken, mOrderID, mPayTest, new OnRechargeListener() {

                    @Override
                    public void onState(Activity activity, RechargeResult result) {
                        if (result != null) {
                            if (result.getResultModel().getCode() == 200) {
                                recharge();
                            }
                        }

                    }

                });
    }

    /**
     * 释放资源
     */
    void release() {
        if (mHelper != null) {
            mHelper.disposeWhenFinished();
        }
        mHelper = null;
    }

    /**
     * 获取商品集合
     */
    private List<String> getGoodsList(
            List<Purchase> mList) {
        mGoodsList = new ArrayList<>();
        for (int i = 0; i < mList.size(); i++) {
            mGoodsList.add(mList.get(i).getSku());
        }
        return mGoodsList;
    }


    /**
     * 上传到服务器验证
     */
    private void uploadToServer3(final Purchase purchase, final String purchaseToken, String mOrderID) {
        LoginRealizeManager.googlePlay(mActivityRef.get(),
                purchaseToken, mOrderID, mPayTest, new OnRechargeListener() {
                    @Override
                    public void onState(Activity activity, RechargeResult result) {
                        if (result != null) {
                            if (result.getResultModel().getCode() == 200) {
                                consumeProduct2(purchase);
                            }
                        }

                    }

                });
    }
}
