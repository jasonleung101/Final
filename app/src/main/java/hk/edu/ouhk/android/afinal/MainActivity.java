package hk.edu.ouhk.android.afinal;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    private String TAG = MainActivity.class.getSimpleName();
    private DrawerLayout drawerLayout;
    private NavigationView navigation_view;
    private ProgressDialog pDialog;
    private ListView lv;
    private URL gmap;
    private URL url2;
    private String row;
    private Double lat;
    private Double lng;
    private String dlat;
    private String dlng;
    private int limit;
    private double dist;

    ArrayList<HashMap<String, String>> contactList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getPermissionForLocation();
        // check if GPS enabled

        GPSTracker  gpsTracker=new GPSTracker(this);
        double lat2 = gpsTracker.getLatitude();
        double lng2 = gpsTracker.getLongitude();
        lat = lat2;
        lng = lng2;

        contactList = new ArrayList<>();

        Intent intent = getIntent();
        String message = intent.getStringExtra(SettingPage.EXTRA_MESSAGE);
        row = message;

        //toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);

        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                return true;
            }
        });
        drawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);
        navigation_view = (NavigationView) findViewById(R.id.navigation_view);
        setSupportActionBar(toolbar);
        toolbar.inflateMenu(R.menu.menu_main);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigation_view.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                drawerLayout.closeDrawer(GravityCompat.START);
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    return true;
                } else if (id == R.id.nav_setting) {
                    ChangeToSetting();
                    return true;
                }
                return false;
            }
        });

        lv = findViewById(R.id.list);

        if (gpsTracker.canGetLocation())
        {

        }
        else
        {
            // can't get location
            // GPS or Network is not enabled
            // Ask user to enable GPS/network in settings
            gpsTracker.showSettingsAlert();
        }

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> ListAdapter, View view, int position, long id) {
                Object item = ListAdapter.getItemAtPosition(position);
                dlat = contactList.get(position).get("lat");
                dlng = contactList.get(position).get("lng");
                BuildGMap();
                Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
                        Uri.parse(gmap.toString()));
                startActivity(intent);
            }
        });
    }

    // Menu icons are inflated just as they were with actionbar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        MenuItem item = menu.findItem(R.id.spinner);
        Spinner spinner = (Spinner) MenuItemCompat.getActionView(item);
        spinner.setOnItemSelectedListener(this);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.spinner_list_item_array, R.layout.spinner_item);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinner.setAdapter(adapter);
        return true;
    }

        public void onRefresh(MenuItem mi) {
            // handle click here
            BuildURL();
            new GetContacts().execute();
            Toast.makeText(getApplicationContext(), getString(R.string.Refreshed), Toast.LENGTH_SHORT).show();
        }

    public void ChangeToSetting(){
        Intent setting = new Intent(this, SettingPage.class);
        finish();
        startActivity(setting);
        overridePendingTransition(0, 0);
    }

    //Permission Control
    private static final int FINE_LOCATION_PERMISSIONS_REQUEST = 1;

    public void getPermissionForLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            if (shouldShowRequestPermissionRationale(
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
            }
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    FINE_LOCATION_PERMISSIONS_REQUEST);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        if (requestCode == FINE_LOCATION_PERMISSIONS_REQUEST) {
            if (grantResults.length == 1 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, getString(R.string.Location_permission_granted), Toast.LENGTH_SHORT).show();
                BuildURL();
                new GetContacts().execute();
            } else {
                boolean showRationale = shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION);
                if (showRationale) {
                } else {
                    Toast.makeText(this, getString(R.string.Location_permission_denied), Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    public void BuildGMap() {
        final GPSTracker gpsTracker = new GPSTracker(this);
        lat = gpsTracker.getLatitude();
        lng = gpsTracker.getLongitude();
        String stringLatitude = lat.toString();
        String stringLongitude = lng.toString();
        String mOriginLoc;
        String mDestLoc;

        StringBuilder OriginLoc = new StringBuilder();
        OriginLoc.append(stringLatitude);
        OriginLoc.append(",");
        OriginLoc.append(stringLongitude);
        mOriginLoc = OriginLoc.toString();

        StringBuilder DestLoc = new StringBuilder();
        DestLoc.append(dlat);
        DestLoc.append(",");
        DestLoc.append(dlng);
        mDestLoc = DestLoc.toString();

        final String BASE_URL =
                "http://maps.google.com/maps?";
        final String start = "saddr";
        final String end = "daddr";

        Uri builtUri = Uri.parse(BASE_URL)
                .buildUpon()
                .appendQueryParameter(start, mOriginLoc)
                .appendQueryParameter(end, mDestLoc)
                .build();
        URL gmap2 = null;
        try {
            gmap2 = new URL(builtUri.toString());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        gmap = gmap2;
    }


    public void BuildURL() {
        final GPSTracker gpsTracker = new GPSTracker(this);
        lat = gpsTracker.getLatitude();
        lng = gpsTracker.getLongitude();
        String stringLatitude = lat.toString();
        String stringLongitude = lng.toString();
        String lang2 = Locale.getDefault().getLanguage();
        final String BASE_URL =
                "http://plbpc013.ouhk.edu.hk/toilet/json-toilet.php?";
        final String QUERY_PARAM = "lat";
        final String FORMAT_PARAM = "lng";
        final String lang = "lang";
        final String row2 = "display_row";
        final String index = "row_index";

        Uri builtUri = Uri.parse(BASE_URL)
                .buildUpon()
                .appendQueryParameter(QUERY_PARAM, stringLatitude)
                .appendQueryParameter(FORMAT_PARAM, stringLongitude)
                .appendQueryParameter(lang, lang2)
                .appendQueryParameter(row2, row)
                .build();
        URL url = null;
        try {
            url = new URL(builtUri.toString());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        url2 = url;
        contactList.clear();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        switch (position) {
            case 0:
                limit = -1;
                BuildURL();
                new GetContacts().execute();
                break;
            case 1:
                limit = 500;
                BuildURL();
                new GetContacts().execute();
                break;
            case 2:
                limit = 1000;
                BuildURL();
                new GetContacts().execute();
                break;
            case 3:
                limit = 2000;
                BuildURL();
                new GetContacts().execute();
                break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }



    /**
     * Async task class to get json by making HTTP call
     */
    private class GetContacts extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Showing progress dialog
            pDialog = new ProgressDialog(MainActivity.this);
            pDialog.setMessage(getString(R.string.Please_wait));
            pDialog.setCancelable(false);
            pDialog.show();

        }

        @Override
        protected Void doInBackground(Void... arg0) {
            HttpHandler sh = new HttpHandler();

            // Making a request to url and getting response
            String jsonStr = sh.makeServiceCall(url2.toString());

            Log.e(TAG, "Response from url: " + jsonStr);

            if (jsonStr != null) {
                try {
                    JSONObject jsonObj = new JSONObject(jsonStr);

                    // Getting JSON Array node
                    JSONArray contacts = jsonObj.getJSONArray("results");

                    // looping through All Contacts
                    for (int i = 0; i < contacts.length(); i++) {
                        JSONObject c = contacts.getJSONObject(i);

                        String id = c.getString("id");
                        String lat = c.getString("lat");
                        String lng = c.getString("lng");
                        String name = c.getString("name");
                        String address = c.getString("address");
                        Double distance = c.getDouble("distance");


                        // tmp hash map for single contact
                        HashMap<String, String> contact = new HashMap<>();

                        // adding each child node to HashMap key => value
                        if(distance<limit || limit == -1) {
                            contact.put("id", id);
                            contact.put("lat", lat);
                            contact.put("lng", lng);
                            contact.put("name", name);
                            contact.put("address", address);
                            contact.put("distance", distance.toString());
                            dist = distance;

                            // adding contact to contact list
                            contactList.add(contact);
                        }
                    }
                } catch (final JSONException e) {
                    Log.e(TAG, "Json parsing error: " + e.getMessage());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(),
                                    "Json parsing error: " + e.getMessage(),
                                    Toast.LENGTH_LONG)
                                    .show();
                        }
                    });

                }
            } else {
                Log.e(TAG, "Couldn't get json from server.");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(),
                                "ERROR"+url2,
                                Toast.LENGTH_LONG)
                                .show();
                    }
                });

            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
                // Dismiss the progress dialog
                if (pDialog.isShowing())
                    pDialog.dismiss();
            if(dist<limit || limit == -1) {
                ListAdapter adapter = new SimpleAdapter(
                        MainActivity.this, contactList,
                        R.layout.list_item, new String[]{"name", "address",
                        "distance"}, new int[]{R.id.name,
                        R.id.address, R.id.distance});

                lv.setAdapter(adapter);
            }
        }
    }
}
