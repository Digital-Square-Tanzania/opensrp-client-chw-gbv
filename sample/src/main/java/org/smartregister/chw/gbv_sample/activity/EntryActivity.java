package org.smartregister.chw.gbv_sample.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;


import androidx.appcompat.widget.Toolbar;

import org.smartregister.chw.gbv_sample.R;
import org.smartregister.chw.gbv.contract.BaseGbvVisitContract;
import org.smartregister.chw.gbv.domain.MemberObject;
import org.smartregister.view.activity.SecuredActivity;

import timber.log.Timber;

public class EntryActivity extends SecuredActivity implements View.OnClickListener, BaseGbvVisitContract.VisitView {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        findViewById(R.id.gbv_activity).setOnClickListener(this);
        findViewById(R.id.gbv_community_visit).setOnClickListener(this);
        findViewById(R.id.gbv_hf_profile).setOnClickListener(this);
    }

    @Override
    protected void onCreation() {
        Timber.v("onCreation");
    }

    @Override
    protected void onResumption() {
        Timber.v("onCreation");
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.gbv_activity:
                startActivity(new Intent(this, GbvRegisterActivity.class));
                break;
            case R.id.gbv_community_visit:
                GbvCommunityMemberProfileActivity.startMe(this, "12345");
                break;
            case R.id.gbv_hf_profile:
                GbvMemberProfileActivity.startMe(this, "12345");
                break;
            default:
                break;
        }
    }

    public static MemberObject getSampleMember() {
        MemberObject memberObject = new MemberObject();
        memberObject.setFirstName("Glory");
        memberObject.setLastName("Juma");
        memberObject.setMiddleName("Ali");
        memberObject.setGender("Female");
        memberObject.setDob("1982-01-18T03:00:00.000+03:00");
        memberObject.setUniqueId("3503504");
        memberObject.setBaseEntityId("3503504");
        memberObject.setFamilyBaseEntityId("3503504");
        memberObject.setAddress("Njiro");

        return memberObject;
    }


    @Override
    public void onDialogOptionUpdated(String jsonString) {
        Timber.v("onDialogOptionUpdated %s", jsonString);
    }

    @Override
    public Context getMyContext() {
        return this;
    }
}