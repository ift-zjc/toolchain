package com.ift.toolchain.model;


import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.util.Date;

/**
 * This is seperate data table for messages passing only.
 */
@Entity
@Data
public class MessageHub {

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name="system-uuid", strategy = "org.hibernate.id.UUIDGenerator")
    private String id;

    private int msgType;        // 0: init, 1: link parameter update, 2: application behavior update, 3: satellite node params update
    private int subMsgType;     // 1-1: link delay update, 1-2: link through update, 1-3: link connectivity update, 2-1: app data transmission msg
    private Date date;
    private boolean los;
    private double distance;

    private float paramData1;
    private float paramData2;
    private float paramData3;
    private float paramData4;
    private float paramData5;


    // Link to satellite
    private String sourceId;
    private String destinationId;
}
