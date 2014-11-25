package com.example.johnandjai.sunshine.app;

import android.test.suitebuilder.TestSuiteBuilder;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Created by John and Jai on 10/21/2014.
 */
public class FullTestSuite extends TestSuite {
    // for JUnit testing; boilerplate code that allows us to add tests in additional classes such
    // as TestDb.
    public static Test suite() {
        return new TestSuiteBuilder(FullTestSuite.class)
                .includeAllPackagesUnderHere().build();
    }

    public FullTestSuite() {
        super();
    }
}
