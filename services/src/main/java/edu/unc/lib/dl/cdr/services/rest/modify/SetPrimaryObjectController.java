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
package edu.unc.lib.dl.cdr.services.rest.modify;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.unc.lib.dl.acl.exception.AccessRestrictionException;
import edu.unc.lib.dl.acl.util.AgentPrincipals;
import edu.unc.lib.dl.cdr.services.processing.SetPrimaryObjectService;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.AuthorizationException;
import edu.unc.lib.dl.fedora.PID;

/**
 * API controller for setting the primary object on a work
 *
 * @author harring
 *
 */
@Controller
public class SetPrimaryObjectController {

    private static final Logger log = LoggerFactory.getLogger(SetPrimaryObjectController.class);

    @Autowired
    private SetPrimaryObjectService setPrimaryObjectService;

    @RequestMapping(value = "/edit/setPrimaryObject/{id}", method = RequestMethod.PUT)
    @ResponseBody
    public ResponseEntity<Object> setPrimaryObject(@PathVariable("id") String id) {
        return setPrimary(id);
    }

    private ResponseEntity<Object> setPrimary(String id) {
        Map<String, Object> result = new HashMap<>();
        result.put("action", "setPrimaryObject");
        result.put("pid", id);

        PID fileObjPid = PIDs.get(id);

        try {
            setPrimaryObjectService.setPrimaryObject(AgentPrincipals.createFromThread(), fileObjPid);
        } catch (Exception e) {
            result.put("error", e.getMessage());
            Throwable t = e.getCause();
            if (t instanceof AuthorizationException || t instanceof AccessRestrictionException) {
                return new ResponseEntity<>(result, HttpStatus.FORBIDDEN);
            } else {
                log.error("Failed to set primary object with pid " + fileObjPid, e);
                return new ResponseEntity<>(result, HttpStatus.BAD_REQUEST);
            }
        }
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

}
