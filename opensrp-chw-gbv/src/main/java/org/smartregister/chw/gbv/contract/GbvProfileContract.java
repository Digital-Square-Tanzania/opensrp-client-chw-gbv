package org.smartregister.chw.gbv.contract;

import org.jetbrains.annotations.Nullable;
import org.smartregister.chw.gbv.domain.MemberObject;

public interface GbvProfileContract {
    interface View extends InteractorCallBack {

        void setProfileViewWithData();

        void setOverDueColor();

        void openMedicalHistory();

        void recordSbc(MemberObject memberObject);

        void showProgressBar(boolean status);

        void hideView();
    }

    interface Presenter {

        void fillProfileData(@Nullable MemberObject memberObject);

        void saveForm(String jsonString);

        @Nullable
        View getView();

        void refreshProfileBottom();

        void recordSbcButton(String visitState);
    }

    interface Interactor {

        void refreshProfileInfo(MemberObject memberObject, InteractorCallBack callback);

        void saveRegistration(String jsonString, final GbvProfileContract.InteractorCallBack callBack);
    }


    interface InteractorCallBack {
        void refreshMedicalHistory(boolean hasHistory);
    }
}