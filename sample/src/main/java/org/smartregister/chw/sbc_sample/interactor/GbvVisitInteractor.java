package org.smartregister.chw.sbc_sample.interactor;

import org.smartregister.chw.gbv.domain.MemberObject;
import org.smartregister.chw.gbv.interactor.BaseGbvVisitInteractor;
import org.smartregister.chw.sbc_sample.activity.EntryActivity;

public class GbvVisitInteractor extends BaseGbvVisitInteractor {
    @Override
    public MemberObject getMemberClient(String memberID) {
        return EntryActivity.getSampleMember();
    }
}
