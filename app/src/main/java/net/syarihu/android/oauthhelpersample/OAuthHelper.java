package net.syarihu.android.oauthhelpersample;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.IOException;

/**
 * OAuth認証を手軽に実装するためのクラス
 *
 * Created by syarihu on 2015/04/27.
 */
public class OAuthHelper {
    private static final String TAG = OAuthHelper.class.getSimpleName();
    public static final int REQUEST_ACCOUNT_PICKER = 0;
    public static final int REQUEST_AUTH_DIALOG = 1;

    private static final String KEY_ACCOUNT_NAME = "account_name";
    private static final String KEY_ACCOUNT_TYPE = "account_type";

    private Activity mActivity;
    AccountManager mAccountManager;
    private String mAccountName;
    private String mAccountType;
    private String mAuthToken;
    private String mScopes;
    private OnAuthListener mOnAuthListener;
    private boolean mInvalidate;

    public OAuthHelper(Activity activity, String scopes, OnAuthListener listener) {
        mActivity = activity;
        mScopes = scopes;
        mOnAuthListener = listener;
        mAccountManager = AccountManager.get(activity);
    }

    /**
     * 認証処理を開始
     *
     * @param invalidate AuthTokenが使えなくなっていた場合にtrueにすることで、Tokenを再取得
     */
    public void startAuth(boolean invalidate) {
        // アカウントが選択されているか確認
        if (getPreference(KEY_ACCOUNT_NAME) == null) {
            // アカウント選択画面を表示する
            selectAccount();
            return;
        }

        mAccountName = getPreference(KEY_ACCOUNT_NAME);
        mAccountType = getPreference(KEY_ACCOUNT_TYPE);
        mInvalidate = invalidate;
        // 認証を開始
        auth();
    }

    /**
     * アカウント選択画面を表示
     */
    private void selectAccount() {
        if (mAccountName != null || mActivity == null) {
            return;
        }

        Intent intent = AccountManager.newChooseAccountIntent(
                null, null, new String[]{
                        "com.google"
                },
                false, null, null, null, null);
        mActivity.startActivityForResult(intent, REQUEST_ACCOUNT_PICKER);
        Log.v(TAG, "start account picker");
    }

    /**
     * Intentから返ってきた時の処理
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.v(TAG, "resultCode:" + resultCode);

        if (resultCode != Activity.RESULT_OK) {
            // キャンセル・失敗処理
            return;
        }

        switch (requestCode) {
            case REQUEST_ACCOUNT_PICKER:
                mAccountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                mAccountType = data.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE);
                // アカウントを保存
                setPreference(KEY_ACCOUNT_NAME, data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME));
                setPreference(KEY_ACCOUNT_TYPE, data.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE));
                auth();
                break;
            case REQUEST_AUTH_DIALOG:
                // 認証が許可された場合
                mAuthToken = data.getStringExtra(AccountManager.KEY_AUTHTOKEN);
                Log.v(TAG, "auth_token: " + mAuthToken);
                mOnAuthListener.getAuthToken(mAuthToken);
                break;
        }
    }

    /**
     * 認証許可やAuthTokenの取得など
     */
    private void auth() {
        mAccountManager.getAuthToken(new Account(mAccountName, mAccountType),
                mScopes,
                null, false, new AccountManagerCallback<Bundle>() {
                    @Override
                    public void run(AccountManagerFuture<Bundle> future) {
                        Bundle bundle;
                        try {
                            bundle = future.getResult();
                            Intent intent = (Intent) bundle.get(AccountManager.KEY_INTENT);
                            // 認証されていない場合は認証許可ダイアログを表示
                            if (intent != null) {
                                Log.v(TAG, "start auth dialog");
                                mActivity.startActivityForResult(intent, REQUEST_AUTH_DIALOG);
                                return;
                            }
                            // 既に認証されている場合はAuthTokenをそのまま取得
                            mAuthToken = bundle.getString(AccountManager.KEY_AUTHTOKEN);
                            // invalidateがtrueならAuthTokenを再取得する
                            if (mInvalidate && mAuthToken != null) {
                                mAccountManager.invalidateAuthToken(mAccountType, mAuthToken);
                                // 再認証
                                auth();
                                Log.v(TAG, "invalidated");
                                mInvalidate = false;
                            } else {
                                // AuthToken取得時の処理を実行
                                mOnAuthListener.getAuthToken(mAuthToken);
                            }
                        } catch (OperationCanceledException | IOException | AuthenticatorException e) {
                            e.printStackTrace();
                        }
                    }
                }, null);
    }

    /**
     * Preferenceに保存
     */
    private void setPreference(String key, String value) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mActivity.getApplicationContext());
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(key, value);
        editor.apply();
    }

    /**
     * Preferenceを取得
     */
    private String getPreference(String key) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mActivity.getApplicationContext());
        return pref.getString(key, null);
    }

    public interface OnAuthListener {
        /**
         * AuthToken取得時
         */
        void getAuthToken(String authToken);
    }
}

