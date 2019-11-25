package com.example.ekg_android;

import android.util.Log;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class Msg {


    /* ********** Message Metadata ********** */

    // The maximum message byte size
    public static final int MSG_BUFFER_MAX = 256;

    // The message header byte
    public static final byte MSG_BYTE_HEAD = (byte)0xFF;


    /*
    // Update this table with new message sizes as necessary
    size_t g_msg_size_tab[MSG_TYPE_MAX] = {
        [MSG_TYPE_STATUS]          = 1,           // 1B status
        [MSG_TYPE_TRAIN_DATA]      = 160,         // 2 * (40 + 20 + 20)
        [MSG_TYPE_SAMPLE_DATA]     = 1 + 2 + 2,   // 1B label + 2B (amp/period)
        [MSG_TYPE_INSTRUCTION]     = 1,           // 1B inst
        [MSG_TYPE_CONFIGURATION]   = 1 + 2,       // 1B comp, 2B value
    };
     */

    // Table containing message body sizes
    private int g_msg_size_tab[] = new int[] {
            1,            // 1B status
            160,          // Training data
            5,            // Sample data
            1,            // Instruction data,
            3             // Configuration data
    };


    /* ********** Message Fields ********** */

    // The message type
    private MsgType msgType = MsgType.MSG_TYPE_MAX;

    // Status of system (1 byte)
    private byte status;

    // Training sets (N)
    private ArrayList<Sample> normal_training_data;
    private ArrayList<Sample> atrial_training_data;
    private ArrayList<Sample> ventrical_training_data;

    // Sample Message
    private Classification sample_label;
    private int sample_amplitude;
    private int sample_period;

    // Instruction byte for dispatching to device
    private byte device_instruction;

    // Configuration: Comparator type
    private Comparator cfg_comp;

    // Configuration: Value
    private int cfg_val;


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
            case INST_EKG_STOP: return 0x0;
            case INST_EKG_START: return 0x1;
            case INST_EKG_CONFIGURE: return 0x2;
            default:
                Log.e("MSG", "No value for given instruction type!?");
        }

        // Return error value
        return (byte)0xFF;
    }

    public static byte getComparatorByteValue (Comparator comparator) {
        switch (comparator) {
            case GREATER_THAN: return 0x0;
            case LESS_THAN: return 0x1;
        }
        return 0x0;
    }


    /* ********** Constructors ********** */


    public Msg () {
        this.msgType = MsgType.MSG_TYPE_MAX;

        // Status
        this.status = 0x0;

        // Training Message
        this.normal_training_data = new ArrayList<Sample>(20);
        this.atrial_training_data = new ArrayList<Sample>(10);
        this.ventrical_training_data = new ArrayList<Sample>(10);


        // Instruction
        this.device_instruction = getInstructionByteValue(MsgInstructionType.INST_EKG_STOP);

        // Configuration
        this.cfg_comp = Comparator.GREATER_THAN;
        this.cfg_val = 0;

        // Error description
        this.errorDescription = null;
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

    // Configures the instance to be a Training message
    /* OMITTED */

    // Configures the instance to be a Sample message
    /* OMITTED */

    // Configures the instance to be a Instruction message
    public Msg configureAsInstructionDataMessage (MsgInstructionType instructionType) {

        // Set the message type
        this.msgType = MsgType.MSG_TYPE_INSTRUCTION_DATA;

        // Set the device instruction
        this.device_instruction = this.getInstructionByteValue(instructionType);

        return this;
    }

    // Configures the instance to be a configuration method
    public Msg configureAsConfigurationMessage (Comparator comparator, int value) {

        // Set the message type
        this.msgType = MsgType.MSG_TYPE_CONFIGURATION;

        // Set the comparator and value
        this.cfg_comp = comparator;
        this.cfg_val = value;

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

            }
            break;


            // Decoding sample data
            case MSG_TYPE_SAMPLE_DATA: {

                // Unpack label (0x0 = N, 0x1 = A, 0x2 = V)
                switch ((byte)(buffer[offset])) {
                    case (byte)(0x0):
                        this.sample_label = Classification.NONE;
                        break;
                    case (byte)(0x1):
                        this.sample_label = Classification.NORMAL;
                        break;
                    case (byte)(0x2):
                        this.sample_label = Classification.ATRIAL;
                        break;
                    case (byte)(0x3):
                        this.sample_label = Classification.VENTRICAL;
                        break;
                    default:
                        this.sample_label = Classification.NONE;
                        break;

                }

                // Increment offset
                offset++;

                // Unpack amplitude
                byte[] sample_amplitude_buffer = new byte[]{0x0, 0x0, buffer[offset + 1], buffer[offset]};
                this.sample_amplitude = ByteBuffer.wrap(sample_amplitude_buffer).getInt();
                offset += 2;

                // Unpack period
                byte[] sample_period_buffer = new byte[]{0x0, 0x0, buffer[offset + 1], buffer[offset]};
                this.sample_period = ByteBuffer.wrap(sample_period_buffer).getInt();
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

            }
            break;

            // Serializing a configuration message
            case MSG_TYPE_CONFIGURATION: {

                // Set the comparator byte
                buffer[offset] = (byte)(this.getComparatorByteValue(this.cfg_comp));
                offset++;

                // Set the value byte
                buffer[offset] = (byte)((this.cfg_val >> 8) & 0xFF); // MSB first
                offset++;
                buffer[offset] = (byte)((this.cfg_val >> 0) & 0xFF); // LSB second
                offset++;
            }
            break;


            // Serializing a training message
            case MSG_TYPE_TRAIN_DATA: {

                // Write N periods
                for (int i = 0; i < 20; i += 2) {
                    int v = this.normal_training_data.get(i).getPeriod();
                    buffer[offset + i + 0] = (byte)((v >> 0) & 0xFF);       // LSB first
                    buffer[offset + i + 1] = (byte)((v >> 8) & 0xFF);       // MSB second
                }
                offset += (2 * 20);

                // Write N amplitudes
                for (int i = 0; i < 20; i += 2) {
                    int v = this.normal_training_data.get(i).getAmplitude();
                    buffer[offset + i + 0] = (byte)((v >> 0) & 0xFF);       // LSB first
                    buffer[offset + i + 1] = (byte)((v >> 8) & 0xFF);       // MSB second
                }
                offset += (2 * 20);


                // Write A periods
                for (int i = 0; i < 10; i += 2) {
                    int v = this.atrial_training_data.get(i).getPeriod();
                    buffer[offset + i + 0] = (byte)((v >> 0) & 0xFF);       // LSB first
                    buffer[offset + i + 1] = (byte)((v >> 8) & 0xFF);       // MSB second
                }
                offset += (2 * 10);

                // Write A amplitudes
                for (int i = 0; i < 10; i += 2) {
                    int v = this.atrial_training_data.get(i).getAmplitude();
                    buffer[offset + i + 0] = (byte)((v >> 0) & 0xFF);       // LSB first
                    buffer[offset + i + 1] = (byte)((v >> 8) & 0xFF);       // MSB second
                }
                offset += (2 * 10);


                // Write V periods
                for (int i = 0; i < 10; i += 2) {
                    int v = this.ventrical_training_data.get(i).getPeriod();
                    buffer[offset + i + 0] = (byte)((v >> 0) & 0xFF);       // LSB first
                    buffer[offset + i + 1] = (byte)((v >> 8) & 0xFF);       // MSB second
                }
                offset += (2 * 10);

                // Write V amplitudes
                for (int i = 0; i < 10; i += 2) {
                    int v = this.ventrical_training_data.get(i).getAmplitude();
                    buffer[offset + i + 0] = (byte)((v >> 0) & 0xFF);       // LSB first
                    buffer[offset + i + 1] = (byte)((v >> 8) & 0xFF);       // MSB second
                }
                offset += (2 * 10);

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

    public byte get_device_instruction () {
        return this.device_instruction;
    }


    public Classification get_sample_label () {
        return sample_label;
    }

    public int get_sample_amplitude () {
        return sample_amplitude;
    }

    public int get_sample_period () {
        return sample_period;
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
