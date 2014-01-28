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

import org.xml.sax.Locator;

/**
 * Provide common function for all node types
 * @author gdavison
 */
public abstract class AbstractNode {
    
    
    public interface NodeVisitor
    {
        public void visit(AbstractNode node);
    }
    
    
    /**
     * @return The location of the node
     */
    public abstract Locator getLocation();
    
    
    /**
     * Allow the provided parameter to visit the current node and any
     * child nodes.
     */
    public void visit(NodeVisitor visitor)
    {
        visitor.visit(this);
    }
    
}
