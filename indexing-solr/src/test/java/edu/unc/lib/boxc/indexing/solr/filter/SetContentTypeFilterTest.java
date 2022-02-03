/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.boxc.indexing.solr.filter;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPackageDataLoader;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.FolderObject;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.facets.CutoffFacet;
import edu.unc.lib.boxc.search.api.requests.SearchRequest;
import edu.unc.lib.boxc.search.solr.facets.CutoffFacetImpl;
import edu.unc.lib.boxc.search.solr.models.ContentObjectSolrRecord;
import edu.unc.lib.boxc.search.solr.models.IndexDocumentBean;
import edu.unc.lib.boxc.search.solr.responses.SearchResultResponse;
import edu.unc.lib.boxc.search.solr.services.SolrSearchService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 *
 * @author harring
 *
 */
public class SetContentTypeFilterTest {

    private DocumentIndexingPackage dip;
    private PID pid;
    @Mock
    private FileObject fileObj;
    @Mock
    private WorkObject workObj;
    @Mock
    private BinaryObject binObj;
    @Mock
    private FolderObject folderObj;
    private IndexDocumentBean idb;
    @Captor
    private ArgumentCaptor<List<String>> listCaptor;
    @Mock
    private SolrSearchService solrSearchService;
    @Mock
    private DocumentIndexingPackageDataLoader documentIndexingPackageDataLoader;
    private CutoffFacet ancestorPath;
    @Mock
    private SearchResultResponse searchResultResponse;

    private SetContentTypeFilter filter;

    @Before
    public void setup() throws Exception {
        initMocks(this);

        pid = PIDs.get(UUID.randomUUID().toString());
        dip = new DocumentIndexingPackage(pid, null, documentIndexingPackageDataLoader);
        dip.setPid(pid);
        idb = dip.getDocument();

        when(fileObj.getOriginalFile()).thenReturn(binObj);
        ancestorPath = new CutoffFacetImpl(SearchFieldKey.ANCESTOR_PATH.name(), Arrays.asList(
                "1,1ed05130-d25f-4890-9086-02d98625275f", "2,5aa1ad67-c494-48dc-839e-241826559abb"), 0);
        when(solrSearchService.getSearchResults(any(SearchRequest.class))).thenReturn(searchResultResponse);
        when(solrSearchService.getAncestorPath(pid.getId(), null)).thenReturn(ancestorPath);

        filter = new SetContentTypeFilter();
        filter.setSolrSearchService(solrSearchService);
    }

    @Test
    public void testGetContentTypeFromWorkObjectWithPrimary() throws Exception {
        dip.setContentObject(workObj);
        when(workObj.getPrimaryObject()).thenReturn(fileObj);

        var fileRec = new ContentObjectSolrRecord();
        fileRec.setContentType(Arrays.asList("^text,Text", "/text^xml,xml"));
        when(searchResultResponse.getResultList()).thenReturn(Collections.singletonList(fileRec));

        filter.filter(dip);

        assertEquals("^text,Text", idb.getContentType().get(0));
        assertEquals("/text^xml,xml", idb.getContentType().get(1));
        assertEquals(2, idb.getContentType().size());
    }

    @Test
    public void testGetContentTypeFromFileObject() throws Exception {
        mockFile("data.csv", "ext.csv");

        filter.filter(dip);

        assertEquals("^dataset,Dataset", idb.getContentType().get(0));
        assertEquals("/dataset^csv,csv", idb.getContentType().get(1));
        assertEquals(2, idb.getContentType().size());
    }

    @Test
    public void testExtensionNotFoundInMapping() throws Exception {
        // use filename with raw image extension not found in our mapping
        mockFile("image.x3f", "some_wacky_type");

        filter.filter(dip);

        assertEquals("^unknown,Unknown", idb.getContentType().get(0));
        assertEquals("/unknown^x3f,x3f", idb.getContentType().get(1));
        assertEquals(2, idb.getContentType().size());
    }

    @Test
    public void testNotWorkAndNotFileObject() throws Exception {
        dip.setContentObject(folderObj);

        filter.filter(dip);

        assertNull(idb.getContentType());
    }

    @Test
    public void testWorkWithoutPrimaryObject() throws Exception {
        dip.setContentObject(workObj);

        var fileRec = new ContentObjectSolrRecord();
        fileRec.setContentType(Arrays.asList("^text,Text", "/text^xml,xml"));
        when(searchResultResponse.getResultList()).thenReturn(Collections.singletonList(fileRec));

        filter.filter(dip);

        assertEquals("^text,Text", idb.getContentType().get(0));
        assertEquals("/text^xml,xml", idb.getContentType().get(1));
        assertEquals(2, idb.getContentType().size());
    }

    @Test
    public void testGetPlainTextContentType() throws Exception {
        mockFile("file.txt", "text/plain");

        filter.filter(dip);

        assertEquals("^text,Text", idb.getContentType().get(0));
        assertEquals("/text^txt,txt", idb.getContentType().get(1));
        assertEquals(2, idb.getContentType().size());
    }

    @Test
    public void testAppleDoublePdf() throws Exception {
        mockFile("._doc.pdf", "multipart/appledouble");

        filter.filter(dip);

        assertEquals("^unknown,Unknown", idb.getContentType().get(0));
        assertEquals("/unknown^pdf,pdf", idb.getContentType().get(1));
        assertEquals(2, idb.getContentType().size());
    }

    @Test
    public void testImageJpg() throws Exception {
        mockFile("picture.jpg", "image/jpg");

        filter.filter(dip);

        assertEquals("^image,Image", idb.getContentType().get(0));
        assertEquals("/image^jpg,jpg", idb.getContentType().get(1));
        assertEquals(2, idb.getContentType().size());
    }

    @Test
    public void testVideoMp4() throws Exception {
        mockFile("my_video", "video/mp4");

        filter.filter(dip);

        assertEquals("^video,Video", idb.getContentType().get(0));
        assertEquals("/video^mp4,mp4", idb.getContentType().get(1));
        assertEquals(2, idb.getContentType().size());
    }

    @Test
    public void testAudioWav() throws Exception {
        mockFile("sound_file.wav", "audio/wav");

        filter.filter(dip);

        assertEquals("^audio,Audio", idb.getContentType().get(0));
        assertEquals("/audio^wav,wav", idb.getContentType().get(1));
        assertEquals(2, idb.getContentType().size());
    }

    @Test
    public void testNoMimetype() throws Exception {
        mockFile("unidentified.stuff", null);

        filter.filter(dip);

        assertEquals("^unknown,Unknown", idb.getContentType().get(0));
        assertEquals("/unknown^stuff,stuff", idb.getContentType().get(1));
        assertEquals(2, idb.getContentType().size());
    }

    @Test
    public void testExtensionTooLongFallbackToMimetype() throws Exception {
        mockFile("unidentified.superlongextension", "text/plain");

        filter.filter(dip);

        assertEquals("^text,Text", idb.getContentType().get(0));
        assertEquals("/text^txt,txt", idb.getContentType().get(1));
        assertEquals(2, idb.getContentType().size());
    }

    @Test
    public void testExtensionTooLongNoFallback() throws Exception {
        mockFile("unidentified.superlongextension", "application/boxc-stuff");

        filter.filter(dip);

        assertEquals("^unknown,Unknown", idb.getContentType().get(0));
        assertEquals("/unknown^unknown,unknown", idb.getContentType().get(1));
        assertEquals(2, idb.getContentType().size());
    }

    @Test
    public void testNoExtensionNoMimetype() throws Exception {
        mockFile("unidentified", null);

        filter.filter(dip);

        assertEquals("^unknown,Unknown", idb.getContentType().get(0));
        assertEquals("/unknown^unknown,unknown", idb.getContentType().get(1));
        assertEquals(2, idb.getContentType().size());
    }

    @Test
    public void testInvalidExtensionFallbackToMimetype() throws Exception {
        mockFile("unidentified.20210401", "text/plain");

        filter.filter(dip);

        assertEquals("^text,Text", idb.getContentType().get(0));
        assertEquals("/text^txt,txt", idb.getContentType().get(1));
        assertEquals(2, idb.getContentType().size());
    }

    @Test
    public void testWorkWithNoFiles() throws Exception {
        dip.setContentObject(workObj);

        when(searchResultResponse.getResultList()).thenReturn(Collections.emptyList());

        filter.filter(dip);

        assertTrue(idb.getContentType().isEmpty());
    }

    @Test
    public void testWorkWithMultipleFileTypes() throws Exception {
        dip.setContentObject(workObj);

        var fileRec1 = new ContentObjectSolrRecord();
        fileRec1.setContentType(Arrays.asList("^text,Text", "/text^xml,xml"));
        var fileRec2 = new ContentObjectSolrRecord();
        fileRec2.setContentType(Arrays.asList("^text,Text", "/text^plain,txt"));
        var fileRec3 = new ContentObjectSolrRecord();
        fileRec3.setContentType(Arrays.asList("^text,Text", "/text^plain,txt"));
        var fileRec4 = new ContentObjectSolrRecord();
        fileRec4.setContentType(Arrays.asList("^audio,Audio", "/audio^wav,wav"));
        when(searchResultResponse.getResultList()).thenReturn(Arrays.asList(
                fileRec1, fileRec2, fileRec3, fileRec4));

        filter.filter(dip);

        var cTypes = idb.getContentType();
        assertTrue(cTypes.contains("^text,Text"));
        assertTrue(cTypes.contains("/text^xml,xml"));
        assertTrue(cTypes.contains("/text^plain,txt"));
        assertTrue(cTypes.contains("^audio,Audio"));
        assertTrue(cTypes.contains("/audio^wav,wav"));
        assertEquals(5, idb.getContentType().size());
    }

    @Test
    public void testWorkInPipelineAfterAncestorPathSet() throws Exception {
        dip.setContentObject(workObj);
        idb.setAncestorPath(Arrays.asList("2," + pid.getId()));

        var fileRec = new ContentObjectSolrRecord();
        fileRec.setContentType(Arrays.asList("^text,Text", "/text^xml,xml"));
        when(searchResultResponse.getResultList()).thenReturn(Collections.singletonList(fileRec));

        filter.filter(dip);

        assertEquals("^text,Text", idb.getContentType().get(0));
        assertEquals("/text^xml,xml", idb.getContentType().get(1));
        assertEquals(2, idb.getContentType().size());
        verify(solrSearchService, never()).getAncestorPath(anyString(), any(AccessGroupSet.class));
    }

    private void mockFile(String filename, String mimetype) {
        when(dip.getContentObject()).thenReturn(fileObj);
        when(fileObj.getOriginalFile()).thenReturn(binObj);
        when(binObj.getFilename()).thenReturn(filename);
        when(binObj.getMimetype()).thenReturn(mimetype);
    }
}
