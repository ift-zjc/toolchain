package com.ift.toolchain.model;


import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.util.List;

@Entity
@Data
public class TimePoint {

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name="system-uuid", strategy = "org.hibernate.id.UUIDGenerator")
    private String id;

    private float offset;

    @OneToMany(mappedBy = "timePoint")
    private List<SatellitePosition> satellitePositions;

    @OneToMany(mappedBy = "timePoint")
    private List<SatelliteXSatellite> satelliteXSatellites;
}
