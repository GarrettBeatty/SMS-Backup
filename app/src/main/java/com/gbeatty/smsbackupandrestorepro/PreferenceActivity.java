package com.gbeatty.smsbackupandrestorepro;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.MenuItem;

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
                getGoogleAccount(true);
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
    public void signInResult() {

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class SettingsFragment extends PreferenceFragment {

        private OnCompleteListener mListener;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
            mListener.onComplete();
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
    }
}
