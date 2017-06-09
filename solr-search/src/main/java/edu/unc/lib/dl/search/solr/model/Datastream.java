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
package edu.unc.lib.dl.search.solr.model;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.ContentModelHelper.DatastreamCategory;

/**
 * 
 * @author bbpennel
 *
 */
public class Datastream {
    private PID owner;
    private String name;
    private Long filesize;
    private String mimetype;
    private String extension;
    private String checksum;
    private ContentModelHelper.Datastream datastreamClass;

    public Datastream(String datastream) {
        if (datastream == null) {
            throw new IllegalArgumentException("Datastream value must not be null");
        }

        String[] dsParts = datastream.split("\\|");

        if (dsParts.length > 0 && dsParts[0].length() > 0) {
            this.name = dsParts[0];
        } else {
            this.name = null;
        }

        if (dsParts.length > 1 && dsParts[1].length() > 0) {
            this.mimetype = dsParts[1];
        } else {
            this.mimetype = null;
        }

        if (dsParts.length > 2 && dsParts[2].length() > 0) {
            this.extension = dsParts[2];
        } else {
            this.extension = null;
        }

        if (dsParts.length > 3 && dsParts[3].length() > 0) {
            try {
                this.filesize = new Long(dsParts[3]);
            } catch (NumberFormatException e) {
                this.filesize = null;
            }
        } else {
            this.filesize = null;
        }

        if (dsParts.length > 4 && dsParts[4].length() > 0) {
            this.checksum = dsParts[4];
        } else {
            this.checksum = null;
        }

        if (dsParts.length > 5 && dsParts[5].length() > 0) {
            this.owner = new PID(dsParts[5]);
        } else {
            this.owner = null;
        }
    }

    public String toString() {
        //DS name|mimetype|extension|filesize|checksum|owner
        StringBuilder sb = new StringBuilder();
        if (name != null) {
            sb.append(name);
        }
        sb.append('|');
        if (mimetype != null) {
            sb.append(mimetype);
        }
        sb.append('|');
        if (extension != null) {
            sb.append(extension);
        }
        sb.append('|');
        if (filesize != null) {
            sb.append(filesize);
        }
        sb.append('|');
        if (checksum != null) {
            sb.append(checksum);
        }
        sb.append('|');
        if (owner != null) {
            sb.append(owner.getPid());
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object object) {
        if (object == null) {
            return false;
        }
        if (object instanceof Datastream) {
            Datastream rightHand = (Datastream)object;
            // Equal if names match and either pids are null or both match
            return name.equals(rightHand.name)
                    && (rightHand.owner == null || owner == null || owner.equals(rightHand.owner));
        }
        if (object instanceof String) {
            String rightHandString = (String)object;
            if (rightHandString.equals(this.name)) {
                return true;
            }
            Datastream rightHand = new Datastream(rightHandString);
            return this.equals(rightHand);
        }
        return false;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    public String getDatastreamIdentifier() {
        if (owner == null) {
            return name;
        }
        return owner.getPid() + "/" + name;
    }

    public String getName() {
        return name;
    }

    public PID getOwner() {
        return owner;
    }

    public Long getFilesize() {
        return filesize;
    }

    public String getMimetype() {
        return mimetype;
    }

    public String getExtension() {
        return extension;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public void setMimetype(String mimetype) {
        this.mimetype = mimetype;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public void setOwner(PID owner) {
        this.owner = owner;
    }

    public void setFilesize(Long filesize) {
        this.filesize = filesize;
    }

    public DatastreamCategory getDatastreamCategory() {
        if (datastreamClass == null) {
            this.datastreamClass = ContentModelHelper.Datastream.getDatastream(this.name);
        }
        if (datastreamClass == null) {
            return null;
        }
        return datastreamClass.getCategory();
    }
}
