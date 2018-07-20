package com.ift.toolchain.model;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

@Entity
@Data
public class Parameter {

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name="system-uuid", strategy = "org.hibernate.id.UUIDGenerator")
    private String id;

    private String paramId;     // user defined.
    private String name;
    private String value;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "satellite_id")
    private Satellite satellite;


    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "station_id")
    private GroundStation groundStation;
}
