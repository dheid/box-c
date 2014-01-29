package edu.unc.lib.dl.data.ingest.solr.action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.data.ingest.solr.ChildSetRequest;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.fedora.PID;

/**
 * Updates the path and inherited properties of one or more objects sharing the same parent container
 * 
 * @author bbpennel
 * 
 */
public class MoveObjectsAction extends UpdateChildSetAction {
	private static final Logger log = LoggerFactory.getLogger(MoveObjectsAction.class);

	public MoveObjectsAction() {
		this.addDocumentMode = false;
	}
	
	@Override
	protected DocumentIndexingPackage getParentDIP(ChildSetRequest childSetRequest) {
		// Store the MD_CONTENTS of the parents so new children can be correctly located
		DocumentIndexingPackage dip = dipFactory.createDocumentIndexingPackageWithMDContents(childSetRequest.getPid());
		// Process the parent to get its inheritable properties
		this.pipeline.process(dip);
		return dip;
	}

	@Override
	public DocumentIndexingPackage getDocumentIndexingPackage(PID pid, DocumentIndexingPackage parent) {
		DocumentIndexingPackage dip = new DocumentIndexingPackage(pid);
		dip.setParentDocument(parent);
		// For the top level children that were just moved we need to check for display order
		if (parent.getMdContents() != null) {
			log.debug("Updating display order for top level moved object {}", pid.getPid());
			dip.getDocument().setDisplayOrder(parent.getDisplayOrder(pid.getPid()));
		}
		return dip;
	}
}
