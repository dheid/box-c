package edu.unc.lib.boxc.search.solr.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import edu.unc.lib.boxc.common.test.TestHelpers;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.api.models.ObjectPath;
import edu.unc.lib.boxc.search.api.models.ObjectPathEntry;
import edu.unc.lib.boxc.search.api.requests.SimpleIdRequest;
import edu.unc.lib.boxc.search.solr.test.BaseEmbeddedSolrTest;
import edu.unc.lib.boxc.search.solr.test.TestCorpus;
import edu.unc.lib.boxc.search.solr.utils.AccessRestrictionUtil;

/**
 *
 * @author bbpennel
 *
 */
public class ObjectPathFactoryIT extends BaseEmbeddedSolrTest {
    private TestCorpus testCorpus;
    private SolrSearchService solrSearchService;

    private ObjectPathFactory objPathFactory;

    private AutoCloseable closeable;

    @Mock
    private AccessRestrictionUtil restrictionUtil;

    public ObjectPathFactoryIT() {
        testCorpus = new TestCorpus();
    }

    @BeforeEach
    public void init() throws Exception {
        closeable = openMocks(this);

        index(testCorpus.populate());

        solrSearchService = new SolrSearchService();
        solrSearchService.setSolrSettings(solrSettings);
        solrSearchService.setAccessRestrictionUtil(restrictionUtil);
        TestHelpers.setField(solrSearchService, "solrClient", server);

        objPathFactory = new ObjectPathFactory();
        objPathFactory.setSolrSettings(solrSettings);
        objPathFactory.setSearch(solrSearchService);
        objPathFactory.setTimeToLiveMilli(500L);
        objPathFactory.setCacheSize(10);
        objPathFactory.init();
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void testGetWorkPathByPid() throws Exception {
        ObjectPath path = objPathFactory.getPath(testCorpus.work1Pid);

        assertPathPids(path, testCorpus.rootPid, testCorpus.unitPid, testCorpus.coll1Pid,
                testCorpus.folder1Pid, testCorpus.work1Pid);
        assertEquals("/Collections/Unit/Collection 1/Folder 1/Work 1", path.toNamePath());
    }

    @Test
    public void testGetFilePathByPid() throws Exception {
        ObjectPath path = objPathFactory.getPath(testCorpus.work3File1Pid);

        assertPathPids(path, testCorpus.rootPid, testCorpus.unitPid, testCorpus.coll2Pid,
                testCorpus.work3Pid, testCorpus.work3File1Pid);
        assertEquals("/Collections/Unit/Collection 2/Work 3/File 1", path.toNamePath());
    }

    @Test
    public void testGetRootPathByPid() throws Exception {
        ObjectPath path = objPathFactory.getPath(testCorpus.rootPid);

        assertPathPids(path, testCorpus.rootPid);
        assertEquals("/Collections", path.toNamePath());
    }

    @Test
    public void testNoAncestorPath() throws Exception {
        ContentObjectRecord bom = mock(ContentObjectRecord.class);

        ObjectPath path = objPathFactory.getPath(bom);

        assertNull(path);
    }

    @Test
    public void testGetPathByMetadata() throws Exception {
        ContentObjectRecord bom = solrSearchService.getObjectById(new SimpleIdRequest(testCorpus.coll1Pid, null));

        ObjectPath path = objPathFactory.getPath(bom);

        assertPathPids(path, testCorpus.rootPid, testCorpus.unitPid, testCorpus.coll1Pid);
        assertEquals("/Collections/Unit/Collection 1", path.toNamePath());
    }

    private void assertPathPids(ObjectPath path, PID... pids) {
        List<ObjectPathEntry> pathEntries = path.getEntries();

        for (int i = 0; i < pids.length; i++) {
            assertEquals(pids[i].getId(), pathEntries.get(i).getPid(), "Path entry did not contain expected value");
        }

        String joinedPath = "/" + Arrays.stream(pids).map(p -> p.getId()).collect(Collectors.joining("/"));
        assertEquals(joinedPath, path.toIdPath());
    }
}
