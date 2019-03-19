package pl.regzand.distributedhashmap;

import org.jgroups.*;
import org.jgroups.util.Util;

import java.io.*;
import java.util.HashMap;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DistributedMap extends ReceiverAdapter implements SimpleStringMap, Closeable {

    private static final Logger logger = Logger.getLogger(DistributedMap.class.getSimpleName());

    private final JChannel channel;
    private HashMap<String, Integer> data = new HashMap<>();

    private DistributedMap(JChannel channel) {
        this.channel = channel;
    }

    public Set<String> getKeySet() {
        return data.keySet();
    }

    @Override
    public boolean containsKey(String key) {
        return data.containsKey(key);
    }

    @Override
    public Integer get(String key) {
        return data.get(key);
    }

    /**
     * Add given value to map and sends sync message.
     */
    @Override
    public void put(String key, Integer value) {
        try {
            channel.send(new Message(null, null, new MessagePut(key, value)));
            data.put(key, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Removes value related by given key and sends sync message.
     */
    @Override
    public Integer remove(String key) {
        try {
            channel.send(new Message(null, null, new MessageRemove(key)));
            return data.remove(key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Receives sync messages which should be an instance of MessagePut or MessageRemove.
     */
    @Override
    public void receive(Message msg) {
        Object obj = msg.getObject();

        if(obj instanceof MessagePut)
            handleMessagePut((MessagePut) obj);
        else if(obj instanceof MessageRemove)
            handleMessageRemove((MessageRemove) obj);
    }

    private void handleMessagePut(MessagePut msg) {
        data.put(msg.key, msg.value);
        logger.log(Level.FINE, String.format("Received put message: %s -> %s", msg.key, msg.value));
    }

    private void handleMessageRemove(MessageRemove msg) {
        data.remove(msg.key);
        logger.log(Level.FINE, String.format("Received remove message: %s", msg.key));
    }

    /**
     * Loads map content form given state. Used for syncing state between instances.
     */
    @Override
    public void getState(OutputStream output) throws Exception {
        Util.objectToStream(data, new DataOutputStream(output));
    }

    /**
     * Writes map content to given stream. Used for syncing state between instances.
     */
    @Override
    public void setState(InputStream input) throws Exception {
        data = (HashMap<String, Integer>) Util.objectFromStream(new DataInputStream(input));
    }

    /**
     * Handles merging desynchronised partitions
     */
    @Override
    public void viewAccepted(View view) {
        // only interested in merge views
        if(!(view instanceof MergeView)) return;

        // if this instance belongs to first group we can ignore this merge
        if(((MergeView) view).getSubgroups().get(0).getMembers().contains(channel.getAddress())) return;

        // else we need to fetch data
        try {
            channel.getState(null, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Disconnects form the cluster and performs cleanup.
     */
    @Override
    public void close() {
        channel.close();
    }

    /**
     * Creates distributed map and establishes connection to other map instances in cluster.
     * @param cluster - name of the cluster to connect to
     */
    public static DistributedMap createDistributedMap(String cluster) throws Exception {

        // setup channel
        JChannel channel = new JChannel(true);
        channel.setDiscardOwnMessages(true);

        // create map
        DistributedMap map = new DistributedMap(channel);
        channel.setReceiver(map);

        // connect to cluster (and fetch state)
        channel.connect(cluster, null, 0);

        return map;
    }

    /**
     * Class used as payload for put messages
     */
    private static class MessagePut implements Serializable {
        final String key;
        final Integer value;

        MessagePut(String key, Integer value) {
            this.key = key;
            this.value = value;
        }
    }

    /**
     * Class used as payload for remove messages
     */
    private static class MessageRemove implements Serializable {
        final String key;

        MessageRemove(String key) {
            this.key = key;
        }
    }
}
