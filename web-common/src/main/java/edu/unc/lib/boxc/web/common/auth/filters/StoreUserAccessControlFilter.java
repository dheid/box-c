package edu.unc.lib.boxc.web.common.auth.filters;

import static edu.unc.lib.boxc.web.common.auth.HttpAuthHeaders.FORWARDED_MAIL_HEADER;
import static edu.unc.lib.boxc.web.common.auth.RemoteUserUtil.getRemoteUser;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.filter.OncePerRequestFilter;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import edu.unc.lib.boxc.web.common.auth.HttpAuthHeaders;
import edu.unc.lib.boxc.web.common.auth.PatronPrincipalProvider;

/**
 * Filter which retrieves the users shibboleth and grouper session information in order to construct their profile as
 * needed.
 *
 * @author bbpennel
 *
 */
public class StoreUserAccessControlFilter extends OncePerRequestFilter implements ServletContextAware {
    private static final Logger log = LoggerFactory.getLogger(StoreUserAccessControlFilter.class);

    protected static String FORWARDING_ROLE = "group-forwarding";

    private boolean retainGroupsThreadStore;

    private PatronPrincipalProvider patronPrincipalProvider;

    @Override
    public void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws IOException,
            ServletException {
        log.debug("In StoreUserAccessControlFilter");
        // Skip processing static content
        if (!req.getServletPath().startsWith("/static/")) {
            storeUserGroupData(req);
        }
        try {
            chain.doFilter(req, res);
        } finally {
            // Clear out group store no matter what happens
            if (!retainGroupsThreadStore) {
                GroupsThreadStore.clearStore();
            }
        }
        res.addHeader("username", getRemoteUser(req));
    }

    protected void storeUserGroupData(HttpServletRequest request) {
        try {
            GroupsThreadStore.storeUsername(getRemoteUser(request));

            String email = getEmailAddress(request);
            GroupsThreadStore.storeEmail(email);

            AccessGroupSet accessGroups = getUserGroups(request);
            GroupsThreadStore.storeGroups(accessGroups);

            AgentPrincipals principals = GroupsThreadStore.getAgentPrincipals();
            request.setAttribute("accessGroupSet", principals.getPrincipals());

            if (log.isDebugEnabled()) {
                log.debug("Setting cdr groups for request processing thread: {}",
                        GroupsThreadStore.getGroupString());
            }
        } catch (Exception e) {
            log.debug("Error while retrieving the users profile", e);
        }
    }

    protected AccessGroupSet getUserGroups(HttpServletRequest request) {
        if (request.isUserInRole(FORWARDING_ROLE)) {
            return this.getForwardedGroups(request);
        } else {
            return this.getGrouperGroups(request);
        }
    }

    protected AccessGroupSet getForwardedGroups(HttpServletRequest request) {
        String forwardedGroups = request.getHeader(HttpAuthHeaders.FORWARDED_GROUPS_HEADER);
        if (log.isDebugEnabled()) {
            log.debug("Forwarding user {} logged in with forwarded groups {}",
                    GroupsThreadStore.getUsername(), forwardedGroups);
        }
        if (forwardedGroups == null) {
            return new AccessGroupSetImpl();
        }

        if (forwardedGroups.trim().length() > 0) {
            return new AccessGroupSetImpl(forwardedGroups);
        }
        return new AccessGroupSetImpl();
    }

    protected AccessGroupSet getGrouperGroups(HttpServletRequest request) {
        String shibGroups = request.getHeader(HttpAuthHeaders.SHIBBOLETH_GROUPS_HEADER);
        AccessGroupSet accessGroups = null;
        String userName = GroupsThreadStore.getUsername();
        if (log.isDebugEnabled()) {
            log.debug("Normal user " + userName + " logged in with groups " + shibGroups);
        }

        if (shibGroups == null || shibGroups.trim().length() == 0) {
            accessGroups = new AccessGroupSetImpl();
        } else {
            accessGroups = new AccessGroupSetImpl(shibGroups);
        }

        // Add all patron principals to group set
        accessGroups.addAll(patronPrincipalProvider.getPrincipals(request));

        return accessGroups;
    }

    private String getEmailAddress(HttpServletRequest request) {
        String email = request.getHeader("mail");
        if (email == null && request.isUserInRole(FORWARDING_ROLE)) {
            email = request.getHeader(FORWARDED_MAIL_HEADER);
        }
        if (email != null) {
            if (email.endsWith("_UNC")) {
                email = email.substring(0, email.length() - 4);
            }
        }
        return email;
    }

    /**
     * @param retainGroupsThreadStore the retainGroupsThreadStore to set
     */
    public void setRetainGroupsThreadStore(boolean retainGroupsThreadStore) {
        this.retainGroupsThreadStore = retainGroupsThreadStore;
    }

    public void setPatronPrincipalProvider(PatronPrincipalProvider patronPrincipalProvider) {
        this.patronPrincipalProvider = patronPrincipalProvider;
    }
}
