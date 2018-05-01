package com.ift.toolchain.model;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.List;

@Entity
@Data
public class Satellite {

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name="system-uuid", strategy = "org.hibernate.id.UUIDGenerator")
    private String id;

    @Column(unique = true)
    private String name;

    private int orderOnOrbit;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "orbit_id")
    private Orbit orbit;

    @OneToMany(mappedBy = "satellitea")
    private List<SatelliteXSatellite> satelliteXSatellitesA;

    @OneToMany(mappedBy = "satelliteb")
    private List<SatelliteXSatellite> satelliteXSatellitesB;
}
