package org.michoko.lazyconnectionclient;

import com.felhr.usbserial.CDCSerialDevice;
import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import org.michocko.lazyio.codec.IMessageCodec;
import org.michocko.lazyio.messaging.AConnection;
import org.michocko.lazyio.messaging.IMessageHandler;

import java.io.IOException;
import java.util.LinkedList;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.PooledByteBufAllocator;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UsbSerialConnection<T> extends AConnection<T> {
    private final UsbSerialDevice serialPort;
    private final int baudRate;
    private final IMessageHandler<T> handler;
    private final IMessageCodec<T> codec;

    private final ByteBufAllocator byteBufAllocator = new PooledByteBufAllocator();

    @Override
    public void open0() throws IOException {
        if (serialPort.open()) {
            serialPort.setBaudRate(baudRate);
            serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
            serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
            serialPort.setParity(UsbSerialInterface.PARITY_NONE);
            /**
             * Current flow control Options:
             * UsbSerialInterface.FLOW_CONTROL_OFF
             * UsbSerialInterface.FLOW_CONTROL_RTS_CTS only for CP2102 and FT232
             * UsbSerialInterface.FLOW_CONTROL_DSR_DTR only for CP2102 and FT232
             */
            serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
            serialPort.read(this::receive);
        } else {
            // Serial port could not be opened, maybe an I/O error or if CDC driver was chosen, it does not really fit
            // Send an Intent to Main Activity
            if (serialPort instanceof CDCSerialDevice) {
                throw new IOException("CDC driver not working");
            } else {
                throw new IOException("USB device not working");
            }
        }
    }

    private void receive(byte[] data) {
        try {
            ByteBuf in = this.byteBufAllocator.ioBuffer(data.length);
            LinkedList<T> decodedMessages = new LinkedList<>();
            try {
                in.writeBytes(data);
                this.codec.decode(in, decodedMessages);
            } finally {
                in.release();
            }

            for (T message : decodedMessages) {
                this.handler.handle(message);
            }
        } catch (Exception e) {
            this.exceptionCatcher.catchException(new IOException("Unable to receive serial data.", e));
        }
    }

    @Override
    public void send0(T msg) throws IOException {
        ByteBuf out = this.byteBufAllocator.ioBuffer();
        try {
            this.codec.encode(msg, out);
            byte[] buffer = ByteBufUtil.getBytes(out);
            this.serialPort.write(buffer);
        } finally {
            out.release();
        }
    }

    @Override
    public void close0() {
        serialPort.close();
    }
}
