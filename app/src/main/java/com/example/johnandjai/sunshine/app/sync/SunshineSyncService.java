package com.example.johnandjai.sunshine.app.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Created by John and Jai on 12/18/2014.
 */
/* This class must be declared as a <service> in the AndroidManifest file.
   Android sees the entry in the manifest and creates SunshineSyncService;
   the onCreate handler then creates the SunshineSyncAdapter.  The <service>
   definition also specifies that additional SyncAdapter setup parameters are
   defined in syncadapter.xml */
public class SunshineSyncService extends Service {
    // Used to deliver the SyncAdapter binder to the SyncManager

    private final String LOG_TAG = SunshineSyncService.class.getSimpleName();

    private static final Object sSyncAdapterLock = new Object();
    private static SunshineSyncAdapter sSunshineSyncAdapter = null;

    public void onCreate() {
//        Log.d(LOG_TAG, "Hello from onCreate!");
        synchronized (sSyncAdapterLock) {
            if (sSunshineSyncAdapter == null) {
                sSunshineSyncAdapter = new SunshineSyncAdapter(getApplicationContext(), true);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return sSunshineSyncAdapter.getSyncAdapterBinder();
    }
}
