/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
 
package com.dlecan.agreg;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author dlecan
 * 
 */
public class EnvoyeurEmail {

	private static Logger logger = LoggerFactory.getLogger(EnvoyeurEmail.class);

	private static final String SUFFIX = "@dlecan.com";

	/** Destinataire du message. */
	protected String messageDest = "xxxxxxxxx@imode.fr";

	/** Objet session de JavaMail. */
	protected Session session;
	/** Objet message de JavaMail. */
	protected Message mesg;

	public EnvoyeurEmail() {
		// Rien
	}

	/**
	 * Envoi un email
	 * 
	 * @throws Exception
	 */
	public void envoyer(boolean recue, String contenu) throws Exception {

		Properties props = new Properties();
		props.put("mail.smtp.host", "to_configure");
		props.put("mail.from", "agreg@dlecan.com");
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.port", "25");

		// Créer l’objet Session.
		session = Session.getDefaultInstance(props, null);
		// session.setProtocolForAddress("rfc822", "smtps");
		session.setDebug(logger.isDebugEnabled());

		// Créer un message.
		mesg = new MimeMessage(session) {

			@Override
			protected void updateMessageID() throws MessagingException {
				StringBuffer s = new StringBuffer();

				s.append('<').append(
						Long.toHexString(System.currentTimeMillis())).append(
						'.').append(s.hashCode()).append(SUFFIX).append('>');

				setHeader("Message-ID", s.toString());
			}

		};
		mesg.addHeader("User-Agent", "Thunderbird 2.0.0.12 (X11/20080227)");

		mesg.setFrom(new InternetAddress("agreg@dlecan.com"));

		// Adresse TO.
		InternetAddress toAddress = new InternetAddress(messageDest);
		mesg.addRecipient(Message.RecipientType.TO, toAddress);

		String title;
		if (recue) {
			title = "Recue !";
		} else {
			title = "Non reçue :(";
		}
		mesg.setSubject(title);

		// Corps du message.
		mesg.setText(title + "\n\n" + contenu);

		Transport t = session.getTransport("smtp");
		try {
			t.connect("agreg@dlecan.com", "agreg");
			t.sendMessage(mesg, mesg.getAllRecipients());
		} finally {
			t.close();
		}
	}

	public static void main(String[] args) throws Exception {
		new EnvoyeurEmail().envoyer(true, "une chaine extraite");
	}
}
