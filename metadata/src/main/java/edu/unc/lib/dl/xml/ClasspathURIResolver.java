package edu.unc.lib.dl.xml;

import java.io.InputStream;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClasspathURIResolver implements URIResolver {
	private static Logger log = LoggerFactory.getLogger(ClasspathURIResolver.class);
	
	@Override
	public Source resolve(String href, String base) throws TransformerException {
		log.debug("href=" + href + " : base=" + base);
		String fileName = null;
		try {
			int k = href.lastIndexOf('/') + 1;
			if (k > 0) {
				fileName = href.substring(k);
			} else {
				fileName = href;
			}
			InputStream input = ClassLoader.getSystemResourceAsStream(fileName);
			if (input != null) {
				return (new StreamSource(input));
			}
		} catch (Exception ie) {
			log.error("File " + fileName + " not found!", ie);
		}

		// 	instruct the caller to use the default lookup
		return (null);
	} 

}
