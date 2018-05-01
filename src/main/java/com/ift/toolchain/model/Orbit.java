package com.ift.toolchain.model;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.List;

@Entity
@Data
public class Orbit {

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name="system-uuid", strategy = "org.hibernate.id.UUIDGenerator")
    private String id;

    @Column(unique = true)
    private String name;
    private int orbitOrder;

    @OneToMany(mappedBy = "orbit", fetch = FetchType.EAGER)
    private List<Satellite> satellites;
}
