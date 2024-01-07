package org.smartregister.chw.gbv_sample.activity;

import static org.smartregister.chw.gbv_sample.utils.JsonFormUtils.REQUEST_CODE_GET_JSON;

import android.app.Activity;
import android.content.Intent;

import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.domain.Form;

import org.json.JSONObject;
import org.smartregister.chw.gbv.activity.BaseGbvProfileActivity;
import org.smartregister.chw.gbv.domain.MemberObject;
import org.smartregister.chw.gbv.util.Constants;
import org.smartregister.chw.gbv.util.GbvJsonFormUtils;
import org.smartregister.chw.gbv_sample.R;

import timber.log.Timber;


public class GbvCommunityMemberProfileActivity extends BaseGbvProfileActivity {

    public static void startMe(Activity activity, String baseEntityID) {
        Intent intent = new Intent(activity, GbvCommunityMemberProfileActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityID);
        activity.startActivityForResult(intent, Constants.REQUEST_CODE_GET_JSON);
    }

    @Override
    protected void onCreation() {
        super.onCreation();
        textViewRecordGbv.setText(R.string.record_gbv_home_visit);
    }

    @Override
    public void recordGbv(MemberObject memberObject) {
        JSONObject jsonObject;
        try {
            jsonObject = GbvJsonFormUtils.getFormAsJson(Constants.FORMS.GBV_HOME_VISIT);
            startFormActivity(jsonObject);
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    @Override
    protected MemberObject getMemberObject(String baseEntityId) {
        return EntryActivity.getSampleMember();
    }


    public void startFormActivity(JSONObject jsonForm) {
        Form form = new Form();
        form.setWizard(false);

        Intent intent = new Intent(this, SampleJsonFormActivity.class);
        intent.putExtra(Constants.JSON_FORM_EXTRA.JSON, jsonForm.toString());
        intent.putExtra(JsonFormConstants.JSON_FORM_KEY.FORM, form);
        startActivityForResult(intent, REQUEST_CODE_GET_JSON);
    }
}