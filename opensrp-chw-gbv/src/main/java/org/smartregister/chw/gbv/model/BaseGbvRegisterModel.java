package org.smartregister.chw.gbv.model;

import org.json.JSONObject;
import org.smartregister.chw.gbv.contract.GbvRegisterContract;
import org.smartregister.chw.gbv.util.GbvJsonFormUtils;

public class BaseGbvRegisterModel implements GbvRegisterContract.Model {

    @Override
    public JSONObject getFormAsJson(String formName, String entityId, String currentLocationId) throws Exception {
        JSONObject jsonObject = GbvJsonFormUtils.getFormAsJson(formName);
        GbvJsonFormUtils.getRegistrationForm(jsonObject, entityId, currentLocationId);

        return jsonObject;
    }

}
