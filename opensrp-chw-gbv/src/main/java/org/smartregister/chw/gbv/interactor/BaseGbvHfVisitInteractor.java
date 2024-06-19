package org.smartregister.chw.gbv.interactor;


import static org.smartregister.client.utils.constants.JsonFormConstants.GLOBAL;

import android.content.Context;

import androidx.annotation.VisibleForTesting;

import com.google.gson.Gson;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;
import org.smartregister.chw.gbv.GbvLibrary;
import org.smartregister.chw.gbv.R;
import org.smartregister.chw.gbv.actionhelper.EducationAndCounsellingActionHelper;
import org.smartregister.chw.gbv.actionhelper.ForensicExaminationActionHelper;
import org.smartregister.chw.gbv.actionhelper.GbvHfConsentActionHelper;
import org.smartregister.chw.gbv.actionhelper.GbvHfConsentFollowupActionHelper;
import org.smartregister.chw.gbv.actionhelper.GbvHfVisitTypeActionHelper;
import org.smartregister.chw.gbv.actionhelper.GbvVisitActionHelper;
import org.smartregister.chw.gbv.actionhelper.HistoryCollectionActionHelper;
import org.smartregister.chw.gbv.actionhelper.LabInvestigationActionHelper;
import org.smartregister.chw.gbv.actionhelper.LinkageActionHelper;
import org.smartregister.chw.gbv.actionhelper.MedicalExaminationActionHelper;
import org.smartregister.chw.gbv.actionhelper.NextAppointmentDateActionHelper;
import org.smartregister.chw.gbv.actionhelper.PhysicalExaminationActionHelper;
import org.smartregister.chw.gbv.actionhelper.ProvideTreatmentActionHelper;
import org.smartregister.chw.gbv.actionhelper.SafetyPlanActionHelper;
import org.smartregister.chw.gbv.contract.BaseGbvVisitContract;
import org.smartregister.chw.gbv.dao.GbvDao;
import org.smartregister.chw.gbv.domain.MemberObject;
import org.smartregister.chw.gbv.domain.Visit;
import org.smartregister.chw.gbv.domain.VisitDetail;
import org.smartregister.chw.gbv.model.BaseGbvVisitAction;
import org.smartregister.chw.gbv.repository.VisitRepository;
import org.smartregister.chw.gbv.util.AppExecutors;
import org.smartregister.chw.gbv.util.Constants;
import org.smartregister.chw.gbv.util.JsonFormUtils;
import org.smartregister.chw.gbv.util.NCUtils;
import org.smartregister.chw.gbv.util.VisitUtils;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.clientandeventmodel.Obs;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.sync.helper.ECSyncHelper;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import timber.log.Timber;


public class BaseGbvHfVisitInteractor implements BaseGbvVisitContract.Interactor {

    private final GbvLibrary gbvLibrary;

    private final LinkedHashMap<String, BaseGbvVisitAction> actionList;

    protected AppExecutors appExecutors;

    private ECSyncHelper syncHelper;

    private Context mContext;

    private Map<String, List<VisitDetail>> details = null;

    private BaseGbvVisitContract.InteractorCallBack callBack;

    private MemberObject memberObject;

    private String mCurrentPregnancyStatus;

    private String mTypeOfAssault;

    private String mHhivStatus;


    @VisibleForTesting
    public BaseGbvHfVisitInteractor(AppExecutors appExecutors, GbvLibrary GbvLibrary, ECSyncHelper syncHelper) {
        this.appExecutors = appExecutors;
        this.gbvLibrary = GbvLibrary;
        this.syncHelper = syncHelper;
        this.actionList = new LinkedHashMap<>();
    }

    public BaseGbvHfVisitInteractor() {
        this(new AppExecutors(), GbvLibrary.getInstance(), GbvLibrary.getInstance().getEcSyncHelper());
    }

    @Override
    public void reloadMemberDetails(String memberID, BaseGbvVisitContract.InteractorCallBack callBack) {
        this.memberObject = getMemberClient(memberID);
        if (memberObject != null) {
            final Runnable runnable = () -> {
                appExecutors.mainThread().execute(() -> callBack.onMemberDetailsReloaded(memberObject));
            };
            appExecutors.diskIO().execute(runnable);
        }
    }

    /**
     * Override this method and return actual member object for the provided user
     *
     * @param memberID unique identifier for the user
     * @return MemberObject wrapper for the user's data
     */
    @Override
    public MemberObject getMemberClient(String memberID) {
        return GbvDao.getMember(memberID);
    }

    @Override
    public void saveRegistration(String jsonString, boolean isEditMode, BaseGbvVisitContract.InteractorCallBack callBack) {
        Timber.v("saveRegistration");
    }

    @Override
    public void calculateActions(final BaseGbvVisitContract.View view, MemberObject memberObject, final BaseGbvVisitContract.InteractorCallBack callBack) {
        mContext = view.getContext();
        this.callBack = callBack;
        if (view.getEditMode()) {
            Visit lastVisit = gbvLibrary.visitRepository().getLatestVisit(memberObject.getBaseEntityId(), Constants.EVENT_TYPE.GBV_FOLLOW_UP_VISIT);
            if (lastVisit != null) {
                details = VisitUtils.getVisitGroups(gbvLibrary.visitDetailsRepository().getVisits(lastVisit.getVisitId()));
            }
        }

        final Runnable runnable = () -> {
            try {
                createGbvHfVisitTypeAction(memberObject, details);
            } catch (BaseGbvVisitAction.ValidationException e) {
                Timber.e(e);
            }

            appExecutors.mainThread().execute(() -> callBack.preloadActions(actionList));
        };

        appExecutors.diskIO().execute(runnable);
    }

    protected void createGbvHfVisitTypeAction(MemberObject memberObject, Map<String, List<VisitDetail>> details) throws BaseGbvVisitAction.ValidationException {
        GbvVisitActionHelper actionHelper =
                new MyGbvHfVisitTypeActionHelper();

        String actionName =
                mContext.getString(R.string.gbv_visit_type_action_title);

        BaseGbvVisitAction action = getBuilder(actionName)
                .withOptional(false)
                .withDetails(details)
                .withHelper(actionHelper)
                .withFormName(Constants.FORMS.GBV_VISIT_TYPE)
                .build();

        actionList.put(actionName, action);
    }

    protected void createGbvHfConsentAction(MemberObject memberObject, Map<String, List<VisitDetail>> details) throws BaseGbvVisitAction.ValidationException {
        GbvVisitActionHelper actionHelper = new MyGbvHfConsentActionHelper(memberObject);

        String actionName = mContext.getString(R.string.gbv_consent_action_title);

        BaseGbvVisitAction action = getBuilder(actionName).withOptional(true).withDetails(details).withHelper(actionHelper).withFormName(Constants.FORMS.GBV_CONSENT_FORM).build();

        actionList.put(actionName, action);
    }

    protected void createGbvHfConsentFollowupAction(MemberObject memberObject, Map<String, List<VisitDetail>> details) throws BaseGbvVisitAction.ValidationException {
        GbvVisitActionHelper actionHelper = new MyGbvHfConsentFollowupActionHelper(memberObject);

        String actionName = mContext.getString(R.string.gbv_consent_followup_action_title);

        BaseGbvVisitAction action = getBuilder(actionName).withOptional(true).withDetails(details).withHelper(actionHelper).withFormName(Constants.FORMS.GBV_CONSENT_FOLLOWUP_FORM).build();

        actionList.put(actionName, action);
    }

    protected void createHistoryCollectionAction(MemberObject memberObject, Map<String, List<VisitDetail>> details) throws BaseGbvVisitAction.ValidationException {
        GbvVisitActionHelper actionHelper = new MyHistoryCollectionActionHelper(memberObject);

        String actionName = mContext.getString(R.string.gbv_history_collection_title);

        BaseGbvVisitAction action = getBuilder(actionName).withOptional(true).withDetails(details).withHelper(actionHelper).withFormName(Constants.FORMS.GBV_HISTORY_COLLECTION_FORM).build();

        actionList.put(actionName, action);
    }

    protected void createMedicalExaminationAction(MemberObject memberObject, Map<String, List<VisitDetail>> details) throws BaseGbvVisitAction.ValidationException {
        GbvVisitActionHelper actionHelper = new MedicalExaminationActionHelper(memberObject);

        String actionName = mContext.getString(R.string.gbv_medical_examination_title);

        BaseGbvVisitAction action = getBuilder(actionName).withOptional(true).withDetails(details).withHelper(actionHelper).withFormName(Constants.FORMS.GBV_MEDICAL_EXAMINATION_FORM).build();

        actionList.put(actionName, action);
    }

    protected void createPhysicalExaminationAction(MemberObject memberObject, Map<String, List<VisitDetail>> details) throws BaseGbvVisitAction.ValidationException {
        GbvVisitActionHelper actionHelper = new PhysicalExaminationActionHelper(memberObject);

        String actionName = mContext.getString(R.string.gbv_physical_examination_title);

        BaseGbvVisitAction action = getBuilder(actionName).withOptional(true).withDetails(details).withHelper(actionHelper).withFormName(Constants.FORMS.GBV_PHYSICAL_EXAMINATION_FORM).build();

        actionList.put(actionName, action);
    }

    protected void createForensicExaminationAction(MemberObject memberObject, Map<String, List<VisitDetail>> details) throws BaseGbvVisitAction.ValidationException {
        GbvVisitActionHelper actionHelper = new MyForensicExaminationActionHelper(memberObject);

        String actionName = mContext.getString(R.string.gbv_forensic_examination_title);

        BaseGbvVisitAction action = getBuilder(actionName).withOptional(true).withDetails(details).withHelper(actionHelper).withFormName(Constants.FORMS.GBV_FORENSIC_EXAMINATION_FORM).build();

        actionList.put(actionName, action);
    }

    protected void createLabInvestigationAction(MemberObject memberObject, Map<String, List<VisitDetail>> details) throws BaseGbvVisitAction.ValidationException {
        GbvVisitActionHelper actionHelper = new MyLabInvestigationActionHelper(memberObject, mCurrentPregnancyStatus, mTypeOfAssault, mHhivStatus);


        String actionName = mContext.getString(R.string.gbv_lab_investigation_title);

        BaseGbvVisitAction action = getBuilder(actionName).withOptional(true).withDetails(details).withHelper(actionHelper).withFormName(Constants.FORMS.GBV_LAB_INVESTIGATION_FORM).build();

        actionList.put(actionName, action);
    }

    protected void createProvideTreatmentAction(MemberObject memberObject, Map<String, List<VisitDetail>> details) throws BaseGbvVisitAction.ValidationException {
        GbvVisitActionHelper actionHelper = new ProvideTreatmentActionHelper(memberObject,mTypeOfAssault);

        String actionName = mContext.getString(R.string.gbv_provide_treatment_title);

        BaseGbvVisitAction action = getBuilder(actionName).withOptional(true).withDetails(details).withHelper(actionHelper).withFormName(Constants.FORMS.GBV_PROVIDE_TREATMENT_FORM).build();

        actionList.put(actionName, action);
    }

    protected void createEducationAndCounsellingAction(MemberObject memberObject, Map<String, List<VisitDetail>> details) throws BaseGbvVisitAction.ValidationException {
        GbvVisitActionHelper actionHelper = new EducationAndCounsellingActionHelper(memberObject);

        String actionName = mContext.getString(R.string.gbv_education_and_counselling_title);

        BaseGbvVisitAction action = getBuilder(actionName).withOptional(true).withDetails(details).withHelper(actionHelper).withFormName(Constants.FORMS.GBV_EDUCATION_AND_COUNSELLING_FORM).build();

        actionList.put(actionName, action);
    }

    protected void createSafetyPlanAction(MemberObject memberObject, Map<String, List<VisitDetail>> details) throws BaseGbvVisitAction.ValidationException {
        GbvVisitActionHelper actionHelper = new SafetyPlanActionHelper(memberObject);

        String actionName = mContext.getString(R.string.gbv_safety_plan_title);

        BaseGbvVisitAction action = getBuilder(actionName).withOptional(true).withDetails(details).withHelper(actionHelper).withFormName(Constants.FORMS.GBV_SAFETY_PLAN).build();

        if (memberObject.getAge() >= 5) {
            actionList.put(actionName, action);
        }
    }

    protected void createLinkageAction(MemberObject memberObject, Map<String, List<VisitDetail>> details) throws BaseGbvVisitAction.ValidationException {
        GbvVisitActionHelper actionHelper = new LinkageActionHelper(memberObject);

        String actionName = mContext.getString(R.string.gbv_linkage_title);

        BaseGbvVisitAction action = getBuilder(actionName).withOptional(true).withDetails(details).withHelper(actionHelper).withFormName(Constants.FORMS.GBV_REFERRAL_AND_LINKAGE).build();

        actionList.put(actionName, action);
    }

    protected void createNextAppointmentDateAction(MemberObject memberObject, Map<String, List<VisitDetail>> details) throws BaseGbvVisitAction.ValidationException {
        GbvVisitActionHelper actionHelper = new NextAppointmentDateActionHelper(memberObject);

        String actionName = mContext.getString(R.string.gbv_next_appointment_date_title);

        BaseGbvVisitAction action = getBuilder(actionName).withOptional(true).withDetails(details).withHelper(actionHelper).withFormName(Constants.FORMS.GBV_NEXT_APPOINTMENT_DATE).build();

        actionList.put(actionName, action);
    }


    public BaseGbvVisitAction.Builder getBuilder(String title) {
        return new BaseGbvVisitAction.Builder(mContext, title);
    }

    @Override
    public void submitVisit(BaseGbvVisitContract.View view, final String memberID, final Map<String, BaseGbvVisitAction> map, final BaseGbvVisitContract.InteractorCallBack callBack, Constants.SaveType saveType) {
        final Runnable runnable = () -> {
            boolean result = true;
            try {
                submitVisit(view.getEditMode(), memberID, map, "");
                view.setEditMode(true);
            } catch (Exception e) {
                Timber.e(e);
                result = false;
            }

            final boolean finalResult = result;
            appExecutors.mainThread().execute(() -> callBack.onSubmitted(finalResult, saveType));
        };

        appExecutors.diskIO().execute(runnable);
    }

    protected void submitVisit(final boolean editMode, final String memberID, final Map<String, BaseGbvVisitAction> map, String parentEventType) throws Exception {
        // create a map of the different types

        Map<String, BaseGbvVisitAction> externalVisits = new HashMap<>();
        Map<String, String> combinedJsons = new HashMap<>();
        String payloadType = null;
        String payloadDetails = null;

        // aggregate forms to be processed
        for (Map.Entry<String, BaseGbvVisitAction> entry : map.entrySet()) {
            String json = entry.getValue().getJsonPayload();
            if (StringUtils.isNotBlank(json)) {
                // do not process events that are meant to be in detached mode
                // in a similar manner to the the aggregated events

                BaseGbvVisitAction action = entry.getValue();
                BaseGbvVisitAction.ProcessingMode mode = action.getProcessingMode();

                if (mode == BaseGbvVisitAction.ProcessingMode.SEPARATE && StringUtils.isBlank(parentEventType)) {
                    externalVisits.put(entry.getKey(), entry.getValue());
                } else {
                    if (action.getActionStatus() != BaseGbvVisitAction.Status.PENDING)
                        combinedJsons.put(entry.getKey(), json);
                }

                payloadType = action.getPayloadType().name();
                payloadDetails = action.getPayloadDetails();
            }
        }

        String type = StringUtils.isBlank(parentEventType) ? getEncounterType() : getEncounterType();

        // persist to database
        Visit visit = saveVisit(editMode, memberID, type, combinedJsons, parentEventType);
        if (visit != null) {
            saveVisitDetails(visit, payloadType, payloadDetails);
            processExternalVisits(visit, externalVisits, memberID);
        }

        if (gbvLibrary.isSubmitOnSave()) {
            List<Visit> visits = new ArrayList<>(1);
            visits.add(visit);
            VisitUtils.processVisits(visits, gbvLibrary.visitRepository(), gbvLibrary.visitDetailsRepository());

            Context context = gbvLibrary.getInstance().context().applicationContext();

        }
    }

    /**
     * recursively persist visits to the db
     *
     * @param visit
     * @param externalVisits
     * @param memberID
     * @throws Exception
     */
    protected void processExternalVisits(Visit visit, Map<String, BaseGbvVisitAction> externalVisits, String memberID) throws Exception {
        if (visit != null && !externalVisits.isEmpty()) {
            for (Map.Entry<String, BaseGbvVisitAction> entry : externalVisits.entrySet()) {
                Map<String, BaseGbvVisitAction> subEvent = new HashMap<>();
                subEvent.put(entry.getKey(), entry.getValue());

                String subMemberID = entry.getValue().getBaseEntityID();
                if (StringUtils.isBlank(subMemberID)) subMemberID = memberID;

                submitVisit(false, subMemberID, subEvent, visit.getVisitType());
            }
        }
    }

    protected @Nullable Visit saveVisit(boolean editMode, String memberID, String encounterType, final Map<String, String> jsonString, String parentEventType) throws Exception {

        AllSharedPreferences allSharedPreferences = gbvLibrary.getInstance().context().allSharedPreferences();

        String derivedEncounterType = StringUtils.isBlank(parentEventType) ? encounterType : "";
        Event baseEvent = JsonFormUtils.processVisitJsonForm(allSharedPreferences, memberID, derivedEncounterType, jsonString, getTableName());

        // only tag the first event with the date
        if (StringUtils.isBlank(parentEventType)) {
            prepareEvent(baseEvent);
        } else {
            prepareSubEvent(baseEvent);
        }

        if (baseEvent != null) {
            baseEvent.setFormSubmissionId(JsonFormUtils.generateRandomUUIDString());
            JsonFormUtils.tagEvent(allSharedPreferences, baseEvent);

            String visitID = (editMode) ? visitRepository().getLatestVisit(memberID, getEncounterType()).getVisitId() : JsonFormUtils.generateRandomUUIDString();

            // reset database
            if (editMode) {
                Visit visit = visitRepository().getVisitByVisitId(visitID);
                if (visit != null) baseEvent.setEventDate(visit.getDate());

                VisitUtils.deleteProcessedVisit(visitID, memberID);
                deleteOldVisit(visitID);
            }

            Visit visit = NCUtils.eventToVisit(baseEvent, visitID);
            visit.setPreProcessedJson(new Gson().toJson(baseEvent));
            visit.setParentVisitID(getParentVisitEventID(visit, parentEventType));

            visitRepository().addVisit(visit);
            return visit;
        }
        return null;
    }

    protected String getParentVisitEventID(Visit visit, String parentEventType) {
        return visitRepository().getParentVisitEventID(visit.getBaseEntityId(), parentEventType, visit.getDate());
    }

    @VisibleForTesting
    public VisitRepository visitRepository() {
        return GbvLibrary.getInstance().visitRepository();
    }

    protected void deleteOldVisit(String visitID) {
        visitRepository().deleteVisit(visitID);
        GbvLibrary.getInstance().visitDetailsRepository().deleteVisitDetails(visitID);

        List<Visit> childVisits = visitRepository().getChildEvents(visitID);
        for (Visit v : childVisits) {
            visitRepository().deleteVisit(v.getVisitId());
            GbvLibrary.getInstance().visitDetailsRepository().deleteVisitDetails(v.getVisitId());
        }
    }


    protected void saveVisitDetails(Visit visit, String payloadType, String payloadDetails) {
        if (visit.getVisitDetails() == null) return;

        for (Map.Entry<String, List<VisitDetail>> entry : visit.getVisitDetails().entrySet()) {
            if (entry.getValue() != null) {
                for (VisitDetail d : entry.getValue()) {
                    d.setPreProcessedJson(payloadDetails);
                    d.setPreProcessedType(payloadType);
                    GbvLibrary.getInstance().visitDetailsRepository().addVisitDetails(d);
                }
            }
        }
    }

    /**
     * Injects implementation specific changes to the event
     *
     * @param baseEvent
     */
    protected void prepareEvent(Event baseEvent) {
        if (baseEvent != null) {
            // add gbv date obs and last
            List<Object> list = new ArrayList<>();
            list.add(new Date());
            baseEvent.addObs(new Obs("concept", "text", "vmmc_visit_date", "", list, new ArrayList<>(), null, "vmmc_visit_date"));
        }
    }

    /**
     * injects additional meta data to the event
     *
     * @param baseEvent
     */
    protected void prepareSubEvent(Event baseEvent) {
        Timber.v("You can add information to sub events");
    }

    protected String getEncounterType() {
        return Constants.EVENT_TYPE.GBV_FOLLOW_UP_VISIT;
    }

    protected String getTableName() {
        return Constants.TABLES.GBV_REGISTER;
    }

    class MyGbvHfVisitTypeActionHelper extends GbvHfVisitTypeActionHelper {

        @Override
        public void processCanManageCase(String canManageCase) {
            if (canManageCase.equalsIgnoreCase("yes")) {
                try {
                    createGbvHfConsentAction(memberObject, details);
                } catch (BaseGbvVisitAction.ValidationException e) {
                    Timber.e(e);
                }
            } else {
                actionList.remove(mContext.getString(R.string.gbv_consent_action_title));
                actionList.remove(mContext.getString(R.string.gbv_consent_followup_action_title));
                actionList.remove(mContext.getString(R.string.gbv_history_collection_title));
                actionList.remove(mContext.getString(R.string.gbv_medical_examination_title));
                actionList.remove(mContext.getString(R.string.gbv_physical_examination_title));
                actionList.remove(mContext.getString(R.string.gbv_forensic_examination_title));
                actionList.remove(mContext.getString(R.string.gbv_lab_investigation_title));
                actionList.remove(mContext.getString(R.string.gbv_provide_treatment_title));
                actionList.remove(mContext.getString(R.string.gbv_education_and_counselling_title));
                actionList.remove(mContext.getString(R.string.gbv_safety_plan_title));
                actionList.remove(mContext.getString(R.string.gbv_linkage_title));
                actionList.remove(mContext.getString(R.string.gbv_next_appointment_date_title));
            }
            appExecutors.mainThread().execute(() -> callBack.preloadActions(actionList));
        }
    }

    class MyGbvHfConsentActionHelper extends GbvHfConsentActionHelper {

        public MyGbvHfConsentActionHelper(MemberObject memberObject) {
            super(memberObject);
        }

        @Override
        public void processClientConsent(String clientConsent) {
            if (clientConsent.equalsIgnoreCase("no")) {
                try {
                    createGbvHfConsentFollowupAction(memberObject, details);
                } catch (BaseGbvVisitAction.ValidationException e) {
                    Timber.e(e);
                }
            } else if (clientConsent.equalsIgnoreCase("yes")) {
                actionList.remove(mContext.getString(R.string.gbv_consent_followup_action_title));
                try {
                    createHistoryCollectionAction(memberObject, details);
                    createMedicalExaminationAction(memberObject, details);
                    createPhysicalExaminationAction(memberObject, details);
                    createForensicExaminationAction(memberObject, details);
                    createProvideTreatmentAction(memberObject, details);
                    createEducationAndCounsellingAction(memberObject, details);
                    createSafetyPlanAction(memberObject, details);
                    createLinkageAction(memberObject, details);
                    createNextAppointmentDateAction(memberObject, details);
                } catch (BaseGbvVisitAction.ValidationException e) {
                    Timber.e(e);
                }
            }
            appExecutors.mainThread().execute(() -> callBack.preloadActions(actionList));
        }
    }

    class MyGbvHfConsentFollowupActionHelper extends GbvHfConsentFollowupActionHelper {
        public MyGbvHfConsentFollowupActionHelper(MemberObject memberObject) {
            super(memberObject);
        }

        @Override
        public void processConsentFollowup(String clientConsentAfterCounseling, String wasSocialWelfareOfficerInvolved) {
            if (clientConsentAfterCounseling.equalsIgnoreCase("yes") || wasSocialWelfareOfficerInvolved.equalsIgnoreCase("yes")) {
                try {
                    createHistoryCollectionAction(memberObject, details);
                    createMedicalExaminationAction(memberObject, details);
                    createPhysicalExaminationAction(memberObject, details);
                    createForensicExaminationAction(memberObject, details);
                    createProvideTreatmentAction(memberObject, details);
                    createEducationAndCounsellingAction(memberObject, details);
                    createSafetyPlanAction(memberObject, details);
                    createLinkageAction(memberObject, details);
                    createNextAppointmentDateAction(memberObject, details);
                } catch (BaseGbvVisitAction.ValidationException e) {
                    Timber.e(e);
                }
            } else {
                actionList.remove(mContext.getString(R.string.gbv_history_collection_title));
                actionList.remove(mContext.getString(R.string.gbv_medical_examination_title));
                actionList.remove(mContext.getString(R.string.gbv_physical_examination_title));
                actionList.remove(mContext.getString(R.string.gbv_forensic_examination_title));
                actionList.remove(mContext.getString(R.string.gbv_lab_investigation_title));
                actionList.remove(mContext.getString(R.string.gbv_provide_treatment_title));
                actionList.remove(mContext.getString(R.string.gbv_education_and_counselling_title));
                actionList.remove(mContext.getString(R.string.gbv_safety_plan_title));
                actionList.remove(mContext.getString(R.string.gbv_linkage_title));
                actionList.remove(mContext.getString(R.string.gbv_next_appointment_date_title));
            }
            appExecutors.mainThread().execute(() -> callBack.preloadActions(actionList));
        }
    }

    class MyForensicExaminationActionHelper extends ForensicExaminationActionHelper {

        public MyForensicExaminationActionHelper(MemberObject memberObject) {
            super(memberObject);
        }

        @Override
        public void processForensicExamination(String doesTheClientNeedLabInvestigation) {
            if (doesTheClientNeedLabInvestigation != null && doesTheClientNeedLabInvestigation.equalsIgnoreCase("yes")) {
                try {
                    createLabInvestigationAction(memberObject, details);
                } catch (BaseGbvVisitAction.ValidationException e) {
                    Timber.e(e);
                }
            } else {
                actionList.remove(mContext.getString(R.string.gbv_lab_investigation_title));
            }
            appExecutors.mainThread().execute(() -> callBack.preloadActions(actionList));
        }
    }

    class MyHistoryCollectionActionHelper extends HistoryCollectionActionHelper {

        public MyHistoryCollectionActionHelper(MemberObject memberObject) {
            super(memberObject);
        }

        @Override
        public void processHistoryCollection(String currentPregnancyStatus, String typeOfAssault, String hivStatus) {
            mCurrentPregnancyStatus = currentPregnancyStatus;
            mTypeOfAssault = typeOfAssault;
            mHhivStatus = hivStatus;

            if (actionList.get(mContext.getString(R.string.gbv_lab_investigation_title)) != null) {
                BaseGbvVisitAction labInvestigationAction = actionList.get(mContext.getString(R.string.gbv_lab_investigation_title));
                String jsonPayloadString = labInvestigationAction.getJsonPayload();

                try {
                    JSONObject jsonPayload = new JSONObject(jsonPayloadString);
                    JSONObject global = jsonPayload.getJSONObject(GLOBAL);
                    global.put("currentPregnancyStatus", currentPregnancyStatus);
                    global.put("typeOfAssault", typeOfAssault);
                    global.put("hivStatus", hivStatus);
                    labInvestigationAction.setJsonPayload(jsonPayload.toString());
                } catch (Exception e) {
                    Timber.e(e);
                }

            }


            if (actionList.get(mContext.getString(R.string.gbv_provide_treatment_title)) != null) {
                BaseGbvVisitAction provideTreatmentAction = actionList.get(mContext.getString(R.string.gbv_provide_treatment_title));
                String jsonPayloadString = provideTreatmentAction.getJsonPayload();

                try {
                    JSONObject jsonPayload = new JSONObject(jsonPayloadString);
                    JSONObject global = jsonPayload.getJSONObject(GLOBAL);
                    global.put("typeOfAssault", typeOfAssault);
                    provideTreatmentAction.setJsonPayload(jsonPayload.toString());
                } catch (Exception e) {
                    Timber.e(e);
                }

            }

        }
    }

    class MyLabInvestigationActionHelper extends LabInvestigationActionHelper {
        public MyLabInvestigationActionHelper(MemberObject memberObject, String currentPregnancyStatus, String typeOfAssault, String hivStatus) {
            super(memberObject, currentPregnancyStatus, typeOfAssault, hivStatus);
        }

        @Override
        public void processTestResults(String hepbTestResults, String hivTestResults) {
            if (actionList.get(mContext.getString(R.string.gbv_provide_treatment_title)) != null) {
                BaseGbvVisitAction provideTreatmentAction = actionList.get(mContext.getString(R.string.gbv_provide_treatment_title));
                String jsonPayloadString = provideTreatmentAction.getJsonPayload();
                try {
                    JSONObject jsonPayload = new JSONObject(jsonPayloadString);
                    JSONObject global = jsonPayload.getJSONObject(GLOBAL);
                    global.put("hepbTestResults", hepbTestResults);
                    provideTreatmentAction.setJsonPayload(jsonPayload.toString());
                } catch (Exception e) {
                    Timber.e(e);
                }
            }

            if (actionList.get(mContext.getString(R.string.gbv_education_and_counselling_title)) != null) {
                BaseGbvVisitAction provideEducationAndCounsellingAction = actionList.get(mContext.getString(R.string.gbv_education_and_counselling_title));
                String jsonPayloadString = provideEducationAndCounsellingAction.getJsonPayload();
                try {
                    JSONObject jsonPayload = new JSONObject(jsonPayloadString);
                    JSONObject global = jsonPayload.getJSONObject(GLOBAL);
                    global.put("hivTestResults", hivTestResults);
                    provideEducationAndCounsellingAction.setJsonPayload(jsonPayload.toString());
                } catch (Exception e) {
                    Timber.e(e);
                }
            }
        }
    }
}