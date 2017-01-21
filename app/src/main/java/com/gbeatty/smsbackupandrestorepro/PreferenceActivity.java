package com.gbeatty.smsbackupandrestorepro;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.view.MenuItem;

import com.afollestad.materialdialogs.prefs.MaterialEditTextPreference;
import com.afollestad.materialdialogs.prefs.MaterialListPreference;

import static com.gbeatty.smsbackupandrestorepro.Utils.PREF_ACCOUNT_NAME;

interface OnCompleteListener {
    void onComplete();
}

@SuppressLint("NewApi")
public class PreferenceActivity extends BaseActivity implements OnCompleteListener {

    SettingsFragment settingsFragment;

    @Override
    public void onComplete() {
        final Preference account = settingsFragment.findPreference("backup_account");
        account.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                loginGoogle(true);
                return false;
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        Preference account = settingsFragment.findPreference("backup_account");
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);

        if (account != null) {
            String accountName = settings.getString(PREF_ACCOUNT_NAME, "No account selected");
            account.setSummary(accountName);
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.preference_activity_custom);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        settingsFragment = new SettingsFragment();
        if (getFragmentManager().findFragmentById(R.id.content_frame) == null) {
            getFragmentManager().beginTransaction().replace(R.id.content_frame, settingsFragment).commit();
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener{

        private OnCompleteListener mListener;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
            mListener.onComplete();
            initSummary(getPreferenceScreen());
        }

        @Override
        public void onAttach(Context context) {
            super.onAttach(context);

            Activity a = null;

            if (context instanceof Activity) {
                a = (Activity) context;
            }

            try {
                this.mListener = (OnCompleteListener) a;
            } catch (final ClassCastException e) {
                throw new ClassCastException(a.toString() + " must implement OnCompleteListener");
            }

        }

        @Override
        public void onResume() {
            super.onResume();
            // Set up a listener whenever a key changes
            getPreferenceScreen().getSharedPreferences()
                    .registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            super.onPause();
            // Unregister the listener whenever a key changes
            getPreferenceScreen().getSharedPreferences()
                    .unregisterOnSharedPreferenceChangeListener(this);
        }

        private void initSummary(Preference p) {
            if (p instanceof PreferenceGroup) {
                PreferenceGroup pGrp = (PreferenceGroup) p;
                for (int i = 0; i < pGrp.getPreferenceCount(); i++) {
                    initSummary(pGrp.getPreference(i));
                }
            } else {
                updatePrefSummary(p);
            }
        }

        private void updatePrefSummary(Preference pref){
            if(pref instanceof MaterialEditTextPreference){
                MaterialEditTextPreference materialEditTextPreference = (MaterialEditTextPreference) pref;
                if(materialEditTextPreference.getKey().equals("backup_interval")){
                    pref.setSummary("Every " + materialEditTextPreference.getText() + " hours");
                }else{
                    pref.setSummary(materialEditTextPreference.getText());
                }
            }
            if(pref instanceof MaterialListPreference){
                MaterialListPreference listPreference = (MaterialListPreference) pref;
                if(listPreference.getEntry() == null){
                    listPreference.setValueIndex(0);
                }
                pref.setSummary(listPreference.getEntry() + " most recent messages");
            }
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
            Preference pref = findPreference(s);
            updatePrefSummary(pref);
        }
    }
}
