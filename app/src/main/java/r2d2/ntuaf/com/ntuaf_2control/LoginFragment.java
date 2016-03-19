package r2d2.ntuaf.com.ntuaf_2control;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.Profile;
import com.facebook.ProfileTracker;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class LoginFragment extends Fragment {

    private TextView mtextview;
    private CallbackManager mcallbackmanager;
    private AccessTokenTracker tracker;
    private ProfileTracker profileTracker;


    private FacebookCallback<LoginResult> mCallback = new FacebookCallback<LoginResult>() {
        @Override
        public void onSuccess(LoginResult loginResult) {

            Log.d("NTUAF", "onSuccess");

            Profile profile = Profile.getCurrentProfile();
            displaywelcomemsg(profile);

        }

        @Override
        public void onCancel() {
            Log.d("NTUAF", "onCancel");
        }

        @Override
        public void onError(FacebookException error) {
            Log.d("NTUAF", "onError:" + error);
        }
    };
    public LoginFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mcallbackmanager = CallbackManager.Factory.create();
        tracker = new AccessTokenTracker() {
            @Override
            protected void onCurrentAccessTokenChanged(AccessToken oldAccessToken, AccessToken newtoken) {

            }
        };
        profileTracker = new ProfileTracker() {
            @Override
            protected void onCurrentProfileChanged(Profile oldProfile, Profile currentProfile) {
                displaywelcomemsg(currentProfile);
            }
        };
        tracker.startTracking();
        profileTracker.startTracking();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        LoginButton loginButton = (LoginButton) view.findViewById(R.id.login_button);
        loginButton.setReadPermissions("user_friends");
        loginButton.setFragment(this);
        loginButton.registerCallback(mcallbackmanager, mCallback);


        mtextview = (TextView)view.findViewById(R.id.txtname);
    }

    private void displaywelcomemsg(Profile profile){
        if (profile !=  null) {
            String user_id = profile.getId();
            mtextview.setText("Hi! "+profile.getName());




        }else{
//            Toast.makeText(getActivity(), "GG!你好像沒登入", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mcallbackmanager.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onResume() {
        super.onResume();
        Profile profile = Profile.getCurrentProfile();
        displaywelcomemsg(profile);
    }

    @Override
    public void onStop() {
        super.onStop();
        profileTracker.stopTracking();
        tracker.stopTracking();
    }
//
//    private boolean isRegisteredUser(String id){
//        Httprequest client = new Httprequest();
//        String result = client.run("123");
//
//    }
//
//    private class Httprequest{
//        OkHttpClient client = new OkHttpClient();
//
//            String run(String url) throws IOException {
//            Request request = new Request.Builder()
//                    .url(url)
//                    .build();
//
//            Response response = client.newCall(request).execute();
//            return response.body().string();
//        }
//
//
//    }
}
