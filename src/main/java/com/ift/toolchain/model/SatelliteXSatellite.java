package com.ift.toolchain.model;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

@Entity
@Data
public class SatelliteXSatellite {

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name="system-uuid", strategy = "org.hibernate.id.UUIDGenerator")
    private String id;


    private double angularVelocity;
    private double distance;

    @ManyToOne
    @JoinColumn(name = "timepoint_id")
    private TimePoint timePoint;

    @ManyToOne
    @JoinColumn(name = "satellite_id_a")
    private Satellite satellitea;

    @ManyToOne
    @JoinColumn(name = "satellite_id_b")
    private Satellite satelliteb;
}
