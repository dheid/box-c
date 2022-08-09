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
package edu.unc.lib.boxc.operations.api.order;

import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.ContentContainerObject;

import java.util.List;

/**
 * Request object for updating the order of children for a single container
 *
 * @author bbpennel
 */
public class OrderChildrenRequest {
    private ContentContainerObject parentObject;
    private List<PID> orderedChildren;

    public OrderChildrenRequest() {
    }

    public ContentContainerObject getParentObject() {
        return parentObject;
    }

    public void setParentObject(ContentContainerObject parentObject) {
        this.parentObject = parentObject;
    }

    public List<PID> getOrderedChildren() {
        return orderedChildren;
    }

    public void setOrderedChildren(List<PID> orderedChildren) {
        this.orderedChildren = orderedChildren;
    }
}
