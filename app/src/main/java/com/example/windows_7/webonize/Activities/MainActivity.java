package com.example.windows_7.webonize.Activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.example.windows_7.webonize.Adapters.AutoCompleteListAdapter;
import com.example.windows_7.webonize.Fragments.PlaceListFragment;
import com.example.windows_7.webonize.Model.Place;
import com.example.windows_7.webonize.Model.PlaceList;
import com.example.windows_7.webonize.Networking.Connectivity;
import com.example.windows_7.webonize.Networking.CurrentLocation;
import com.example.windows_7.webonize.Networking.VolleyCustomRequest;
import com.example.windows_7.webonize.R;
import com.example.windows_7.webonize.Utils.KeyboardUtils;
import com.example.windows_7.webonize.Utils.UrlUtils;
import com.google.gson.Gson;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements PlaceListFragment.OnFragmentInteractionListener{


    AutoCompleteListAdapter listAdapter;
    AutoCompleteTextView searchText;
    ArrayList<String> suggestions=new ArrayList<>();
    Button searchButton;
    CurrentLocation mCurrentLocation;
    double lat,lng;
    RequestQueue requestQueue;
    PlaceListFragment placeListFragment;
    Gson gson;
    PlaceList placeList;
    public static final String API_KEY = "AIzaSyD7KTtcY7KFlGgkZnwRJgkNtuNSa32Jc-A";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        searchText=(AutoCompleteTextView)findViewById(R.id.searchbar);
        searchButton=(Button)findViewById(R.id.searchButton);
        placeListFragment=(PlaceListFragment)getSupportFragmentManager().findFragmentById(R.id.fragment);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                KeyboardUtils.tryToHideKeyboard(MainActivity.this);
                if(!Connectivity.isNetworkAvailable(getApplicationContext())){
                    Toast.makeText(getApplicationContext(),"Not connected to internet",Toast.LENGTH_SHORT).show();
                }else
                {
                    mCurrentLocation=new CurrentLocation(getApplicationContext());

                    if(searchText.getText().toString().equalsIgnoreCase("")||searchText.getText().toString().equalsIgnoreCase("null")){
                        Toast.makeText(getApplicationContext(),"please enter place name",Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if(mCurrentLocation.canGetLocation()){
                        lat=mCurrentLocation.getLatitude();
                        lng=mCurrentLocation.getLongitude();
                        requestQueue= Volley.newRequestQueue(getApplicationContext());
                        showFetchingDataProgressDialog(true);
                        Map<String,String> object=new HashMap();

                            object.put("location",lat+","+lng);
                            object.put("radius","3000");
                            object.put("sensor","true");
                            object.put("key",MainActivity.API_KEY);
                            object.put("type", UrlUtils.encode(searchText.getText().toString(), "UTF-8"));
                        String customUrl="https://maps.googleapis.com/maps/api/place/search/json";
                        VolleyCustomRequest volleyCustomRequest=new VolleyCustomRequest(Request.Method.GET, customUrl,
                                object, new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject response) {
                                Log.d("Response: ", response.toString());
                                gson=new Gson();
                                placeList=gson.fromJson(response.toString(),PlaceList.class);
                                Log.d("placeList", placeList.getResults().toString());
                                suggestions.add(searchText.getText().toString());
                                listAdapter=new AutoCompleteListAdapter(getApplicationContext(),suggestions);
                                searchText.setAdapter(listAdapter);
                                searchText.setThreshold(1);;
                                listAdapter.updateDataSource(suggestions);
                                notifyPlaceListDataReceived();
                                showFetchingDataProgressDialog(false);
                            }
                        }, new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Log.d("Error: ",error.toString());
                            }
                        });
                        requestQueue.add(volleyCustomRequest);
                    }else
                        showSettingsAlert();
                }
            }
        });
    }


    public void showSettingsAlert(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
        alertDialog.setTitle("GPS settings");
        alertDialog.setMessage("GPS is not enabled. Do you want to go to settings menu?");
        alertDialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        });
        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        alertDialog.show();
    }

    @Override
    public List<Place> getPlaceList() {
        if(placeList!=null)
        return placeList.getResults();
        else
            return null;
    }


    private void notifyPlaceListDataReceived(){
        if (placeListFragment != null) {
            placeListFragment.showPlacesList(lat,lng);
        }
    }

    public void showFetchingDataProgressDialog(boolean flag){

        if(flag) {
            progress = new ProgressDialog(MainActivity.this);
            progress.setMessage("Searching "+searchText.getText().toString());
            progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progress.setIndeterminate(true);
            progress.show();
        }else if(progress!=null && progress.isShowing()){
            progress.dismiss();
        }
    }

    private ProgressDialog progress;
}
