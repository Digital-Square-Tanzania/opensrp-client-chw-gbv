package org.smartregister.chw.gbv.listener;


import android.view.View;

import org.smartregister.chw.gbv.R;
import org.smartregister.chw.gbv.fragment.BaseGbvCallDialogFragment;
import org.smartregister.chw.gbv.util.GbvUtil;

import timber.log.Timber;

public class BaseGbvCallWidgetDialogListener implements View.OnClickListener {

    private BaseGbvCallDialogFragment callDialogFragment;

    public BaseGbvCallWidgetDialogListener(BaseGbvCallDialogFragment dialogFragment) {
        callDialogFragment = dialogFragment;
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.gbv_call_close) {
            callDialogFragment.dismiss();
        } else if (i == R.id.gbv_call_head_phone) {
            try {
                String phoneNumber = (String) v.getTag();
                GbvUtil.launchDialer(callDialogFragment.getActivity(), callDialogFragment, phoneNumber);
                callDialogFragment.dismiss();
            } catch (Exception e) {
                Timber.e(e);
            }
        } else if (i == R.id.call_gbv_client_phone) {
            try {
                String phoneNumber = (String) v.getTag();
                GbvUtil.launchDialer(callDialogFragment.getActivity(), callDialogFragment, phoneNumber);
                callDialogFragment.dismiss();
            } catch (Exception e) {
                Timber.e(e);
            }
        }
    }
}
