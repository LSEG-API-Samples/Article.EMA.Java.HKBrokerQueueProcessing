package com.refinitiv.ema.examples.article.consumer.parsepage;

public class Broker {
	public String id;
	public String name;
	public String shortName;

	public Broker(String id, String name, String shortName) {
		this.id = id;
		this.name = name;
		this.shortName = shortName;
	}
	
	@Override
	public String toString() {
	  return getClass().getSimpleName() + "ID:" + id + ", Name: " + name + ", shortName: " + shortName;
	}
}