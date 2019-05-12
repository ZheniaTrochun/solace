package com.yevhenii.solace.protocol;

import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.U16;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;



/**
 * Represents an ofp_flow_mod message
 * @author David Erickson (daviderickson@cs.stanford.edu)
 *
 */
public class OFFlowMod extends OFMessageOld {
    public static int MINIMUM_LENGTH = 72;

    public static final short OFPFC_ADD = 0;                /* New flow. */
    public static final short OFPFC_MODIFY = 1;             /* Modify all matching flows. */
    public static final short OFPFC_MODIFY_STRICT = 2;      /* Modify entry strictly matching wildcards */
    public static final short OFPFC_DELETE=3;               /* Delete all matching flows. */
    public static final short OFPFC_DELETE_STRICT =4;       /* Strictly match wildcards and priority. */

    // Flags
    public static final short OFPFF_SEND_FLOW_REM = 1;
    public static final short OFPFF_CHECK_OVERLAP = 2;
    public static final short OFPFF_EMERG         = 4;

    protected OFMatch match;
    protected long cookie;
    protected short command;
    protected short idleTimeout;
    protected short hardTimeout;
    protected short priority;
    protected int bufferId;
    protected short outPort;
    protected short flags;
    protected List<OFActionOld> actions;

    public OFFlowMod() {
        super();
        this.type = OFType.FLOW_MOD;
        this.length = U16.t(MINIMUM_LENGTH);
    }

    /**
     * Get buffer_id
     * @return
     */
    public int getBufferId() {
        return this.bufferId;
    }

    /**
     * Set buffer_id
     * @param bufferId
     */
    public OFFlowMod setBufferId(int bufferId) {
        this.bufferId = bufferId;
        return this;
    }

    /**
     * Get cookie
     * @return
     */
    public long getCookie() {
        return this.cookie;
    }

    /**
     * Set cookie
     * @param cookie
     */
    public OFFlowMod setCookie(long cookie) {
        this.cookie = cookie;
        return this;
    }

    /**
     * Get command
     * @return
     */
    public short getCommand() {
        return this.command;
    }

    /**
     * Set command
     * @param command
     */
    public OFFlowMod setCommand(short command) {
        this.command = command;
        return this;
    }

    /**
     * Get flags, see OFPFF_* constants
     * @return
     */
    public short getFlags() {
        return this.flags;
    }

    /**
     * Set flags, see OFPFF_* constants
     * @param flags
     */
    public OFFlowMod setFlags(short flags) {
        this.flags = flags;
        return this;
    }

    /**
     * Get hard_timeout
     * @return
     */
    public short getHardTimeout() {
        return this.hardTimeout;
    }

    /**
     * Set hard_timeout
     * @param hardTimeout
     */
    public OFFlowMod setHardTimeout(short hardTimeout) {
        this.hardTimeout = hardTimeout;
        return this;
    }

    /**
     * Get idle_timeout
     * @return
     */
    public short getIdleTimeout() {
        return this.idleTimeout;
    }

    /**
     * Set idle_timeout
     * @param idleTimeout
     */
    public OFFlowMod setIdleTimeout(short idleTimeout) {
        this.idleTimeout = idleTimeout;
        return this;
    }

    /**
     * Gets a copy of the OFMatch object for this FlowMod, changes to this
     * object do not modify the FlowMod
     * @return
     */
    public OFMatch getMatch() {
        return this.match;
    }

    /**
     * Set match
     * @param match
     */
    public OFFlowMod setMatch(OFMatch match) {
        this.match = match;
        return this;
    }

    /**
     * Get out_port
     * @return
     */
    public short getOutPort() {
        return this.outPort;
    }

    /**
     * Set out_port
     * @param outPort
     */
    public OFFlowMod setOutPort(short outPort) {
        this.outPort = outPort;
        return this;
    }

    /**
     * Set out_port
     * @param port
     */
    public OFFlowMod setOutPort(OFPort port) {
        this.outPort = port.getShortPortNumber();
        return this;
    }

    /**
     * Get priority
     * @return
     */
    public short getPriority() {
        return this.priority;
    }

    /**
     * Set priority
     * @param priority
     */
    public OFFlowMod setPriority(short priority) {
        this.priority = priority;
        return this;
    }

    /**
     * Returns read-only copies of the actions contained in this Flow Mod
     * @return a list of ordered OFAction objects
     */
    public List<OFActionOld> getActions() {
        return this.actions;
    }

    /**
     * Sets the list of actions this Flow Mod contains
     * @param actions a list of ordered OFAction objects
     */
    public OFFlowMod setActions(List<OFActionOld> actions) {
        this.actions = actions;
        return this;
    }

    @Override
    public void writeTo(ByteBuffer data) {
        super.writeTo(data);
        this.match.writeTo(data);
        data.putLong(cookie);
        data.putShort(command);
        data.putShort(idleTimeout);
        data.putShort(hardTimeout);
        data.putShort(priority);
        data.putInt(bufferId);
        data.putShort(outPort);
        data.putShort(flags);
        if (actions != null) {
            for (OFActionOld action : actions) {
                action.writeTo(data); // todo
            }
        }
    }

    @Override
    public int hashCode() {
        final int prime = 227;
        int result = super.hashCode();
        result = prime * result + ((actions == null) ? 0 : actions.hashCode());
        result = prime * result + bufferId;
        result = prime * result + command;
        result = prime * result + (int) (cookie ^ (cookie >>> 32));
        result = prime * result + flags;
        result = prime * result + hardTimeout;
        result = prime * result + idleTimeout;
        result = prime * result + ((match == null) ? 0 : match.hashCode());
        result = prime * result + outPort;
        result = prime * result + priority;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof OFFlowMod)) {
            return false;
        }
        OFFlowMod other = (OFFlowMod) obj;
        if (actions == null) {
            if (other.actions != null) {
                return false;
            }
        } else if (!actions.equals(other.actions)) {
            return false;
        }
        if (bufferId != other.bufferId) {
            return false;
        }
        if (command != other.command) {
            return false;
        }
        if (cookie != other.cookie) {
            return false;
        }
        if (flags != other.flags) {
            return false;
        }
        if (hardTimeout != other.hardTimeout) {
            return false;
        }
        if (idleTimeout != other.idleTimeout) {
            return false;
        }
        if (match == null) {
            if (other.match != null) {
                return false;
            }
        } else if (!match.equals(other.match)) {
            return false;
        }
        if (outPort != other.outPort) {
            return false;
        }
        if (priority != other.priority) {
            return false;
        }
        return true;
    }
}
