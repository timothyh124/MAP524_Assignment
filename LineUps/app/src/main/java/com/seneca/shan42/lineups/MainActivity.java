package com.seneca.shan42.lineups;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.ActionBarDrawerToggle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;

import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by timothy on 29/07/15.
 */
public class MainActivity extends ActionBarActivity {
    private SharedPreferences sharedPref;
    private DrawerLayout m_drawer;
    private ActionBarDrawerToggle m_drawerToggle;
    private ListView m_drawerList;
    private String m_title;
    private String[] m_gates;
    private DataFetchManager dfMgr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Context context = this;
        final Activity thisActivity = this;
        // get shared preferences
        sharedPref = context.getSharedPreferences(Constants.PREFERENCE_FILE_KEY, Context.MODE_PRIVATE);

        dfMgr = new DataFetchManager(context);
        m_title=getString(R.string.app_name);
        setContentView(R.layout.activity_main);
        // receive data from previous activity
        Intent intent = getIntent();
        String selectedOffice = intent.getStringExtra("prefOffice");
        // m_gates is sorte list of offices by distance ascending or default list
        m_gates = intent.getStringArrayExtra("officeList");

        // parse data and set the initial display
        dfMgr.parseAndDisplayData(selectedOffice, thisActivity);

        // if shared preference for travel type is not set, the ask
        // if user wants to see Travel Line wait time or Commercial Line wait time
        /*
         * source for dialog from
         * http://developer.android.com/guide/topics/ui/dialogs.html
         * http://developer.android.com/reference/android/app/Dialog.html
         */
        if(!sharedPref.contains("type")) { // check user preference existence
            Log.d("LOADING", "Shared Preference does not exist! Creating new set");
            DialogFragment dlog_type = new DialogFragment() {
                @Override
                public Dialog onCreateDialog(Bundle savedInstanceState) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle(R.string.dialog_ask_type)
                            .setItems(R.array.types_array, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    SharedPreferences.Editor editor = sharedPref.edit();
                                    String[] typeOption = getResources().getStringArray(R.array.types_array);
                                    editor.putString("type", typeOption[which]);
                                    editor.commit();
                                    dfMgr.updateGUI(thisActivity);
                                    dismiss();
                                }
                            });
                    return builder.create();
                }
            };
            dlog_type.setCancelable(false);
            dlog_type.show(getSupportFragmentManager(), "types");
        }

        // setup the left drawer menu
        /*
         * source for drawer from
         * https://developer.android.com/training/implementing-navigation/nav-drawer.html
         */
        m_drawerList = (ListView) findViewById(R.id.left_drawer);
        ArrayAdapter aa = new ArrayAdapter(this,R.layout.activity_listview,m_gates);
        m_drawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                parent.setBackgroundColor(Color.parseColor("#96D0FF"));
                setTitle(m_title);
                m_drawerList.setItemChecked(position, true);
                dfMgr.setCurOffice(m_gates[position]);
                dfMgr.updateGUI(thisActivity);
                m_drawer.closeDrawer(m_drawerList);
                parent.setBackgroundColor(Color.parseColor("#FFD1D1D1"));
            }
        });
        m_drawerList.setAdapter(aa);
        m_drawer=(DrawerLayout) findViewById(R.id.drawer_layout);

        m_drawerToggle=new ActionBarDrawerToggle(this, m_drawer, R.drawable.ic_drawer,
                                                    R.string.open, R.string.close) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed (View view) {
                super.onDrawerClosed(view);
            }

            /** Called when a drawer has settled in a completely open state. */

            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
            }

        };
        m_drawer.setDrawerListener(m_drawerToggle);
        // update data and data shown GUI on button click
        Button updateBtn = (Button) findViewById(R.id.update_btn);
        updateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dfMgr.updateAndDisplayData(thisActivity);
            }
        });

        // turn on update and notification send daemon for selected office
        Switch notiSwitch = (Switch) findViewById(R.id.noti_switch);

        notiSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                String officeForNoti = ((TextView) findViewById(R.id.office_name)).getText().toString();
                if(isChecked) {
                    if(!dfMgr.isCurrentNotification(officeForNoti)) {
                        dfMgr.setCurrentNotification(officeForNoti);
                        Toast.makeText(thisActivity, "Update & Notification in every 10 minutes, Previous request is cancelled", Toast.LENGTH_SHORT).show();
                        dfMgr.beginUpdateCycle(thisActivity, officeForNoti);
                    }
                } else {
                    if(dfMgr.isCurrentNotification(officeForNoti)) {
                        Toast.makeText(thisActivity, "Update & Notification are disabled", Toast.LENGTH_SHORT).show();
                        dfMgr.endUpdateCycle();
                        dfMgr.setCurrentNotification("");
                    }
                }
            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

    }
    @Override
    public void setTitle(CharSequence title) {
        m_title = title.toString();
        getSupportActionBar().setTitle(m_title);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        m_drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        m_drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    /**
     * Action bar option provides either user wants to see
     * Travel Line wait time or Commercial Line wait time
     * and updates UI on user selection
     */
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (m_drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_travel) {
            SharedPreferences.Editor editor = sharedPref.edit();
            String[] typeOption = getResources().getStringArray(R.array.types_array);
            editor.putString("type", typeOption[0]);
            editor.commit();
            dfMgr.updateGUI(this);
        }
        if (id == R.id.action_commercial) {
            SharedPreferences.Editor editor = sharedPref.edit();
            String[] typeOption = getResources().getStringArray(R.array.types_array);
            editor.putString("type", typeOption[1]);
            editor.commit();
            dfMgr.updateGUI(this);
        }

        return super.onOptionsItemSelected(item);
    }
}
