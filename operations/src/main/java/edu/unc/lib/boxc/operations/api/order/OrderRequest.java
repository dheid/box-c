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

import edu.unc.lib.boxc.model.api.ids.PID;

import java.util.List;

/**
 * Request to perform an ordering operation
 *
 * @author bbpennel
 */
public interface OrderRequest {
    /**
     * @return the operation type for this request
     */
    public OrderOperationType getOperation();

    /**
     * @return parent of the objects being ordered
     */
    public PID getParentPid();

    /**
     * @return list of ordered children
     */
    public List<PID> getOrderedChildren();
}
