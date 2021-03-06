package oauthclient;

import android.content.Context;

import com.android.emobilepos.R;
import com.crashlytics.android.Crashlytics;
import com.google.gson.Gson;

import java.security.SecureRandom;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import io.realm.Realm;

/**
 * Created by guarionex on 8/10/16.
 */
public class OAuthManager {
    private HttpClient httpClient = new HttpClient();
    private String requestTokenUrl;

    private OAuthManager(Context context, String clientId, String clientSecret) {
        byte[] key = new byte[64];
        new SecureRandom().nextBytes(key);
        requestTokenUrl = context.getString(R.string.oauth_token_url);
        Realm realm = Realm.getDefaultInstance();
        try {
            realm.beginTransaction();
            OAuthClient authClient = realm.createObject(OAuthClient.class);
            authClient.setClient_id(clientId);
            authClient.setClient_secret(clientSecret);
        } catch (Exception e) {
            Crashlytics.logException(e);
        } finally {
            realm.commitTransaction();
            if(realm!=null) {
                realm.close();
            }
        }
        try {
            requestToken();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isExpired(Context context) {
        OAuthClient authClient = getOAuthClient(context);
        if (authClient == null || authClient.getExpirationDate() == null) {
            return true;
        }
        Calendar now = GregorianCalendar.getInstance();
        now.setTime(new Date());
        Calendar oauthCal = GregorianCalendar.getInstance();
        oauthCal.setTime(authClient.getExpirationDate());
        return now.compareTo(oauthCal) >= 0;

    }

    public static OAuthManager getInstance(Context context, String clientId, String clientSecret) {
        return new OAuthManager(context, clientId, clientSecret);
    }

    public static OAuthClient getOAuthClient(Context context) {
        Realm realm = Realm.getDefaultInstance();
        OAuthClient authClient = realm.where(OAuthClient.class).findFirst();
        if (authClient != null)
            authClient = realm.copyFromRealm(authClient);
        realm.close();
        return authClient;

    }

    public String requestToken() throws Exception {
        final String[] requestToken = new String[1];
        Realm realm = Realm.getDefaultInstance();
        OAuthClient authClient = realm.where(OAuthClient.class).findFirst();
        String urlOAuthParams = "grant_type=client_credentials&client_id=%s&client_secret=%s";
        final String oauthUrl = String.format(urlOAuthParams, authClient.getClient_id(), authClient.getClient_secret());
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    requestToken[0] = httpClient.post(requestTokenUrl, oauthUrl, true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                synchronized (requestToken) {
                    requestToken.notifyAll();
                }
            }
        }).start();
        synchronized (requestToken) {
            requestToken.wait();
        }
        realm.beginTransaction();
        Gson gson = new Gson();
        OAuthClient fromJson = gson.fromJson(requestToken[0], OAuthClient.class);
        authClient.setAccessToken(fromJson.getAccessToken());
        Calendar cal = GregorianCalendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.SECOND, fromJson.getExpiresIn());
        authClient.setExpirationDate(cal.getTime());
        authClient.setExpiresIn(fromJson.getExpiresIn());
        authClient.setRefreshToken(fromJson.getRefreshToken());
        realm.commitTransaction();
        realm.close();
        return requestToken[0];
    }

}