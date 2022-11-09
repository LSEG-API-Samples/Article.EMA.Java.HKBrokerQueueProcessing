// *|-------------------------------------------------------------------------------------------------
// *|            This source code is provided under the Apache 2.0 license      					--
// *|  and is provided AS IS with no warranty or guarantee of fit for purpose.  					--
// *|                See the project's LICENSE.md for details.                  					--
// *|           Copyright (C) 2022 LSEG. All rights reserved.            							--
// *|---------------------------------------------------------------------------------------------------------
// *|                                   ## Disclaimer ##                              						--
// *|The source presented here has been written by LSEG for the only purpose of illustrating an article 	--
// *|published on the Developer Community. They have not been tested for usage in production environments. 	--
// *|LSEG cannot be held responsible for any issues that may happen if these objects or the related 		--
// *|source code are used in production or any other client environment.									--
// *|---------------------------------------------------------------------------------------------------------

package com.refinitiv.ema.examples.article.consumer.parsepage;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.refinitiv.ema.access.FieldEntry;
import com.refinitiv.ema.access.Msg;
import com.refinitiv.ema.access.OmmArray;
import com.refinitiv.ema.access.AckMsg;
import com.refinitiv.ema.access.Data;
import com.refinitiv.ema.access.GenericMsg;
import com.refinitiv.ema.access.RefreshMsg;
import com.refinitiv.ema.access.StatusMsg;
import com.refinitiv.ema.access.UpdateMsg;
import com.refinitiv.ema.access.DataType.DataTypes;
import com.refinitiv.ema.rdm.EmaRdm;
import com.refinitiv.ema.access.DataType;
import com.refinitiv.ema.access.ElementList;
import com.refinitiv.ema.access.EmaFactory;
import com.refinitiv.ema.access.FieldList;
import com.refinitiv.ema.access.OmmConsumer;
import com.refinitiv.ema.access.OmmConsumerClient;
import com.refinitiv.ema.access.OmmConsumerEvent;
import com.refinitiv.ema.access.OmmException;
//for partial update
import java.util.TreeMap;
import com.refinitiv.ema.access.RmtesBuffer;

// This class retrieves the Broker Information (to create the mapping of Broker ID and Broker Name/Short Name) 
class BrokerIDClient implements OmmConsumerClient {

	// create a mapping to store Broker Info: Broker ID (String), Broker Names (Broker)
	public HashMap<String, Broker> brokerInfoMapping = new HashMap<String, Broker>();
	// create a mapping to check the Broker Info mapping creation status (if it's ready): Broker Info Page RIC (String), Is ready flag (Boolean)
	public HashMap<String, Boolean> brokerInfoMappingStatus = new HashMap<String, Boolean>();

	// This function used to check that the Broker Information (of each Broker Info page) is ready for the next step
	public boolean isReady() {
		for (Map.Entry<String, Boolean> set : brokerInfoMappingStatus.entrySet()) {
			if (!set.getValue()) {
				return false;
			}
		}
		return true;
	}

	// Decode the data returned.
	// Plus, to make sure that the Broker Information is ready for the next step.
	// The mapping between Broker Info Page RIC (e.g. HKBK02) and its message status identifies if the data is already received.
	public void onRefreshMsg(RefreshMsg refreshMsg, OmmConsumerEvent event) {
		if (DataType.DataTypes.FIELD_LIST == refreshMsg.payload().dataType())
			// Call the decode function
			decode(refreshMsg.payload().fieldList());

		brokerInfoMappingStatus.put(refreshMsg.name(), true);
		System.out.println();	}

	//For Acknowledge message, do nothing
	@Override
	public void onAckMsg(AckMsg arg0, OmmConsumerEvent arg1) {
	}

	//For All message, do nothing
	@Override
	public void onAllMsg(Msg arg0, OmmConsumerEvent arg1) {
	}

	//For generic message, do nothing
	@Override
	public void onGenericMsg(GenericMsg arg0, OmmConsumerEvent arg1) {
	}

	//For status message, print status(Item name, Service name, Service state)
	@Override
	public void onStatusMsg(StatusMsg statusMsg, OmmConsumerEvent arg1) {
		if (statusMsg.hasName())
			System.out.println("Item Name: " + statusMsg.name());

		if (statusMsg.hasServiceName())
			System.out.println("Service Name: " + statusMsg.serviceName());

		if (statusMsg.hasState())
			System.out.println("Service State: " + statusMsg.state());

		System.out.println("\n");
	}
	
	// This is unused, as we only use the RefreshMsg to get the broker ID/names mapping
	// (no update is expected on this data)
	@Override
	public void onUpdateMsg(UpdateMsg updateMsg, OmmConsumerEvent arg1) {
	}
	
	// when String returned represents more than one number, it can be processed by checking the delimiter(s) in the String.
	// the format of this string is, starting with the first number then the delimiter followed by the last digit of the next number,
	// the delimiter(s) in the String shows how to process the Broker ID list:
		// Slash (/): Iterate from the start number till the number which its last digit matched with the digit after the delimiter.
		// Comma (,): The start number and the number with the same last digit as the digit after the delimiter.
	public void pasreBrokerID(String str, String brokerName, String brokerShortName) {
		if (str.trim().length() < 4) {
			// not a broker, skip
			// ". "
			if (str.trim().isEmpty()) {
				// no broker ID exist on that position
				return;
			}
		} else if (str.trim().length() == 4) {
			// maybe a 4 digit
			// "1234"
			try {
				int id = Integer.parseInt(str.trim());
				// a valid 4 digit -> put it (Broker ID) along with its names into the mapping
				brokerInfoMapping.put(String.format("%04d", id), new Broker(String.format("%04d", id), brokerName, brokerShortName));
			} catch (NumberFormatException e) {
				// cannot be parsed, not a valid digit, skip
			}
		} else if (str.trim().length() == 6 && str.trim().indexOf(",") == 4) {
			// must contain 4 digits then ,
			// "0380,2" -> 0380, 0382 -> put them (Broker IDs) along with the names into the mapping
			int id = Integer.parseInt(str.trim().substring(0, 4));
			int id2 = Integer.parseInt(str.trim().substring(0, 3) + str.trim().substring(5, 6));
			brokerInfoMapping.put(String.format("%04d", id), new Broker(String.format("%04d", id), brokerName, brokerShortName));
			brokerInfoMapping.put(String.format("%04d", id2), new Broker(String.format("%04d", id2), brokerName, brokerShortName));
		} else if (str.trim().length() == 6 && str.trim().indexOf("/") == 4) {
			// must contain 4 digits then /
			// "0380/5" -> 0380, 0381, 0382, 0383, 0384, 0385 -> put them (Broker IDs) along with the names into the mapping
			int id = Integer.parseInt(str.trim().substring(0, 4));
			int id2 = Integer.parseInt(str.trim().substring(0, 3) + str.trim().substring(5, 6));
			for (int i = id; i <= id2; i++) {
				brokerInfoMapping.put(String.format("%04d", i), new Broker(String.format("%04d", i), brokerName, brokerShortName));
			}
		} else if (str.trim().length() == 8 && str.trim().indexOf(",") == 4 && str.trim().indexOf("/") == 6) {
			// must contain , then /
			// "0380,2/4" -> 0380, 0382, 0383, 0384 -> put them (Broker IDs) along with the names into the mapping
			int id = Integer.parseInt(str.trim().substring(0, 4));
			brokerInfoMapping.put(String.format("%04d", id), new Broker(String.format("%04d", id), brokerName, brokerShortName));

			int id2 = Integer.parseInt(str.trim().substring(0, 3) + str.trim().substring(5, 6));
			int id3 = Integer.parseInt(str.trim().substring(0, 3) + str.trim().substring(7, 8));
			for (int i = id2; i <= id3; i++) {
				brokerInfoMapping.put(String.format("%04d", i), new Broker(String.format("%04d", i), brokerName, brokerShortName));
			}
		} else if (str.trim().length() == 8 && str.trim().indexOf("/") == 4 && str.trim().indexOf(",") == 6) {
			// must contain , then /
			// "0380/2,4" -> 0380, 0381, 0382, 0384 -> put them (Broker IDs) along with the names into the mapping
			int id = Integer.parseInt(str.trim().substring(0, 4));
			int id2 = Integer.parseInt(str.trim().substring(0, 3) + str.trim().substring(5, 6));
			for (int i = id; i <= id2; i++) {
				brokerInfoMapping.put(String.format("%04d", i), new Broker(String.format("%04d", i), brokerName, brokerShortName));
			}

			int id3 = Integer.parseInt(str.trim().substring(0, 3) + str.trim().substring(7, 8));
			brokerInfoMapping.put(String.format("%04d", id3), new Broker(String.format("%04d", id3), brokerName, brokerShortName));
		} else if (str.trim().length() == 10) {
			// must contain , then /
			// "0380,2/3/7" -> 0380, 0382, 0383, 0384, 0385, 0386, 0387 -> put them (Broker IDs) along with the names into the mapping
			if ((str.trim().charAt(4) == ',' || str.trim().charAt(4) == '/')
					&& (str.trim().charAt(6) == ',' || str.trim().charAt(6) == '/')
					&& (str.trim().charAt(8) == ',' || str.trim().charAt(8) == '/')) {

				int id = Integer.parseInt(str.trim().substring(0, 4));
				int id2 = Integer.parseInt(str.trim().substring(0, 3) + str.trim().substring(5, 6));
				int id3 = Integer.parseInt(str.trim().substring(0, 3) + str.trim().substring(7, 8));
				int id4 = Integer.parseInt(str.trim().substring(0, 3) + str.trim().substring(9, 10));
            
				if (str.trim().charAt(4) == ',') {
					// add id
					brokerInfoMapping.put(String.format("%04d", id),
							new Broker(String.format("%04d", id), brokerName, brokerShortName));
					if (str.trim().charAt(6) == '/') {
						// for id2 to id3
						for (int i = id2; i <= id3; i++) {
							brokerInfoMapping.put(String.format("%04d", i),
									new Broker(String.format("%04d", i), brokerName, brokerShortName));
						}
						if (str.trim().charAt(8) == ',') {
							// add id4
							brokerInfoMapping.put(String.format("%04d", id4),
									new Broker(String.format("%04d", id4), brokerName, brokerShortName));
						} else if (str.trim().charAt(8) == '/') {
							// should not happen as / then /
						}
					} else if (str.trim().charAt(6) == ',') {
						// add id2
						brokerInfoMapping.put(String.format("%04d", id2),
								new Broker(String.format("%04d", id2), brokerName, brokerShortName));

						if (str.trim().charAt(8) == ',') {
							// add id3, id4
							brokerInfoMapping.put(String.format("%04d", id3),
									new Broker(String.format("%04d", id3), brokerName, brokerShortName));
							brokerInfoMapping.put(String.format("%04d", id4),
									new Broker(String.format("%04d", id4), brokerName, brokerShortName));
						} else if (str.trim().charAt(8) == '/') {
							// for id3 to id4
							for (int i = id3; i <= id4; i++) {
								brokerInfoMapping.put(String.format("%04d", i),
										new Broker(String.format("%04d", i), brokerName, brokerShortName));
							}
						}
					}
				} else if (str.trim().charAt(4) == '/') {
					// must contain / then , then / or ,
					for (int i = id; i <= id2; i++) {
						brokerInfoMapping.put(String.format("%04d", i),
								new Broker(String.format("%04d", i), brokerName, brokerShortName));
					}
					if (str.trim().charAt(6) == '/') {
						// should not happen
					} else if (str.trim().charAt(6) == ',') {
						// do nothing
						if (str.trim().charAt(8) == ',') {
							// add id3, id4
							brokerInfoMapping.put(String.format("%04d", id3),
									new Broker(String.format("%04d", id3), brokerName, brokerShortName));
							brokerInfoMapping.put(String.format("%04d", id4),
									new Broker(String.format("%04d", id4), brokerName, brokerShortName));
						} else if (str.trim().charAt(8) == '/') {
							// for id3 to id4
							for (int i = id3; i <= id4; i++) {
								brokerInfoMapping.put(String.format("%04d", i),
										new Broker(String.format("%04d", i), brokerName, brokerShortName));
							}
						}
					}
				}
			} else {
				// invalid broker (non-ID), Do nothing (Ignore it)
				return;
			}

		}
	}

	// decode the field data based on its data type
	void decode(FieldList fieldList) {
		// check each field in the message
		for (FieldEntry fieldEntry : fieldList) {
			if (Data.DataCode.BLANK == fieldEntry.code())
				System.out.println(" blank");
			else
				// check a type of the field
				switch (fieldEntry.loadType()) {
				// field is RMTES data type (e.g. Broker Info data)
				// -> convert msg to String and substring to extract value of Broker IDs and Broker Names by substring on the fixed positions
				case DataTypes.RMTES:
					String brokerInfoMapping = fieldEntry.rmtes().toString();
					int brokerInfoMappingLen = brokerInfoMapping.length() - 1;
					String brokerIDs1 = brokerInfoMapping.substring(0, 8);
					String brokerIDs2 = brokerInfoMapping.substring(8, 19);
					String brokerName = brokerInfoMapping.substring(19, 64).trim();
					String brokerShortName = brokerInfoMapping.substring(64, brokerInfoMappingLen).trim();
					pasreBrokerID(brokerIDs1, brokerName, brokerShortName);
					pasreBrokerID(brokerIDs2, brokerName, brokerShortName);
					break;
				case DataTypes.ERROR:
					System.out.println("(" + fieldEntry.error().errorCodeAsString() + ")");
					break;
				default:
					System.out.println();
					break;
				}
		}
	}
}

// This class retrieve the Broker Queue Page data and lookup Broker Names by Broker ID in the mapping created
// -> Then print the data out to the console
class BrokerQueueClient implements OmmConsumerClient {
	//for counting the number of messages retrieved and print it out on the console
	int count = 0;
	
	// Broker Queue Object (Consist of Broker Queue data)
	ArrayList<BrokerQueue> brokerQueueObject;
	// Broker Information (The Mapping from another client)
	HashMap<String, Broker> brokerInformation;

	// a Map keeps field Id as a key with RmtesBuffer
	TreeMap<Integer, RmtesBuffer> pageMap = new TreeMap<Integer, RmtesBuffer>();


	// Decode the data returned.
	public void onRefreshMsg(RefreshMsg refreshMsg, OmmConsumerEvent event) {
		if (refreshMsg.hasName())
			System.out.println("Item Name: " + refreshMsg.name());

		if (refreshMsg.hasServiceName())
			System.out.println("Service Name: " + refreshMsg.serviceName());

		System.out.println("Item State: " + refreshMsg.state());

		if (DataType.DataTypes.FIELD_LIST == refreshMsg.payload().dataType())
			decode(refreshMsg.payload().fieldList());

		System.out.println("\n");
	}

	// Decode the data returned and to parse it into the output.
	public void onUpdateMsg(UpdateMsg updateMsg, OmmConsumerEvent event) {
		if (updateMsg.hasName())
			System.out.println("Item Name: " + updateMsg.name());

		if (updateMsg.hasServiceName())
			System.out.println("Service Name: " + updateMsg.serviceName());

		if (DataType.DataTypes.FIELD_LIST == updateMsg.payload().dataType())
			decode(updateMsg.payload().fieldList());

		System.out.println("\n");
	}
	
	// For status message, print status(Item name, Service name, Service state)
	public void onStatusMsg(StatusMsg statusMsg, OmmConsumerEvent event) {
		if (statusMsg.hasName())
			System.out.println("Item Name: " + statusMsg.name());

		if (statusMsg.hasServiceName())
			System.out.println("Service Name: " + statusMsg.serviceName());

		if (statusMsg.hasState())
			System.out.println("Service State: " + statusMsg.state());

		System.out.println("\n");
	}

	// For Generic message, do nothing
	public void onGenericMsg(GenericMsg genericMsg, OmmConsumerEvent consumerEvent) {}

	// For Acknowledge message, do nothing
	public void onAckMsg(AckMsg ackMsg, OmmConsumerEvent consumerEvent) {}

	// For All message, do nothing
	public void onAllMsg(Msg msg, OmmConsumerEvent consumerEvent) {}

	// decode the field data based on its data type
	void decode(FieldList fieldList) {
		brokerQueueObject = new ArrayList<>();
		// iterate through each field in the message
		Iterator<FieldEntry> iter = fieldList.iterator();
		FieldEntry fieldEntry;
		while (iter.hasNext()) {
			// set next field (if exist) into fieldEntry variable
			fieldEntry = iter.next();
			// check if the field ID is ID we're interested and set it on the 
			// regarding the detail of this code, you're recommended to check an article
			// "How to parse page-based data using Refinitiv Real-time SDK Java":
			//		https://developers.refinitiv.com/en/article-catalog/article/how-to-parse-page-based-data-using-elektron-sdk-java#
			if ((fieldEntry.fieldId() >= 215 && fieldEntry.fieldId() <= 228)
					|| (fieldEntry.fieldId() >= 315 && fieldEntry.fieldId() <= 339)
					|| (fieldEntry.fieldId() >= 1359 && fieldEntry.fieldId() <= 1378)) {
				// if the field id does not exist in the map, create new RmtesBuffer object
				if (!pageMap.containsKey(fieldEntry.fieldId())) {
					pageMap.put(fieldEntry.fieldId(), EmaFactory.createRmtesBuffer());

				}
				// call apply() to interpret the full update and the partial update
				pageMap.get(fieldEntry.fieldId()).apply(fieldEntry.rmtes());
			}
		}
		System.out.println(
				"=================================================================================================================");
		// prints all page fields in the map on the console to display the page
		try {
			//        				BID                    ASK                      
			//  		 i=0   i=1  i=2   i=3     i=4   i=5    i=6  i=7	
			// (j=0)	6998  7389  8565  8443 |  4973  0757  3436  1799                            
			// (j=1)	7389  7389   -1s  8444 |  3448  1045  8565  8565                                 
			String brokerQueue[][] = new String[8][10];

			// for each field ID in the message
			for (Integer fieldId : pageMap.keySet()) {
				byte[] utf16Bytes = pageMap.get(fieldId).toString().getBytes("UTF16");

				// take only fields that contain the broker queue list data
				if (fieldId >= 317 && fieldId <= 326) {
					String row = new String(utf16Bytes, Charset.forName("UTF-16"));

					for (int i = 0; i < 4; i++) {
						brokerQueue[i][fieldId - 317] = row.substring(1 + (6 * i), 5 + (6 * i));
					}
					// ASK
					for (int i = 0; i < 4; i++) {
						brokerQueue[i + 4][fieldId - 317] = row.substring(27 + (6 * i), 31 + (6 * i));
					}
				}
			}

			// set level (orders of the best bid/ask price minus/plus 'n' spread(s))
			int bidLevel = 0;
			int askLevel = 0;
			for (int i = 0; i < brokerQueue.length; i++) {
				// Loop through all elements of each row in the broker queue
				for (int j = 0; j < brokerQueue[i].length; j++) {
					String brokerID = brokerQueue[i][j];
					// i is 0-3 -> bid data
					if (0 <= i && i <= 3) {
						if (brokerID.contains("s")) {
							bidLevel += 1;
							// found -ns (n is any digit such as -1s), add bid level by 1
							continue;
						}
						if (brokerID.trim().length() == 0) {
							// skip empty substring, do nothing
							continue;
						}
						
						// create BrokerQueue object and add it into the BrokerQueueObject list
						BrokerQueue brokerQueueBid = new BrokerQueue(brokerID, "BID", bidLevel,
								this.brokerInformation.get(brokerID));
						brokerQueueObject.add(brokerQueueBid);
					// i is 4-7 -> ask data
					} else if (4 <= i && i <= 7) {
						if (brokerID.contains("s")) {
							askLevel += 1;
							// found +ns (n is any digit such as +1s), add ask level by 1
							continue;
						} else if (brokerID.trim().length() == 0) {
							// skip empty substring, do nothing
							continue;
						}

						// create BrokerQueue object and add it into the BrokerQueueObject list
						BrokerQueue brokerQueueAsk = new BrokerQueue(brokerID, "ASK", askLevel,
								this.brokerInformation.get(brokerID));
						brokerQueueObject.add(brokerQueueAsk);
					}
				}
			}
			Thread.sleep(3000);
			count+=1;	
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// Print the output table out
		System.out.println("Message: "+count);
		
		// Output table header: BrokerQueueID | Level | Side | Broker | Name
		String tableHeader = String.format("%-5s%-10s%-7s%-7s%-50s%-20s", "Row", "BrokerID", "Level", "Side", "Broker Name", "Broker Short Name");
		
		System.out.println(tableHeader);
		int rowNum = 1;
		// For each row of broker queue, print each broker object base on the format below
		// Row  BrokerID  Level  Side   	Broker Name                                       Broker Short Name   
		//	1    1836      0      BID    INSTINET PACIFIC LTD                              INSTINET PACIFI     
		for (BrokerQueue brokerInQueue: brokerQueueObject) {
			String tableRow = String.format("%-5s%-89s%n", rowNum, brokerInQueue);
			System.out.print(tableRow);
			rowNum += 1;
		}
	}
}

public class Consumer {
	// As Broker Information Pages list of Hong Kong Stock exchange RIC is stored in around HKBK02-HKBK50
	// hence create method to return the list of RICs mentioned to retrieve the data
	public static String[] returnBrokerInfoPageList(String broker, int pageNum) {
		String[] brokerInfoList = new String[pageNum - 1];
		for (int i = 2; i <= pageNum; i++) {
			if (i < 10) {
				// i = 2 -> HKBK02
				brokerInfoList[i - 2] = broker + "0" + i;
			} else {
				// i = 10 -> HKBK10
				brokerInfoList[i - 2] = broker + i;
			}
		}
		return (brokerInfoList);
	}

	public static void main(String[] args) {
		OmmConsumer consumer = null;
		try {

			// [ Step 1)] Retrieve Broker Information Data (output is a mapping of Broker ID, Broker Names)
			// set the broker RIC Name (Hong Kong) and the RIC length. In this case we'd like to get HKBK02, HKBK03, ..., HKBK49, HKBK50
			String brokerRicName = "HKBK";
			int brokerInfoLen = 50;

			String[] brokerInfoList = returnBrokerInfoPageList(brokerRicName, brokerInfoLen);

			// create a client of broker queue
			BrokerQueueClient brokerQueueClient = new BrokerQueueClient();

			consumer = EmaFactory.createOmmConsumer(
					EmaFactory.createOmmConsumerConfig().host("192.168.1.1:14002").username("user"));

			BrokerIDClient brokerIDClient = new BrokerIDClient();

			// To filter only selected field (example code that is useful, you may check these example files: 360_MP_View, 370 MP_Batch)
			// Prepare multiple field IDs in an OmmArray
			OmmArray array = EmaFactory.createOmmArray();
			array.fixedWidth(2);

			// create a client of broker Info (to get only specified fields, which are fields with Broker Information data) [View]
			for (int i = 317; i <= 338; i++) {
				array.add(EmaFactory.createOmmArrayEntry().intValue(i));
			}

			// Prepare multiple RICs in an OmmArray [Batch]
			OmmArray arrayI = EmaFactory.createOmmArray();

			for (String brokerInfo : brokerInfoList) {
				arrayI.add(EmaFactory.createOmmArrayEntry().ascii(brokerInfo));
				// Add the Broker ID as a key of Hashmap and status if the mapping has been done properly on each Broker Info Page RIC
				// initial status is False (it'll be updated to True after the mapping of each RIC is finished)
				brokerIDClient.brokerInfoMappingStatus.put(brokerInfo, false);
			}

			// Combine both Batch and View and add them to ElementList
			ElementList batchView = EmaFactory.createElementList();
			batchView.add(EmaFactory.createElementEntry().array(EmaRdm.ENAME_BATCH_ITEM_LIST, arrayI));
			batchView.add(EmaFactory.createElementEntry().uintValue(EmaRdm.ENAME_VIEW_TYPE, 1));
			batchView.add(EmaFactory.createElementEntry().array(EmaRdm.ENAME_VIEW_DATA, array));

			// Register a Request Message of the Broker Info
			// add Batch and View Request to the Request
			consumer.registerClient(
					EmaFactory.createReqMsg().serviceName("ELEKTRON_DD").payload(batchView).interestAfterRefresh(false),
					brokerIDClient);

			// Wait until all the Broker information mapping is created
			while (!brokerIDClient.isReady()) {
				Thread.sleep(1000);
			}
			// [ Step 2)] Retrieve Broker Queue and print the data with Broker Info into the console (an example code output is below)
			//	Row  BrokerID  Level  Side   Broker Name                                       Broker Short Name   
			//	1    1836      0      BID    INSTINET PACIFIC LTD                              INSTINET PACIFI     
			//	2    8578      0      BID    HSBC SECURITIES BROKERS (ASIA) LTD                HSBC SEC      
			// set the Broker Information Mapping (copy this variable from broker queue client to broker ID client)
			brokerQueueClient.brokerInformation = brokerIDClient.brokerInfoMapping;      

			// Register a Request Message of the Broker Queue
			consumer.registerClient(EmaFactory.createReqMsg().serviceName("ELEKTRON_DD").name("0006bk.HKd"), brokerQueueClient,
					0);
			

			Thread.sleep(30000); // API calls onRefreshMsg(), onUpdateMsg() and onStatusMsg()
		} catch (InterruptedException | OmmException excp) {
			System.out.println(excp.getMessage());
		} finally {
			if (consumer != null)
				consumer.uninitialize();
		}
	}
}