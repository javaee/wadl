/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jvnet.ws.wadl2java;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author mh124079
 */
public class ElementResolver {
    
    private Map<String, Object> map;
    
    public ElementResolver() {
        map = new HashMap<String, Object>();
    }
    
    /**
     * Get the element for an id
     * @param id the element identifier as returned by {@link #addReference}.
     * @return the corresponding element or null if not found
     */
    public Object get(String id) {
        return map.get(id);
    }
    
    /**
     * Resolve a href and return the element if it is of the expected type.
     * @return the resolved object
     * @param file the URI of the file in which the referenced element is located, used
     * to absolutize references
     * @param href the reference to resolve
     * @param clazz the class of object expected
     */
    @SuppressWarnings("unchecked")
    public <T> T resolve(URI file, String href, Class<T> clazz) {
        Object o = null;
        String id = file.toString()+href.substring(href.indexOf('#'));
        o = map.get(id);
        if (o == null)
            System.err.println(Wadl2JavaMessages.SKIPPING_REFERENCE(href, file.toString()));
        else if (!clazz.isInstance(o))
            System.err.println(Wadl2JavaMessages.SKIPPING_REFERENCE_TYPE(href, file.toString()));
        return (T)o;
    }
    
    /**
     * Add a reference to an element if it has an identifier
     * @param file the URI of the file that contains the element
     * @param id the id of the element, may be null
     * @param o the element 
     * @return the unique identifier of the element or null if the element did
     * not contain an identifier
     */
    public String addReference(URI file, String id, Object o) {
        String uniqueId = null;
        if (id != null && id.length()>0) {
            // if the element has an ID then add it to the id map
            uniqueId = file.toString()+"#"+id;
            map.put(uniqueId, o);
        }
        return uniqueId;
    }
}
