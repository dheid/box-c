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
package edu.unc.lib.dl.util;

import java.util.Arrays;
import java.util.List;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.SimpleSelector;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;

import edu.unc.lib.dl.rdf.DcElements;
import edu.unc.lib.dl.rdf.Ebucore;
import edu.unc.lib.dl.rdf.Premis;

/**
 * Filters statements against a list of permitted predicates
 *
 * @author harring
 *
 */
public class MultiPropertySelector extends SimpleSelector {

    List<Property> permittedPredicates = Arrays.asList(
            DcElements.title, Ebucore.filename, Ebucore.hasMimeType,
            Premis.hasOriginalName, Premis.hasMessageDigest,
            Premis.hasSize, RDF.type);

    /**
     * Selects only those statements whose predicates match one of the permitted predicates
     */
    @Override
    public boolean selects(Statement s) {
        return (subject == null || s.getSubject().equals(subject))
            && (permittedPredicates.contains(s.getPredicate()))
                && (object == null || s.getObject().equals(object)) ;
    }

    public MultiPropertySelector(Resource subject) {
        super(subject, (Property) null, (Object) null);
     }

    public MultiPropertySelector() {
        super();
    }

    public MultiPropertySelector(Resource subject, Property predicate, RDFNode object) {
        super(subject, predicate, object);

    }

    public MultiPropertySelector(Resource subject, Property predicate, boolean object) {
        super(subject, predicate, object);

    }

    public MultiPropertySelector(Resource subject, Property predicate, long object) {
        super(subject, predicate, object);

    }

    public MultiPropertySelector(Resource subject, Property predicate, char object) {
        super(subject, predicate, object);

    }

    public MultiPropertySelector(Resource subject, Property predicate, float object) {
        super(subject, predicate, object);

    }

    public MultiPropertySelector(Resource subject, Property predicate, double object) {
        super(subject, predicate, object);

    }

    public MultiPropertySelector(Resource subject, Property predicate, String object) {
        super(subject, predicate, object);

    }

    public MultiPropertySelector(Resource subject, Property predicate, Object object) {
        super(subject, predicate, object);

    }

    public MultiPropertySelector(Resource subject, Property predicate, String object, String language) {
        super(subject, predicate, object, language);

    }

}
