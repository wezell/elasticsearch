/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.core.ilm;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.action.ActionType;
import org.elasticsearch.action.support.master.MasterNodeRequest;
import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.test.client.NoOpClient;
import org.elasticsearch.threadpool.TestThreadPool;
import org.elasticsearch.threadpool.ThreadPool;
import org.junit.After;
import org.junit.Before;

import static org.elasticsearch.xpack.core.ilm.LifecycleSettings.LIFECYCLE_STEP_MASTER_TIMEOUT;
import static org.hamcrest.Matchers.equalTo;

public abstract class AbstractStepMasterTimeoutTestCase<T extends AsyncActionStep> extends AbstractStepTestCase<T> {

    protected ThreadPool pool;

    @Before
    public void setupThreadPool() {
        pool = new TestThreadPool("timeoutTestPool");
    }

    @After
    public void shutdownThreadPool() {
        pool.shutdownNow();
    }

    public void testMasterTimeout() {
        checkMasterTimeout(TimeValue.timeValueSeconds(30),
            ClusterState.builder(ClusterName.DEFAULT).metaData(MetaData.builder().build()).build());
        checkMasterTimeout(TimeValue.timeValueSeconds(10),
            ClusterState.builder(ClusterName.DEFAULT)
                .metaData(MetaData.builder()
                    .persistentSettings(Settings.builder().put(LIFECYCLE_STEP_MASTER_TIMEOUT, "10s").build())
                    .build())
                .build());
    }

    private void checkMasterTimeout(TimeValue timeValue, ClusterState currentClusterState) {
        T instance = createRandomInstance();
        instance.setClient(new NoOpClient(pool) {
            @Override
            protected <Request extends ActionRequest, Response extends ActionResponse> void doExecute(ActionType<Response> action,
                                                                                                      Request request,
                                                                                                      ActionListener<Response> listener) {
                if (request instanceof MasterNodeRequest) {
                    assertThat(((MasterNodeRequest<?>) request).masterNodeTimeout(), equalTo(timeValue));
                }
            }
        });
        instance.performAction(getIndexMetaData(), currentClusterState, null, new AsyncActionStep.Listener() {
            @Override
            public void onResponse(boolean complete) {

            }

            @Override
            public void onFailure(Exception e) {

            }
        });
    }

    protected abstract IndexMetaData getIndexMetaData();

    public static ClusterState emptyClusterState() {
        return ClusterState.builder(ClusterName.DEFAULT).build();
    }
}
