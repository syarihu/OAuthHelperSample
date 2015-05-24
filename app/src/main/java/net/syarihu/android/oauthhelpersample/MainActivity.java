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

import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class MainActivity extends ActionBarActivity implements OAuthHelper.OnAuthListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    OAuthHelper mHelper;
    private boolean mInvalidated = false;

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
        * TODO ここでstartAuthすると無限ループするので、AuthTokenの再取得処理を入れる際は注意して実装する
        */
        new AsyncTask<Void, Void, Boolean>() {
            boolean mResult;

            @Override
            protected Boolean doInBackground(Void... voids) {
                mResult = true;
                Uri.Builder builder = new Uri.Builder();
                builder.path("https://www.googleapis.com/urlshortener/v1/url");
                // AccountManagerで取得したAuthTokenをaccess_tokenパラメータにセットする
                builder.appendQueryParameter("access_token", authToken);
                String postUrl = Uri.decode(builder.build().toString());
                DefaultHttpClient client = new DefaultHttpClient();
                HttpPost post = new HttpPost(postUrl);
                post.setHeader("Content-type", "application/json");
                try {
                    JSONObject jsonRequest = new JSONObject();
                    jsonRequest.put("longUrl", "http://www.google.com/");
                    StringEntity stringEntity = new StringEntity(jsonRequest.toString());
                    post.setEntity(stringEntity);
                    client.execute(post, new ResponseHandler<String>() {
                        @Override
                        public String handleResponse(HttpResponse response) throws IOException {
                            String res_entity = EntityUtils.toString(response.getEntity(), "UTF-8");
                            Log.v(TAG, res_entity);
                            try {
                                JSONObject json = new JSONObject(res_entity);
                                mResult = !json.has("error");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            return null;
                        }
                    });
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                    mResult = false;
                }

                return mResult;
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if(!result) {
                    // 失敗時の処理
                    if(!mInvalidated) {
                        mHelper.startAuth(true);
                        mInvalidated = true;
                    }
                }
            }
        }.execute();
    }
}