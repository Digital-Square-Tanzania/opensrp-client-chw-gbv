package org.smartregister.chw.gbv.contract;

import android.content.Context;

public interface BaseGbvCallDialogContract {

    interface View {
        void setPendingCallRequest(Dialer dialer);
        Context getCurrentContext();
    }

    interface Dialer {
        void callMe();
    }
}
