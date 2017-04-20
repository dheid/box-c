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
package edu.unc.lib.dl.acl.fcrepo4;

import static edu.unc.lib.dl.acl.util.PrincipalClassifier.getPatronPrincipals;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.unc.lib.dl.acl.service.PatronAccess;
import edu.unc.lib.dl.acl.util.UserRole;
import edu.unc.lib.dl.fedora.ContentPathFactory;
import edu.unc.lib.dl.fedora.PID;

/**
 * Factory which provides access control details that apply to particular
 * objects, either via direct definition or inherited from parent objects
 * 
 * @author bbpennel
 *
 */
public class InheritedAclFactory implements AclFactory {

	private ObjectAclFactory objectAclFactory;

	private ContentPathFactory pathFactory;

	private ObjectPermissionEvaluator objectPermissionEvaluator;

	@Override
	public Map<String, Set<String>> getPrincipalRoles(PID target) {

		// Retrieve the path of objects up to and including the target
		List<PID> path = getPidPath(target);

		Map<String, Set<String>> inheritedPrincRoles = new HashMap<>();
		Set<String> patronPrincipals = null;

		// Iterate through each step in the path except for the root content node
		int depth = 0;
		for (; depth < path.size(); depth++) {
			PID pathPid = path.get(depth);

			// For the first two objects (unit, collection), staff roles should be considered
			if (depth < 2) {

				Map<String, Set<String>> objectPrincipalRoles = objectAclFactory.getPrincipalRoles(pathPid);

				// Add this object's principals/roles to the result
				mergePrincipalRoles(inheritedPrincRoles, objectPrincipalRoles);

			} else {
				// No roles left, and no more can be added, so further processing is not needed
				if (inheritedPrincRoles.isEmpty()) {
					return inheritedPrincRoles;
				}
				// Determine the set of patron principals to evaluate going forward
				if (patronPrincipals == null) {
					patronPrincipals = getPatronPrincipals(inheritedPrincRoles.keySet());
				}
				// No patron principals, so no further changes can occur
				if (patronPrincipals.size() == 0) {
					return inheritedPrincRoles;
				}

				// Evaluate remaining inherited patron roles
				removeRevokedPatronRoles(pathPid, inheritedPrincRoles, patronPrincipals);
			}
		}

		// Units cannot be assigned patron roles, but have an assumed non-inheritable everyone permission
		if (depth == 1) {
			Set<String> roles = new HashSet<>();
			roles.add(UserRole.canViewOriginals.getPropertyString());
			inheritedPrincRoles.put("everyone", roles);
		}

		return inheritedPrincRoles;
	}

	private void mergePrincipalRoles(Map<String, Set<String>> basePrincRoles, Map<String,
			Set<String>> addPrincRoles) {
		if (basePrincRoles.isEmpty()) {
			basePrincRoles.putAll(addPrincRoles);
		} else {
			// Add to inherited data.  If principals overlap, extra roles are added (not overridden)
			addPrincRoles.forEach((principal, roles) -> {
				if (basePrincRoles.containsKey(principal)) {
					basePrincRoles.get(principal).addAll(roles);
				} else {
					basePrincRoles.put(principal, roles);
				}
			});
		}
	}

	private void removeRevokedPatronRoles(PID pid, Map<String, Set<String>> princRoles, Set<String> patronPrincipals) {
		// Check each remaining patron principal to see if it still has patron access
		Iterator<String> patronIt = patronPrincipals.iterator();
		while (patronIt.hasNext()) {
			String patronPrinc = patronIt.next();

			// Patron access revoked for this principal, so remove it from inherited roles
			if (!objectPermissionEvaluator.hasPatronAccess(pid, patronPrinc)) {
				patronIt.remove();
				princRoles.remove(patronPrinc);
			}
		}
	}

	@Override
	public PatronAccess getPatronAccess(PID target) {
		List<PID> pidPath = getPidPath(target);
		if (pidPath.size() < 3) {
			return null;
		}

		PatronAccess computedAccess = PatronAccess.parent;
		for (int i = 2; i < pidPath.size(); i++) {
			PID pathPid = pidPath.get(i);

			PatronAccess access = objectAclFactory.getPatronAccess(pathPid);
			if (PatronAccess.none.equals(access)) {
				return PatronAccess.none;
			}

			if (PatronAccess.authenticated.equals(access)) {
				computedAccess = access;
			}
		}

		return computedAccess;
	}

	@Override
	public Date getEmbargoUntil(PID target) {
		return getPidPath(target).stream()
				.map(p -> objectAclFactory.getEmbargoUntil(p))
				.filter(p -> p != null)
				.max(Comparator.naturalOrder())
				.orElse(null);
	}

	@Override
	public boolean isMarkedForDeletion(PID target) {
		return getPidPath(target).stream()
				.anyMatch(p -> objectAclFactory.isMarkedForDeletion(p));
	}

	private List<PID> getPidPath(PID pid) {
		List<PID> path = pathFactory.getAncestorPids(pid);
		path = new ArrayList<>(path.subList(1, path.size()));
		path.add(pid);

		return path;
	}

	public void setObjectAclFactory(ObjectAclFactory objectAclFactory) {
		this.objectAclFactory = objectAclFactory;
	}

	public void setPathFactory(ContentPathFactory pathFactory) {
		this.pathFactory = pathFactory;
	}

	public void setObjectPermissionEvaluator(ObjectPermissionEvaluator objectPermissionEvaluator) {
		this.objectPermissionEvaluator = objectPermissionEvaluator;
	}
}
