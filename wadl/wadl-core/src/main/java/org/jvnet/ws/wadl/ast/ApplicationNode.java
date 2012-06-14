/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the "License").  You may not use this file except
 * in compliance with the License.
 * 
 * You can obtain a copy of the license at
 * http://www.opensource.org/licenses/cddl1.php
 * See the License for the specific language governing
 * permissions and limitations under the License.
 */
package org.jvnet.ws.wadl.ast;

import java.util.ArrayList;
import java.util.List;
import org.jvnet.ws.wadl.Application;
import org.xml.sax.Locator;

/**
 * Provides an abstraction of the Application class that contains a normalised
 * tree of off the elements required to define this WADL with the references
 * correctly substituted. 
 * 
 * @author gdavison
 */
public class ApplicationNode  extends AbstractNode {

    
    private Application application;
    private List<ResourceNode> resources;
    
    public ApplicationNode(Application application, List<ResourceNode> resources) {
        this.application = application;
        // Defensive copy
        this.resources = java.util.Collections.unmodifiableList(
                new ArrayList<ResourceNode>(resources));
    }
    
    public List<ResourceNode> getResources() {
        return resources;
    }
    
    /**
     * @return The location of the node
     */
    @Override
    public Locator getLocation() {
        return application.sourceLocation();
    }
    
    
    /**
     * Allow the provided parameter to visit the current node and any
     * child nodes.
     */
    public void visit(NodeVisitor visitor)
    {
        super.visit(visitor);
        
        for (ResourceNode node : getResources()) {
            node.visit(visitor);
        }
    }

    
}
