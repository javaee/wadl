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

/*
 * Main.java
 *
 * Created on May 1, 2006, 5:10 PM
 *
 */

package com.sun.research.wadl2java.yahoo;

import com.yahoo.search.*;
import com.yahoo.search.Endpoint.NewsSearch;
import com.yahoo.search.Endpoint.NewsSearch.*;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

/**
 * Simple command line example to query the Yahoo News Search service
 * @author mh124079
 */
public class Main {
    
    /**
     * Query the Yahoo News Search service for stories that contain the word Java.
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            NewsSearch s = new NewsSearch();
            ResultSet results = s.getAsResultSet( 
                    "jaxws_restful_sample", "java", Type.ANY, 10, 1, 
                    Sort.DATE, "en", Output.XML, null);
            for (ResultType result: results.getResult()) {
//                System.out.println(result.getTitle()+
//                        " ("+result.getClickUrl()+")");
                System.out.printf("%s (%s)\n", result.getTitle(),
                        result.getClickUrl());
            }
        } catch (JAXBException ex) {
            ex.printStackTrace();
        } catch (SearchErrorException ex) {
            ex.printStackTrace();
        }
    }
}
