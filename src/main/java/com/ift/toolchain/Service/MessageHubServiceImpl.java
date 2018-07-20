package com.ift.toolchain.Service;

import com.ift.toolchain.dto.ObjectEvent;
import com.ift.toolchain.model.MessageHub;
import com.ift.toolchain.repository.MessageHubRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MessageHubServiceImpl implements MessageHubService {

    @Autowired
    MessageHubRepository messageHubRepository;

    @Override
    public MessageHub create(String sourceId, String destId, long msgEffTime, int msgType, int subMsgType, float paramData1, float paramData2) {
        MessageHub messageHub = new MessageHub();
        messageHub.setSourceId(sourceId);
        messageHub.setDestinationId(destId);
        messageHub.setMsgEffTime(msgEffTime);
        messageHub.setMsgType(msgType);
        messageHub.setSubMsgType(subMsgType);
        messageHub.setParamData1(paramData1);
        messageHub.setParamData2(paramData2);

        // Check for msgtype = 1 and subMsgType = 3, ignore if connectivity status keep unchanged.


        return messageHubRepository.save(messageHub);
    }

    @Override
    public MessageHub create(ObjectEvent objectEvent) {
        return create(objectEvent.getSourceid(), objectEvent.getDestid(), objectEvent.getMsgEffTime(), objectEvent.getMsgType(), objectEvent.getSubMsgType(), 0f, 0f);
    }
}
