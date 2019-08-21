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
package edu.unc.lib.dl.rdf;

import static org.apache.jena.rdf.model.ResourceFactory.createProperty;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

/**
 *
 * @author bbpennel
 *
 */
public class Memento {
    private Memento() {
    }

    /** The namespace of the vocabulary as a string */
    public static final String NS = "http://mementoweb.org/ns#";

    public static final Resource Memento = createResource(
            "http://mementoweb.org/ns#Memento" );

    public static final Resource OriginalResource = createResource(
            "http://mementoweb.org/ns#OriginalResource" );

    public static final Resource TimeGate = createResource(
            "http://mementoweb.org/ns#TimeGate" );

    public static final Resource TimeMap = createResource(
            "http://mementoweb.org/ns#TimeMap" );

    public static final Property memento = createProperty(
            "http://mementoweb.org/ns#memento" );

    public static final Property mementoDatetime = createProperty(
            "http://mementoweb.org/ns#mementoDatetime" );

    public static final Property timegate = createProperty(
            "http://mementoweb.org/ns#timegate" );

    public static final Property timemap = createProperty(
            "http://mementoweb.org/ns#timemap" );

    public static final Property original = createProperty(
            "http://mementoweb.org/ns#original" );
}
