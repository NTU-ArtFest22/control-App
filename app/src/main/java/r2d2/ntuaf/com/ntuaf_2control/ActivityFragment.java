/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package r2d2.ntuaf.com.ntuaf_2control;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.facebook.Profile;
import com.facebook.login.LoginManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Encapsulates fetching the forecast and displaying it as a {@link ListView} layout.
 */
public class ActivityFragment extends Fragment {
    private String[] all_activity_id;
    private ArrayAdapter<String> mActAdapter;

    public ActivityFragment() {
    }
    private Profile profile = Profile.getCurrentProfile();
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Add this line in order for this fragment to handle menu events.

        if (profile==null){
            Log.i("NTUAF_ACT", "NO user data");

            Toast.makeText(getActivity(), "沒有你的資料，請重新登入", Toast.LENGTH_LONG).show();
            LoginManager.getInstance().logOut();
            backtologin();

        }else{
            FetchActivityTask fetchActivityTask = new FetchActivityTask();
            fetchActivityTask.execute(profile.getId());
            Log.i("NTUAF_ACT", "get user data");
        }


        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.activity_menu, menu);
    }
    private void backtologin(){
        Fragment fragment = new LoginFragment();
        Bundle bundle = new Bundle();
        bundle.putString("message", "logout");
        fragment.setArguments(bundle);
        //back to login
        FragmentManager fm = getActivity().getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        transaction.replace(android.R.id.content, fragment);
        transaction.commit();
    }
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id){
            case R.id.menu_btn_logout:
                Log.i("NTUAF_ACT", "User press logout");
                LoginManager.getInstance().logOut();
                backtologin();
                return true;
            case R.id.menu_btn_refresh:
                Log.i("NTUAF_ACT", "User press refresh");
                FetchActivityTask task = new FetchActivityTask();

                task.execute(profile.getId());
                return true;

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Create some dummy data for the ListView.  Here's a sample weekly forecast
        String[] data = {
                "沒有活動"
        };
        List<String> weekForecast = new ArrayList<String>(Arrays.asList(data));

        // Now that we have some dummy forecast data, create an ArrayAdapter.
        // The ArrayAdapter will take data from a source (like our dummy forecast) and
        // use it to populate the ListView it's attached to.
        mActAdapter =
                new ArrayAdapter<String>(
                        getActivity(), // The current context (this activity)
                        R.layout.fragment_act_list, // The name of the layout ID.
                        R.id.list_item_activity_textview, // The ID of the textview to populate.
                        weekForecast);

        View rootView = inflater.inflate(R.layout.fragment_act, container, false);


        ListView listView = (ListView) rootView.findViewById(R.id.listview_activity);
        listView.setAdapter(mActAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                String act_id;
                if (all_activity_id.length>position){
                    act_id = all_activity_id[position];
                    Log.i("NTUAF_ACT", "user press item "+position+", and the act_id is "+act_id);
                    Intent intent = new Intent(getActivity(), activityRTC.class)
                            .putExtra("act_id", act_id);

                    startActivity(intent);
                }


            }
        });

        return rootView;
    }
    public class FetchActivityTask extends AsyncTask<String, Void, String[]> {

        private final String LOG_ACT = FetchActivityTask.class.getSimpleName();
        private OkHttpClient client = new OkHttpClient();
        private final String LOG_TAG = ActivityFragment.class.getSimpleName();
        private String param_id;


        /**
         * Take the String representing the complete forecast in JSON Format and
         * pull out the data we need to construct the Strings needed for the wireframes.
         * <p/>
         * Fortunately parsing is easy:  constructor takes the JSON string and converts it
         * into an Object hierarchy for us.
         */

        @Override
        protected String[] doInBackground(String... params) {
            if (params.length==0){
                return null;
            }
            Log.i("NTUAF_ACT", "start async task");
            param_id = params[0];
            String result;
            try {
                Log.i("NTUAF_ACT", "id from fb:" + params[0]);
                result = run(getString(R.string.server_location) + getString(R.string.api_act_get_all) + params[0]);
                Log.i("NTUAF_ACT", "url:"+getString(R.string.server_location) + getString(R.string.api_act_get_all) + params[0]);
                Log.i("NTUAF_ACT", "result:"+result);


            } catch (IOException e) {
                Log.e("NTUAF_ACT", "Error(internet):" + e);



                return null;
            }

            try{
                if (result!=null){
                    String[] strArray = getuserDataFromJson(result);
                    return strArray;
                }else{
                    return null;
                }

            }catch (JSONException e){
                Log.e("NTUAF_ACT", "Error(JSON):" + e);
                return null;
            }
        }

        private String run(String url) throws IOException {
            Request request = new Request.Builder()
                    .url(url)
                    .build();

            Response response = client.newCall(request).execute();
            return response.body().string();
        }

        private String[] getuserDataFromJson(String JsonStr)
                throws JSONException {

            // These are the names of the JSON objects that need to be extracted.
            final String AF_LIST = "list";
            final String AF_ACT = "fb";
            final String AF_ID = "id";
            final String AF_GAME_NAME = "gameName";



            JSONArray act_list = new JSONArray(JsonStr);
            String[] result = new String[act_list.length()];
            List<String> act_id = new ArrayList<String>();

            for (int i=0; i<act_list.length();i++){
                result[i] = act_list.getJSONObject(i).getString(AF_GAME_NAME);
                act_id.add(act_list.getJSONObject(i).getString((AF_ID)));
            }
            all_activity_id = new String[act_id.size()];
            act_id.toArray(all_activity_id);
            return result;

        }

        @Override

        protected void onPostExecute(String[] result) {
            if (result != null) {
                mActAdapter.clear();
                for(String dayForecastStr : result) {
                    mActAdapter.add(dayForecastStr);
                }
            }
        }
    }


}