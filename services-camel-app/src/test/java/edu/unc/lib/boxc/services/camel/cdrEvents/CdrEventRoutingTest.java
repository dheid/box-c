package edu.unc.lib.boxc.services.camel.cdrEvents;

import static edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType.ADD_SET_TO_PARENT;
import static edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType.UPDATE_ACCESS_TREE;
import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.test.spring.CamelSpringRunner;
import org.apache.camel.test.spring.CamelTestContextBootstrapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;

import edu.unc.lib.boxc.indexing.solr.ChildSetRequest;
import edu.unc.lib.boxc.indexing.solr.SolrUpdateRequest;
import edu.unc.lib.boxc.indexing.solr.action.IndexingAction;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import edu.unc.lib.boxc.operations.jms.OperationsMessageSender;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType;
import edu.unc.lib.boxc.services.camel.solrUpdate.SolrUpdateProcessor;

/**
 *
 * @author bbpennel
 *
 */
@RunWith(CamelSpringRunner.class)
@BootstrapWith(CamelTestContextBootstrapper.class)
@ContextHierarchy({
    @ContextConfiguration("/spring-test/jms-context.xml"),
    @ContextConfiguration("/cdr-event-routing-it-context.xml")
})
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class CdrEventRoutingTest {

    private static final String USER_ID = "user";
    private static final String DEPOSIT_ID = "deposit";
    private static final String BASE_URI = "http://example.com/rest/";
    private AutoCloseable closeable;

    @Autowired
    private OperationsMessageSender opsMsgSender;

    @Autowired
    private SolrUpdateProcessor solrSmallUpdateProcessor;

    @Autowired
    private SolrUpdateProcessor solrLargeUpdateProcessor;

    @Autowired
    private CamelContext cdrServiceSolrUpdate;

    @Mock
    private Map<IndexingActionType, IndexingAction> mockActionMap;
    @Mock
    private IndexingAction mockIndexingAction;
    @Captor
    private ArgumentCaptor<SolrUpdateRequest> updateRequestCaptor;

    @Before
    public void init() throws Exception {
        closeable = openMocks(this);

        TestHelper.setContentBase(BASE_URI);

        solrSmallUpdateProcessor.setSolrIndexingActionMap(mockActionMap);
        solrLargeUpdateProcessor.setSolrIndexingActionMap(mockActionMap);

        when(mockActionMap.get(any(IndexingActionType.class)))
                .thenReturn(mockIndexingAction);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void testAddAction() throws Exception {
        List<PID> added = pidList(3);
        List<PID> destinations = pidList(1);

        NotifyBuilder notify = new NotifyBuilder(cdrServiceSolrUpdate)
                .whenCompleted(1)
                .create();

        opsMsgSender.sendAddOperation(USER_ID, destinations, added, emptyList(), DEPOSIT_ID);

        notify.matches(3l, TimeUnit.SECONDS);

        verify(mockIndexingAction).performAction(updateRequestCaptor.capture());

        ChildSetRequest updateRequest = (ChildSetRequest) updateRequestCaptor.getValue();
        assertEquals(ADD_SET_TO_PARENT, updateRequest.getUpdateAction());

        assertTrue(updateRequest.getChildren().containsAll(added));
    }

    @Test
    public void testEditAccessControlAction() throws Exception {
        int numPids = 3;
        List<PID> pids = pidList(numPids);

        NotifyBuilder notify = new NotifyBuilder(cdrServiceSolrUpdate)
                .whenCompleted(1)
                .create();

        opsMsgSender.sendMarkForDeletionOperation(USER_ID, pids);

        notify.matches(3l, TimeUnit.SECONDS);

        verify(mockIndexingAction).performAction(updateRequestCaptor.capture());

        ChildSetRequest updateRequest = (ChildSetRequest) updateRequestCaptor.getValue();
        assertEquals(UPDATE_ACCESS_TREE, updateRequest.getUpdateAction());

        assertTrue(updateRequest.getChildren().containsAll(pids));
    }

    private List<PID> pidList(int count) {
        List<PID> pidList = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            pidList.add(PIDs.get(UUID.randomUUID().toString()));
        }
        return pidList;
    }
}
