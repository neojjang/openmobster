/**
 * Copyright (c) {2003,2009} {openmobster@gmail.com} {individual contributors as indicated by the @authors tag}.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.openmobster.device.agent.test.framework;

import java.util.Vector;
import java.util.List;

import org.apache.log4j.Logger;

import org.openmobster.core.common.ServiceManager;
import org.openmobster.core.security.Provisioner;
import org.openmobster.core.synchronizer.server.SyncServer;
import org.openmobster.device.api.service.Request;
import org.openmobster.device.api.service.Response;
import org.openmobster.device.api.service.MobileService;

import org.openmobster.device.agent.frameworks.mobileObject.MobileObjectDatabase;
import org.openmobster.device.agent.sync.SyncAdapter;
import org.openmobster.device.agent.sync.SyncAdapterRequest;
import org.openmobster.device.agent.sync.SyncAdapterResponse;
import org.openmobster.device.agent.sync.engine.SyncEngine;
import org.openmobster.device.agent.sync.SyncService;
import org.openmobster.device.agent.frameworks.mobileObject.MobileObject;

/**
 * @author openmobster@gmail.com
 */
public class MobileBeanRunner 
{
	private static Logger log = Logger.getLogger(MobileBeanRunner.class);
	
	private String deviceId;
	private String serverId;
	private String service;
	private String user;
	private String credential;
	
	private SyncEngine  deviceSyncEngine;
	private SyncService syncService;
	private MobileObjectDatabase deviceDatabase;
	private Provisioner provisioner;
	private Configuration configuration;
	
	private CometDaemon cometDaemon;
	
	public MobileBeanRunner()
	{
		
	}

	public String getDeviceId() 
	{
		return deviceId;
	}

	public void setDeviceId(String deviceId) 
	{
		this.deviceId = deviceId;
	}

	public String getServerId() 
	{
		return serverId;
	}

	public void setServerId(String serverId) 
	{
		this.serverId = serverId;
	}

	public String getService() 
	{
		return service;
	}

	public void setService(String service) 
	{
		this.service = service;
	}

	public MobileObjectDatabase getDeviceDatabase() 
	{
		return deviceDatabase;
	}

	public void setDeviceDatabase(MobileObjectDatabase deviceDatabase) 
	{
		this.deviceDatabase = deviceDatabase;
	}

	public SyncEngine getDeviceSyncEngine() 
	{
		return deviceSyncEngine;
	}

	public void setDeviceSyncEngine(SyncEngine deviceSyncEngine) 
	{
		this.deviceSyncEngine = deviceSyncEngine;
	}
		
	public SyncService getSyncService() 
	{
		return syncService;
	}

	public void setSyncService(SyncService syncService) 
	{
		this.syncService = syncService;
	}
	
	public Provisioner getProvisioner() 
	{
		return provisioner;
	}

	public void setProvisioner(Provisioner provisioner) 
	{
		this.provisioner = provisioner;
	}
		
	public String getUser() 
	{
		return user;
	}

	public void setUser(String user) 
	{
		this.user = user;
	}
		
	public String getCredential() 
	{
		return credential;
	}

	public void setCredential(String credential) 
	{
		this.credential = credential;
	}
	
	

	public CometDaemon getCometDaemon() 
	{
		return cometDaemon;
	}

	public void setCometDaemon(CometDaemon cometDaemon) 
	{
		this.cometDaemon = cometDaemon;
	}
	
	public Configuration getConfiguration() 
	{
		return configuration;
	}

	public void setConfiguration(Configuration configuration) 
	{
		this.configuration = configuration;
	}

	public void start()
	{
		org.openmobster.device.agent.configuration.Configuration.getInstance().cleanup();			
	}
	
	public void stop()
	{
	}
	//----------------------------------------------------------------------------------------------------------------------
	public void activateDevice()
	{
		if(this.provisioner.getIdentityController().read(this.user) == null)
		{
			this.provisioner.registerIdentity(this.user, this.credential);
		}
		
		String server = null;
		String email = null;
		String deviceIdentifier = null;
		try
		{						
			deviceIdentifier = this.deviceId;						
			server = "127.0.0.1";
			email = this.user;
			String password = this.credential;
												
			//Set the unique device identifier
			configuration.setDeviceId(deviceIdentifier);
			configuration.setServerIp(server);
			configuration.setEmail(email);
			configuration.deActivateSSL();
			
			//TODO: clean this nasty nasty hack...with this, only single device sync scenarios
			//can be tested. Multi-Device Comet scenarios are still possible
			org.openmobster.device.agent.configuration.Configuration.getInstance().setDeviceId(deviceIdentifier);
			org.openmobster.device.agent.configuration.Configuration.getInstance().setServerIp(server);
			org.openmobster.device.agent.configuration.Configuration.getInstance().setEmail(email);
			org.openmobster.device.agent.configuration.Configuration.getInstance().deActivateSSL();
			
			Request request = new Request("provisioning");
			request.setAttribute("email", email);
			request.setAttribute("password", password);			
			request.setAttribute("identifier", deviceIdentifier);
			
			Response response = MobileService.invoke(request);
						
			if(response.getAttribute("error") == null)
			{
				//Success Scenario
				processProvisioningSuccess(response);
			}
			else
			{
				//Error Scenario
				String errorKey = response.getAttribute("error");
								
				throw new RuntimeException(errorKey);
			}						
		}
		catch(Exception e)
		{
			throw new RuntimeException(e.toString());
		}
	}
	
	private void processProvisioningSuccess(Response response)
	{				
		//Read the Server Response
		String serverId = response.getAttribute("serverId");
		String plainServerPort = response.getAttribute("plainServerPort");
		String secureServerPort = response.getAttribute("secureServerPort");
		String isSSlActive = response.getAttribute("isSSLActive");
		String maxPacketSize = response.getAttribute("maxPacketSize");
		String authenticationHash = response.getAttribute("authenticationHash");
		
		//Setup the configuration
		configuration.setServerId(serverId);
		configuration.setPlainServerPort(plainServerPort);
		if(secureServerPort != null && secureServerPort.trim().length()>0)
		{
			configuration.setSecureServerPort(secureServerPort);
		}
		
		/*if(isSSlActive.equalsIgnoreCase("true"))
		{
			configuration.activateSSL();
		}
		else
		{
			configuration.deActivateSSL();
		}*/
		
		configuration.setMaxPacketSize(Integer.parseInt(maxPacketSize));
		configuration.setAuthenticationHash(authenticationHash);
		
		
		//Setup the configuration
		//TODO: clean this nasty nasty hack...with this, only single device sync scenarios
		//can be tested. Multi-Device Comet scenarios are still possible
		org.openmobster.device.agent.configuration.Configuration.getInstance().setServerId(serverId);
		org.openmobster.device.agent.configuration.Configuration.getInstance().setPlainServerPort(plainServerPort);
		if(secureServerPort != null && secureServerPort.trim().length()>0)
		{
			org.openmobster.device.agent.configuration.Configuration.getInstance().setSecureServerPort(secureServerPort);
		}
		
		/*if(isSSlActive.equalsIgnoreCase("true"))
		{
			configuration.activateSSL();
		}
		else
		{
			configuration.deActivateSSL();
		}*/
		
		org.openmobster.device.agent.configuration.Configuration.getInstance().setMaxPacketSize(Integer.parseInt(maxPacketSize));
		org.openmobster.device.agent.configuration.Configuration.getInstance().setAuthenticationHash(authenticationHash);
	}
	
	public void bootService()
	{
		try
		{			
			this.performSync(0, SyncAdapter.BOOT_SYNC);
		}
		catch(Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	public void resetChannel()
	{
		try
		{
			this.performSync(0, SyncAdapter.ONE_WAY_CLIENT);
			this.performSync(0, SyncAdapter.BOOT_SYNC);
		}
		catch(Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	public void longBootup()
	{
		try
		{
			this.performSync(0, SyncAdapter.SLOW_SYNC);
		}
		catch(Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	public void syncService()
	{
		try
		{
			this.performSync(0, SyncAdapter.TWO_WAY);
		}
		catch(Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	public void syncObject(String objectId)
	{
		try
		{
			this.performSync(0, SyncAdapter.STREAM, objectId);
		}
		catch(Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	public void destroyService()
	{
		try
		{
			List<MobileObject> all = this.deviceDatabase.readByStorage(this.service);
			
			if(all != null)
			{
				for(MobileObject mobileObject: all)
				{
					this.delete(mobileObject);
				}
			}
			
			this.performSync(0, SyncAdapter.TWO_WAY);
		}
		catch(Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	public void create(MobileObject mobileObject)
	{
		try
		{
			mobileObject.setStorageId(this.service);
			this.deviceDatabase.create(mobileObject);
			
			Vector changelog = new Vector();
			org.openmobster.device.agent.sync.engine.ChangeLogEntry entry = 
			new org.openmobster.device.agent.sync.engine.ChangeLogEntry();
			entry.setNodeId(this.service);
			entry.setOperation(SyncEngine.OPERATION_ADD);
			entry.setRecordId(mobileObject.getRecordId());
			changelog.add(entry);
			this.deviceSyncEngine.addChangeLogEntries(changelog);
		}
		catch(Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	public void update(MobileObject mobileObject)
	{
		try
		{
			this.deviceDatabase.update(mobileObject);
			
			Vector changelog = new Vector();
			org.openmobster.device.agent.sync.engine.ChangeLogEntry entry = 
			new org.openmobster.device.agent.sync.engine.ChangeLogEntry();
			entry.setNodeId(this.service);
			entry.setOperation(SyncEngine.OPERATION_UPDATE);
			entry.setRecordId(mobileObject.getRecordId());
			changelog.add(entry);
			this.deviceSyncEngine.addChangeLogEntries(changelog);
		}
		catch(Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	public void delete(MobileObject mobileObject)
	{
		try
		{
			this.deviceDatabase.delete(mobileObject);
			
			Vector changelog = new Vector();
			org.openmobster.device.agent.sync.engine.ChangeLogEntry entry = 
			new org.openmobster.device.agent.sync.engine.ChangeLogEntry();
			entry.setNodeId(this.service);
			entry.setOperation(SyncEngine.OPERATION_DELETE);
			entry.setRecordId(mobileObject.getRecordId());
			changelog.add(entry);
			this.deviceSyncEngine.addChangeLogEntries(changelog);
		}
		catch(Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	public MobileObject read(String id)
	{
		try
		{
			MobileObject mobileObject = this.deviceDatabase.read(this.service, id);			
			return mobileObject;
		}
		catch(Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	public List<MobileObject> readAll()
	{
		try
		{
			return this.deviceDatabase.readByStorage(this.service);			
		}
		catch(Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	//------Synchronization related------------------------------------------------------------------------------------------------
	private void performSync(int maxClientSize, String syncType) throws Exception
	{
		// Get the initialization payload
		SyncAdapterRequest initRequest = new SyncAdapterRequest();
		initRequest.setAttribute(SyncServer.SOURCE, this.deviceId);
		initRequest.setAttribute(SyncServer.TARGET, this.serverId);
		initRequest.setAttribute(SyncServer.MAX_CLIENT_SIZE, String.valueOf(maxClientSize));
		initRequest.setAttribute(SyncServer.CLIENT_INITIATED, "true");
		initRequest.setAttribute(SyncServer.DATA_SOURCE, service);
		initRequest.setAttribute(SyncServer.DATA_TARGET, service);
		initRequest.setAttribute(SyncServer.SYNC_TYPE, syncType);
		this.executeSyncProtocol(initRequest);		
	}
	
	private void performSync(int maxClientSize, String syncType, String oid) throws Exception
	{
		// Get the initialization payload
		SyncAdapterRequest initRequest = new SyncAdapterRequest();
		initRequest.setAttribute(SyncServer.SOURCE, this.deviceId);
		initRequest.setAttribute(SyncServer.TARGET, this.serverId);
		initRequest.setAttribute(SyncServer.MAX_CLIENT_SIZE, String.valueOf(maxClientSize));
		initRequest.setAttribute(SyncServer.CLIENT_INITIATED, "true");
		initRequest.setAttribute(SyncServer.DATA_SOURCE, service);
		initRequest.setAttribute(SyncServer.DATA_TARGET, service);
		initRequest.setAttribute(SyncServer.SYNC_TYPE, syncType);
		initRequest.setAttribute(SyncServer.STREAM_RECORD_ID, oid);
		this.executeSyncProtocol(initRequest);		
	}
	
	private void executeSyncProtocol(SyncAdapterRequest initRequest) throws Exception
	{
		//Get the Server Sync Adapter
		SyncServer serverSyncAdapter = (SyncServer)ServiceManager.locate("synchronizer://SyncServerAdapter");		

		// Get the Device Sync Adapter
		SyncAdapter deviceSyncAdapter = new SyncAdapter();
		deviceSyncAdapter.setSyncEngine(this.deviceSyncEngine);
		
		SyncAdapterResponse initResponse = deviceSyncAdapter.start(initRequest);

		log.info("-----------------------------");
		log.info("Client ="
				+ ((String)initResponse.getAttribute(SyncServer.PAYLOAD)).replaceAll("\n", ""));
		log.info("-----------------------------");

		boolean close = false;
		SyncAdapterRequest clientRequest = new SyncAdapterRequest();
		org.openmobster.core.synchronizer.model.SyncAdapterRequest serverRequest = 
		new org.openmobster.core.synchronizer.model.SyncAdapterRequest();
		serverRequest.setAttribute(SyncServer.PAYLOAD, initResponse
				.getAttribute(SyncServer.PAYLOAD));
		do
		{
			org.openmobster.core.synchronizer.model.SyncAdapterResponse serverResponse = serverSyncAdapter
					.service(serverRequest);

			// Setup request to be sent to the Client Syncher
			// based on payload received from the server
			String payload = (String) serverResponse
					.getAttribute(SyncServer.PAYLOAD);
			log.info("-----------------------------");
			log.info("Server =" + payload.replaceAll("\n", ""));
			log.info("-----------------------------");

			clientRequest = new SyncAdapterRequest();
			clientRequest.setAttribute(SyncServer.PAYLOAD, payload);
			SyncAdapterResponse clientResponse = deviceSyncAdapter
					.service(clientRequest);

			if (clientResponse.getStatus() == SyncServer.RESPONSE_CLOSE)
			{
				close = true;
			}
			else
			{
				payload = (String) clientResponse
						.getAttribute(SyncServer.PAYLOAD);
				serverRequest.setAttribute(SyncServer.PAYLOAD, payload);
				log.info("-----------------------------");
				log.info("Client =" + payload.replaceAll("\n", ""));
				log.info("-----------------------------");
			}

		} while (!close);
	}				
	//------Device Side Only--------------------------------------------------------------------------------------------------------------------
	public void appReset()
	{
		try
		{
			List<MobileObject> all = this.deviceDatabase.readByStorage(this.service);
			
			if(all != null)
			{
				for(MobileObject mobileObject: all)
				{
					this.deviceDatabase.delete(mobileObject);
				}
			}						
		}
		catch(Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	public void resetSyncAnchor()
	{
		try
		{
			this.deviceSyncEngine.getSyncDataSource().deleteAnchor();
		}
		catch(Exception e)
		{
			throw new RuntimeException(e.toString());
		}
	}
	//-----------------------------------------------------------------------------------------------------------
	public void startCometDaemon()
	{
		Thread thread = new Thread(new Runnable(){
			public void run()
			{
				try
				{
					MobileBeanRunner.this.cometDaemon.startSubscription();
					MobileBeanRunner.this.cometDaemon.waitforCometDaemon();
				}
				catch(Exception e)
				{
					log.error(this, e);
				}
			}
		});
		thread.start();
	}
}