package org.smartregister.chw.sbc_sample.activity;

import android.app.Activity;
import android.content.Intent;

import com.vijay.jsonwizard.constants.JsonFormConstants;

import org.json.JSONObject;
import org.smartregister.chw.gbv.activity.BaseGbvVisitActivity;
import org.smartregister.chw.gbv.domain.MemberObject;
import org.smartregister.chw.gbv.presenter.BaseGbvVisitPresenter;
import org.smartregister.chw.gbv.util.Constants;
import org.smartregister.chw.sbc_sample.interactor.GbvVisitInteractor;

public class GbvVisitActivity extends BaseGbvVisitActivity {
    public static void startMe(Activity activity, String baseEntityID, Boolean isEditMode) {
        Intent intent = new Intent(activity, GbvVisitActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityID);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.EDIT_MODE, isEditMode);
        activity.startActivityForResult(intent, Constants.REQUEST_CODE_GET_JSON);
    }

    @Override
    protected MemberObject getMemberObject(String baseEntityId) {
        return EntryActivity.getSampleMember();
    }

    protected void registerPresenter() {
        presenter = new BaseGbvVisitPresenter(memberObject, this, new GbvVisitInteractor());
    }

    @Override
    public void startFormActivity(JSONObject jsonForm) {
        Intent intent = new Intent(this, SampleJsonFormActivity.class);
        intent.putExtra(Constants.JSON_FORM_EXTRA.JSON, jsonForm.toString());

        if (getFormConfig() != null) {
            intent.putExtra(JsonFormConstants.JSON_FORM_KEY.FORM, getFormConfig());
        }

        startActivityForResult(intent, Constants.REQUEST_CODE_GET_JSON);
    }
}
