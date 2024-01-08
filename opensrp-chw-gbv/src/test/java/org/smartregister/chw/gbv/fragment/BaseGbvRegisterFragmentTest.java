package org.smartregister.chw.gbv.fragment;

import static org.mockito.Mockito.times;

import org.junit.Test;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.reflect.Whitebox;
import org.smartregister.chw.gbv.activity.BaseGbvProfileActivity;
import org.smartregister.commonregistry.CommonPersonObjectClient;

public class BaseGbvRegisterFragmentTest {
    @Mock
    public BaseGbvRegisterFragment baseTestRegisterFragment;

    @Mock
    public CommonPersonObjectClient client;

    @Test(expected = Exception.class)
    public void openProfile() throws Exception {
        Whitebox.invokeMethod(baseTestRegisterFragment, "openProfile", client);
        PowerMockito.mockStatic(BaseGbvProfileActivity.class);
        BaseGbvProfileActivity.startProfileActivity(null, null);
        PowerMockito.verifyStatic(BaseGbvProfileActivity.class, times(1));

    }
}
