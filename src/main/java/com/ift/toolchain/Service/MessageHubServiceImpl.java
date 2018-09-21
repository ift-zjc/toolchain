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
    public MessageHub save(MessageHub messageHub) {
        return messageHubRepository.save(messageHub);
    }

    @Override
    public void removeAll() {
        messageHubRepository.deleteAll();
    }
}
