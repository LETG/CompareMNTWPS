package fr.indigeo.wps.mnt.utils;

import org.locationtech.jts.geom.Coordinate;

public class ElevationInformation {
    
    Integer number;
	Coordinate coordinate;
	Double altitude;
	
	public ElevationInformation() {}

	public ElevationInformation(Integer number, Coordinate coordinate, Double altitude) {
		super();
		this.number = number;
		this.coordinate = coordinate;
		this.altitude = altitude;
	}

	public Integer getNumber() {
		return number;
	}

	public void setNumber(Integer number) {
		this.number = number;
	}

	public Coordinate getCoordinate() {
		return coordinate;
	}

	public void setCoordinate(Coordinate coordinate) {
		this.coordinate = coordinate;
	}

	public Double getAltitude() {
		return altitude;
	}

	public void setAltitude(Double altitude) {
		this.altitude = altitude;
	}

}
