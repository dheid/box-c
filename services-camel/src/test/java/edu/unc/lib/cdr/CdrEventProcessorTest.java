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
package edu.unc.lib.cdr;

import static edu.unc.lib.cdr.headers.CdrFcrepoHeaders.CdrSolrUpdateAction;
import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.ATOM_NS;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.jdom2.Document;
import org.jdom2.Element;
import org.joda.time.DateTimeUtils;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import edu.unc.lib.dl.util.JMSMessageUtil.CDRActions;

/**
 *
 * @author lfarrell
 *
 */
public class CdrEventProcessorTest {
    private CdrEventProcessor processor;
    private String actionType = CDRActions.INDEX.getName();

    @Mock
    private Exchange exchange;

    @Mock
    private Message message;

    @Before
    public void init() throws Exception {
        initMocks(this);

        processor = new CdrEventProcessor();

        when(exchange.getIn())
            .thenReturn(message);
    }

    @Test
    public void testCreateSolrActionHeader() throws Exception {
        Document msg = new Document();
        createAtomEntry(msg, actionType);

        when(message.getBody())
            .thenReturn(msg);

        processor.process(exchange);

        verify(message).setHeader(CdrSolrUpdateAction, actionType);
    }

    @Test
    public void testCreateSolrNoActionHeader() throws Exception {
        Document msg = new Document();
        createAtomEntry(msg, null);

        when(message.getBody())
            .thenReturn(msg);

        processor.process(exchange);

        verify(message).setHeader(CdrSolrUpdateAction, "none");
    }

    private Element createAtomEntry(Document msg, String operation) {
        Element entry = new Element("entry", ATOM_NS);
        msg.addContent(entry);
        DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
        String timestamp = fmt.print(DateTimeUtils.currentTimeMillis());
        entry.addContent(new Element("updated", ATOM_NS).setText(timestamp));

        if (operation != null) {
            entry.addContent(new Element("title", ATOM_NS).setText(operation).setAttribute("type", "text"));
        }

        Element content = new Element("content", ATOM_NS).setAttribute("type", "text/xml");
        entry.addContent(content);

        return content;
    }
}