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
package edu.unc.lib.dl.cdr.services.processing;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.cdr.services.rest.modify.ExportXMLController.XMLExportRequest;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.search.solr.service.SearchStateFactory;
import edu.unc.lib.dl.ui.service.SolrQueryLayerService;

/**
 *
 * @author harring
 *
 */
public class XMLExportServiceTest {

    @Mock
    private AccessControlService aclService;
    @Mock
    private SearchStateFactory searchStateFactory;
    @Mock
    private SolrQueryLayerService queryLayer;
    @Mock
    private XMLExportRequest request;
    @Mock
    private AccessGroupSet group;
    @Mock
    private XMLExportJob job;

    private String username;
    private List<String> pids;
    private XMLExportService service;

    @Before
    public void init() {
        initMocks(this);

        username = "user";
        pids = new ArrayList<>();
        pids.add(PIDs.get(UUID.randomUUID().toString()).toString());
        pids.add(PIDs.get(UUID.randomUUID().toString()).toString());
        service = new XMLExportService();
    }

    @Test
    public void exportXMLTest() {
        when(request.exportChildren()).thenReturn(false);
        when(request.getPids()).thenReturn(pids);

        Map<String,String> response = service.exportXml(username, group, request);

        assertEquals("Metadata export for " + request.getPids().size()
                + " objects has begun, you will receive the data via email soon", response.get("message"));
    }

}
