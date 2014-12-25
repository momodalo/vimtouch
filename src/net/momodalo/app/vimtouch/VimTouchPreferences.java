/*
 * Copyright (C) 2007 The Android Open Source Project
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

package net.momodalo.app.vimtouch;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;

import net.momodalo.app.vimtouch.R;

public class VimTouchPreferences extends PreferenceActivity implements OnSharedPreferenceChangeListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        for(int i=0;i<getPreferenceScreen().getPreferenceCount();i++){
            initSummary(getPreferenceScreen().getPreference(i));
        }
    }

    @Override 
    protected void onResume(){
        super.onResume();
        // Set up a listener whenever a key changes             
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override 
    protected void onPause() { 
        super.onPause();
        // Unregister the listener whenever a key changes             
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);     
    } 

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) { 
        updatePrefSummary(findPreference(key));
    }


    private void initSummary(Preference p){
        if (p instanceof PreferenceCategory){
            PreferenceCategory pCat = (PreferenceCategory)p;
            for(int i=0;i<pCat.getPreferenceCount();i++){
                initSummary(pCat.getPreference(i));
            }
        }else{
            updatePrefSummary(p);
        }
    }

    private void updatePrefSummary(Preference p){
        if (p instanceof EditTextPreference) {
            EditTextPreference editTextPref = (EditTextPreference) p; 
            p.setSummary(editTextPref.getText()); 
        }
    }
}
