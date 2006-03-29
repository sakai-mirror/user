/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2003, 2004, 2005, 2006 The Sakai Foundation.
 * 
 * Licensed under the Educational Community License, Version 1.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 * 
 *      http://www.opensource.org/licenses/ecl1.php
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 *
 **********************************************************************************/

package org.sakaiproject.user.impl;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.security.MessageDigest;

import javax.mail.internet.MimeUtility;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <p>
 * OneWayHash converts a plain text string into an encoded string.
 * </p>
 */
public class OneWayHash
{
	/** Our log (commons). */
	private static Log M_log = LogFactory.getLog(OneWayHash.class);

	/**
	 * Encode the clear text into an encoded form.
	 * 
	 * @param clear
	 *        The text to encode.
	 * @return The encoded text.
	 */
	public static String encode(String clear)
	{
		try
		{
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] digest = md.digest(clear.getBytes("UTF-8"));
			ByteArrayOutputStream bas = new ByteArrayOutputStream(digest.length + digest.length / 3 + 1);
			OutputStream encodedStream = MimeUtility.encode(bas, "base64");
			encodedStream.write(digest);
			return bas.toString();
		}
		catch (Exception e)
		{
			M_log.warn("OneWayHash.encode: exception: " + e);
			return null;
		}
	}
}
