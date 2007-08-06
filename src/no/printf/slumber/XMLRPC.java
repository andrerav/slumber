/**
 * XML-RPC Bridge for the Sleep programming language
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

package no.printf.slumber;

import java.net.URL;
import java.util.*;

import sleep.interfaces.*;
import sleep.runtime.*;
import sleep.bridges.*;

import org.apache.xmlrpc.client.*;

/**
 * This bridge builds on (and requires) the Apache XML-RPC implementation
 * to provide a robust XML-RPC API for Sleep. 
 * 
 * @author andreas
 *
 */
public class XMLRPC implements Loadable {

	/* (non-Javadoc)
	 * @see sleep.interfaces.Loadable#scriptLoaded(sleep.runtime.ScriptInstance)
	 */
	public boolean scriptLoaded(ScriptInstance s) {

        Hashtable<String, Function> env = (Hashtable<String, Function>)(s.getScriptEnvironment().getEnvironment());

        // Connection
        env.put("&xmlRpcCreateClient", new XMLRPC.XmlRpcCreateClient());
        env.put("&xmlRpcExecute",      new XMLRPC.XmlRpcExecute());

		
		return true;
	}

	/* (non-Javadoc)
	 * @see sleep.interfaces.Loadable#scriptUnloaded(sleep.runtime.ScriptInstance)
	 */
	public boolean scriptUnloaded(ScriptInstance s) {
		// TODO Auto-generated method stub
		return true;
	}
	
	/**
	 * @author andreas
	 *
	 */
	private static class XmlRpcCreateClient implements Function {

		/**
		 * 
		 */
		private static final long serialVersionUID = -2820187184573514404L;

		/* (non-Javadoc)
		 * @see sleep.interfaces.Function#evaluate(java.lang.String, sleep.runtime.ScriptInstance, java.util.Stack)
		 */
		public Scalar evaluate(String name, ScriptInstance instance, Stack args) {

			/* Check number of arguments */
            if (args.size() < 1) {
                instance.getScriptEnvironment().flagError("Not enough arguments");
                return null;
            }
            
            /* Get configuration for XML-RPC client */
            String url = BridgeUtilities.getString(args, null);
            
			try {
				
				/* Create config */
			    XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
			    config.setServerURL(new URL(url));
			    
			    /* Create new client and feed it configuration */
			    XmlRpcClient client = new XmlRpcClient();
			    client.setConfig(config);
			    
			    return SleepUtils.getScalar(client);

			} catch (Exception e) {
                instance.getScriptEnvironment().flagError(e.getMessage());
			}
			
			return null;
		}

	}


    /**
	 * @author andreas
	 *
	 */
	private static class XmlRpcExecute implements Function {

		/**
		 * 
		 */
		private static final long serialVersionUID = 5514138836627741294L;

		/* (non-Javadoc)
		 * @see sleep.interfaces.Function#evaluate(java.lang.String, sleep.runtime.ScriptInstance, java.util.Stack)
		 */
		public Scalar evaluate(String name, ScriptInstance instance, Stack args) {

			/* Check number of arguments */
            if (args.size() < 2) {
                instance.getScriptEnvironment().flagError("Not enough arguments");
                return null;
            }
            
            /* Get XML-RPC client */
            XmlRpcClient client = (XmlRpcClient)(BridgeUtilities.getObject(args));
            
            /* Get procedure name to execute */
            String proc = BridgeUtilities.getString(args, null);

            /* Params for the service, if any */
            Object[] params = new Object[0];
            
            if (args.size() > 0) {
	            /* Get params, this is pretty stupid */
	            ScalarArray scalarParams = BridgeUtilities.getArray(args);
	            params = new Object[scalarParams.size()];
	            int i = 0;
	            while(scalarParams.scalarIterator().hasNext()) {
	            	params[i] = scalarParams.scalarIterator().next();
	            	i++;
	            }
            }

			try {
					return SleepUtils.getScalar(client.execute(proc, params));

			} catch (Exception e) {
                instance.getScriptEnvironment().flagError(e.getMessage());
			}
			
			return null;
		}

	}
}
