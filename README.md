# [Retrieving and Parsing Hong Kong Stock Exchange Broker’s Information and Broker Queue Page from the real-time data feed](https://developers.refinitiv.com/en/article-catalog/article/Working-with-real-time-data-to-retrieve-Broker-Queue-Page-data)

## <a id="Overview"></a>Overview
The broker queue data is available in Refinitiv Real-time feed and can be retrieved by Refinitiv's APIs. In this article, we're going to retrieve the data of Hong Kong Stock Exchange Broker Queue Page by using Refinitiv Real-time SDK (RTSDK): Enterprise Message API (EMA) – Java to create the consumers and use them to subscribe to the data we’re interested “Broker Information” to create mapping between Broker ID and Broker Name/Broker Short Name and the data of “Broker Queue” that contains the broker ID of brokers in the order queue with orders of the best bid/ask price minus/plus 'n' spread(s).

_A screenshot of the broker queue data_

![image](https://user-images.githubusercontent.com/89068039/200814341-7d0272c2-52f6-4e58-a3f1-658d335ff634.png)

## <a id="Prerequisite"></a>Prerequisite
To be able to execute this example code, the below are required.

Real-Time-SDK-2.0.7.L1.java
JaveSE-1.8.
Deployed RTDS server
Code editor (Eclipse)

## <a id="FinalOutput"></a>Final output
_Here's an output of the code printed out to the console, consisting of Broker ID, Level (spread), Side (BID/ASK), Broker Name, and Broker Short Name._
```
Row  BrokerID  Level  Side   Broker Name                                       Broker Short Name   
1    1196      0      BID    CREDIT SUISSE SECURITIES (HONG KONG) LTD          CREDIT SUISSE       
2    1196      1      BID    CREDIT SUISSE SECURITIES (HONG KONG) LTD          CREDIT SUISSE       
3    8914      2      BID    BOCI SECURITIES LTD                               BOCI                
4    6998      2      BID    CHINA INVESTMENT INFORMATION SERVICES LIMITED     CHINA INVESTMEN     
5    2311      2      BID    HANG SENG SECURITIES LTD                          HANG SENG           
6    1196      2      BID    CREDIT SUISSE SECURITIES (HONG KONG) LTD          CREDIT SUISSE       
7    7572      3      BID    EMPEROR SECURITIES LTD                            EMPEROR             
8    1196      3      BID    CREDIT SUISSE SECURITIES (HONG KONG) LTD          CREDIT SUISSE       
9    2040      3      BID    BARCLAYS CAPITAL ASIA LTD                         BARCLAYS            
10   4373      3      BID    BARCLAYS CAPITAL ASIA LTD                         BARCLAYS            
11   4373      3      BID    BARCLAYS CAPITAL ASIA LTD                         BARCLAYS            
12   2311      4      BID    HANG SENG SECURITIES LTD                          HANG SENG           
13   1799      4      BID    BRIGHT SMART SEC INT (H.K.) LTD                   BRIGHT SMART        
14   8577      4      BID    HSBC SECURITIES BROKERS (ASIA) LTD                HSBC SEC            
15   1677      4      BID    CHINA TONGHAI SECURITIES LIMITED                  CHINA TONGHAI S     
16   1196      4      BID    CREDIT SUISSE SECURITIES (HONG KONG) LTD          CREDIT SUISSE       
17   2040      4      BID    BARCLAYS CAPITAL ASIA LTD                         BARCLAYS            
18   4373      4      BID    BARCLAYS CAPITAL ASIA LTD                         BARCLAYS            
19   2077      5      BID    BARCLAYS CAPITAL ASIA LTD                         BARCLAYS            
20   6387      5      BID    MORGAN STANLEY HONG KONG SECURITIES LTD           MORGAN STANLEY      
21   1196      5      BID    CREDIT SUISSE SECURITIES (HONG KONG) LTD          CREDIT SUISSE       
22   2040      5      BID    BARCLAYS CAPITAL ASIA LTD                         BARCLAYS            
23   4373      5      BID    BARCLAYS CAPITAL ASIA LTD                         BARCLAYS            
24   4373      5      BID    BARCLAYS CAPITAL ASIA LTD                         BARCLAYS            
25   8467      6      BID    FUTU SECURITIES INTERNATIONAL (HONG KONG) LIM     FUTU SECURITIES     
26   1799      6      BID    BRIGHT SMART SEC INT (H.K.) LTD                   BRIGHT SMART        
27   1799      6      BID    BRIGHT SMART SEC INT (H.K.) LTD                   BRIGHT SMART        
28   8577      6      BID    HSBC SECURITIES BROKERS (ASIA) LTD                HSBC SEC            
29   8574      6      BID    HSBC SECURITIES BROKERS (ASIA) LTD                HSBC SEC            
30   2079      6      BID    BARCLAYS CAPITAL ASIA LTD                         BARCLAYS            
31   6387      6      BID    MORGAN STANLEY HONG KONG SECURITIES LTD           MORGAN STANLEY      
32   4375      6      BID    BARCLAYS CAPITAL ASIA LTD                         BARCLAYS            
33   1196      6      BID    CREDIT SUISSE SECURITIES (HONG KONG) LTD          CREDIT SUISSE       
34   8550      6      BID    ABN AMRO CLEARING HONG KONG LTD                   ABN AMRO            
35   4373      0      ASK    BARCLAYS CAPITAL ASIA LTD                         BARCLAYS            
36   4373      0      ASK    BARCLAYS CAPITAL ASIA LTD                         BARCLAYS            
37   4373      0      ASK    BARCLAYS CAPITAL ASIA LTD                         BARCLAYS            
38   9069      0      ASK    UBS SECURITIES HONG KONG LTD                      UBS SEC             
39   6387      0      ASK    MORGAN STANLEY HONG KONG SECURITIES LTD           MORGAN STANLEY      
40   6387      0      ASK    MORGAN STANLEY HONG KONG SECURITIES LTD           MORGAN STANLEY      
41   6387      0      ASK    MORGAN STANLEY HONG KONG SECURITIES LTD           MORGAN STANLEY      
42   2075      0      ASK    BARCLAYS CAPITAL ASIA LTD                         BARCLAYS            
43   2310      1      ASK    HANG SENG SECURITIES LTD                          HANG SENG           
44   2077      1      ASK    BARCLAYS CAPITAL ASIA LTD                         BARCLAYS            
45   1196      1      ASK    CREDIT SUISSE SECURITIES (HONG KONG) LTD          CREDIT SUISSE       
46   8550      1      ASK    ABN AMRO CLEARING HONG KONG LTD                   ABN AMRO            
47   4373      1      ASK    BARCLAYS CAPITAL ASIA LTD                         BARCLAYS            
48   2040      1      ASK    BARCLAYS CAPITAL ASIA LTD                         BARCLAYS            
49   4373      1      ASK    BARCLAYS CAPITAL ASIA LTD                         BARCLAYS            
50   2428      1      ASK    BARCLAYS CAPITAL ASIA LTD                         BARCLAYS            
51   5344      2      ASK    J.P. MORGAN BROKING (HONG KONG) LTD               J.P. MORGAN         
52   4375      2      ASK    BARCLAYS CAPITAL ASIA LTD                         BARCLAYS            
53   8130      2      ASK    BOCI SECURITIES LTD                               BOCI                
54   1196      2      ASK    CREDIT SUISSE SECURITIES (HONG KONG) LTD          CREDIT SUISSE       
55   4373      2      ASK    BARCLAYS CAPITAL ASIA LTD                         BARCLAYS            
56   2042      2      ASK    BARCLAYS CAPITAL ASIA LTD                         BARCLAYS            
57   4373      2      ASK    BARCLAYS CAPITAL ASIA LTD                         BARCLAYS            
58   4373      2      ASK    BARCLAYS CAPITAL ASIA LTD                         BARCLAYS            
59   2075      2      ASK    BARCLAYS CAPITAL ASIA LTD                         BARCLAYS            
60   5342      2      ASK    J.P. MORGAN BROKING (HONG KONG) LTD               J.P. MORGAN         
61   5838      3      ASK    DBS VICKERS (HONG KONG) LTD                       DBS VICKERS         
62   6387      3      ASK    MORGAN STANLEY HONG KONG SECURITIES LTD           MORGAN STANLEY      
63   6387      3      ASK    MORGAN STANLEY HONG KONG SECURITIES LTD           MORGAN STANLEY      
64   1196      3      ASK    CREDIT SUISSE SECURITIES (HONG KONG) LTD          CREDIT SUISSE       
65   2042      3      ASK    BARCLAYS CAPITAL ASIA LTD                         BARCLAYS            
66   4373      3      ASK    BARCLAYS CAPITAL ASIA LTD                         BARCLAYS            
67   1292      4      ASK    CMB INTERNATIONAL SECURITIES LTD                  CMB                 
68   5838      4      ASK    DBS VICKERS (HONG KONG) LTD                       DBS VICKERS         
69   5339      4      ASK    J.P. MORGAN BROKING (HONG KONG) LTD               J.P. MORGAN         
70   5346      4      ASK    J.P. MORGAN BROKING (HONG KONG) LTD               J.P. MORGAN
```

## <a id="Summary"></a>Summary
Broker Queue data can be used as information about the broker and the price it sells/buys the equities. In this article, we map the Broker ID with the Broker Names to provide more information and make it easier to understand at a glance. Plus, explain how to retrieve the data of Broker Information and Broker Queue with Real-Time SDK Java - EMA then print it out to a readable format to make it easier to understand. If you have any comments or questions regarding this article, feel free to leave them on our [Q&A forum](https://community.developers.refinitiv.com/), we're happy to assist you regarding our APIs usage.
