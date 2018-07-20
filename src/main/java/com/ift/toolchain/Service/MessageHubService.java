package com.ift.toolchain.Service;

import com.ift.toolchain.dto.ObjectEvent;
import com.ift.toolchain.model.MessageHub;

public interface MessageHubService {

    public MessageHub create(String sourceId,
                             String destId,
                             long msgEffTime,
                             int msgType,
                             int subMsgType,
                             float paramData1,
                             float paramData2);

    public MessageHub create(ObjectEvent objectEvent);
}
