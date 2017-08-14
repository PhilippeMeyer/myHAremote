package today.meyer.myharemote;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.widget.ToggleButton;


import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttClient;

import static today.meyer.myharemote.ActivityConstants.*;


public class myHAremote extends AppCompatActivity {

    SharedPreferences.OnSharedPreferenceChangeListener listener;
    SharedPreferences prefs;
    MqttAndroidClient client = null;
    byte[] macroPayload = new byte[0];
    byte[] replayPayload = new byte[0];


    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.myha_remote);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                Toast toast = Toast.makeText(getApplicationContext(), "Reconnection needed", Toast.LENGTH_SHORT);
                toast.show();
                connect();
            }
        };

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(listener);

    }


    @Override
    public void onResume() {
        super.onResume();

        if (client == null) connect();
        if (!client.isConnected()) connect();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, PreferencesDialog.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void connect(){
        String cnxString;

        String myHAserver = prefs.getString(server, null);
        String myHAport = prefs.getString(port, null);
        if (myHAserver == null || myHAport == null) return;

        cnxString = "tcp://".concat(myHAserver).concat(":").concat(myHAport);
        Toast toast = Toast.makeText(getApplicationContext(), connectionTo.concat(cnxString), Toast.LENGTH_SHORT);
        toast.show();

        if (client == null){
            try {
                macroPayload = macro.getBytes("UTF-8");
                replayPayload = replay.getBytes("UTF-8");
            } catch (Exception e) {
                e.printStackTrace();
            }

            String clientId = MqttClient.generateClientId();
            client = new MqttAndroidClient(this.getApplicationContext(), cnxString, clientId);
        }

        try {
            if (client.isConnected()) client.disconnect();
            IMqttToken token = client.connect();
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // We are connected
                    Toast toast = Toast.makeText(getApplicationContext(), connectionSuccess, Toast.LENGTH_SHORT);
                    toast.show();
                    Log.d(TAG, onSuccess);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    // Something went wrong e.g. connection timeout or firewall problems
                    Toast toast = Toast.makeText(getApplicationContext(), connectionNoSuccess, Toast.LENGTH_SHORT);
                    toast.show();
                    Log.d(TAG, onFailure);

                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }


    public void sendMessage(View v) {
        String topic = (String)v.getTag();
        byte[] payload;

        Log.d(TAG, topic);

        if (v instanceof ToggleButton) {
            if (!topic.contains(";")) {
                Log.d(TAG, noConformTag);
                return;
            }

            if(((ToggleButton)v).isChecked())
                topic = topic.substring(0, topic.indexOf(';'));
            else
                topic = topic.substring(topic.indexOf(';')+1);
        }

        if (topic.contains(macro)) payload = macroPayload;
        else payload = replayPayload;


        if (!client.isConnected()) {
            Toast toast = Toast.makeText(getApplicationContext(), notConnected, Toast.LENGTH_SHORT);
            toast.show();
            return;
        }

        try {
            MqttMessage message = new MqttMessage(payload);
            client.publish(topic, message);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }


    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        public PlaceholderFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            int position = getArguments().getInt(ARG_SECTION_NUMBER);
            View rootView = null;

            if (position == 1)  rootView = inflater.inflate(R.layout.fragment_tv, container, false);
            else if (position == 2)  rootView = inflater.inflate(R.layout.fragment_dvd, container, false);

            return rootView;
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return PlaceholderFragment.newInstance(position + 1);
        }

        @Override
        public int getCount() {
            // Show 2 total pages.
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "TV";
                case 1:
                    return "DVD";
            }
            return null;
        }
    }
}
