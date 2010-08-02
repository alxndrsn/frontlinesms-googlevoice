/*
 * FrontlineSMS <http://www.frontlinesms.com>
 * Copyright 2007-2010 kiwanja
 * 
 * This file is part of FrontlineSMS.
 * 
 * FrontlineSMS is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 * 
 * FrontlineSMS is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with FrontlineSMS. If not, see <http://www.gnu.org/licenses/>.
 */
package net.frontlinesms.smsdevice.internet;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.smslib.CIncomingMessage;

import com.techventus.server.voice.Voice;
import com.techventus.server.voice.interpreted.GvSmsMessage;
import com.techventus.server.voice.interpreted.GvSmsReceiveException;
import com.techventus.server.voice.interpreted.GvSmsSendException;
import com.techventus.server.voice.interpreted.GvSmsSendResponse;
import com.techventus.server.voice.interpreted.InterpretedVoice;

import net.frontlinesms.data.domain.Message;
// import net.frontlinesms.smsdevice.SmsInternetServiceProvider;
import net.frontlinesms.smsdevice.properties.PasswordString;
import net.frontlinesms.smsdevice.properties.PhoneSection;

/**
 * Implementation of {@link SmsInternetService} using the Google Voice API.
 * @author Alex Anderson <alex@frontlinesms.com>
 */
// @SmsInternetServiceProvider(name="Google Voice", icon="/icons/sms_http.png")
public class GoogleVoiceSmsInternetService extends AbstractSmsInternetService {

//> CONSTANTS
	/** Prefix attached to every property name. */
	private static final String PROPERTY_PREFIX = "smsdevice.internet.googlevoice.";
	/** Property key: username for {@link #service} */
	protected static final String PROPERTY_USERNAME = PROPERTY_PREFIX + "username";
	/** Property key: password for {@link #service} */
	protected static final String PROPERTY_PASSWORD = PROPERTY_PREFIX + "password";
	/** Property key: phone number for SMS to appear "FROM" */
	protected static final String PROPERTY_FROM_MSISDN = PROPERTY_PREFIX + "from.msisdn";
	
//> INSTANCE PROPERTIES
	/** Indicates whether the service is connected and ready to use. */
	private boolean connected;
	/** Google voice service that we are connecting to. */
	private InterpretedVoice service;
	
//> INITIALISATION METHODS
	/** @see AbstractSmsInternetService#init() */
	@Override
	protected synchronized void init() throws SmsInternetServiceInitialisationException {
		try {
			this.service = new InterpretedVoice(new Voice(getUsername(), getPassword(), getClass().getName() + ":" + getUsername(), false));
			this.setStatus(SmsInternetServiceStatus.CONNECTED, null);
		} catch (Exception ex) {
			this.setStatus(SmsInternetServiceStatus.FAILED_TO_CONNECT, ex.getMessage());
			throw new SmsInternetServiceInitialisationException(ex);
		}
		connected = true;
	}

	/** @see AbstractSmsInternetService#deinit() */
	@Override
	protected void deinit() {
		// TODO check if we have to de-init this at all
		this.connected = false;
	}

//> COMMUNICATION METHODS
	/** @see AbstractSmsInternetService#receiveSms() */
	@Override
	protected void receiveSms() throws SmsInternetServiceReceiveException {
		try {
			List<GvSmsMessage> receivedMessages = this.service.receiveSms();
			for(GvSmsMessage msg : receivedMessages) {
				CIncomingMessage cMessage = convertToSmsLibIncomingMessage(msg);
				smsListener.incomingMessageEvent(this, cMessage);
			}
		} catch (GvSmsReceiveException ex) {
			throw new SmsInternetServiceReceiveException("no extra details", ex); // FIXME remove message from exception instantiation
		}
	}

	/** @see AbstractSmsInternetService#sendSmsDirect(Message) */
	@Override
	protected void sendSmsDirect(Message message) {
		try {
			GvSmsSendResponse response = this.service.sendSms(message.getRecipientMsisdn(), message.getTextContent());
			// TODO handle response << these should ultimately be handled by AbstractSmsInternetService
		} catch (GvSmsSendException e) {
			e.printStackTrace();
			// TODO handle exception << these should ultimately be handled by AbstractSmsInternetService
		}
		
	}

//> ACCESSORS
	private String getUsername() {
		return getPropertyValue(PROPERTY_USERNAME, String.class);
	}
	
	private String getPassword() {
		return getPropertyValue(PROPERTY_PASSWORD, PasswordString.class).getValue();	
	}
	
	/** @see SmsInternetService#getIdentifier() */
	public String getIdentifier() {
		return "GoogleVoice:" + this.getUsername();
	}

	/** @see SmsInternetService#getMsisdn() */
	public String getMsisdn() {
		return super.getPropertyValue(PROPERTY_FROM_MSISDN, PhoneSection.class).getValue();
	}

	/** @see SmsInternetService#getPropertiesStructure() */
	public Map<String, Object> getPropertiesStructure() {
		LinkedHashMap<String, Object> defaultSettings = new LinkedHashMap<String, Object>();
		defaultSettings.put(PROPERTY_USERNAME, "");
		defaultSettings.put(PROPERTY_PASSWORD, new PasswordString(""));
		defaultSettings.put(PROPERTY_FROM_MSISDN, new PhoneSection(""));
		defaultSettings.put(PROPERTY_USE_FOR_SENDING, Boolean.TRUE);
		defaultSettings.put(PROPERTY_USE_FOR_RECEIVING, Boolean.TRUE);
		return defaultSettings;
	}

	/** @see SmsInternetService#isConnected() */
	public synchronized boolean isConnected() {
		return this.connected;
	}

	/** @see SmsInternetService#isEncrypted() */
	public boolean isEncrypted() {
		return true;
	}

	/** @see SmsDevice#isBinarySendingSupported() */
	public boolean isBinarySendingSupported() {
		// Not sure about what Google Voice supports, but the Google Voice Java API we're
		// using doesn't support binary messages.
		return false;
	}

	/** @see SmsDevice#isUcs2SendingSupported() */
	public boolean isUcs2SendingSupported() {
		// TODO This needs testing before we know if UCS2 is supported or not
		return false;
	}

	/** @see SmsDevice#isUseForReceiving() */
	public boolean isUseForReceiving() {
		return getPropertyValue(PROPERTY_USE_FOR_RECEIVING, Boolean.class);
	}

	/** @see SmsDevice#isUseForSending() */
	public boolean isUseForSending() {
		return getPropertyValue(PROPERTY_USE_FOR_SENDING, Boolean.class);
	}

	/** @see SmsDevice#setUseForReceiving(boolean) */
	public void setUseForReceiving(boolean use) {
		this.setProperty(PROPERTY_USE_FOR_RECEIVING, new Boolean(use));
	}

	/** @see SmsDevice#setUseForSending(boolean) */
	public void setUseForSending(boolean use) {
		this.setProperty(PROPERTY_USE_FOR_SENDING, new Boolean(use));
	}

	/** @see SmsDevice#supportsReceive() */
	public boolean supportsReceive() {
		return true;
	}

//> PRIVATE HELPER METHODS
	/** @return {@link CIncomingMessage} representation of {@link GvSmsMessage} */
	private CIncomingMessage convertToSmsLibIncomingMessage(GvSmsMessage message) {
		return new CIncomingMessage(message.getSourceNumber(), message.getMessageContent());
	}
}
