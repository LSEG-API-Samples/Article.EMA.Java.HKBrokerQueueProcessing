package com.refinitiv.ema.examples.article.consumer.parsepage;

public class BrokerQueue {
	public String brokerID;
	public String side;
	public int level;
	public Broker broker;

	public BrokerQueue(String brokerID, String side, int level, Broker broker) {
		this.brokerID = brokerID;
		this.level = level;
		this.broker = broker;
		this.side = side;
	}

	@Override
	public String toString() {
		return String.format("%-10s%-7s%-7s%-50s%-20s", brokerID, level, side, this.broker.name, this.broker.shortName);
	}
}
