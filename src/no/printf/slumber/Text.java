/**
 * 
 */
package no.printf.slumber;

/**
 * Text Bridge for the Sleep programming language
 * Copyright (C) 2006 Andreas Ravnestad
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Stack;

import sleep.bridges.BridgeUtilities;
import sleep.interfaces.Function;
import sleep.interfaces.Loadable;
import sleep.runtime.*;

/**
 * Slightly extended text function library for sleep
 * 
 * @author Andreas
 *
 */
public class Text implements Loadable {

    /* (non-Javadoc)
     * @see sleep.interfaces.Loadable#scriptLoaded(sleep.runtime.ScriptInstance)
     */
    public boolean scriptLoaded(ScriptInstance s) {

        Hashtable<String, Function> env = (Hashtable<String, Function>)(s.getScriptEnvironment().getEnvironment());
        
        env.put("&format",            new Text.format());
        env.put("&htmlEntities",      new Text.htmlEntities());
        env.put("&removeHtmlTags",    new Text.removeHtmlTags());
        env.put("&urlEncode",         new Text.urlEncode());
        env.put("&urlDecode",         new Text.urlDecode());
        
        // TODO Auto-generated method stub
        return true;
    }

    /* (non-Javadoc)
     * @see sleep.interfaces.Loadable#scriptUnloaded(sleep.runtime.ScriptInstance)
     */
    public boolean scriptUnloaded(ScriptInstance arg0) {
        // TODO Auto-generated method stub
        return true;
    }

    /**
     * Simple formatting of strings
     * 
     * @author Andreas
     *
     */
    private static class format implements Function
    {

        /**
         * 
         */
        private static final long serialVersionUID = -2428624017944627180L;

        /* (non-Javadoc)
         * @see sleep.interfaces.Function#evaluate(java.lang.String, sleep.runtime.ScriptInstance, java.util.Stack)
         */
        public Scalar evaluate(String name, ScriptInstance instance, Stack args) {

            /* Check number of arguments */
            if (args.size() < 2) {
                instance.getScriptEnvironment().flagError("Not enough arguments");
                return null;
            }
            
            /* Replacement subject */
            String subject = (String)(args.pop());
            
            /* Result buffer */
            StringBuffer result = new StringBuffer();
            
            /* Number of replacements */
            int size = args.size();
            
            for(int i = 0; i < size; i++)
            {
                String current_replacement = (String)(args.pop());
                
                result.append(subject.replace("{" + i + "}", current_replacement));
            }
            
            // TODO Auto-generated method stub
            return SleepUtils.getScalar(result.toString());
        }
        
    }
    
    // htmlEntities(<string>)
    // Converts all special chars to html entities
    private static class htmlEntities implements Function
    {
        /**
         * 
         */
        private static final long serialVersionUID = 5875931634385066112L;

        public Scalar evaluate(String name, ScriptInstance script, Stack args)
        {
            String subject = BridgeUtilities.getString(args,"");
            String ignore = BridgeUtilities.getString(args,"");
            return SleepUtils.getScalar(HtmlEntityEncoder.encode(subject, ignore));
        }
    }
    
    // removeHtmlTags(<string>[, excp1, excp2, ...])
    // Removes html tags, except the given exceptions
    // TODO: proper removal of html tags and whitelists
    private static class removeHtmlTags implements Function
    {
        /**
         * 
         */
        private static final long serialVersionUID = 1675658738117802923L;

        public Scalar evaluate(String name, ScriptInstance script, Stack args)
        {

            String subject = BridgeUtilities.getString(args,""); // The string to strip
            return SleepUtils.getScalar(subject.replaceAll("\\<.*?\\>",""));
        }
    }
    
    // urlEncode(<string>)
    // Encodes a string with standard url-encoding
    private static class urlEncode implements Function
    {
        /**
         * 
         */
        private static final long serialVersionUID = -5265675455560368298L;

        public Scalar evaluate(String name, ScriptInstance script, Stack args)
        {
            String subject = BridgeUtilities.getString(args,""); // The string to encode
            String encoding = BridgeUtilities.getString(args,"UTF-8"); // Optional encoding
            if (!subject.equals("")) {
                try {
                    return SleepUtils.getScalar(URLEncoder.encode(subject, encoding));
                } catch(Exception e) {
                    return SleepUtils.getEmptyScalar();
                }
            }

            // No/invalid arguments
            else {
                return SleepUtils.getEmptyScalar();
            }
        }
    }
    
    // urlDecode(<string>)
    // Decodes a string with standard url-encoding
    private static class urlDecode implements Function
    {
        /**
         * 
         */
        private static final long serialVersionUID = 4863806887745305606L;

        public Scalar evaluate(String name, ScriptInstance script, Stack args)
        {
            String subject = BridgeUtilities.getString(args,""); // The string to decode
            String encoding = BridgeUtilities.getString(args,"UTF-8"); // Optional encoding
            if (!subject.equals("")) {
                try {
                    return SleepUtils.getScalar(URLDecoder.decode(subject, encoding));
                } catch(Exception e) {
                    return SleepUtils.getEmptyScalar();
                }
            }

            // No/invalid arguments
            else {
                return SleepUtils.getEmptyScalar();
            }
        }
    }    
}

class HtmlEntityEncoder {
    
    private static HashMap<String, String> entityTable;

    private final static String[] ENTITYLIST = {
        " ", " ", "-", "-", "'", "'", "`","`",

        "&Uuml;","\u00dc",
        "&Auml;","\u00c4",
        "&Ouml;","\u00d6",
        "&Euml;","\u00cb",
        "&Ccedil;","\u00c7",
        "&AElig;","\u00c6",
        "&Aring;","\u00c5",
        "&Oslash;","\u00d8",

        "&uuml;","\u00fc",
        "&auml;","\u00e4",
        "&ouml;","\u00f6",
        "&euml;","\u00eb",
        "&ccedil;","\u00e7",
        "&aring;","\u00e5",
        "&oslash;","\u00f8",
        "&grave;","`",
        "&agrave;","\u00e0",
        "&egrave;","\u00e8",
        "&igrave;","\u00ec",
        "&ograve;","\u00f2",
        "&ugrave;","\u00f9",
        "&amp;","&",
        "&#34;","\"",

        "&szlig;","\u00df",
        "&nbsp;"," ",
        "&gt;",">",
        "&lt;","<",
        "&copy;","(C)",
        "&cent;","\u00a2",
        "&pound;","\u00a3",
        "&laquo;","\u00ab",
        "&raquo;","\u00bb",
        "&reg;","(R)",
        "&middot;"," - ",
        "&times;"," x ",
        "&acute;","'",
        "&aacute;","\u00e1",
        "&uacute;","\u00fa",
        "&oacute;","\u00f3",
        "&eacute;","\u00e9",
        "&iacute;","\u00ed",
        "&ntilde;","\u00f1",
        "&sect;","\u00a7",
        "&egrave;","\u00e8",
        "&icirc;","\u00ee",
        "&ocirc;","\u00f4",
        "&acirc;","\u00e2",
        "&ucirc;","\u00fb",
        "&ecirc;","\u00ea",
        "&aelig;","\u00e6",
        "&iexcl;","\u00a1",
        "&#151;","-",
        "&#0151;","-",
        "&#0146;","'",
        "&#146;","'",
        "&#0145;","'",
        "&#145;","'",
        "&quot;","\"", };
    
    // Create the initial hashmap
    private static void buildTable() {
        entityTable = new HashMap<String, String>(ENTITYLIST.length);

        for (int i = 0; i < ENTITYLIST.length; i += 2) {
            if (!entityTable.containsKey(ENTITYLIST[i + 1])) {
                entityTable.put(ENTITYLIST[i + 1], ENTITYLIST[i]);
            }
        }
    }
    
    // Overload
    public static String encode(String subject, String ignore) {
        return encode(subject, 0, subject.length(), ignore);
    }
    
    // Replace special characters with html entities
    public static String encode(String s, int start, int end, String ignore) {
        if (entityTable == null) {
            buildTable(); // Build the table if necessary
        }
    
        StringBuffer sb = new StringBuffer((end - start) * 2);
        char ch;
        
        for (int i = start; i < end; ++i) {
            ch = s.charAt(i);
            
            // Check if the char can be excluded
            if ((ch >= 63 && ch <= 90) || (ch >= 97 && ch <= 122) || ignore.indexOf(ch) != -1) {
                sb.append(ch);
            }
    
            else {
                
                // Encode this char
                sb.append(encodeSingleChar(String.valueOf(ch)));
            }
        }
        
        // Done
        return sb.toString();
    }
    
    // Looks up a single char in the table and returns its equivalent, if any
    protected static String encodeSingleChar(String subject) {
        String replacement = entityTable.get(subject);
        return (replacement == null) ? subject : replacement;
    }
}
