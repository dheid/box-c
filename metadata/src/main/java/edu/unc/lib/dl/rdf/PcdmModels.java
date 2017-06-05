package edu.unc.lib.dl.rdf;

import static org.apache.jena.rdf.model.ResourceFactory.createProperty;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

/**
 * Vocabulary definitions from rdf-schemas/pcdmModels.rdf
 * @author Auto-generated by schemagen on 21 Apr 2016 12:16
 */
public class PcdmModels {
    private PcdmModels() {
    }

    /** The namespace of the vocabulary as a string */
    public static final String NS = "http://pcdm.org/models#";

    /** The namespace of the vocabulary as a string
     *  @see #NS */
    public static String getURI() {
        return NS; }

    /** The namespace of the vocabulary as a resource */
    public static final Resource NAMESPACE = createResource( NS );

    /** Links from a File to its containing Object. */
    public static final Property fileOf = createProperty( "http://pcdm.org/models#fileOf" );

    /** Links to a File contained by this Object. */
    public static final Property hasFile = createProperty( "http://pcdm.org/models#hasFile" );

    /** Links to a subsidiary Object or Collection. Typically used to link to component
     *  parts, such as a book linking to a page. Note on transitivity: hasMember is
     *  not defined as transitive, but applications may treat it as transitive as
     *  local needs dictate.
     */
    public static final Property hasMember = createProperty( "http://pcdm.org/models#hasMember" );

    /** Links to a related Object that is not a component part, such as an object
     *  representing a donor agreement or policies that govern the resource.
     */
    public static final Property hasRelatedObject = createProperty( "http://pcdm.org/models#hasRelatedObject" );

    /** Links from an Object or Collection to a containing Object or Collection. */
    public static final Property memberOf = createProperty( "http://pcdm.org/models#memberOf" );

    /** Links from an Object to a Object or Collection that it is related to. */
    public static final Property relatedObjectOf = createProperty( "http://pcdm.org/models#relatedObjectOf" );

    public static final Resource Object = createResource( "http://pcdm.org/models#Object" );
    public static final Resource Collection = createResource( "http://pcdm.org/models#Object" );
    public static final Resource File = createResource( "http://pcdm.org/models#File" );
    public static final Resource FileSet = createResource( "http://pcdm.org/models#FileSet" );
}
