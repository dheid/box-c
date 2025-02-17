package edu.unc.lib.boxc.web.common.services;

import static edu.unc.lib.boxc.auth.api.Permission.editDescription;
import static edu.unc.lib.boxc.auth.api.Permission.viewAccessCopies;
import static edu.unc.lib.boxc.auth.api.Permission.viewMetadata;
import static edu.unc.lib.boxc.auth.api.Permission.viewOriginal;
import static edu.unc.lib.boxc.model.api.DatastreamType.JP2_ACCESS_COPY;
import static edu.unc.lib.boxc.model.api.DatastreamType.ORIGINAL_FILE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.search.solr.models.ContentObjectSolrRecord;

/**
 *
 * @author bbpennel
 *
 */
public class PermissionsHelperTest {

    private PermissionsHelper helper;

    private ContentObjectSolrRecord mdObject;

    private List<String> roleGroups;

    private AccessGroupSet principals;

    private AutoCloseable closeable;

    @Mock
    private AccessControlService accessControlService;

    @BeforeEach
    public void init() {
        closeable = openMocks(this);

        roleGroups = new ArrayList<>();

        mdObject = new ContentObjectSolrRecord();
        mdObject.setId("9ef8d1c5-14a1-4ed3-b0c0-6da67fa5f6d1");
        mdObject.setRoleGroup(roleGroups);
        List<String> datastreams = Arrays.asList(
                ORIGINAL_FILE.getId() + "|application/pdf|file.pdf|pdf|766|urn:sha1:checksum|",
                JP2_ACCESS_COPY.getId() + "|application/jp2|file.jp2|jp2|884||");
        mdObject.setDatastream(datastreams);

        principals = new AccessGroupSetImpl("group");

        helper = new PermissionsHelper();
        helper.setAccessControlService(accessControlService);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void testAllowsPublicAccess() {
        roleGroups.add("canViewOriginals|everyone");

        assertTrue(helper.allowsPublicAccess(mdObject), "Failed to determine object has full patron access");
    }

    @Test
    public void testDoesNotAllowPublicAccess() {
        roleGroups.add("canViewMetadata|everyone");

        assertFalse(helper.allowsPublicAccess(mdObject), "Object must not have full patron access");
    }

    @Test
    public void testAllowsPublicAccessNoRoleGroups() {
        mdObject.setRoleGroup(null);

        assertFalse(helper.allowsPublicAccess(mdObject), "Object must not have full patron access");
    }

    @Test
    public void testPermitOriginalAccess() {
        assignPermission(viewOriginal, true);

        assertTrue(helper.hasDatastreamAccess(principals, ORIGINAL_FILE, mdObject));
    }

    @Test
    public void testPermitDerivativeAccess() {
        assignPermission(viewAccessCopies, true);

        assertTrue(helper.hasDatastreamAccess(principals, JP2_ACCESS_COPY, mdObject));
    }

    @Test
    public void testDenyOriginalAccess() {
        assignPermission(viewOriginal, false);

        assertFalse(helper.hasDatastreamAccess(principals, ORIGINAL_FILE, mdObject));
    }

    @Test
    public void testHasEditAccess() {
        assignPermission(editDescription, true);

        assertTrue(helper.hasEditAccess(principals, mdObject));
    }

    @Test
    public void testDoesNotHaveEditAccess() {
        assignPermission(editDescription, false);

        assertFalse(helper.hasEditAccess(principals, mdObject));
    }

    @Test
    public void testHasEditAccessNoPrincipals() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            assignPermission(editDescription, true);

            assertTrue(helper.hasEditAccess(null, mdObject));
        });
    }

    @Test
    public void testHasEnhancedAccessNoGroups() {
        assertFalse(helper.allowsFullAuthenticatedAccess(mdObject));
    }

    @Test
    public void testHasEnhancedAccessIfLoggedIn() {
        assignPermission(viewMetadata, true);
        roleGroups.add("canViewOriginals|authenticated");
        assertTrue(helper.allowsFullAuthenticatedAccess(mdObject));
    }

    @Test
    public void testDoesNotHaveEnhancedIfLoggedIn() {
        assignPermission(viewMetadata, true);
        roleGroups.add("canViewMetadata|authenticated");
        assertFalse(helper.allowsFullAuthenticatedAccess(mdObject));
    }

    private void assignPermission(Permission permission, boolean value) {
        when(accessControlService.hasAccess(any(PID.class), eq(principals), eq(permission))).thenReturn(value);
    }
}
