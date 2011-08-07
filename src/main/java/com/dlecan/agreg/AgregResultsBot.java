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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author dlecan
 * 
 */
public class AgregResultsBot {

	private static final String USER_AGENT = "Mozilla/5.0 (X11; U; Linux i686; "
			+ "fr; rv:1.8.1.12) Gecko/20080207 Ubuntu/7.10 (gutsy) Firefox/2.0.0.12";

	private static final String ADMISSION = "ADMISSION";

	private static final String ADMISSIBILITE = "ADMISSIBILITE";

	private static final String URL_PUBLINET_PREFIX = "http://publinetce2.education.fr/"
			+ "publinet/Servlet/PublinetServlet?_page=LISTE_RES&_section=2002"
			+ "&_type=";

	private static final String URL_PUBLINET_SUFFIX = "&_concours=EAI&_acad=FRANCE&_lettre=v";

	private static final String LOCK_FILE = System.getProperty("user.home")
			+ File.separator + "agreg.lock";

	private static Logger logger = LoggerFactory
			.getLogger(AgregResultsBot.class);

	private final HttpClient client;

	private boolean recue;

	private String messageAEnvoyer;

	public AgregResultsBot() {
		client = new HttpClient();
		client.getParams().setParameter("http.useragent", USER_AGENT);

		List<Header> headers = new ArrayList<Header>();
		headers
				.add(new Header(
						"Accept",
						"text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5"));
		headers.add(new Header("Accept-Language",
				"fr,fr-fr;q=0.8,en-us;q=0.5,en;q=0.3"));
		headers.add(new Header("Accept-Encoding", "gzip,deflate"));
		headers.add(new Header("Accept-Charset",
				"ISO-8859-1,utf-8;q=0.7,*;q=0.7"));
		client.getParams().setParameter("http.default-headers", headers);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {

		AgregResultsBot bot = new AgregResultsBot();

		boolean dispo = bot.isResultatsDisponibles(args.length == 0 ? ADMISSION
				: ADMISSIBILITE);
		logger.info("Résultats dipo : {}", dispo);
		logger.info("Admise : {}", bot.recue);

		if (dispo) {
			bot.neutraliserAlgo();
			bot.notifierResultat();
		}

	}

	public boolean isResultatsDisponibles(String type) throws Exception {
		boolean resultatsDisponibles = false;

		String urlAppelee = URL_PUBLINET_PREFIX + type + URL_PUBLINET_SUFFIX;

		HttpMethod getUrlPublinet = new GetMethod(urlAppelee);
		try {
			int status = client.executeMethod(getUrlPublinet);

			if (status == HttpStatus.SC_OK) {
				InputStream streamPage = getUrlPublinet
						.getResponseBodyAsStream();

				BufferedReader reader = new BufferedReader(
						new InputStreamReader(streamPage));

				String line;
				while ((line = reader.readLine()) != null) {

					if (line.toUpperCase().contains("AUCUN CANDIDAT ADMIS")) {
						resultatsDisponibles = false;
						break;
					} else if (line.toUpperCase().contains(
							"Cliquez sur une des lettres de l'alphabet"
									.toUpperCase())) {
						resultatsDisponibles = true;
						break;
					} else {
						// Le système déconne
					}
				}
				if (resultatsDisponibles) {
					while ((line = reader.readLine()) != null) {

						if (line.toUpperCase().contains("VALADE")) {
							recue = true;
							messageAEnvoyer = urlAppelee + "\n\n" + line;
							break;
						} else {
							// Le système déconne
						}
					}
				}
			} else {
				logger.error("Method failed: {}", getUrlPublinet
						.getStatusLine());
			}

		} finally {
			getUrlPublinet.releaseConnection();
		}
		return resultatsDisponibles;
	}

	public void notifierResultat() throws Exception {
		new EnvoyeurEmail().envoyer(recue, messageAEnvoyer);
	}

	public void neutraliserAlgo() throws Exception {
		Writer writer = new FileWriter(LOCK_FILE);
		writer.close();
	}
}
