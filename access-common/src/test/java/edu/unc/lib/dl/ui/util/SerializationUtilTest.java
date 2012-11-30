package edu.unc.lib.dl.ui.util;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import edu.unc.lib.dl.search.solr.model.BriefObjectMetadataBean;

public class SerializationUtilTest extends Assert {

	@Test
	public void briefMetadataToJSONTest() {
		BriefObjectMetadataBean md = new BriefObjectMetadataBean();
		md.setId("uuid:test");
		md.setTitle("Test Item");
		md.setDatastream(Arrays.asList("DATA_FILE|image/jpeg|orig|582753|]"));

		System.out.print(SerializationUtil.objectToJSON(md));
	}

	@Test
	public void briefMetadataListToJSONTest() {
		BriefObjectMetadataBean md = new BriefObjectMetadataBean();
		md.setId("uuid:test");
		md.setTitle("Test Item");
		md.setDatastream(Arrays.asList("DATA_FILE|image/jpeg|orig|582753|]"));

		BriefObjectMetadataBean md2 = new BriefObjectMetadataBean();
		md2.setId("uuid:test2");
		md2.setTitle("Test Item 2");
		md2.setDatastream(Arrays.asList("DATA_FILE|application/msword|orig|596318|",
				"NORM_FILE|application/pdf|deriv|290733|", "MD_TECHNICAL|text/xml|admin|6406|",
				"AUDIT|text/xml|admin|405|", "RELS-EXT|text/xml|admin|1042|", "MD_DESCRIPTIVE|text/xml|meta|2301|",
				"DC|text/xml|meta|809|", "MD_EVENTS|text/xml|meta|5063|"));

		List<BriefObjectMetadataBean> mdList = Arrays.asList(md, md2);

		assertTrue(SerializationUtil.objectToJSON(mdList).length() > 2);
	}
}
