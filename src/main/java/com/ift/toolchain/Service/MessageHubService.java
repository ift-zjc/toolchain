package com.ift.toolchain.Service;

import com.ift.toolchain.dto.ObjectEvent;
import com.ift.toolchain.model.MessageHub;

public interface MessageHubService {

    public MessageHub save(MessageHub messageHub);
    public void removeAll();
}
