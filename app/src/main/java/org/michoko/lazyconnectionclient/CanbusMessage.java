package org.michoko.lazyconnectionclient;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CanbusMessage implements ICanbusMessage {
    private final int id;
    private final boolean rtr;
    private final boolean extended;
    private final boolean error;
    private final byte[] data;
}
