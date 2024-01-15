package org.smartregister.chw.gbv_sample.interactor;

import org.smartregister.chw.gbv.domain.MemberObject;
import org.smartregister.chw.gbv.interactor.BaseGbvHfVisitInteractor;
import org.smartregister.chw.gbv_sample.activity.EntryActivity;

public class GbvHfVisitInteractor extends BaseGbvHfVisitInteractor {
    @Override
    public MemberObject getMemberClient(String memberID) {
        return EntryActivity.getSampleMember();
    }
}
