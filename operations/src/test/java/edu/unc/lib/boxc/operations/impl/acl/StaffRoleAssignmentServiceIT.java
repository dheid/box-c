package edu.unc.lib.boxc.operations.impl.acl;

import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.AUTHENTICATED_PRINC;
import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.PUBLIC_PRINC;
import static edu.unc.lib.boxc.auth.api.UserRole.canAccess;
import static edu.unc.lib.boxc.auth.api.UserRole.canManage;
import static edu.unc.lib.boxc.auth.api.UserRole.canViewOriginals;
import static edu.unc.lib.boxc.auth.api.UserRole.unitOwner;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.openMocks;

import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.UserRole;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.exceptions.InvalidAssignmentException;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.models.RoleAssignment;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.auth.fcrepo.services.InheritedAclFactory;
import edu.unc.lib.boxc.fcrepo.exceptions.ServiceException;
import edu.unc.lib.boxc.fcrepo.utils.TransactionManager;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.ids.PIDMinter;
import edu.unc.lib.boxc.model.api.objects.AdminUnit;
import edu.unc.lib.boxc.model.api.objects.CollectionObject;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.objects.ContentRootObject;
import edu.unc.lib.boxc.model.api.objects.FolderObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.rdf.CdrAcl;
import edu.unc.lib.boxc.model.api.rdf.Premis;
import edu.unc.lib.boxc.model.api.rdf.Prov;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.AgentPids;
import edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths;
import edu.unc.lib.boxc.model.fcrepo.services.RepositoryInitializer;
import edu.unc.lib.boxc.model.fcrepo.test.AclModelBuilder;
import edu.unc.lib.boxc.model.fcrepo.test.RepositoryObjectTreeIndexer;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import edu.unc.lib.boxc.operations.api.events.PremisLoggerFactory;
import edu.unc.lib.boxc.operations.jms.OperationsMessageSender;
import edu.unc.lib.boxc.operations.jms.JMSMessageUtil.CDRActions;

/**
 *
 * @author bbpennel
 *
 */
@ExtendWith({SpringExtension.class})
@ContextHierarchy({
    @ContextConfiguration("/spring-test/test-fedora-container.xml"),
    @ContextConfiguration("/spring-test/cdr-client-container.xml"),
    @ContextConfiguration("/spring-test/staff-role-service-container.xml"),
})
public class StaffRoleAssignmentServiceIT {

    private static final String USER_PRINC = "user";
    private static final String GRP_PRINC = "group";
    private static final Calendar TOMORROW = GregorianCalendar.from(ZonedDateTime.now().plusDays(1));

    private AutoCloseable closeable;

    @Autowired
    private String baseAddress;
    @Mock
    private AccessControlService aclService;
    @Autowired
    private RepositoryObjectFactory repoObjFactory;
    @Autowired
    private RepositoryObjectLoader repoObjLoader;
    @Autowired
    private InheritedAclFactory aclFactory;
    @Autowired
    private PIDMinter pidMinter;
    @Mock
    private OperationsMessageSender operationsMessageSender;
    @Autowired
    private RepositoryObjectTreeIndexer treeIndexer;
    @Autowired
    private TransactionManager txManager;
    @Autowired
    private RepositoryInitializer repoInitializer;
    @Autowired
    private PremisLoggerFactory premisLoggerFactory;
    @Captor
    private ArgumentCaptor<List<PID>> pidListCaptor;

    private AgentPrincipals agent;
    private AccessGroupSet groups;

    private StaffRoleAssignmentService roleService;

    private ContentRootObject contentRoot;

    @BeforeEach
    public void init() throws Exception {
        closeable = openMocks(this);
        TestHelper.setContentBase(baseAddress);

        groups = new AccessGroupSetImpl(GRP_PRINC);
        agent = new AgentPrincipalsImpl(USER_PRINC, groups);

        roleService = new StaffRoleAssignmentService();
        roleService.setAclFactory(aclFactory);
        roleService.setAclService(aclService);
        roleService.setOperationsMessageSender(operationsMessageSender);
        roleService.setRepositoryObjectLoader(repoObjLoader);
        roleService.setRepositoryObjectFactory(repoObjFactory);
        roleService.setTransactionManager(txManager);
        roleService.setPremisLoggerFactory(premisLoggerFactory);

        PID contentRootPid = RepositoryPaths.getContentRootPid();
        repoInitializer.initializeRepository();
        contentRoot = repoObjLoader.getContentRootObject(contentRootPid);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void testInsufficientPermissions() throws Exception {
        Assertions.assertThrows(AccessRestrictionException.class, () -> {
            PID pid = pidMinter.mintContentPid();

            doThrow(new AccessRestrictionException()).when(aclService)
                    .assertHasAccess(anyString(), eq(pid), any(AccessGroupSetImpl.class), eq(Permission.assignStaffRoles));

            Set<RoleAssignment> assignments = new HashSet<>(asList(
                    new RoleAssignment(USER_PRINC, canAccess)));

            roleService.updateRoles(agent, pid, assignments);
        });
    }

    @Test
    public void testNoAgent() throws Exception {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            PID pid = pidMinter.mintContentPid();
            AdminUnit unit = repoObjFactory.createAdminUnit(pid, null);
            contentRoot.addMember(unit);
            treeIndexer.indexAll(baseAddress);

            Set<RoleAssignment> assignments = new HashSet<>(asList(
                    new RoleAssignment(USER_PRINC, canAccess)));

            roleService.updateRoles(null, pid, assignments);
        });
    }

    @Test
    public void testEmptyAssignmentSet() throws Exception {
        PID pid = pidMinter.mintContentPid();
        AdminUnit unit = repoObjFactory.createAdminUnit(pid, null);
        contentRoot.addMember(unit);
        treeIndexer.indexAll(baseAddress);

        Set<RoleAssignment> assignments = new HashSet<>();

        roleService.updateRoles(agent, pid, assignments);

        AdminUnit updated = repoObjLoader.getAdminUnit(pid);

        assertNoStaffRoles(updated);

        assertMessageSent(pid);

        String eventDetail = assertEventCreatedAndGetDetail(updated);
        assertThat(eventDetail, containsString("No roles assigned"));
    }

    @Test
    public void testEmptyAssignmentForObjectWithRoles() throws Exception {
        PID pid = pidMinter.mintContentPid();
        AdminUnit unit = repoObjFactory.createAdminUnit(null);
        contentRoot.addMember(unit);
        CollectionObject coll = repoObjFactory.createCollectionObject(pid,
                new AclModelBuilder("Collection with existing acls")
                .addCanManage(GRP_PRINC)
                .addEmbargoUntil(TOMORROW)
                .model);
        unit.addMember(coll);
        treeIndexer.indexAll(baseAddress);

        Set<RoleAssignment> assignments = new HashSet<>();

        roleService.updateRoles(agent, pid, assignments);

        CollectionObject updated = repoObjLoader.getCollectionObject(pid);

        assertNoStaffRoles(updated);
        // Verify that non-staff role acl was not cleared
        assertEmbargoPresent(updated);

        assertMessageSent(pid);

        String eventDetail = assertEventCreatedAndGetDetail(updated);
        assertThat(eventDetail, containsString("No roles assigned"));
    }

    @Test
    public void testSetUnitOwnerForAdminUnit() throws Exception {
        PID pid = pidMinter.mintContentPid();
        AdminUnit unit = repoObjFactory.createAdminUnit(pid, null);
        contentRoot.addMember(unit);
        treeIndexer.indexAll(baseAddress);

        Set<RoleAssignment> assignments = new HashSet<>(asList(
                new RoleAssignment(USER_PRINC, unitOwner)));

        roleService.updateRoles(agent, pid, assignments);

        AdminUnit updated = repoObjLoader.getAdminUnit(pid);

        assertHasAssignment(USER_PRINC, unitOwner, updated);

        assertMessageSent(pid);

        String eventDetail = assertEventCreatedAndGetDetail(updated);
        assertThat(eventDetail, containsString(unitOwner.name() + ": " + USER_PRINC));
    }

    @Test
    public void testSetUnitOwnerForCollection() throws Exception {
        Assertions.assertThrows(InvalidAssignmentException.class, () -> {
            PID pid = pidMinter.mintContentPid();
            AdminUnit unit = repoObjFactory.createAdminUnit(null);
            contentRoot.addMember(unit);
            CollectionObject coll = repoObjFactory.createCollectionObject(pid, null);
            unit.addMember(coll);
            treeIndexer.indexAll(baseAddress);

            Set<RoleAssignment> assignments = new HashSet<>(asList(
                    new RoleAssignment(USER_PRINC, unitOwner)));

            roleService.updateRoles(agent, pid, assignments);
        });
    }

    @Test
    public void testSetCanManageForFolder() throws Exception {
        Assertions.assertThrows(InvalidAssignmentException.class, () -> {
            PID pid = pidMinter.mintContentPid();
            AdminUnit unit = repoObjFactory.createAdminUnit(null);
            contentRoot.addMember(unit);
            CollectionObject coll = repoObjFactory.createCollectionObject(null);
            unit.addMember(coll);
            FolderObject folder = repoObjFactory.createFolderObject(pid, null);
            coll.addMember(folder);
            treeIndexer.indexAll(baseAddress);

            Set<RoleAssignment> assignments = new HashSet<>(asList(
                    new RoleAssignment(USER_PRINC, canManage)));

            roleService.updateRoles(agent, pid, assignments);
        });
    }

    @Test
    public void testSetCanManageForWork() throws Exception {
        Assertions.assertThrows(InvalidAssignmentException.class, () -> {
            PID pid = pidMinter.mintContentPid();
            AdminUnit unit = repoObjFactory.createAdminUnit(null);
            contentRoot.addMember(unit);
            CollectionObject coll = repoObjFactory.createCollectionObject(null);
            unit.addMember(coll);
            WorkObject work = repoObjFactory.createWorkObject(pid, null);
            coll.addMember(work);
            treeIndexer.indexAll(baseAddress);

            Set<RoleAssignment> assignments = new HashSet<>(asList(
                    new RoleAssignment(USER_PRINC, canManage)));

            roleService.updateRoles(agent, pid, assignments);
        });
    }

    @Test
    public void testOverrideRoleAssignments() throws Exception {
        PID pid = pidMinter.mintContentPid();
        AdminUnit unit = repoObjFactory.createAdminUnit(pid,
                new AclModelBuilder("Admin Unit Replace")
                .addCanManage(GRP_PRINC)
                .model);
        contentRoot.addMember(unit);
        treeIndexer.indexAll(baseAddress);

        Set<RoleAssignment> assignments = new HashSet<>(asList(
                new RoleAssignment(USER_PRINC, canManage)));

        roleService.updateRoles(agent, pid, assignments);

        AdminUnit updated = repoObjLoader.getAdminUnit(pid);

        assertHasAssignment(USER_PRINC, canManage, updated);
        assertNoAssignment(GRP_PRINC, canManage, updated);

        assertMessageSent(pid);

        String eventDetail = assertEventCreatedAndGetDetail(updated);
        assertThat(eventDetail, containsString(canManage.name() + ": " + USER_PRINC));
    }

    @Test
    public void testPrincipalWithMultipleRoles() throws Exception {
        Assertions.assertThrows(InvalidAssignmentException.class, () -> {
            PID pid = pidMinter.mintContentPid();
            AdminUnit unit = repoObjFactory.createAdminUnit(null);
            contentRoot.addMember(unit);
            CollectionObject coll = repoObjFactory.createCollectionObject(pid, null);
            unit.addMember(coll);
            treeIndexer.indexAll(baseAddress);

            Set<RoleAssignment> assignments = new HashSet<>(asList(
                    new RoleAssignment(USER_PRINC, canManage),
                    new RoleAssignment(USER_PRINC, canAccess)));

            roleService.updateRoles(agent, pid, assignments);
        });
    }

    @Test
    public void testRoleWithMultiplePrincipals() throws Exception {
        PID pid = pidMinter.mintContentPid();
        AdminUnit unit = repoObjFactory.createAdminUnit(null);
        contentRoot.addMember(unit);
        CollectionObject coll = repoObjFactory.createCollectionObject(pid, null);
        unit.addMember(coll);
        treeIndexer.indexAll(baseAddress);

        Set<RoleAssignment> assignments = new HashSet<>(asList(
                new RoleAssignment(USER_PRINC, canManage),
                new RoleAssignment(GRP_PRINC, canManage)));

        roleService.updateRoles(agent, pid, assignments);

        CollectionObject updated = repoObjLoader.getCollectionObject(pid);

        assertHasAssignment(USER_PRINC, canManage, updated);
        assertHasAssignment(GRP_PRINC, canManage, updated);

        assertMessageSent(pid);

        String eventDetail = assertEventCreatedAndGetDetail(updated);
        assertThat(eventDetail, containsString(
                canManage.name() + ": " + GRP_PRINC + ", " + USER_PRINC));
    }

    @Test
    public void testWithInheritedRole() throws Exception {
        PID pid = pidMinter.mintContentPid();
        AdminUnit unit = repoObjFactory.createAdminUnit(
                new AclModelBuilder("Admin Unit With Owner")
                .addUnitOwner(USER_PRINC)
                .addCanManage("another")
                .model);
        contentRoot.addMember(unit);
        CollectionObject coll = repoObjFactory.createCollectionObject(pid, null);
        unit.addMember(coll);
        treeIndexer.indexAll(baseAddress);

        Set<RoleAssignment> assignments = new HashSet<>(asList(
                new RoleAssignment(GRP_PRINC, canManage)));

        roleService.updateRoles(agent, pid, assignments);

        CollectionObject updated = repoObjLoader.getCollectionObject(pid);

        assertHasAssignment(GRP_PRINC, canManage, updated);
        // Inherited unit owner should not appear on the child
        assertNoAssignment(USER_PRINC, unitOwner, updated);

        assertMessageSent(pid);

        // Inherited and local roles should be logged
        String eventDetail = assertEventCreatedAndGetDetail(updated);
        assertThat(eventDetail, containsString(
                canManage.name() + ": another, " + GRP_PRINC));
        assertThat(eventDetail, containsString(
                unitOwner.name() + ": " + USER_PRINC));
    }

    @Test
    public void testWithPatronRoles() throws Exception {
        PID pid = pidMinter.mintContentPid();
        AdminUnit unit = repoObjFactory.createAdminUnit(null);
        contentRoot.addMember(unit);
        CollectionObject coll = repoObjFactory.createCollectionObject(pid,
                new AclModelBuilder("Collection with patrons")
                .addCanViewOriginals(AUTHENTICATED_PRINC)
                .model);
        unit.addMember(coll);
        treeIndexer.indexAll(baseAddress);

        Set<RoleAssignment> assignments = new HashSet<>(asList(
                new RoleAssignment(GRP_PRINC, canManage)));

        roleService.updateRoles(agent, pid, assignments);

        CollectionObject updated = repoObjLoader.getCollectionObject(pid);

        assertHasAssignment(GRP_PRINC, canManage, updated);
        // Ensure that existing patron role was not impacted
        assertHasAssignment(AUTHENTICATED_PRINC, canViewOriginals, updated);

        assertMessageSent(pid);

        String eventDetail = assertEventCreatedAndGetDetail(updated);
        assertThat(eventDetail, containsString(
                canManage.name() + ": " + GRP_PRINC));
        // Must not contain the patron assignment
        assertThat(eventDetail, not(containsString(AUTHENTICATED_PRINC)));
    }

    @Test
    public void testAssignStaffRoleToPatronPrincipal() throws Exception {
        Assertions.assertThrows(InvalidAssignmentException.class, () -> {
            PID pid = pidMinter.mintContentPid();
            AdminUnit unit = repoObjFactory.createAdminUnit(null);
            contentRoot.addMember(unit);
            CollectionObject coll = repoObjFactory.createCollectionObject(pid, null);
            unit.addMember(coll);
            treeIndexer.indexAll(baseAddress);

            Set<RoleAssignment> assignments = new HashSet<>(asList(
                    new RoleAssignment(PUBLIC_PRINC, canManage)));

            roleService.updateRoles(agent, pid, assignments);
        });
    }

    @Test
    public void testAssignPatron() throws Exception {
        Assertions.assertThrows(ServiceException.class, () -> {
            PID pid = pidMinter.mintContentPid();
            AdminUnit unit = repoObjFactory.createAdminUnit(null);
            contentRoot.addMember(unit);
            CollectionObject coll = repoObjFactory.createCollectionObject(pid, null);
            unit.addMember(coll);
            treeIndexer.indexAll(baseAddress);

            Set<RoleAssignment> assignments = new HashSet<>(asList(
                    new RoleAssignment(PUBLIC_PRINC, canViewOriginals)));

            roleService.updateRoles(agent, pid, assignments);
        });
    }

    private void assertNoStaffRoles(ContentObject obj) {
        List<UserRole> staffRoles = UserRole.getStaffRoles();

        Resource resc = obj.getResource();
        StmtIterator it = resc.listProperties();
        while (it.hasNext()) {
            Statement stmt = it.next();
            UserRole role = UserRole.getRoleByProperty(stmt.getPredicate().getURI());
            if (role != null) {
                assertFalse(staffRoles.contains(role), "No staff roles should be set, but " + role + " present");
            }
        }
    }

    private void assertHasAssignment(String princ, UserRole role, ContentObject obj) {
        Resource resc = obj.getResource();
        assertTrue(resc.hasProperty(role.getProperty(), princ),
                "Expected role " + role.name() + " was not assigned for " + princ);
    }

    private void assertNoAssignment(String princ, UserRole role, ContentObject obj) {
        Resource resc = obj.getResource();
        assertFalse(resc.hasProperty(role.getProperty(), princ),
                "Unexpected role " + role.name() + " was assigned for " + princ);
    }

    private void assertMessageSent(PID pid) {
        verify(operationsMessageSender).sendOperationMessage(
                eq(USER_PRINC), eq(CDRActions.EDIT_ACCESS_CONTROL), pidListCaptor.capture());
        assertTrue(pidListCaptor.getValue().contains(pid));
    }

    private String assertEventCreatedAndGetDetail(ContentObject repoObj) {
        Model eventsModel = repoObj.getPremisLog().getEventsModel();
        Resource objResc = eventsModel.getResource(repoObj.getPid().getRepositoryPath());
        List<Resource> eventRescs = eventsModel.listResourcesWithProperty(Prov.used, objResc).toList();
        Resource eventResc = eventRescs.get(0);
        assertTrue(eventResc.hasProperty(RDF.type, Premis.PolicyAssignment),
                "Event type was not set");
        Resource agentResc = eventResc.getPropertyResourceValue(Premis.hasEventRelatedAgentImplementor);
        assertEquals(AgentPids.forPerson(USER_PRINC).getRepositoryPath(), agentResc.getURI());
        String eventDetail = eventResc.getProperty(Premis.note).getString();
        assertThat(eventDetail, containsString("Staff roles for item set to:"));
        return eventDetail;
    }

    private void assertEmbargoPresent(ContentObject obj) {
        Resource resc = obj.getResource();
        assertTrue(resc.hasProperty(CdrAcl.embargoUntil), "Embargo was not present");
    }
}
