package com.chapslife.septasquare.push.models;

import java.util.List;

import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

@PersistenceCapable
public class SeptaStop {

	@PrimaryKey
	private String venueId;
	@Persistent
	private String routeId;
	@Persistent
	private String stopId;
	@Persistent
	private String direction1;
	@Persistent
	private String direction2;
	
	public SeptaStop(String venueId, String routeId, String stopId, String direction1, String direction2) {
		this.venueId = venueId;
		this.routeId = routeId;
		this.stopId = stopId;
		this.direction1 = direction1;
		this.direction2 = direction2;
	}
	
	public String venueId() { return venueId; }
	public String routeId() { return routeId; }
	public String stopId() { return stopId; }
	public String direction1() { return direction1; }
	public String direction2() { return direction2; }
	
	public void save(PersistenceManager pm) {
		pm.makePersistent(this);
	}
	
	public static SeptaStop loadOrCreate(PersistenceManager pm, String venueId) {
	    try {
	      return pm.getObjectById(SeptaStop.class, venueId);
	    } catch (JDOObjectNotFoundException e) {
	      return new SeptaStop(venueId, "8495", null, "Eastbound","Westbound");
	    }
	  }
}
