package com.example.ekg_android;

import android.util.Log;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public class Msg {


    /* ********** Message Metadata ********** */

    // The maximum message byte size
    public static final int MSG_BUFFER_MAX = 128;

    // The message header byte
    public static final byte MSG_BYTE_HEAD = (byte)0xFF;


    // Table containing message body sizes
    private int g_msg_size_tab[] = new int[] {
            1,            // 1B status
            160,          // 2 * (40 + 20 + 20)
            4,            // 2B amplitude + 2B period
            1             // 1B instruction
    };


    /* ********** Message Fields ********** */

    // The message type
    private MsgType msgType = MsgType.MSG_TYPE_MAX;

    // Status of system (1 byte)
    private byte status;

    // Training sets (N)
    private byte[] train_amplitude_n;
    private byte[] train_period_n;

    // Training sets (A)
    private byte[] train_amplitude_a;
    private byte[] train_period_a;

    // Training sets (V)
    private byte[] train_amplitude_v;
    private byte[] train_period_v;

    // Sample Amplitude
    private byte[] sample_amplitude;

    // Sample Period
    private byte[] sample_period;

    // Instruction byte for dispatching to device
    private byte device_instruction;


    /* ********** Internal Properties ********** */

    // String containing internal error message
    public String errorDescription = null;

    // Encloses a buffer and its length (instead of capacity)
    class MsgData {
        public int len;
        public byte[] data;

        public MsgData (int len, byte[] data) {
            this.len = len;
            this.data = data;
        }
    }


    /* ********** Helper Methods ********** */


    public static byte getInstructionByteValue (MsgInstructionType instructionType) {
        switch (instructionType) {
            case INST_EKG_SAMPLE: return 0x0;
            case INST_EKG_MONITOR: return 0x1;
            case INST_EKG_IDLE: return 0x2;
            default:
                Log.e("MSG", "No value for given instruction type!?");
        }

        // Return error value
        return (byte)0xFF;
    }


    /* ********** Constructors ********** */

    /*
         private byte[] train_amplitude_n[];
    private byte[] train_period_n[];

    // Training sets (A)
    private byte[] train_amplitude_a[];
    private byte[] train_period_a[];

    // Training sets (V)
    private byte[] train_amplitude_v[];
    private byte[] train_period_v[];

    // Sample Amplitude
    private byte[] sample_amplitude;

    // Sample Period
    private byte[] sample_period;
     */

    public Msg () {
        this.msgType = MsgType.MSG_TYPE_MAX;

        // Training Message
        this.train_amplitude_n = new byte[20 * 2];
        this.train_period_n = new byte[20 * 2];
        this.train_amplitude_a = new byte[10 * 2];
        this.train_period_a = new byte[10 * 2];
        this.train_amplitude_v = new byte[10 * 2];
        this.train_period_v = new byte[10 * 2];

        // Sample message
        this.sample_amplitude = new byte[2];
        this.sample_period = new byte[2];

        this.errorDescription = null;
        this.status = 0x0;
    }


    /* ********** Message Configuration Methods ********** */


    // Configures the instance to be a status message
    public Msg configureAsStatusMessage (byte status) {

        // Set the message type
        this.msgType = MsgType.MSG_TYPE_STATUS;

        // Set the status
        this.status = status;


        // Return instance
        return this;
    }


    // Configures the instance to be a WiFi message
    public Msg configureAsWiFiDataMessage (String ssid, String pswd) {

        // Set the message type
        this.msgType = MsgType.MSG_TYPE_WIFI_DATA;

        // Convert Java Strings to byte arrays
        byte[] ssid_bytes = ssid.getBytes();
        byte[] pswd_bytes = pswd.getBytes();

        // Initialize the internal byte buffers
        this.wifi_ssid = new byte[32];
        this.wifi_pswd = new byte[64];

        // Copy over data safely
        System.arraycopy(ssid_bytes, 0, this.wifi_ssid, 0,
                Math.min(this.wifi_ssid.length, ssid_bytes.length));
        System.arraycopy(pswd_bytes, 0, this.wifi_pswd, 0,
                Math.min(this.wifi_pswd.length, pswd_bytes.length));

        // Return instance
        return this;
    }

    // Configures the instance to be a Stream message
    public Msg configureAsStreamDataMessage (String raw_url) throws MessageSerializationException {
        URL url;
        InetAddress addr;
        int port = 80;

        // Set the message type
        this.msgType = MsgType.MSG_TYPE_STREAM_DATA;

        // Configure as URL
        try {
            url = new URL(raw_url);
        } catch (MalformedURLException exception) {
            throw new MessageSerializationException(exception.toString());
        }

        // Extract base and path
        String base = url.getHost();
        String path = url.getPath().substring(1); // Strip the leading slash (/)

        // Attempt to resolve the domain to an IP address
        try {
            addr = InetAddress.getByName(base);
        } catch (UnknownHostException exception) {
            throw new MessageSerializationException(exception.toString());
        }

        // Java streams use network byte order already. No need to convert
        this.stream_addr = ByteBuffer.allocate(4).put(addr.getAddress()).array();

        // Create port bytes
        this.stream_port = ByteBuffer.allocate(2).putShort((short)port).array();

        // Create path bytes
        this.stream_path = ByteBuffer.allocate(64).put(path.getBytes()).array();

        // Return instance
        return this;
    }

    // Configures the instance to be a Telemetry message
    public Msg configureAsTelemetryDataMessage (String raw_addr, String raw_port) throws MessageSerializationException {
        InetAddress addr;

        // Set the message type
        this.msgType = MsgType.MSG_TYPE_TELEMETRY_DATA;

        // Convert string form of the address to an InetAddress type
        try {
            addr = InetAddress.getByName(raw_addr);
        } catch (UnknownHostException exception) {
            throw new MessageSerializationException(exception.toString());
        }


        // Create address buffer (network byte order)
        this.telemetry_addr = ByteBuffer.allocate(4).put(addr.getAddress()).array();

        // Create port buffer
        this.telemetry_port = ByteBuffer.allocate(2).putShort((short) Integer.parseInt(raw_port)).array();

        // Return instance
        return this;
    }


    // Configures the instance to be a Instruction message
    public Msg configureAsInstructionDataMessage (MsgInstructionType instructionType) {

        // Set the message type
        this.msgType = MsgType.MSG_TYPE_INSTRUCTION_DATA;

        // Set the device instruction
        this.device_instruction = this.getInstructionByteValue(instructionType);

        return this;
    }


    /* ********** Message Deserialization Methods ********** */


    // Configures the instance using a received message buffer
    public Msg fromByteBuffer (byte[] buffer) throws MessageSerializationException {
        int offset = 3;
        MsgType type = MsgType.MSG_TYPE_MAX;

        // Check the message size
        if (buffer.length < 3) {
            throw new MessageSerializationException("Buffer doesn't meet minimal message size requirements");
        }

        // Check the message head markers
        if (buffer[0] != MSG_BYTE_HEAD || buffer[1] != MSG_BYTE_HEAD) {
            throw new MessageSerializationException("Buffer doesn't begin with message head markers");
        }

        // Check the message type
        boolean isValidMessageType = false;
        for (MsgType t : MsgType.values()) {
            if (t.getByteValue() == buffer[2]) {
                isValidMessageType = true;
                break;
            }
        }
        if (!isValidMessageType) {
            throw new MessageSerializationException("Message type is not valid");
        } else {
            type = MsgType.fromByte(buffer[2]);
        }

        // Check if the remaining buffer is sufficient to decode message of type
        if (buffer.length - offset < g_msg_size_tab[(int)type.getByteValue()]) {
            throw new MessageSerializationException("Buffer not large enough to decode message of given type");
        }

        // Switch on the message type
        switch (type) {

            // Decoding status message
            case MSG_TYPE_STATUS: {

                // Deserialize status
                this.status = buffer[offset];
                offset += 1;

                // Deserialize IP address (big endian packed in little endian unpacked inverse)
                this.ip_addr = new byte[]{buffer[offset],
                        buffer[offset + 1], buffer[offset + 2], buffer[offset + 3]};
                offset += 4;

            }
            break;

            // Decoding WiFi data
            case MSG_TYPE_WIFI_DATA: {
                System.arraycopy(buffer, offset, this.wifi_ssid, 0, 32);
                offset += 32;
                System.arraycopy(buffer, offset, this.wifi_pswd, 0, 64);
                offset += 64;
            }
            break;

            // Decoding stream data
            case MSG_TYPE_STREAM_DATA: {

                // Unpack address (little endian -> big endian for JVM)
                this.stream_addr = new byte[] {
                        buffer[offset], buffer[offset + 1], buffer[offset + 2],
                        buffer[offset + 3]
                };
                offset += 4;

                // Unpack port (little endian -> big endian for JVM)
                this.stream_port = new byte[] {
                        buffer[offset], buffer[offset + 1]
                };
                offset += 2;

                // Unpack path buffer
                System.arraycopy(buffer, offset, this.stream_path, 0, 64);
                offset += 64;
            }
            break;


            // Decoding telemetry data
            case MSG_TYPE_TELEMETRY_DATA: {

                // Unpack address (little endian -> big endian for JVM)
                this.telemetry_addr = new byte[] {
                        buffer[offset], buffer[offset + 1], buffer[offset + 2],
                        buffer[offset + 3]
                };
                offset += 4;

                // Unpack port (little endian -> big endian for JVM)
                this.telemetry_port = new byte[] {
                        buffer[offset], buffer[offset + 1]
                };
                offset += 2;
            }
            break;


            // Decoding instruction data
            case MSG_TYPE_INSTRUCTION_DATA: {

                // Unpack instruction
                this.device_instruction = buffer[offset++];
            }
            break;

            // Unreachable case
            default:
                throw new MessageSerializationException("Unexpected message type");
        }

        // Update internal message type
        this.msgType = type;

        // Return instance
        return this;
    }

    // TODO: Add more constructors here for different message types


    /* ********** Message Serialization Methods ********** */

    // Serializes the message for transmission
    public MsgData getSerialized () throws MessageSerializationException {
        byte[] buffer = new byte[MSG_BUFFER_MAX];
        int offset = 0;

        // Set the message header
        buffer[0] = buffer[1] = MSG_BYTE_HEAD;

        // Set the message type
        buffer[2] = this.msgType.getByteValue();

        // Update offset
        offset = 3;

        switch (this.msgType) {

            // Serializing a status message (Doesn't apply to Mobile Device)
            case MSG_TYPE_STATUS: {

                // Set the status byte
                buffer[offset] = this.status;
                offset++;

                // Set the address field (will be sent as big-endian, but thats okay)
                System.arraycopy(this.ip_addr, 0, buffer, offset, this.ip_addr.length);
                offset += 4;
            }
            break;

            // Serializing a WiFi data message
            case MSG_TYPE_WIFI_DATA: {
                System.arraycopy(this.wifi_ssid, 0, buffer, offset, this.wifi_ssid.length);
                offset += this.wifi_ssid.length;
                System.arraycopy(this.wifi_pswd, 0, buffer, offset, this.wifi_pswd.length);
                offset += this.wifi_pswd.length;
            }
            break;

            // Serializing a Stream data message
            case MSG_TYPE_STREAM_DATA: {

                // Address and port will be sent as big endian but this is okay
                System.arraycopy(this.stream_addr, 0, buffer, offset, this.stream_addr.length);
                offset += this.stream_addr.length;
                System.arraycopy(this.stream_port, 0, buffer, offset, this.stream_port.length);
                offset += this.stream_port.length;
                System.arraycopy(this.stream_path, 0, buffer, offset, this.stream_path.length);
                offset += this.stream_path.length;
            }
            break;

            // Serializing a Telemetry data message
            case MSG_TYPE_TELEMETRY_DATA: {

                // Address and port will be sent as big endian but this is okay
                System.arraycopy(this.telemetry_addr, 0, buffer, offset, this.telemetry_addr.length);
                offset += this.telemetry_addr.length;
                System.arraycopy(this.telemetry_port, 0, buffer, offset, this.telemetry_port.length);
                offset += this.telemetry_port.length;
            }
            break;

            // Serializing a Instruction data message
            case MSG_TYPE_INSTRUCTION_DATA: {
                buffer[offset] = this.device_instruction;
                offset += 1;
            }
            break;

            default:
                throw new MessageSerializationException("Cannot serialize unknown type!");
        }

        // Trim the outgoing data buffer
        byte[] outgoing_buffer = new byte[offset];
        System.arraycopy(buffer, 0, outgoing_buffer, 0, offset);

        return new MsgData(offset, outgoing_buffer);
    }

    // Returns the internal status byte
    public byte get_status () {
        return this.status;
    }

    public byte[] get_ip_addr () {
        return this.ip_addr;
    }

    public byte[] get_wifi_ssid () {
        return this.wifi_ssid;
    }

    public byte[] get_wifi_pswd () {
        return this.wifi_pswd;
    }

    public byte[] get_stream_addr () {
        return this.stream_addr;
    }

    public byte[] get_stream_port () {
        return this.stream_port;
    }

    public byte[] get_stream_path () {
        return this.stream_path;
    }

    public byte[] get_telemetry_addr () {
        return this.telemetry_addr;
    }

    public byte[] get_telemetry_port () {
        return this.telemetry_port;
    }

    public byte get_device_instruction () {
        return this.device_instruction;
    }

    // Returns message type. If MSG_TYPE_MAX then it was unable to successfully parse
    public MsgType getMessageType () {
        return this.msgType;
    }

    // Returns internal error description
    public String getErrorDescription () {
        return this.errorDescription;
    }

}
