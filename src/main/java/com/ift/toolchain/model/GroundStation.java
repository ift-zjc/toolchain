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
public class GroundStation {

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name="system-uuid", strategy = "org.hibernate.id.UUIDGenerator")
    private String id;

    private String name;
    private String stationId;   // User defined
    private double x;
    private double y;
    private double z;

    private int gsId;

    @OneToMany(mappedBy = "groundStation")
    private List<Parameter> stationParams;

}
