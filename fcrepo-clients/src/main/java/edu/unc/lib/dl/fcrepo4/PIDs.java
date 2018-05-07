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
package edu.unc.lib.dl.fcrepo4;

import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.CONTENT_BASE;
import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.HASHED_PATH_DEPTH;
import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.HASHED_PATH_SIZE;
import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.REPOSITORY_ROOT_ID;
import static edu.unc.lib.dl.fcrepo4.RepositoryPaths.getBaseUri;
import static edu.unc.lib.dl.fcrepo4.RepositoryPaths.getContentBase;
import static edu.unc.lib.dl.fcrepo4.RepositoryPaths.idToPath;

import java.net.URI;
import java.util.regex.Matcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.fedora.PID;

/**
 * Provides static methods for creating PID objects
 *
 * @author bbpennel
 *
 */
public class PIDs {

    private static final Logger log = LoggerFactory.getLogger(PIDs.class);

    private PIDs() {

    }
    /**
     * Get a PID object for the given URI.
     *
     * @param uri
     * @return new PID object for the given URI
     */
    public static PID get(URI uri) {
        return get(uri.toString());
    }

    /**
     * Get a PID object for the given identifier or URI string. Should either be
     * a fully qualified repository URI or follow the syntax for an identifier,
     * such as: deposit/uuid:0411cf7e-9ac0-4ab0-8c24-ff367e8e77f6
     *
     * @param value
     * @return new PID object for the given identifier or URI
     */
    public static PID get(String value) {
        if (value == null) {
            return null;
        }

        String id;
        String qualifier;
        String componentPath;
        String repositoryPath;

        if (value.startsWith(getBaseUri())) {
            // Given value was a fedora path. Remove the base and decompose
            String path = value.substring(getBaseUri().length());

            Matcher matcher = RepositoryPathConstants.repositoryPathPattern.matcher(path);
            if (matcher.matches()) {
                // extract the qualifier/category portion of the path, ex: deposit, content, etc.
                qualifier = matcher.group(2);
                // store the trailing component path, which is everything after the object identifier
                componentPath = matcher.group(8);
                // store the identifier for the main object
                id = matcher.group(5);
                if (id == null) {
                    id = matcher.group(6);
                }
                // Reconstruct the repository path from wanted components (excluding things like tx ids)
                repositoryPath = getRepositoryPath(matcher.group(3), qualifier, componentPath, false);
            } else {
                // Handle base object paths
                PID basePid = getBaseResourcePidFromUri(value);
                if (basePid == null) {
                    log.warn("Invalid path {}, cannot construct PID", value);
                }
                // Return either a pid to a base resource or null if invalid path
                return basePid;
            }
        } else {
            // Determine if the value matches the pattern for an identifier
            Matcher matcher = RepositoryPathConstants.identifierPattern.matcher(value);
            if (matcher.matches()) {
                // Store the qualifier if specified, otherwise use the default "content" qualifier
                qualifier = matcher.group(2);
                if (qualifier == null) {
                    qualifier = RepositoryPathConstants.CONTENT_BASE;
                }
                // store the trailing component path
                componentPath = matcher.group(8);
                if (matcher.group(5) != null) {
                    // store the identifier for the main object
                    id = matcher.group(5);

                    // Expand the identifier into a repository path
                    repositoryPath = getRepositoryPath(id, qualifier, componentPath, true);
                } else {
                    // Reserved id found, path does not need to be expanded
                    id = matcher.group(6);

                    repositoryPath = getRepositoryPath(id, qualifier, componentPath, false);
                }
            } else {
                // Handle base object ids
                PID basePid = getBaseResourcePidFromId(value);
                if (basePid == null) {
                    log.warn("Invalid qualified path {}, cannot construct PID", value);
                }
                // Return either a pid to a base resource or null if invalid path
                return basePid;
            }
        }

        // Build and return the new pid object
        return new FedoraPID(id, qualifier, componentPath, URI.create(repositoryPath));
    }

    private static PID getBaseResourcePidFromUri(String uri) {
        if (getBaseUri().equals(uri)) {
            return RepositoryPaths.getRootPid();
        } else if (getContentBase().equals(uri)) {
            return RepositoryPaths.getContentBasePid();
        }

        return null;
    }

    private static PID getBaseResourcePidFromId(String id) {
        if (REPOSITORY_ROOT_ID.equals(id)) {
            return RepositoryPaths.getRootPid();
        } else if (CONTENT_BASE.equals(id)) {
            return RepositoryPaths.getContentBasePid();
        }

        return null;
    }

    /**
     * Get a PID object with the given qualifier and id
     *
     * @param qualifier
     * @param id
     * @return
     */
    public static PID get(String qualifier, String id) {
        return get(qualifier + "/" + id);
    }

    /**
     * Expands the identifier for a repository object into the full repository path.
     *
     * @param id
     * @param qualifier
     * @param componentPath
     * @param expand if true, then the id will be prepended with hashed subfolders
     * @return
     */
    private static String getRepositoryPath(String id, String qualifier, String componentPath, boolean expand) {
        StringBuilder builder = new StringBuilder(getBaseUri());
        builder.append(qualifier).append('/');

        if (expand) {
            // Expand the id into chunked subfolders
            builder.append(idToPath(id, HASHED_PATH_DEPTH, HASHED_PATH_SIZE));
        }

        builder.append(id);
        if (componentPath != null) {
            builder.append('/').append(componentPath);
        }
        return builder.toString();
    }
}
