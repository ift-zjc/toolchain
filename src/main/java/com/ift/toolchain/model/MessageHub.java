package com.ift.toolchain.model;


import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

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

    private int msgType;        // 1: link parameter update, 2: application behavior update, 3: satellite node params update
    private int subMsgType;     // 1-1: link delay update, 1-2: link through update, 1-3: link connectivity update, 2-1: app data transmission msg
    private long msgSendTime;   // Millisecond after simulation start (offset)
    private long msgEffTime;    // Millisecond after simulation start the message should be effective.

    private float paramData1;
    private float paramData2;
    private float paramData3;
    private float paramData4;
    private float paramData5;
    private float paramData6;
    private float paramData7;
    private float paramData8;
    private float paramData9;
    private float paramData10;

    // Link to satellite
    private String sourceId;
    private String destinationId;
}
