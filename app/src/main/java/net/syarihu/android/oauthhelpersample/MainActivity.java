package net.syarihu.android.oauthhelpersample;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.api.services.urlshortener.UrlshortenerScopes;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends ActionBarActivity implements OAuthHelper.OnAuthListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    OAuthHelper mHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mHelper = new OAuthHelper(this, "oauth2:" + UrlshortenerScopes.URLSHORTENER, this);
        mHelper.startAuth(false);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // アカウント選択や認証画面から返ってきた時の処理をOAuthHelperで受け取る
        mHelper.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void getAuthToken(final String authToken) {
        /*
        * TODO ここにAPIリクエストを書く
        * TODO ここでは例として、Google Url ShortenerでURLを短縮している
        */
        new AsyncTask<Void, Void, Boolean>() {
            boolean mResult;

            @Override
            protected Boolean doInBackground(Void... voids) {
                mResult = true;
                try {
                    // POST URLの生成
                    Uri.Builder builder = new Uri.Builder();
                    builder.path("https://www.googleapis.com/urlshortener/v1/url");
                    // AccountManagerで取得したAuthTokenをaccess_tokenパラメータにセットする
                    builder.appendQueryParameter("access_token", authToken);
                    String postUrl = Uri.decode(builder.build().toString());

                    JSONObject jsonRequest = new JSONObject();
                    jsonRequest.put("longUrl", "http://www.google.co.jp/");
                    URL url = new URL(postUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoInput(true);
                    conn.setDoOutput(true);
                    PrintStream ps = new PrintStream(conn.getOutputStream());
                    ps.print(jsonRequest.toString());
                    ps.close();

                    // POSTした結果を取得
                    InputStream is = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    String s;
                    String postResponse = "";
                    while ((s = reader.readLine()) != null) {
                        postResponse += s + "\n";
                    }
                    reader.close();
                    Log.v(TAG, postResponse);

                    JSONObject shortenInfo = new JSONObject(postResponse);
                    // エラー判定
                    if(shortenInfo.has("error")) {
                        Log.e(TAG, postResponse);
                        mResult = false;
                    }
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                    mResult = false;
                }
                Log.v(TAG, "shorten finished.");

                return mResult;
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if(result) return;
                Log.v(TAG, "再認証");
                mHelper.startAuth(true);
            }
        }.execute();
    }
}