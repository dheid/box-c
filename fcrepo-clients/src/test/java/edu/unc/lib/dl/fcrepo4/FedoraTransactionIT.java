/**
 * Copyright 2017 The University of North Carolina at Chapel Hill
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
package edu.unc.lib.dl.fcrepo4;

import static org.junit.Assert.*;

import java.net.URI;

import org.apache.http.client.methods.HttpRequestBase;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoResponse;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.DcElements;
import edu.unc.lib.dl.rdf.PcdmModels;
import edu.unc.lib.dl.util.URIUtil;

/**
 * 
 * @author harring
 *
 */
public class FedoraTransactionIT extends AbstractFedoraIT {
	
	private PID pid;
	private Model model;
	
	@Mock
	private HttpRequestBase request;

	@Before
	public void init() {
		MockitoAnnotations.initMocks(this);
		pid = repository.mintContentPid();
		model = ModelFactory.createDefaultModel();
		Resource resc = model.createResource(pid.getRepositoryPath());
		resc.addProperty(DcElements.title, "Folder Title");
		
	}
	
	//@Test
	public void createTxTest() throws Exception {
		FedoraTransaction tx = repository.startTransaction();
		
		FolderObject obj = repository.createFolderObject(pid, model);

		assertTrue(FedoraTransaction.hasTxId());
		assertTrue(obj.getTypes().contains(Cdr.Folder.getURI()));
		assertTrue(obj.getTypes().contains(PcdmModels.Object.getURI()));
		assertEquals("Folder Title", obj.getResource()
				.getProperty(DcElements.title).getString());
		tx.close();
		assertFalse(FedoraTransaction.hasTxId());
		assertNull(tx.getTxUri());
	}
	
	//@Test (expected = TransactionCancelledException.class)
	public void createRollbackTxTest() {
		FedoraTransaction tx = repository.startTransaction();
		repository.createFolderObject(pid, model);
		tx.cancel();
	}
	
	//@Test
	public void nestedTxTest() throws Exception {
		FedoraTransaction parentTx = repository.startTransaction();
		repository.createFolderObject(pid, model);
		
		FedoraTransaction subTx = repository.startTransaction();
		PID workPid = repository.mintContentPid();
		repository.createWorkObject(workPid);
		subTx.close();
		
		assertNull(subTx.getTxUri());
		assertNotNull((parentTx.getTxUri()));
		assertTrue(FedoraTransaction.isStillAlive());
		
		parentTx.close();
		assertNull(parentTx.getTxUri());
		assertFalse(FedoraTransaction.isStillAlive());
	}
	
	@Test
	public void cannotAccessObjectOutsideTxTest() throws Exception {
		FedoraTransaction tx = repository.startTransaction();
		FolderObject folder = repository.createFolderObject(pid);
		URI txContentUri = URI.create(URIUtil.join(tx.getTxUri(), pid.toString()));
		client.put(txContentUri).perform();
		
		FcrepoClient nonTxClient = FcrepoClientFactory.makeClient();
		FcrepoResponse response = nonTxClient.get(folder.getUri()).perform();
		assertEquals(404, response.getStatusCode());
		tx.close();
	}

}
