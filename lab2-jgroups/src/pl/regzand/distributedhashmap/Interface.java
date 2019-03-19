package pl.regzand.distributedhashmap;

import asg.cliche.Command;
import asg.cliche.ShellFactory;

public class Interface {

    private static final String CLUSTER = "distributed-map";

    private final DistributedMap map;

    private Interface(DistributedMap map) {
        this.map = map;
    }

    @Command
    public void put(String key, Integer value) {
        map.put(key, value);
    }

    @Command
    public Integer remove(String key) {
        return map.remove(key);
    }

    @Command
    public Integer get(String key) {
        return map.get(key);
    }

    @Command
    public String keys() {
        return String.join(", ", map.getKeySet());
    }

    @Command
    public Boolean containsKey(String key) {
        return map.containsKey(key);
    }

    @Command
    public void exit(){
        map.close();
        System.exit(0);
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("java.net.preferIPv4Stack","true");

        ShellFactory
                .createConsoleShell("", "Distributed Map CLI (tip: enter ?list)", new Interface(DistributedMap.createDistributedMap(CLUSTER)))
                .commandLoop();
    }
}
