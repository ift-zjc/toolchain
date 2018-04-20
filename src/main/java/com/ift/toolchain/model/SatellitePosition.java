package com.ift.toolchain.model;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

@Entity
@Data
public class SatellitePosition {

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name="system-uuid", strategy = "org.hibernate.id.UUIDGenerator")
    private String id;

    @ManyToOne
    @JoinColumn(name = "timepoint_id")
    private TimePoint timePoint;

    @ManyToOne
    @JoinColumn(name = "satellite_id")
    private Satellite satellite;

    private double x;
    private double y;
    private double z;

}
