package com.ift.toolchain.model;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;



import javax.persistence.*;
import java.util.Date;
import java.util.List;

@Entity
@Data
public class MSAApplication {

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name="system-uuid", strategy = "org.hibernate.id.UUIDGenerator")
    private String id;
    private String appName;

    private String sourceObj;
    private String destObj;
    private String trafficModelCode;
    private String protocol;
    private Date startTime;
    private Date endTime;

    @Lob
    private String trafficModelConfig;

    @OneToMany(mappedBy = "applicationTrafficModel")
    private List<AppTrafficData> trafficData;

    @OneToMany(mappedBy = "msaApplication")
    private List<MSAApplicationEvent> applicationEvents;
}
