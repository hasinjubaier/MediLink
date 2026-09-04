package com.medilink.model.pharmacy;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Domain model representing a verified Pharmacy.
 * Mapped as a JPA @Entity for PostgreSQL persistence.
 */
@Entity
@Table(name = "pharmacies")
public class Pharmacy {

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "address", nullable = false, length = 200)
    private String address;

    @Column(name = "area", length = 100)
    private String area;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "latitude")
    private Double latitude = 0.0;

    @Column(name = "longitude")
    private Double longitude = 0.0;

    @Column(name = "is_24_hours")
    private Boolean is24Hours = true;

    @Column(name = "has_emergency_delivery")
    private Boolean hasEmergencyDelivery = true;

    public Pharmacy() {}

    public Pharmacy(String id, String name, String address, String area, String phone,
                    Double latitude, Double longitude, Boolean is24Hours, Boolean hasEmergencyDelivery) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.area = area;
        this.phone = phone;
        this.latitude = latitude;
        this.longitude = longitude;
        this.is24Hours = is24Hours != null ? is24Hours : true;
        this.hasEmergencyDelivery = hasEmergencyDelivery != null ? hasEmergencyDelivery : true;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public double getLatitude() { return latitude != null ? latitude : 0.0; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude != null ? longitude : 0.0; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public boolean is24Hours() { return is24Hours != null && is24Hours; }
    public void set24Hours(Boolean is24Hours) { this.is24Hours = is24Hours; }

    public boolean hasEmergencyDelivery() { return hasEmergencyDelivery != null && hasEmergencyDelivery; }
    public void setHasEmergencyDelivery(Boolean hasEmergencyDelivery) { this.hasEmergencyDelivery = hasEmergencyDelivery; }

    public double calculateDistanceKm(double userLat, double userLng) {
        double lat = getLatitude();
        double lng = getLongitude();
        double dLat = Math.toRadians(lat - userLat);
        double dLng = Math.toRadians(lng - userLng);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(userLat)) * Math.cos(Math.toRadians(lat)) *
                   Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 6371.0 * c; // Earth's radius in KM
    }
}
