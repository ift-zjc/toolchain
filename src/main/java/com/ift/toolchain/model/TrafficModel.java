package com.ift.toolchain.model;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.List;

@Entity
@Data
public class TrafficModel {

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name="system-uuid", strategy = "org.hibernate.id.UUIDGenerator")
    private String id;

    private String name;

    @Column(unique = true)
    private String code;

    private String description;


    @OneToMany(mappedBy = "trafficModel")
    List<TrafficModelConfig> trafficModelConfigs;

}
