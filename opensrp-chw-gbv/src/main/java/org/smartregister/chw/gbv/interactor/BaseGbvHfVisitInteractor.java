package org.smartregister.chw.gbv.interactor;


import android.content.Context;

import androidx.annotation.VisibleForTesting;

import com.google.gson.Gson;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;
import org.smartregister.chw.gbv.GbvLibrary;
import org.smartregister.chw.gbv.R;
import org.smartregister.chw.gbv.actionhelper.GbvHfVisitTypeActionHelper;
import org.smartregister.chw.gbv.actionhelper.GbvVisitActionHelper;
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
            Visit lastVisit = gbvLibrary.visitRepository().getLatestVisit(memberObject.getBaseEntityId(), Constants.EVENT_TYPE.SBC_FOLLOW_UP_VISIT);
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

    protected void evaluateArtAdherenceCounselling(MemberObject memberObject, Map<String, List<VisitDetail>> details) throws BaseGbvVisitAction.ValidationException {
        GbvVisitActionHelper actionHelper = null;

        String actionName = mContext.getString(R.string.sbc_visit_action_title_art_and_condom_education);

        BaseGbvVisitAction action = getBuilder(actionName).withOptional(false).withDetails(details).withHelper(actionHelper).withFormName(Constants.FORMS.SBC_ART_CONDOM_EDUCATION).build();

        actionList.put(actionName, action);
    }

    protected void createGbvHfConsentAction(MemberObject memberObject, Map<String, List<VisitDetail>> details) throws BaseGbvVisitAction.ValidationException {
        GbvVisitActionHelper actionHelper = null;

        String actionName = mContext.getString(R.string.sbc_visit_action_title_comments);

        BaseGbvVisitAction action = getBuilder(actionName).withOptional(true).withDetails(details).withHelper(actionHelper).withFormName(Constants.FORMS.SBC_COMMENTS).build();

        actionList.put(actionName, action);
    }

    public BaseGbvVisitAction.Builder getBuilder(String title) {
        return new BaseGbvVisitAction.Builder(mContext, title);
    }

    @Override
    public void submitVisit(final boolean editMode, final String memberID, final Map<String, BaseGbvVisitAction> map, final BaseGbvVisitContract.InteractorCallBack callBack) {
        final Runnable runnable = () -> {
            boolean result = true;
            try {
                submitVisit(editMode, memberID, map, "");
            } catch (Exception e) {
                Timber.e(e);
                result = false;
            }

            final boolean finalResult = result;
            appExecutors.mainThread().execute(() -> callBack.onSubmitted(finalResult));
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
            // add sbc date obs and last
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
        return Constants.EVENT_TYPE.SBC_FOLLOW_UP_VISIT;
    }

    protected String getTableName() {
        return Constants.TABLES.SBC_REGISTER;
    }

    class HivStatusActionHelper extends GbvVisitActionHelper {
        protected Context context;
        protected MemberObject memberObject;
        protected String hivStatus;

        public HivStatusActionHelper(Context context, MemberObject memberObject) {
            this.context = context;
            this.memberObject = memberObject;
        }

        /**
         * set preprocessed status to be inert
         *
         * @return null
         */
        @Override
        public String getPreProcessed() {
            return null;
        }

        @Override
        public void onPayloadReceived(String jsonPayload) {
            try {
                JSONObject jsonObject = new JSONObject(jsonPayload);
                hivStatus = JsonFormUtils.getValue(jsonObject, "hiv_status");

                if (hivStatus.contains("positive")) {
                    evaluateArtAdherenceCounselling(memberObject, details);
                } else {
                    actionList.remove(mContext.getString(R.string.sbc_visit_action_title_art_and_condom_education));
                }
                appExecutors.mainThread().execute(() -> callBack.preloadActions(actionList));
            } catch (Exception e) {
                Timber.e(e);
            }
        }

        @Override
        public String evaluateSubTitle() {
            return null;
        }

        @Override
        public BaseGbvVisitAction.Status evaluateStatusOnPayload() {
            if (StringUtils.isNotBlank(hivStatus)) {
                return BaseGbvVisitAction.Status.COMPLETED;
            } else {
                return BaseGbvVisitAction.Status.PENDING;
            }
        }
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
                actionList.remove(mContext.getString(R.string.sbc_visit_action_title_comments));
            }

            appExecutors.mainThread().execute(() -> callBack.preloadActions(actionList));
        }
    }
}