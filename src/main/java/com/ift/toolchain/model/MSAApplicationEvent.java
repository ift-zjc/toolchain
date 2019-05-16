package com.ift.toolchain.model;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

@Data
@Entity
public class MSAApplicationEvent {

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name="system-uuid", strategy = "org.hibernate.id.UUIDGenerator")
    private String id;

    private String tick;
    private String routing;

    @ManyToOne
    @JoinColumn(name = "app_id")
    private MSAApplication msaApplication;
}
