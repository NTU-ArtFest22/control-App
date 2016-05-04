package r2d2.ntuaf.com.ntuaf_2control;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
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
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.security.spec.ECField;

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
            if (profile != null) {
                displaywelcomemsg(profile);
                LoginTask logintask = new LoginTask();
                logintask.execute(profile.getId());
            }else{
                Log.i("NTUAF", "No user data");
            }
        }

        @Override
        public void onCancel() {
            Log.d("NTUAF", "onCancel");
        }

        @Override
        public void onError(FacebookException error) {
            Log.d("NTUAF", "onError(FB):" + error);
        }
    };

    public LoginFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        check if there is a logout request
    //log out before for init

        LoginManager.getInstance().logOut();

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


        mtextview = (TextView) view.findViewById(R.id.txtname);
    }

    private void displaywelcomemsg(Profile profile) {
        if (profile != null) {
            mtextview.setText("Hi! " + profile.getName());

        } else {
//            Toast.makeText(getActivity(), "GG!你好像沒登入", Toast.LENGTH_SHORT).show();
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
        if (profile==null){
            user_logout();
            LoginManager.getInstance().logOut();
            Log.i("NTUAF", "user logout successfully");
        }else{
            displaywelcomemsg(profile);
        }


    }

    @Override
    public void onStop() {
        super.onStop();
        profileTracker.stopTracking();
        tracker.stopTracking();
    }

    private void user_logout(){

        mtextview.setText("HI!請用Facebook登入");
    }

    public class LoginTask extends AsyncTask<String, Void, Boolean> {

        private OkHttpClient client = new OkHttpClient();
        private final String LOG_TAG = LoginTask.class.getSimpleName();

        String token, email, dname, id, param_id;

        /**
         * Take the String representing the complete forecast in JSON Format and
         * pull out the data we need to construct the Strings needed for the wireframes.
         * <p/>
         * Fortunately parsing is easy:  constructor takes the JSON string and converts it
         * into an Object hierarchy for us.
         */

        @Override
        protected Boolean doInBackground(String... params) {
            if (params.length==0){
                return false;
            }
            Log.i("NTUAF", "start async task");
            param_id = params[0];
            String result;
            try {
                Log.i("NTUAF", "id from fb:" + params[0]);
                result = run(getString(R.string.server_location) + getString(R.string.api_login) + params[0]);
                Log.i("NTUAF", "result:"+result);


            } catch (IOException e) {
                Log.e("NTUAF", "Error(internet):" + e);



                return false;
            }
            try{
                if (result!=null){
                    getuserDataFromJson(result);
                    return true;
                }else{
                    return false;
                }

            }catch (JSONException e){
                Log.e("NTUAF-web", "Error(JSON):" + e);
                return false;
            }
        }

        private String run(String url) throws IOException {
            Request request = new Request.Builder()
                    .url(url)
                    .build();

            Response response = client.newCall(request).execute();
            return response.body().string();
        }

        private boolean getuserDataFromJson(String JsonStr)
                throws JSONException {

            // These are the names of the JSON objects that need to be extracted.
            final String AF_LIST = "list";
            final String AF_FB = "fb";
            final String AF_EMAIL = "email";
            final String AF_DISPLAY_NAME = "displayName";
            final String AF_ID = "id";
            final String AF_TOKEN = "token";

            JSONObject userjson = new JSONObject(JsonStr);
            JSONObject user_fb_data = userjson.getJSONObject(AF_FB);
//            token = user_fb_data.getString(AF_TOKEN);
            email = user_fb_data.getString(AF_EMAIL);
            dname = user_fb_data.getString(AF_DISPLAY_NAME);
            id = user_fb_data.getString(AF_ID);



            return true;

        }

        @Override

        protected void onPostExecute(Boolean result) {
            if (result == true) {
                if (param_id.length() != 0) {
                    Log.i("NTUAF", param_id+" "+id+" "+param_id.equals(id));
                    if (param_id.equals(id)) {
                        Toast.makeText(getActivity(), "登入成功", Toast.LENGTH_SHORT).show();
                        Log.i("NTUAF", "Login success!");
                        Fragment fragment = new ActivityFragment();
                        FragmentManager fm = getActivity().getSupportFragmentManager();
                        FragmentTransaction transaction = fm.beginTransaction();
                        transaction.replace(android.R.id.content, fragment);
                        transaction.commit();
                    } else {
                        Toast.makeText(getActivity(), "沒有你的資料！", Toast.LENGTH_SHORT).show();
                        Log.i("NTUAF", "Error:profile is null in async task2");
                    }
                } else {
                    Toast.makeText(getActivity(), "沒有你的資料！", Toast.LENGTH_SHORT).show();
                    Log.e("NTUAF", "Error:profile is null in async task");
                }
            } else {
                Toast.makeText(getActivity(), "網路出錯！請重新登入", Toast.LENGTH_SHORT).show();

//                mForecastAdapter.clear();
//                for(String dayForecastStr : result) {
//                    mForecastAdapter.add(dayForecastStr);
//                }
                // New data is back from the server.  Hooray!
            }
        }
    }
}