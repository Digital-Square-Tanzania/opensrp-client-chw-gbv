package org.smartregister.chw.gbv.interactor;

import androidx.annotation.VisibleForTesting;

import org.smartregister.chw.gbv.GbvLibrary;
import org.smartregister.chw.gbv.contract.GbvProfileContract;
import org.smartregister.chw.gbv.domain.MemberObject;
import org.smartregister.chw.gbv.domain.Visit;
import org.smartregister.chw.gbv.util.AppExecutors;
import org.smartregister.chw.gbv.util.Constants;
import org.smartregister.chw.gbv.util.GbvUtil;

public class BaseGbvProfileInteractor implements GbvProfileContract.Interactor {
    protected AppExecutors appExecutors;

    @VisibleForTesting
    BaseGbvProfileInteractor(AppExecutors appExecutors) {
        this.appExecutors = appExecutors;
    }

    public BaseGbvProfileInteractor() {
        this(new AppExecutors());
    }

    @Override
    public void refreshProfileInfo(MemberObject memberObject, GbvProfileContract.InteractorCallBack callback) {
        Runnable runnable = () -> appExecutors.mainThread().execute(() -> {
            callback.refreshMedicalHistory(getVisit(Constants.EVENT_TYPE.GBV_FOLLOW_UP_VISIT, memberObject) != null);
        });
        appExecutors.diskIO().execute(runnable);
    }

    @Override
    public void saveRegistration(final String jsonString, final GbvProfileContract.InteractorCallBack callback) {

        Runnable runnable = () -> {
            try {
                GbvUtil.saveFormEvent(jsonString);
            } catch (Exception e) {
                e.printStackTrace();
            }

        };
        appExecutors.diskIO().execute(runnable);
    }

    private Visit getVisit(String eventType, MemberObject memberObject) {
        try {
            return GbvLibrary.getInstance().visitRepository().getLatestVisit(memberObject.getBaseEntityId(), eventType);
        } catch (Exception e) {
            return null;
        }
    }
}
