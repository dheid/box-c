package edu.unc.lib.dl.security.objecthandlers;

import java.io.IOException;

import java.net.URI;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sun.xacml.attr.AnyURIAttribute;
import com.sun.xacml.attr.AttributeValue;
import com.sun.xacml.attr.StringAttribute;
import com.sun.xacml.ctx.RequestCtx;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.fcrepo.common.Constants;

import org.fcrepo.server.security.xacml.pep.PEPException;
import org.fcrepo.server.security.xacml.pep.rest.filters.AbstractFilter;
import org.fcrepo.server.security.xacml.util.LogUtil;


/**
 * Handles REST API method addRelationship
 *
 * @author Stephen Bayliss
 * @version $Id$
 */
public class PurgeRelationship
        extends AbstractFilter {
    private static final Logger logger =
        LoggerFactory.getLogger(PurgeRelationship.class);

    public PurgeRelationship()
    throws PEPException {
        super();
    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.server.security.xacml.pep.rest.filters.RESTFilter#handleRequest(javax.servlet
     * .http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    public RequestCtx handleRequest(HttpServletRequest request,
                                    HttpServletResponse response)
            throws IOException, ServletException {
        if (logger.isDebugEnabled()) {
            logger.debug(this.getClass().getName() + "/handleRequest!");
        }

        String path = request.getPathInfo();
        String[] parts = path.split("/");

        String pid = parts[1];

        RequestCtx req = null;
        Map<URI, AttributeValue> actions = new HashMap<URI, AttributeValue>();
        Map<URI, AttributeValue> resAttr = new HashMap<URI, AttributeValue>();
        try {
            if (pid != null && !"".equals(pid)) {
                resAttr.put(Constants.OBJECT.PID.getURI(),
                            new StringAttribute(pid));
            }
            if (pid != null && !"".equals(pid)) {
                resAttr.put(new URI(XACML_RESOURCE_ID),
                            new AnyURIAttribute(new URI(pid)));
            }
            
            // adding predicates to the evaluation context
            String predicate = request.getParameter("relationship");
            if (predicate != null && !"".equals(predicate)) {
            	resAttr.put(new URI(AddRelationship.RESOURCE_RELATIONSHIP_PREDICATE), new AnyURIAttribute(new URI(predicate)));
            }

            actions.put(Constants.ACTION.ID.getURI(),
                        new StringAttribute(Constants.ACTION.PURGE_RELATIONSHIP
                                .getURI().toASCIIString()));
            actions.put(Constants.ACTION.API.getURI(),
                        new StringAttribute(Constants.ACTION.APIM.getURI()
                                .toASCIIString()));

            req =
                    getContextHandler().buildRequest(getSubjects(request),
                                                     actions,
                                                     resAttr,
                                                     getEnvironment(request));

            LogUtil.statLog(request.getRemoteUser(),
                            Constants.ACTION.PURGE_RELATIONSHIP.getURI()
                                    .toASCIIString(),
                            pid,
                            null);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ServletException(e.getMessage(), e);
        }

        return req;
    }

}
