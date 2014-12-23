package com.example.johnandjai.sunshine.app.sync;

/**
 * Created by John and Jai on 12/18/2014.
 */

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Lesson 6: implement a bound Service that instantiates the authenticator
 * when started, as shown at
 * http://developer.android.com/training/sync-adapters/creating-authenticator.html
 */
/* This class must be declared as a <service> in the AndroidManifest file.
   Android sees the entry in the manifest and creates SunshineAuthenticatorService;
   the onCreate handler then creates the SunshineAuthenticator.  The <service>
   definition also specifies that additional Authenticator setup parameters are
   defined in the authenticator.xml file. */
public class SunshineAuthenticatorService extends Service {

    // Instance field that stores the authenticator object
    private SunshineAuthenticator mAuthenticator;

    @Override
    public void onCreate() {
        // Create a new authenticator object
        mAuthenticator = new SunshineAuthenticator(this);
    }
    /*
     * When the system binds to this Service to make the RPC call
     * return the authenticator's IBinder.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return mAuthenticator.getIBinder();
    }
}