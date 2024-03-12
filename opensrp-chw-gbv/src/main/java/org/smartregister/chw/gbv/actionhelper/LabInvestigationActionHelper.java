package org.smartregister.chw.gbv.actionhelper;

import static org.smartregister.client.utils.constants.JsonFormConstants.GLOBAL;

import android.content.Context;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.gbv.domain.MemberObject;
import org.smartregister.chw.gbv.domain.VisitDetail;
import org.smartregister.chw.gbv.model.BaseGbvVisitAction;
import org.smartregister.chw.gbv.util.JsonFormUtils;

import java.util.List;
import java.util.Map;

import timber.log.Timber;

public abstract class LabInvestigationActionHelper extends GbvVisitActionHelper {
    private MemberObject memberObject;

    private JSONObject jsonForm;


    private String currentPregnancyStatus;

    private String typeOfAssault;

    private String hivStatus;

    private String uptTestResults;

    private String hivTestResults;

    private String stiTestResults;

    private String hepbTestResults;


    public LabInvestigationActionHelper(MemberObject memberObject, String currentPregnancyStatus, String typeOfAssault, String hivStatus) {
        this.memberObject = memberObject;
        this.currentPregnancyStatus = currentPregnancyStatus;
        this.typeOfAssault = typeOfAssault;
        this.hivStatus = hivStatus;
    }

    @Override
    public void onJsonFormLoaded(String jsonString, Context context, Map<String, List<VisitDetail>> details) {
        super.onJsonFormLoaded(jsonString, context, details);
        try {
            jsonForm = new JSONObject(jsonString);
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    /**
     * Update the preprocessed form to pass client age as global parameter
     *
     * @return the updated form
     */
    @Override
    public String getPreProcessed() {
        try {
            JSONObject global = jsonForm.getJSONObject(GLOBAL);
            global.put("age", memberObject.getAge());
            global.put("gender", memberObject.getGender());
            global.put("currentPregnancyStatus", currentPregnancyStatus);
            global.put("typeOfAssault", typeOfAssault);
            global.put("hivStatus", hivStatus);
            return jsonForm.toString();
        } catch (JSONException e) {
            Timber.e(e);
        }
        return null;
    }

    @Override
    public void onPayloadReceived(String jsonPayload) {
        JSONObject payload;
        try {
            payload = new JSONObject(jsonPayload);

            uptTestResults = JsonFormUtils.getValue(payload, "upt_test_results");
            hivTestResults = JsonFormUtils.getValue(payload, "hiv_test_results");
            stiTestResults = JsonFormUtils.getValue(payload, "sti_test_results");
            hepbTestResults = JsonFormUtils.getValue(payload, "hepb_test_results");

            processTestResults(JsonFormUtils.getValue(payload, "hepb_test_results"), JsonFormUtils.getValue(payload, "hiv_test_results"));
        } catch (JSONException e) {
            Timber.d(e);
        }
    }

    public abstract void processTestResults(String hepbTestResults, String hivTestResults);

    @Override
    public String evaluateSubTitle() {
        return null;
    }

    @Override
    public BaseGbvVisitAction.Status evaluateStatusOnPayload() {
        if (StringUtils.isNotBlank(uptTestResults) || StringUtils.isNotBlank(hivTestResults) || StringUtils.isNotBlank(stiTestResults) || StringUtils.isNotBlank(hepbTestResults)) {
            return BaseGbvVisitAction.Status.COMPLETED;
        } else
            return BaseGbvVisitAction.Status.PENDING;
    }

    public void setMemberObject(MemberObject memberObject) {
        this.memberObject = memberObject;
    }
}
