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
package edu.unc.lib.dl.persist.services.acl;

import java.util.Date;
import java.util.List;

import edu.unc.lib.dl.acl.util.RoleAssignment;

/**
 *
 *
 * @author bbpennel
 *
 */
public class PatronAccessDetails {

    private List<RoleAssignment> roles;
    private Date embargo;
    private boolean deleted;

    public List<RoleAssignment> getRoles() {
        return roles;
    }

    public void setRoles(List<RoleAssignment> roles) {
        this.roles = roles;
    }

    public Date getEmbargo() {
        return embargo;
    }

    public void setEmbargo(Date embargo) {
        this.embargo = embargo;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
}
