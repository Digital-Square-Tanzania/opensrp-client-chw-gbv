package org.smartregister.chw.gbv.actionhelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.smartregister.client.utils.constants.JsonFormConstants.JSON_FORM_KEY.GLOBAL;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.smartregister.chw.gbv.domain.MemberObject;
import org.smartregister.chw.gbv.model.BaseGbvVisitAction;

import java.util.Calendar;

import timber.log.Timber;

public class GbvHfConsentActionHelperTest {
    private GbvHfConsentActionHelper helper;

    private MemberObject memberObject = new MemberObject();

    private String jsonString = "{\"count\":\"1\",\"encounter_type\":\"GBV_VAC_CONSENT_FOLLOWUP\",\"metadata\":{\"start\":{\"openmrs_entity_parent\":\"\",\"openmrs_entity\":\"concept\",\"openmrs_data_type\":\"start\",\"openmrs_entity_id\":\"163137AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\"},\"end\":{\"openmrs_entity_parent\":\"\",\"openmrs_entity\":\"concept\",\"openmrs_data_type\":\"end\",\"openmrs_entity_id\":\"163138AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\"},\"today\":{\"openmrs_entity_parent\":\"\",\"openmrs_entity\":\"encounter\",\"openmrs_entity_id\":\"encounter_date\"},\"deviceid\":{\"openmrs_entity_parent\":\"\",\"openmrs_entity\":\"concept\",\"openmrs_data_type\":\"deviceid\",\"openmrs_entity_id\":\"163149AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\"},\"subscriberid\":{\"openmrs_entity_parent\":\"\",\"openmrs_entity\":\"concept\",\"openmrs_data_type\":\"subscriberid\",\"openmrs_entity_id\":\"163150AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\"},\"simserial\":{\"openmrs_entity_parent\":\"\",\"openmrs_entity\":\"concept\",\"openmrs_data_type\":\"simserial\",\"openmrs_entity_id\":\"163151AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\"},\"phonenumber\":{\"openmrs_entity_parent\":\"\",\"openmrs_entity\":\"concept\",\"openmrs_data_type\":\"phonenumber\",\"openmrs_entity_id\":\"163152AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\"},\"encounter_location\":\"\",\"look_up\":{\"entity_id\":\"\",\"value\":\"\"}},\"global\":{},\"step1\":{\"title\":\"Consent Followup\",\"fields\":[{\"key\":\"client_consent_after_counseling\",\"openmrs_entity_parent\":\"\",\"openmrs_entity\":\"concept\",\"openmrs_entity_id\":\"client_consent_after_counseling\",\"type\":\"native_radio\",\"label\":\"Did the survivor consent after counseling?\",\"options\":[{\"key\":\"yes\",\"openmrs_entity_parent\":\"\",\"openmrs_entity\":\"concept\",\"openmrs_entity_id\":\"yes\",\"text\":\"Yes\"},{\"key\":\"no\",\"openmrs_entity_parent\":\"\",\"openmrs_entity\":\"concept\",\"openmrs_entity_id\":\"no\",\"text\":\"No\"}],\"v_required\":{\"value\":\"true\",\"err\":\"Please select whether the survivor has provided consent to the service\"},\"relevance\":{\"rules-engine\":{\"ex-rules\":{\"rules-file\":\"gbv_consent_relevance.yml\"}}}},{\"key\":\"was_social_welfare_officer_involved\",\"openmrs_entity_parent\":\"\",\"openmrs_entity\":\"concept\",\"openmrs_entity_id\":\"was_social_welfare_officer_involved\",\"type\":\"native_radio\",\"label\":\"Was the social welfare officer involved?\",\"options\":[{\"key\":\"yes\",\"openmrs_entity_parent\":\"\",\"openmrs_entity\":\"concept\",\"openmrs_entity_id\":\"yes\",\"text\":\"Yes\"},{\"key\":\"no\",\"openmrs_entity_parent\":\"\",\"openmrs_entity\":\"concept\",\"openmrs_entity_id\":\"no\",\"text\":\"No\"}],\"v_required\":{\"value\":\"true\",\"err\":\"Please select whether the social welfare officer was involved\"},\"relevance\":{\"rules-engine\":{\"ex-rules\":{\"rules-file\":\"gbv_consent_relevance.yml\"}}}}]}}";

    @Before
    public void setUp() {
        helper = Mockito.mock(GbvHfConsentActionHelper.class, Mockito.CALLS_REAL_METHODS);
        memberObject.setDob(Calendar.getInstance().getTime().toString());
        helper.setMemberObject(memberObject);
    }

    @Test
    public void testOnJsonFormLoadedInitializesContext() {
        Context context = Mockito.mock(Context.class);
        helper.onJsonFormLoaded("{}", context, null);

        assertEquals(context, helper.getContext());
    }

    @Test
    public void testGetPreProcessedIsNull() throws JSONException {
        Context context = Mockito.mock(Context.class);
        helper.onJsonFormLoaded(jsonString, context, null);
        JSONObject global = null;
        try {
            global = new JSONObject(helper.getPreProcessed()).getJSONObject(GLOBAL);
        } catch (JSONException e) {
            Timber.e(e);
        }
        assertNotNull(global);
        assertEquals(0, global.getInt("age"));
    }

    @Test
    public void testGetPreProcessedStatus() {
        assertEquals(BaseGbvVisitAction.ScheduleStatus.DUE, helper.getPreProcessedStatus());
    }

    @Test
    public void testGetPreProcessedSubTitle() {
        assertNull(helper.getPreProcessedSubTitle());
    }

    @Test
    public void testPostProcessIsInert() {
        assertNull(helper.postProcess("Random string"));
    }

    @Test
    public void testOnPayloadReceived() {

        BaseGbvVisitAction action = Mockito.mock(BaseGbvVisitAction.class);
        int hashCode = action.hashCode();

        helper.onPayloadReceived(action);

        assertEquals(hashCode, action.hashCode());
    }


    @Test
    public void testOnPayloadReceivedWithValidJson() throws JSONException {
        String jsonPayload = "{\"count\":\"1\",\"encounter_type\":\"GBV_VAC_CONSENT\",\"global\":{},\"step1\":{\"title\":\"Consent Form\",\"fields\":[{\"key\":\"client_consent\",\"openmrs_entity_parent\":\"\",\"openmrs_entity\":\"concept\",\"openmrs_entity_id\":\"client_consent_after_counseling\",\"type\":\"native_radio\",\"label\":\"Did the survivor consent after counseling?\",\"options\":[{\"key\":\"yes\",\"openmrs_entity_parent\":\"\",\"openmrs_entity\":\"concept\",\"openmrs_entity_id\":\"yes\",\"text\":\"Yes\"},{\"key\":\"no\",\"openmrs_entity_parent\":\"\",\"openmrs_entity\":\"concept\",\"openmrs_entity_id\":\"no\",\"text\":\"No\"}],\"v_required\":{\"value\":\"true\",\"err\":\"Please select whether the survivor has provided consent to the service\"},\"relevance\":{\"rules-engine\":{\"ex-rules\":{\"rules-file\":\"gbv_consent_relevance.yml\"}}},\"value\":\"yes\"}]}}";
        helper.onPayloadReceived(jsonPayload);

        assertEquals("yes", helper.getClientConsent());
    }

    @Test
    public void testOnPayloadReceivedWithInvalidJson() {
        String jsonPayload = "Invalid JSON";
        helper.onPayloadReceived(jsonPayload);

        assertNull(helper.getClientConsent());
    }

    @Test
    public void testEvaluateSubTitleWithConsentProvided() {
        helper.setClientConsent("Yes");
        assertEquals("Was Consent Provided: Yes", helper.evaluateSubTitle());
    }

    @Test
    public void testEvaluateSubTitleWithNoConsent() {
        assertNull(helper.evaluateSubTitle());
    }

    @Test
    public void testEvaluateStatusOnPayloadWithConsentProvided() {
        helper.setClientConsent("Yes");
        assertEquals(BaseGbvVisitAction.Status.COMPLETED, helper.evaluateStatusOnPayload());
    }

    @Test
    public void testEvaluateStatusOnPayloadWithNoConsent() {
        assertNull(helper.getClientConsent());
        assertEquals(BaseGbvVisitAction.Status.PENDING, helper.evaluateStatusOnPayload());
    }
}