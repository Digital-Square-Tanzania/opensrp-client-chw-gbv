package org.smartregister.chw.gbv.interactor;

import androidx.annotation.VisibleForTesting;

import org.smartregister.chw.gbv.contract.GbvRegisterContract;
import org.smartregister.chw.gbv.util.GbvUtil;
import org.smartregister.chw.gbv.util.AppExecutors;

public class BaseGbvRegisterInteractor implements GbvRegisterContract.Interactor {

    private AppExecutors appExecutors;

    @VisibleForTesting
    BaseGbvRegisterInteractor(AppExecutors appExecutors) {
        this.appExecutors = appExecutors;
    }

    public BaseGbvRegisterInteractor() {
        this(new AppExecutors());
    }

    @Override
    public void saveRegistration(final String jsonString, final GbvRegisterContract.InteractorCallBack callBack) {

        Runnable runnable = () -> {
            try {
                GbvUtil.saveFormEvent(jsonString);
            } catch (Exception e) {
                e.printStackTrace();
            }

            appExecutors.mainThread().execute(() -> callBack.onRegistrationSaved());
        };
        appExecutors.diskIO().execute(runnable);
    }
}
