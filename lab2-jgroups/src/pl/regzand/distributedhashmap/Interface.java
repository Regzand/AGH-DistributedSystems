package pl.regzand.distributedhashmap;

import asg.cliche.Command;
import asg.cliche.Param;
import asg.cliche.ShellFactory;

public class Interface {

    private static final String CLUSTER = "distributed-map";

    private final DistributedMap map;

    private Interface(DistributedMap map) {
        this.map = map;
    }

    @Command
    public void put(@Param(name="key") String key, @Param(name="value") Integer value) {
        map.put(key, value);
    }

    @Command
    public Integer remove(@Param(name="key") String key) {
        return map.remove(key);
    }

    @Command
    public Integer get(@Param(name="key") String key) {
        return map.get(key);
    }

    @Command
    public String keys() {
        return String.join(", ", map.getKeySet());
    }

    @Command
    public Boolean contains(@Param(name="key") String key) {
        return map.containsKey(key);
    }

    @Command
    public void exit(){
        map.close();
        System.exit(0);
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("java.net.preferIPv4Stack","true");
        // new UDP().setValue("mcast_group_addr",InetAddress.getByName("230.100.200.x"))

        ShellFactory
                .createConsoleShell("", "Distributed Map CLI (tip: enter ?list)", new Interface(DistributedMap.createDistributedMap(CLUSTER)))
                .commandLoop();
    }
}
